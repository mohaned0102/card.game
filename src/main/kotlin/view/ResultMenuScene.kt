package view

import service.RootService
import service.ScoreResult
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene

/** Zeigt die Endwertung und den/die Gewinner*in. */
class ResultMenuScene(private val rootService: RootService) :
    MenuScene(width = 1100, height = 700, background = Styles.BACKGROUND) {

    internal val title = Label(posX = 50, posY = 30, width = 1000, height = 60, font = Styles.TITLE)

    internal val rows: List<Label> = (0 until 4).map { i ->
        Label(posX = 50, posY = 120 + i * 110, width = 1000, height = 100,
            font = Styles.TEXT, isWrapText = true)
    }

    val newGameButton = menuButton("Neues Spiel", posX = 300, posY = 590, width = 230, height = 60).apply {
        visual = Styles.BUTTON_PRIMARY
    }

    val quitButton = menuButton("Beenden", posX = 570, posY = 590, width = 230, height = 60)

    init {
        addComponents(title, newGameButton, quitButton)
        rows.forEach { addComponents(it) }
    }

    /** Füllt die Szene mit der sortierten Endwertung [scores]. */
    fun showScores(scores: List<ScoreResult>) {
        val winners = rootService.gameService.winners(scores)
        title.text = if (winners.size == 1) "🏆 ${winners.first().faculty.name} gewinnt!"
        else "🏆 Gleichstand: ${winners.joinToString(" & ") { it.faculty.name }}"

        rows.forEachIndexed { i, label ->
            val s = scores.getOrNull(i)
            label.isVisible = s != null
            if (s != null) {
                label.text = "${i + 1}. ${s.faculty.name}: ${s.total} Punkte  —  " +
                    "Publikationen ${s.publicationPoints} · Sichtbarkeit ${s.visibilityBonus} · " +
                    "Breite ${s.breadthBonus} · Schwerpunkt ${s.focusBonus} · " +
                    "Angefangene Forschung ${s.unfinishedResearch}"
                label.font = if (s in winners) Styles.TEXT_BOLD else Styles.TEXT
            }
        }
    }
}
