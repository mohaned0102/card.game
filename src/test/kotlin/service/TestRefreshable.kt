package service

import entity.ResearchCard

/**
 * Test-Double für die View: merkt sich, welche Refresh-Methoden aufgerufen wurden.
 * So kann man prüfen, ob die Services die View korrekt benachrichtigen.
 */
class TestRefreshable : Refreshable {
    var startNewGameCalls = 0
    var startTurnCalls = 0
    var actionCalls = 0
    var strategyCards: List<ResearchCard>? = null
    var scores: List<ScoreResult>? = null
    val logs = mutableListOf<String>()

    val gameEnded get() = scores != null

    override fun refreshAfterStartNewGame() { startNewGameCalls++ }
    override fun refreshAfterStartTurn() { startTurnCalls++ }
    override fun refreshAfterAction() { actionCalls++ }
    override fun refreshAfterStrategyCardsDrawn(drawnCards: List<ResearchCard>) { strategyCards = drawnCards }
    override fun refreshAfterGameEnd(scores: List<ScoreResult>) { this.scores = scores }
    override fun refreshAfterLog(message: String) { logs += message }
}
