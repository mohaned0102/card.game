package service

import entity.Faculty
import entity.FacultyGame
import entity.ResearchArea
import entity.ResearchCard
import entity.ResearchTopic

/** Service für den Spielablauf: Spielstart, Zugwechsel, Spielende und Endwertung. */
class GameService(private val rootService: RootService) : AbstractRefreshingService() {

    /**
     * Startet ein neues Spiel.
     *
     * @param facultyNames Namen der 2–4 Fakultäten in der gewünschten Spielreihenfolge
     * @param randomOrder Falls `true`, wird die Reihenfolge zufällig gemischt
     * @param deck Optional ein festes Deck (oberste Karte = letztes Element), z.B. für Tests.
     *   Standard: neu gemischtes 52-Karten-Deck.
     */
    fun startNewGame(
        facultyNames: List<String>,
        randomOrder: Boolean = false,
        deck: List<ResearchCard> = createShuffledDeck(),
    ) {
        val names = facultyNames.map { it.trim() }
        require(names.size in 2..4) { "Es werden 2 bis 4 Spieler*innen benötigt." }
        require(names.all { it.isNotEmpty() }) { "Jede Fakultät braucht einen Namen." }
        require(names.toSet().size == names.size) { "Die Namen müssen verschieden sein." }
        require(deck.size >= names.size * HAND_SIZE_AT_START + LITERATURE_SIZE) {
            "Das Deck enthält zu wenige Karten."
        }

        val orderedNames = if (randomOrder) names.shuffled() else names
        val game = FacultyGame(orderedNames.map { Faculty(it) })
        game.drawStack.addAll(deck)

        repeat(HAND_SIZE_AT_START) {
            game.faculties.forEach { it.handCards += game.drawStack.removeLast() }
        }
        repeat(LITERATURE_SIZE) { game.centralLiterature += game.drawStack.removeLast() }

        rootService.currentGame = game
        rootService.log("=== Neues Spiel: ${orderedNames.joinToString(" → ")} ===")
        onAllRefreshables { refreshAfterStartNewGame() }
        startTurn()
    }

    /** Beginnt den Zug der aktuellen Fakultät: 2 Aktionspunkte. */
    fun startTurn() {
        val game = checkNotNull(rootService.currentGame) { "Kein laufendes Spiel." }
        val faculty = game.currentFaculty
        faculty.resetActionPoints()
        val endgameInfo =
            if (game.endgameTurnsLeft > 0) " (letzte Runde, noch ${game.endgameTurnsLeft} Zug/Züge)" else ""
        rootService.log("--- ${faculty.name} ist am Zug$endgameInfo ---")
        onAllRefreshables { refreshAfterStartTurn() }
    }

    /**
     * Beendet den Zug der aktuellen Fakultät. Ist der Nachziehstapel leer, darf danach jede andere
     * Fakultät noch einmal ziehen; anschließend endet das Spiel.
     */
    fun endTurn() {
        val game = checkNotNull(rootService.currentGame) { "Kein laufendes Spiel." }
        check(!rootService.playerActionService.isStrategyChoicePending) {
            "Zuerst muss eine der gezogenen Karten behalten werden."
        }
        val faculty = game.currentFaculty
        faculty.actionPoints = 0
        rootService.log("${faculty.name} beendet den Zug.")

        if (game.endgameTurnsLeft == -1 && game.drawStack.isEmpty()) {
            game.endgameTurnsLeft = game.faculties.size - 1
            rootService.log("Der Nachziehstapel ist leer – jede andere Fakultät ist noch einmal am Zug.")
        } else if (game.endgameTurnsLeft > 0) {
            game.endgameTurnsLeft--
        }

        if (game.endgameTurnsLeft == 0) {
            endGame()
        } else {
            game.nextFaculty()
            startTurn()
        }
    }

    /** Beendet das Spiel, berechnet die Endwertung und gibt sie aus. */
    fun endGame() {
        val game = checkNotNull(rootService.currentGame) { "Kein laufendes Spiel." }
        val scores = computeScores(game).sortedByDescending { it.total }

        rootService.log("=== Spielende ===")
        scores.forEach {
            rootService.log(
                "${it.faculty.name}: ${it.total} Punkte (Publikationen ${it.publicationPoints}, " +
                    "Sichtbarkeit ${it.visibilityBonus}, Breite ${it.breadthBonus}, " +
                    "Schwerpunkt ${it.focusBonus}, angefangene Forschung ${it.unfinishedResearch})"
            )
        }
        val winners = winners(scores)
        rootService.log("Gewonnen: ${winners.joinToString(" & ") { it.faculty.name }}")
        onAllRefreshables { refreshAfterGameEnd(scores) }
    }

    /** Berechnet die Endwertung aller Fakultäten in Spielreihenfolge. */
    fun computeScores(game: FacultyGame): List<ScoreResult> {
        val visibility = game.faculties.associateWith { 0 }.toMutableMap()

        // 1. Sichtbarkeit: Mehrheiten an publizierten Karten pro Teilgebiet
        for (area in ResearchArea.entries) {
            val counts = game.faculties.associateWith { f -> f.publications.count { it.suit == area } }
            val distinct = counts.values.filter { it > 0 }.distinct().sortedDescending()
            val most = distinct.getOrNull(0)
            val second = distinct.getOrNull(1)
            counts.forEach { (faculty, count) ->
                if (count == most) visibility[faculty] = visibility.getValue(faculty) + MAJORITY_BONUS
                else if (count == second) visibility[faculty] = visibility.getValue(faculty) + SECOND_MAJORITY_BONUS
            }
        }

        return game.faculties.map { faculty ->
            val publishedAreas = faculty.publications.map { it.suit }.toSet()
            ScoreResult(
                faculty = faculty,
                publicationPoints = faculty.researchPoints,
                visibilityBonus = visibility.getValue(faculty),
                // 2. Breite Aufstellung
                breadthBonus = if (publishedAreas.size == ResearchArea.entries.size) BREADTH_BONUS else 0,
                // 3. Forschungsschwerpunkt (pro Teilgebiet mit 8+ publizierten Karten)
                focusBonus = ResearchArea.entries.count { area ->
                    faculty.publications.count { it.suit == area } >= FOCUS_THRESHOLD
                } * FOCUS_BONUS,
                // 4. Angefangene Forschung
                unfinishedResearch = faculty.researchActivity.values.sumOf { it.size },
            )
        }
    }

    /** Alle Fakultäten mit der höchsten Gesamtpunktzahl (bei Gleichstand mehrere). */
    fun winners(scores: List<ScoreResult>): List<ScoreResult> {
        val best = scores.maxOf { it.total }
        return scores.filter { it.total == best }
    }

    companion object {
        const val HAND_SIZE_AT_START = 5
        const val LITERATURE_SIZE = 6
        const val MAJORITY_BONUS = 5
        const val SECOND_MAJORITY_BONUS = 2
        const val BREADTH_BONUS = 3
        const val FOCUS_THRESHOLD = 8
        const val FOCUS_BONUS = 4

        /** Erzeugt ein gemischtes Standarddeck mit 52 Karten. */
        fun createShuffledDeck(): List<ResearchCard> =
            ResearchArea.entries.flatMap { area -> ResearchTopic.entries.map { ResearchCard(it, area) } }
                .shuffled()
    }
}
