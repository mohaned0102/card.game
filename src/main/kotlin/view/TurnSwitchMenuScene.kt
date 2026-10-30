package view

import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.MenuScene

/**
 * Hotseat-Zwischenbildschirm: verdeckt die Handkarten, bis die nächste Person bereit ist.
 */
class TurnSwitchMenuScene : MenuScene(width = 700, height = 340, background = Styles.BACKGROUND) {

    internal val infoLabel = Label(posX = 50, posY = 60, width = 600, height = 120,
        font = Styles.TITLE, isWrapText = true)

    val readyButton = menuButton("Bereit – Hand zeigen", posX = 200, posY = 220, width = 300, height = 60).apply {
        visual = Styles.BUTTON_PRIMARY
    }

    init {
        addComponents(infoLabel, readyButton)
    }

    /** Zeigt an, wer als Nächstes am Zug ist. */
    fun showFaculty(name: String, info: String) {
        infoLabel.text = "$name ist am Zug.$info"
    }
}
