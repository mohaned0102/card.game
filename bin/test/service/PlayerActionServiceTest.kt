package service

import entity.ResearchArea
import entity.ResearchCard
import entity.ResearchTopic
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PlayerActionServiceTest {

    private lateinit var root: RootService
    private val actions get() = root.playerActionService
    private val game get() = root.currentGame!!

    @BeforeTest
    fun setUp() {
        root = RootService()
        root.gameService.startNewGame(listOf("A", "B"))
    }

    @Test
    fun `advance research moves card and refreshes literature`() {
        val faculty = game.currentFaculty
        val card = faculty.handCards.first()
        val oldest = game.centralLiterature.first()
        val nextDraw = game.drawStack.last()

        actions.advanceResearch(card)

        assertSame(card, faculty.researchActivity.getValue(card.suit).peek())
        assertEquals(6, game.centralLiterature.size)
        assertTrue(oldest in game.discardStack)
        assertSame(nextDraw, game.centralLiterature.last())
        assertEquals(1, faculty.actionPoints)
    }

    @Test
    fun `turn ends automatically when action points are used up`() {
        actions.advanceResearch(game.currentFaculty.handCards.first())
        actions.gainNewIdea()
        assertEquals("B", game.currentFaculty.name)
        assertEquals(2, game.currentFaculty.actionPoints)
    }

    @Test
    fun `hand limit is six`() {
        val faculty = game.currentFaculty
        actions.gainNewIdea() // 6 Karten
        assertEquals(6, faculty.handCards.size)
        assertFailsWith<IllegalStateException> { actions.gainNewIdea() }
    }

    @Test
    fun `adjust strategy discards one, draws two and keeps one`() {
        val faculty = game.currentFaculty
        val discard = faculty.handCards.first()
        actions.adjustStrategy(discard)
        assertTrue(actions.isStrategyChoicePending)
        assertFailsWith<IllegalStateException> { actions.gainNewIdea() }

        val keep = actions.pendingStrategyCards.first()
        actions.keepStrategyCard(keep)
        assertEquals(5, faculty.handCards.size)
        assertTrue(keep in faculty.handCards)
        assertEquals(2, game.discardStack.size)
    }

    @Test
    fun `publish scores points and first publication bonus`() {
        val a = game.faculties[0]
        val b = game.faculties[1]
        repeat(3) { a.addResearch(ResearchCard(ResearchTopic.FIVE, ResearchArea.AI)) }
        repeat(2) { b.addResearch(ResearchCard(ResearchTopic.SIX, ResearchArea.AI)) }

        actions.publish(ResearchArea.AI)
        assertEquals(5 + 1, a.researchPoints)
        assertEquals(3, a.publications.size)
        assertTrue(a.researchActivity.getValue(ResearchArea.AI).isEmpty())

        // B ist jetzt am Zug; kein Erstpublikationsbonus mehr
        actions.publish(ResearchArea.AI)
        assertEquals(3, b.researchPoints)
    }

    @Test
    fun `publishing needs at least two cards`() {
        game.currentFaculty.addResearch(ResearchCard(ResearchTopic.ACE, ResearchArea.SE))
        assertFailsWith<IllegalStateException> { actions.publish(ResearchArea.SE) }
    }

    @Test
    fun `slow down removes top card of another faculty`() {
        val b = game.faculties[1]
        val top = ResearchCard(ResearchTopic.KING, ResearchArea.THEORETICAL)
        b.addResearch(ResearchCard(ResearchTopic.ACE, ResearchArea.THEORETICAL))
        b.addResearch(top)

        actions.slowDownCompetition(b, ResearchArea.THEORETICAL)
        assertEquals(1, b.researchActivity.getValue(ResearchArea.THEORETICAL).size)
        assertTrue(top in game.discardStack)
    }

    @Test
    fun `cannot slow down yourself`() {
        val a = game.currentFaculty
        a.addResearch(ResearchCard(ResearchTopic.ACE, ResearchArea.AI))
        assertFailsWith<IllegalArgumentException> { actions.slowDownCompetition(a, ResearchArea.AI) }
    }

    @Test
    fun `use literature takes any card and closes the gap`() {
        val faculty = game.currentFaculty
        val card = game.centralLiterature[2]
        actions.useLiterature(card)
        assertEquals(5, game.centralLiterature.size)
        assertSame(card, game.faculties[0].researchActivity.getValue(card.suit).peek())
        assertEquals(0, faculty.actionPoints)
    }

    @Test
    fun `publication points table`() {
        assertEquals(3, PlayerActionService.publicationPoints(2).second)
        assertEquals(5, PlayerActionService.publicationPoints(3).second)
        assertEquals(7, PlayerActionService.publicationPoints(4).second)
        assertEquals(9, PlayerActionService.publicationPoints(7).second)
    }
}
