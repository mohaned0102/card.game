package service

import entity.ResearchArea
import entity.ResearchCard
import entity.ResearchTopic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameServiceTest {

    @Test
    fun `start deals 5 cards each and 6 literature cards`() {
        val root = RootService()
        root.gameService.startNewGame(listOf("A", "B", "C"))
        val game = assertNotNull(root.currentGame)
        game.faculties.forEach { assertEquals(5, it.handCards.size) }
        assertEquals(6, game.centralLiterature.size)
        assertEquals(52 - 15 - 6, game.drawStack.size)
        assertEquals(2, game.currentFaculty.actionPoints)
    }

    @Test
    fun `invalid player configurations are rejected`() {
        val root = RootService()
        assertFailsWith<IllegalArgumentException> { root.gameService.startNewGame(listOf("A")) }
        assertFailsWith<IllegalArgumentException> { root.gameService.startNewGame(listOf("A", "B", "C", "D", "E")) }
        assertFailsWith<IllegalArgumentException> { root.gameService.startNewGame(listOf("A", "A")) }
        assertFailsWith<IllegalArgumentException> { root.gameService.startNewGame(listOf("A", " ")) }
    }

    @Test
    fun `random order keeps all names`() {
        val root = RootService()
        root.gameService.startNewGame(listOf("A", "B", "C", "D"), randomOrder = true)
        assertEquals(setOf("A", "B", "C", "D"), root.currentGame!!.faculties.map { it.name }.toSet())
    }

    @Test
    fun `game ends after every other faculty had one more turn`() {
        val root = RootService()
        val deck = GameService.createShuffledDeck().take(2 * 5 + 6 + 1)
        root.gameService.startNewGame(listOf("A", "B"), deck = deck)
        val game = root.currentGame!!
        var ended = false
        root.addRefreshable(object : Refreshable {
            override fun refreshAfterGameEnd(scores: List<ScoreResult>) { ended = true }
        })

        root.playerActionService.gainNewIdea() // A zieht die letzte Karte
        root.gameService.endTurn()             // Stapel leer → B hat noch einen Zug
        assertEquals(1, game.endgameTurnsLeft)
        assertEquals("B", game.currentFaculty.name)
        root.gameService.endTurn()
        assertTrue(ended)
    }

    @Test
    fun `final scoring applies all bonuses`() {
        val root = RootService()
        root.gameService.startNewGame(listOf("A", "B", "C"))
        val game = root.currentGame!!
        val (a, b, c) = game.faculties
        listOf(a, b, c).forEach { it.handCards.clear() }

        // A: 8 KI-Karten publiziert + je 1 Karte in den anderen Gebieten
        a.publications += List(8) { card(ResearchArea.AI) }
        a.publications += listOf(ResearchArea.IT_SECURITY, ResearchArea.SE, ResearchArea.THEORETICAL).map { card(it) }
        // B: 3 KI-Karten publiziert, 2 Karten liegen noch aus
        b.publications += List(3) { card(ResearchArea.AI) }
        b.addResearch(card(ResearchArea.SE))
        b.addResearch(card(ResearchArea.SE))

        val scores = root.gameService.computeScores(game).associateBy { it.faculty.name }
        // A: Mehrheit in allen 4 Gebieten (4 × 5), Breite 3, Schwerpunkt 4
        assertEquals(20, scores.getValue("A").visibilityBonus)
        assertEquals(3, scores.getValue("A").breadthBonus)
        assertEquals(4, scores.getValue("A").focusBonus)
        // B: Zweitmeiste in KI, 2 ausliegende Karten
        assertEquals(2, scores.getValue("B").visibilityBonus)
        assertEquals(2, scores.getValue("B").unfinishedResearch)
        assertEquals(0, scores.getValue("C").total)
    }

    private fun card(area: ResearchArea) = ResearchCard(ResearchTopic.TWO, area)
}
