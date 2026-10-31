package service

import entity.FacultyGame
import entity.ResearchArea
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Lässt zufällige "Bots" viele komplette Spiele spielen. Nach jedem Schritt wird geprüft, dass keine
 * Karte verloren geht oder doppelt existiert (immer genau 52) und die Regeln eingehalten werden.
 */
class FullGameSimulationTest {

    @Test
    fun `random games always end and keep all 52 cards`() {
        for (seed in 1..30) {
            val players = 2 + seed % 3
            playRandomGame(Random(seed), players)
        }
    }

    private fun playRandomGame(random: Random, players: Int) {
        val root = RootService()
        val view = TestRefreshable()
        root.addRefreshable(view)
        root.gameService.startNewGame(List(players) { "P$it" })
        val game = root.currentGame!!
        val actions = root.playerActionService

        var steps = 0
        while (!view.gameEnded) {
            if (++steps > 5_000) fail("Spiel endet nicht")
            val faculty = game.currentFaculty
            val others = game.faculties.filter { it !== faculty }

            val candidates: List<() -> Unit> = listOf(
                { actions.advanceResearch(faculty.handCards.random(random)) },
                { actions.gainNewIdea() },
                { actions.adjustStrategy(faculty.handCards.random(random)) },
                { actions.publish(ResearchArea.entries.random(random)) },
                { actions.slowDownCompetition(others.random(random), ResearchArea.entries.random(random)) },
                { actions.useLiterature(game.centralLiterature.random(random)) },
            )
            val done = candidates.shuffled(random).any { tryAction(it) }
            if (actions.isStrategyChoicePending) actions.keepStrategyCard(actions.pendingStrategyCards.random(random))
            if (!done && !view.gameEnded) root.gameService.endTurn()

            assertCardInvariants(game)
        }
        assertTrue(game.drawStack.isEmpty())
        assertEquals(players, view.scores!!.size)
    }

    private fun tryAction(action: () -> Unit): Boolean = try {
        action(); true
    } catch (e: IllegalStateException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    } catch (e: NoSuchElementException) { // random() auf leerer Liste
        false
    }

    private fun assertCardInvariants(game: FacultyGame) {
        val all = game.drawStack + game.discardStack + game.centralLiterature +
            game.faculties.flatMap { f -> f.handCards + f.publications + f.researchActivity.values.flatten() }
        assertEquals(52, all.size, "Kartenanzahl stimmt nicht")
        assertEquals(52, all.map { System.identityHashCode(it) }.toSet().size, "Karte doppelt vorhanden")
        assertTrue(game.centralLiterature.size <= 6)
        game.faculties.forEach { assertTrue(it.handCards.size <= 6) }
    }
}
