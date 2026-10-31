package service

import entity.ResearchArea
import entity.ResearchCard
import entity.ResearchTopic
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Grenzfälle der Spielregeln und Benachrichtigung der View. */
class RuleEdgeCaseTest {

    private lateinit var root: RootService
    private lateinit var view: TestRefreshable
    private val actions get() = root.playerActionService
    private val game get() = root.currentGame!!

    @BeforeTest
    fun setUp() {
        root = RootService()
        view = TestRefreshable()
        root.addRefreshable(view)
        root.gameService.startNewGame(listOf("A", "B"))
    }

    @Test
    fun `start notifies view and writes log`() {
        assertEquals(1, view.startNewGameCalls)
        assertEquals(1, view.startTurnCalls)
        assertTrue(view.logs.first().contains("Neues Spiel"))
    }

    @Test
    fun `every action notifies the view and is logged`() {
        val logsBefore = view.logs.size
        actions.gainNewIdea()
        assertEquals(1, view.actionCalls)
        assertTrue(view.logs.size > logsBefore)
        assertTrue(view.logs.any { it.contains("A gewinnt eine neue Idee") })
    }

    @Test
    fun `advance research with empty draw stack adds no literature`() {
        game.drawStack.clear()
        val literatureBefore = game.centralLiterature.toList()
        actions.advanceResearch(game.currentFaculty.handCards.first())
        assertEquals(literatureBefore, game.centralLiterature)
    }

    @Test
    fun `advance research rejects card not on hand`() {
        val foreign = ResearchCard(ResearchTopic.ACE, ResearchArea.AI)
        assertFailsWith<IllegalArgumentException> { actions.advanceResearch(foreign) }
        assertEquals(2, game.currentFaculty.actionPoints)
    }

    @Test
    fun `new idea fails on empty draw stack`() {
        game.drawStack.clear()
        assertFailsWith<IllegalStateException> { actions.gainNewIdea() }
    }

    @Test
    fun `two point actions need two action points`() {
        actions.gainNewIdea() // 1 AP übrig
        assertFailsWith<IllegalStateException> { actions.useLiterature(game.centralLiterature.first()) }
    }

    @Test
    fun `strategy needs two cards in draw stack`() {
        while (game.drawStack.size > 1) game.drawStack.removeLast()
        assertFailsWith<IllegalStateException> { actions.adjustStrategy(game.currentFaculty.handCards.first()) }
    }

    @Test
    fun `strategy choice blocks other actions and end turn`() {
        actions.adjustStrategy(game.currentFaculty.handCards.first())
        assertEquals(2, assertNotNull(view.strategyCards).size)
        assertFailsWith<IllegalStateException> { root.gameService.endTurn() }
        assertFailsWith<IllegalArgumentException> {
            actions.keepStrategyCard(ResearchCard(ResearchTopic.ACE, ResearchArea.AI))
        }
    }

    @Test
    fun `keeping without pending strategy fails`() {
        assertFailsWith<IllegalStateException> { actions.keepStrategyCard(game.drawStack.last()) }
    }

    @Test
    fun `strategy as last action ends the turn after choosing`() {
        actions.gainNewIdea()
        actions.adjustStrategy(game.currentFaculty.handCards.first())
        assertEquals("A", game.currentFaculty.name) // noch nicht gewechselt
        actions.keepStrategyCard(actions.pendingStrategyCards.last())
        assertEquals("B", game.currentFaculty.name)
    }

    @Test
    fun `journal article for five or more cards`() {
        repeat(6) { game.currentFaculty.addResearch(ResearchCard(ResearchTopic.TWO, ResearchArea.THEORETICAL)) }
        actions.publish(ResearchArea.THEORETICAL)
        assertEquals(9 + 1, game.faculties[0].researchPoints)
    }

    @Test
    fun `slow down on empty stack fails`() {
        assertFailsWith<IllegalStateException> { actions.slowDownCompetition(game.faculties[1], ResearchArea.AI) }
    }

    @Test
    fun `actions are rejected after game end`() {
        game.endgameTurnsLeft = 0
        assertFailsWith<IllegalStateException> { actions.gainNewIdea() }
    }

    @Test
    fun `four players get three more turns after draw stack is empty`() {
        root.gameService.startNewGame(listOf("A", "B", "C", "D"))
        game.drawStack.clear()
        root.gameService.endTurn()
        assertEquals(3, game.endgameTurnsLeft)
        repeat(2) { root.gameService.endTurn() }
        assertTrue(!view.gameEnded)
        root.gameService.endTurn()
        assertTrue(view.gameEnded)
    }

    @Test
    fun `majority tie gives everyone with most cards five points`() {
        val (a, b) = game.faculties
        a.publications += List(2) { ResearchCard(ResearchTopic.ACE, ResearchArea.AI) }
        b.publications += List(2) { ResearchCard(ResearchTopic.TWO, ResearchArea.AI) }
        val scores = root.gameService.computeScores(game)
        assertEquals(listOf(5, 5), scores.map { it.visibilityBonus })
    }

    @Test
    fun `equal totals produce several winners`() {
        game.faculties.forEach { it.handCards.clear() }
        val scores = root.gameService.computeScores(game)
        assertEquals(2, root.gameService.winners(scores).size)
    }
}
