package view

import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.CheckBox
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.components.uicomponents.TextField
import tools.aqua.bgw.core.MenuScene

/**
 * Startmenü: 2–4 Namen eingeben, Reihenfolge frei festlegen (▲-Buttons) oder zufällig mischen.
 * Die Reihenfolge der Felder von oben nach unten ist die Spielreihenfolge.
 */
class StartMenuScene(private val rootService: RootService) :
    MenuScene(width = 800, height = 760, background = Styles.BACKGROUND) {

    internal val title = Label(posX = 50, posY = 30, width = 700, height = 60,
        text = "Exzellente Informatik-Forschung", font = Styles.TITLE)

    private val hint = Label(posX = 50, posY = 95, width = 700, height = 40,
        text = "2–4 Fakultäten. Leere Felder werden ignoriert.", font = Styles.TEXT)

    internal val nameFields: List<TextField> = (0 until 4).map { i ->
        TextField(posX = 150, posY = 160 + i * 80, width = 420, height = 55,
            prompt = "Fakultät ${i + 1}" + if (i >= 2) " (optional)" else "",
            text = if (i < 2) "Fakultät ${i + 1}" else "", font = Styles.BUTTON_FONT)
    }

    private val positionLabels: List<Label> = (0 until 4).map { i ->
        Label(posX = 50, posY = 160 + i * 80, width = 90, height = 55, text = "${i + 1}.", font = Styles.TEXT_BOLD)
    }

    /** Vertauscht Feld i mit Feld i-1. */
    internal val moveUpButtons: List<Button> = (1 until 4).map { i ->
        menuButton("▲", posX = 590, posY = 160 + i * 80, width = 60).apply {
            onMouseClicked = { swap(i, i - 1) }
        }
    }

    internal val shuffleButton = menuButton("Reihenfolge mischen", posX = 150, posY = 490, width = 420).apply {
        onMouseClicked = { shuffleNames() }
    }

    internal val randomAtStart = CheckBox(posX = 150, posY = 560, width = 420, height = 40,
        text = "Beim Start zufällig mischen", font = Styles.TEXT)

    internal val errorLabel = Label(posX = 50, posY = 605, width = 700, height = 40, font = Styles.ERROR)

    val startButton = menuButton("Spiel starten", posX = 150, posY = 660, width = 200).apply {
        visual = Styles.BUTTON_PRIMARY
        onMouseClicked = { startGame() }
    }

    val quitButton = menuButton("Beenden", posX = 370, posY = 660, width = 200)

    init {
        addComponents(title, hint, shuffleButton, randomAtStart, errorLabel, startButton, quitButton)
        nameFields.forEach { addComponents(it) }
        positionLabels.forEach { addComponents(it) }
        moveUpButtons.forEach { addComponents(it) }
    }

    private fun swap(i: Int, j: Int) {
        val tmp = nameFields[i].text
        nameFields[i].text = nameFields[j].text
        nameFields[j].text = tmp
    }

    private fun shuffleNames() {
        val shuffled = nameFields.map { it.text }.filter { it.isNotBlank() }.shuffled()
        nameFields.forEachIndexed { i, field -> field.text = shuffled.getOrElse(i) { "" } }
    }

    private fun startGame() {
        val names = nameFields.map { it.text.trim() }.filter { it.isNotEmpty() }
        try {
            errorLabel.text = ""
            rootService.gameService.startNewGame(names, randomOrder = randomAtStart.isChecked)
        } catch (e: IllegalArgumentException) {
            errorLabel.text = e.message ?: "Ungültige Eingabe."
        }
    }
}
