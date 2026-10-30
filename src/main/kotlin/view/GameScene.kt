package view

import entity.Faculty
import entity.FacultyGame
import entity.ResearchArea
import entity.ResearchCard
import service.Refreshable
import service.RootService
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.components.uicomponents.Label
import tools.aqua.bgw.core.Alignment
import tools.aqua.bgw.core.BoardGameScene
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font

/**
 * Hauptspielfeld. Bedienung: erst Karte bzw. Stapel anklicken (wird gelb markiert),
 * dann den Aktionsbutton wählen.
 */
class GameScene(private val rootService: RootService) :
    BoardGameScene(width = 1920, height = 1080, background = Styles.BACKGROUND), Refreshable {

    // region Auswahlzustand
    private var selectedHandCard: ResearchCard? = null
    private var selectedLiterature: ResearchCard? = null
    private var selectedStack: Pair<Faculty, ResearchArea>? = null

    /** Verdeckt die Hand beim Spielerwechsel (Hotseat). */
    private var handHidden = true
    private val logLines = ArrayDeque<String>()
    // endregion

    // region Kopfbereich & zentrale Literatur
    internal val statusLabel = Label(posX = 40, posY = 15, width = 1840, height = 55,
        font = Styles.TITLE, alignment = Alignment.CENTER_LEFT)

    private val literatureTitle = Label(posX = 40, posY = 80, width = 760, height = 40,
        text = "Zentrale Literatur (links = älteste)", font = Styles.TEXT_BOLD, alignment = Alignment.CENTER_LEFT)

    internal val literatureButtons: List<CardButton> = (0 until 6).map { i ->
        CardButton(posX = 40 + i * 125, posY = 125).apply {
            onMouseClicked = { card?.let { toggleLiterature(it) } }
        }
    }

    internal val stackInfoLabel = Label(posX = 820, posY = 125, width = 600, height = 150,
        font = Styles.TEXT, alignment = Alignment.TOP_LEFT, isWrapText = true)
    // endregion

    // region Fakultäts-Bereiche
    internal inner class FacultyPanel(index: Int) {
        val x = 20 + index * 470
        val y = 290
        var faculty: Faculty? = null

        val background = Label(posX = x, posY = y, width = 450, height = 440, visual = Styles.PANEL)
        val nameLabel = Label(posX = x + 15, posY = y + 8, width = 420, height = 40,
            font = Styles.TEXT_BOLD, alignment = Alignment.CENTER_LEFT)
        val infoLabel = Label(posX = x + 15, posY = y + 48, width = 420, height = 35,
            font = Styles.TEXT, alignment = Alignment.CENTER_LEFT)
        val stackButtons: Map<ResearchArea, Button> = ResearchArea.entries.withIndex().associate { (k, area) ->
            area to Button(posX = x + 15, posY = y + 90 + k * 60, width = 420, height = 52,
                font = Styles.BUTTON_FONT, alignment = Alignment.CENTER_LEFT, visual = Styles.CARD).apply {
                onMouseClicked = { faculty?.let { toggleStack(it, area) } }
            }
        }
        val publicationLabel = Label(posX = x + 15, posY = y + 335, width = 420, height = 95,
            font = Styles.TEXT, alignment = Alignment.TOP_LEFT, isWrapText = true)

        val all get() = listOf(background, nameLabel, infoLabel, publicationLabel) + stackButtons.values
    }

    internal val panels = (0 until 4).map { FacultyPanel(it) }
    // endregion

    // region Hand, Strategie-Auswahl, Log
    internal val handTitle = Label(posX = 40, posY = 745, width = 780, height = 35,
        font = Styles.TEXT_BOLD, alignment = Alignment.CENTER_LEFT)

    internal val handButtons: List<CardButton> = (0 until 6).map { i ->
        CardButton(posX = 40 + i * 125, posY = 780).apply {
            onMouseClicked = { card?.let { toggleHandCard(it) } }
        }
    }

    internal val strategyTitle = Label(posX = 860, posY = 745, width = 380, height = 35,
        text = "Behalte eine Karte:", font = Styles.TEXT_BOLD, alignment = Alignment.CENTER_LEFT)

    internal val strategyButtons: List<CardButton> = (0 until 2).map { i ->
        CardButton(posX = 870 + i * 130, posY = 780).apply {
            onMouseClicked = { card?.let { runAction { rootService.playerActionService.keepStrategyCard(it) } } }
        }
    }

    /** Die letzten Log-Zeilen (eine Label pro Zeile, da Labels keine Zeilenumbrüche darstellen). */
    internal val logLabels: List<Label> = (0 until LOG_LINES).map { i ->
        Label(posX = 40, posY = 935 + i * 28, width = 1200, height = 28,
            font = Font(size = 17, color = Color.WHITE), alignment = Alignment.CENTER_LEFT)
    }
    // endregion

    // region Aktionen
    private val actions = rootService.playerActionService

    internal val advanceButton = actionButton("Forschung vorantreiben (1)", 0, 0) {
        actions.advanceResearch(requireNotNull(selectedHandCard) { "Wähle zuerst eine Handkarte." })
    }
    internal val ideaButton = actionButton("Neue Ideen gewinnen (1)", 1, 0) { actions.gainNewIdea() }
    internal val strategyButton = actionButton("Strategie anpassen (1)", 0, 1) {
        actions.adjustStrategy(requireNotNull(selectedHandCard) { "Wähle zuerst die Handkarte zum Abwerfen." })
    }
    internal val publishButton = actionButton("Publizieren (2)", 1, 1) {
        val (faculty, area) = requireNotNull(selectedStack) { "Wähle zuerst einen eigenen Stapel." }
        require(faculty === currentGame().currentFaculty) { "Du kannst nur eigene Stapel publizieren." }
        actions.publish(area)
    }
    internal val slowDownButton = actionButton("Konkurrenz ausbremsen (2)", 0, 2) {
        val (faculty, area) = requireNotNull(selectedStack) { "Wähle zuerst einen Stapel einer anderen Fakultät." }
        actions.slowDownCompetition(faculty, area)
    }
    internal val literatureButton = actionButton("Literatur nutzen (2)", 1, 2) {
        actions.useLiterature(requireNotNull(selectedLiterature) { "Wähle zuerst eine Karte aus der Literatur." })
    }
    internal val endTurnButton = actionButton("Zug beenden", 0, 3) { rootService.gameService.endTurn() }

    internal val messageLabel = Label(posX = 1280, posY = 1020, width = 600, height = 50,
        font = Styles.ERROR, isWrapText = true)
    // endregion

    init {
        addComponents(statusLabel, literatureTitle, stackInfoLabel, handTitle, strategyTitle, messageLabel)
        logLabels.forEach { addComponents(it) }
        literatureButtons.forEach { addComponents(it) }
        panels.forEach { panel -> panel.all.forEach { addComponents(it) } }
        handButtons.forEach { addComponents(it) }
        strategyButtons.forEach { addComponents(it) }
        listOf(advanceButton, ideaButton, strategyButton, publishButton, slowDownButton, literatureButton,
            endTurnButton).forEach { addComponents(it) }
        endTurnButton.width = 600.0
        showStrategyChoice(emptyList())
    }

    /** Erzeugt einen Aktionsbutton in Spalte [col], Zeile [row]. */
    private fun actionButton(text: String, col: Int, row: Int, action: () -> Unit) =
        menuButton(text, posX = 1280 + col * 310, posY = 745 + row * 68).apply {
            onMouseClicked = { runAction(action) }
        }

    /** Führt [action] aus und zeigt Regelverstöße als Meldung an, statt abzustürzen. */
    private fun runAction(action: () -> Unit) {
        messageLabel.text = ""
        try {
            action()
        } catch (e: IllegalStateException) {
            messageLabel.text = e.message ?: "Aktion nicht möglich."
        } catch (e: IllegalArgumentException) {
            messageLabel.text = e.message ?: "Aktion nicht möglich."
        }
    }

    private fun currentGame(): FacultyGame = checkNotNull(rootService.currentGame)

    // region Auswahl
    private fun toggleHandCard(card: ResearchCard) {
        selectedHandCard = if (selectedHandCard === card) null else card
        render()
    }

    private fun toggleLiterature(card: ResearchCard) {
        selectedLiterature = if (selectedLiterature === card) null else card
        render()
    }

    private fun toggleStack(faculty: Faculty, area: ResearchArea) {
        selectedStack = if (selectedStack?.first === faculty && selectedStack?.second == area) null else faculty to area
        render()
    }

    private fun clearSelection() {
        selectedHandCard = null
        selectedLiterature = null
        selectedStack = null
    }
    // endregion

    /** Deckt die Hand der aktuellen Fakultät auf (nach dem Hotseat-Zwischenbildschirm). */
    fun revealHand() {
        handHidden = false
        render()
    }

    // region Refreshable
    override fun refreshAfterStartNewGame() {
        clearSelection()
        showStrategyChoice(emptyList())
        messageLabel.text = ""
    }

    override fun refreshAfterStartTurn() {
        handHidden = true
        clearSelection()
        render()
    }

    override fun refreshAfterAction() {
        clearSelection()
        render()
    }

    override fun refreshAfterStrategyCardsDrawn(drawnCards: List<ResearchCard>) = showStrategyChoice(drawnCards)

    override fun refreshAfterLog(message: String) {
        logLines.addLast(message)
        while (logLines.size > LOG_LINES) logLines.removeFirst()
        logLabels.forEachIndexed { i, label -> label.text = logLines.getOrElse(i) { "" } }
    }
    // endregion

    private fun showStrategyChoice(cards: List<ResearchCard>) {
        strategyTitle.isVisible = cards.isNotEmpty()
        strategyButtons.forEachIndexed { i, button -> button.card = cards.getOrNull(i) }
    }

    /** Zeichnet die gesamte Szene aus dem aktuellen Spielzustand neu. */
    private fun render() {
        val game = rootService.currentGame ?: return
        val current = game.currentFaculty
        if (!actions.isStrategyChoicePending) showStrategyChoice(emptyList())

        val endgame = if (game.endgameTurnsLeft > 0) "   ·   Letzte Runde!" else ""
        statusLabel.text = "Am Zug: ${current.name}   ·   Aktionspunkte: ${current.actionPoints}$endgame"
        stackInfoLabel.text = "Nachziehstapel: ${game.drawStack.size} Karten   ·   " +
            "Ablagestapel: ${game.discardStack.size} Karten"

        literatureButtons.forEachIndexed { i, button ->
            button.card = game.centralLiterature.getOrNull(i)
            button.isSelected = button.card != null && button.card === selectedLiterature
        }

        handTitle.text = if (handHidden) "Hand verdeckt" else "Hand von ${current.name} (${current.handCards.size}/6)"
        handButtons.forEachIndexed { i, button ->
            button.card = if (handHidden) null else current.handCards.getOrNull(i)
            button.isSelected = button.card != null && button.card === selectedHandCard
        }

        panels.forEachIndexed { i, panel ->
            val faculty = game.faculties.getOrNull(i)
            panel.faculty = faculty
            panel.all.forEach { it.isVisible = faculty != null }
            if (faculty != null) renderPanel(panel, faculty, faculty === current)
        }
    }

    private fun renderPanel(panel: FacultyPanel, faculty: Faculty, isCurrent: Boolean) {
        panel.background.visual = if (isCurrent) Styles.PANEL_ACTIVE else Styles.PANEL
        panel.nameLabel.text = (if (isCurrent) "▶ " else "") + faculty.name
        panel.infoLabel.text = "Punkte: ${faculty.researchPoints}   ·   Handkarten: ${faculty.handCards.size}"

        panel.stackButtons.forEach { (area, button) ->
            val stack = faculty.researchActivity.getValue(area)
            val cards = if (stack.isEmpty()) "–" else stack.joinToString(" ") { it.value.shortName }
            button.text = "${area.symbol} ${shortAreaName(area)} (${stack.size}): $cards"
            val selected = selectedStack?.first === faculty && selectedStack?.second == area
            button.visual = if (selected) Styles.CARD_SELECTED else Styles.CARD
        }

        val published = ResearchArea.entries.joinToString("  ") { area ->
            "${area.symbol}${faculty.publications.count { it.suit == area }}"
        }
        panel.publicationLabel.text = "Publiziert: $published"
    }

    private fun shortAreaName(area: ResearchArea) = when (area) {
        ResearchArea.AI -> "KI"
        ResearchArea.IT_SECURITY -> "IT-Sicherheit"
        ResearchArea.SE -> "SE"
        ResearchArea.THEORETICAL -> "Theorie"
    }

    private companion object {
        const val LOG_LINES = 5
    }
}
