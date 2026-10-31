package view

import entity.FacultyGame
import entity.ResearchArea
import service.Refreshable
import service.ScoreResult
import kotlin.system.exitProcess

/**
 * Sichtbarer End-to-End-Test: öffnet das echte Spielfenster und spielt eine komplette Partie,
 * indem er wie ein Mensch auf die Buttons klickt. Nach jedem Schritt werden Regeln geprüft.
 *
 * Start: `./gradlew visualTest` (langsamer: `./gradlew visualTest -Pdelay=800`)
 */
fun main(args: Array<String>) {
    val delay = args.firstOrNull()?.toLongOrNull() ?: 400L
    val app = SopraApplication()
    val driver = VisualGameDriver(app, delay)
    app.rootService.addRefreshable(driver)
    app.onWindowShown = { Thread(driver::run, "visual-test").apply { isDaemon = true }.start() }
    app.show()
}

private class VisualGameDriver(private val app: SopraApplication, private val delay: Long) : Refreshable {

    @Volatile private var turnStarted = false
    @Volatile private var scores: List<ScoreResult>? = null
    private var stepCount = 0

    override fun refreshAfterStartTurn() { turnStarted = true }
    override fun refreshAfterGameEnd(scores: List<ScoreResult>) { this.scores = scores }

    private val scene get() = app.gameScene
    private val game: FacultyGame get() = checkNotNull(app.rootService.currentGame)

    fun run() {
        try {
            Thread.sleep(1500)
            setUpPlayers()
            playUntilEnd()
            checkResults()
            println("\n✅ VISUELLER TEST BESTANDEN ($stepCount Schritte)")
            Thread.sleep(4000)
            exitProcess(0)
        } catch (e: Throwable) {
            println("\n❌ VISUELLER TEST FEHLGESCHLAGEN: ${e.message}")
            e.printStackTrace()
            Thread.sleep(6000) // Fenster offen lassen, damit man den Fehler sieht
            exitProcess(1)
        }
    }

    // region Schritte
    private fun setUpPlayers() {
        val menu = app.startMenuScene
        listOf("Dortmund", "Bochum", "Essen").forEachIndexed { i, name ->
            menu.nameFields[i].text = name
            pause()
        }
        step("Namen eingegeben")
        clickAndWait(menu.moveUpButtons[1])
        check(menu.nameFields[1].text == "Essen") { "▲-Button hat die Reihenfolge nicht geändert" }
        step("Reihenfolge geändert: Dortmund, Essen, Bochum")

        clickAndWait(menu.startButton)
        check(game.faculties.map { it.name } == listOf("Dortmund", "Essen", "Bochum")) { "Falsche Spielreihenfolge" }
        step("Spiel gestartet")
    }

    private fun playUntilEnd() {
        var action = 0
        while (scores == null) {
            check(stepCount < 2_000) { "Spiel endet nicht" }
            if (turnStarted) {
                turnStarted = false
                check(scene.handButtons.none { it.isVisible }) { "Hand ist beim Spielerwechsel nicht verdeckt" }
                clickAndWait(app.turnSwitchMenuScene.readyButton)
                check(scene.statusLabel.text.contains(game.currentFaculty.name)) { "Statuszeile zeigt falsche Fakultät" }
                continue
            }
            // Alle Aktionen reihum ausprobieren; scheitert eine, kommt die nächste dran
            val done = (0 until 6).any { tryAction((action + it) % 6) }
            action++
            if (!done && scores == null && !turnStarted) {
                clickAndWait(scene.endTurnButton)
                step("Zug beendet")
            }
            checkInvariants()
        }
    }

    /** Versucht Aktion Nr. [index] per Klicks. @return `true`, wenn sie ausgeführt wurde. */
    private fun tryAction(index: Int): Boolean {
        val faculty = game.currentFaculty
        val apBefore = faculty.actionPoints
        val turnBefore = game.facultyAtTurn
        val ownStack = faculty.researchActivity.entries.maxByOrNull { it.value.size }

        when (index) {
            0 -> { // Publizieren, sobald ein Stapel 2+ Karten hat
                if (ownStack == null || ownStack.value.size < 2) return false
                select(scene.panels[game.facultyAtTurn].stackButtons.getValue(ownStack.key), isSelected = false)
                clickAndWait(scene.publishButton)
            }
            1 -> { // Forschung vorantreiben mit der Karte, deren Stapel am größten ist
                val button = scene.handButtons.filter { it.isVisible }
                    .maxByOrNull { b -> faculty.researchActivity.getValue(b.card!!.suit).size } ?: return false
                select(button, button.isSelected)
                clickAndWait(scene.advanceButton)
            }
            2 -> clickAndWait(scene.ideaButton)
            3 -> { // Literatur nutzen
                val button = scene.literatureButtons.firstOrNull { it.isVisible } ?: return false
                select(button, button.isSelected)
                clickAndWait(scene.literatureButton)
            }
            4 -> { // Konkurrenz ausbremsen: größter fremder Stapel
                val target = game.faculties.withIndex().filter { it.value !== faculty }
                    .flatMap { (i, f) -> ResearchArea.entries.map { Triple(i, it, f.researchActivity.getValue(it).size) } }
                    .filter { it.third > 0 }.maxByOrNull { it.third } ?: return false
                select(scene.panels[target.first].stackButtons.getValue(target.second), isSelected = false)
                clickAndWait(scene.slowDownButton)
            }
            5 -> { // Strategie anpassen
                val button = scene.handButtons.firstOrNull { it.isVisible } ?: return false
                select(button, button.isSelected)
                clickAndWait(scene.strategyButton)
                if (scene.strategyTitle.isVisible) {
                    check(scene.strategyButtons.count { it.isVisible } == 2) { "Es werden nicht 2 Karten angezeigt" }
                    clickAndWait(scene.strategyButtons[0])
                }
            }
        }

        val executed = game.facultyAtTurn != turnBefore || scores != null || faculty.actionPoints < apBefore
        if (executed) {
            step(ACTION_NAMES[index] + " (${faculty.name})")
        } else {
            check(scene.messageLabel.text.isNotEmpty()) { "Abgelehnte Aktion ohne Fehlermeldung" }
        }
        return executed
    }

    private fun checkResults() {
        val results = checkNotNull(scores)
        val best = results.maxOf { it.total }
        val title = app.resultMenuScene.title.text
        check(results.filter { it.total == best }.all { title.contains(it.faculty.name) }) {
            "Ergebnisanzeige nennt falsche*n Gewinner*in: $title"
        }
        check(game.drawStack.isEmpty()) { "Spiel endete, obwohl der Nachziehstapel nicht leer ist" }
        step("Ergebnis angezeigt: $title")
    }
    // endregion

    // region Hilfsfunktionen
    private fun checkInvariants() {
        val all = game.drawStack + game.discardStack + game.centralLiterature +
            game.faculties.flatMap { f -> f.handCards + f.publications + f.researchActivity.values.flatten() }
        check(all.size == 52) { "Es gibt ${all.size} statt 52 Karten" }
        check(game.centralLiterature.size <= 6) { "Mehr als 6 Karten in der Literatur" }
        check(game.faculties.all { it.handCards.size <= 6 }) { "Handkartenlimit überschritten" }
    }

    private fun clickAndWait(component: tools.aqua.bgw.components.ComponentView) {
        click(component)
        pause()
    }

    /** Klickt [component] nur an, wenn es noch nicht ausgewählt ist (ein 2. Klick hebt die Auswahl auf). */
    private fun select(component: tools.aqua.bgw.components.ComponentView, isSelected: Boolean) {
        if (!isSelected) clickAndWait(component)
    }

    private fun pause() = Thread.sleep(delay)

    private fun step(text: String) = println("✔ [${++stepCount}] $text")
    // endregion

    companion object {
        val ACTION_NAMES = listOf(
            "Publizieren", "Forschung vorantreiben", "Neue Ideen gewinnen",
            "Literatur nutzen", "Konkurrenz ausbremsen", "Strategie anpassen",
        )
    }
}
