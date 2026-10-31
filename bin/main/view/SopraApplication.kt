package view

import service.Refreshable
import service.RootService
import service.ScoreResult
import tools.aqua.bgw.core.BoardGameApplication

/** Einstiegspunkt der Oberfläche: verwaltet Szenen und wechselt zwischen ihnen. */
class SopraApplication : BoardGameApplication("Exzellente Informatik-Forschung"), Refreshable {

    internal val rootService = RootService()

    internal val gameScene = GameScene(rootService)
    internal val startMenuScene = StartMenuScene(rootService)
    internal val turnSwitchMenuScene = TurnSwitchMenuScene()
    internal val resultMenuScene = ResultMenuScene(rootService)

    init {
        rootService.addRefreshables(this, gameScene)

        startMenuScene.quitButton.onMouseClicked = { exit() }
        turnSwitchMenuScene.readyButton.onMouseClicked = {
            gameScene.revealHand()
            hideMenuScene()
        }
        resultMenuScene.newGameButton.onMouseClicked = { showMenuScene(startMenuScene) }
        resultMenuScene.quitButton.onMouseClicked = { exit() }

        showGameScene(gameScene)
        showMenuScene(startMenuScene)
    }

    override fun refreshAfterStartTurn() {
        val game = rootService.currentGame ?: return
        val info = if (game.endgameTurnsLeft > 0) " Letzte Runde!" else ""
        turnSwitchMenuScene.showFaculty(game.currentFaculty.name, info)
        showMenuScene(turnSwitchMenuScene)
    }

    override fun refreshAfterGameEnd(scores: List<ScoreResult>) {
        resultMenuScene.showScores(scores)
        showMenuScene(resultMenuScene)
    }
}
