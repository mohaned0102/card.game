package service

import entity.Faculty
import entity.FacultyGame
import entity.ResearchArea
import entity.ResearchCard

/**
 * Service für die Aktionen der Spielenden. Jede Aktion prüft die Spielregeln und wirft bei einem
 * ungültigen Zug eine [IllegalStateException] oder [IllegalArgumentException] mit lesbarer Meldung.
 * Sind alle Aktionspunkte verbraucht, endet der Zug automatisch.
 */
class PlayerActionService(private val rootService: RootService) : AbstractRefreshingService() {

    /** Die bei "Strategie anpassen" gezogenen Karten, von denen eine behalten werden muss. */
    var pendingStrategyCards: List<ResearchCard> = emptyList()
        private set

    /** `true`, solange bei "Strategie anpassen" noch eine Karte ausgewählt werden muss. */
    val isStrategyChoicePending: Boolean
        get() = pendingStrategyCards.isNotEmpty()

    /**
     * Forschung vorantreiben (1 AP): [card] von der Hand auf den passenden Stapel legen. Zusätzlich
     * wandert eine Karte vom Nachziehstapel in die zentrale Literatur (bei 6 Karten fliegt die älteste raus).
     */
    fun advanceResearch(card: ResearchCard) {
        val (game, faculty) = startAction(cost = 1)
        require(card in faculty.handCards) { "Diese Karte ist nicht auf deiner Hand." }

        faculty.handCards.remove(card)
        faculty.addResearch(card)
        rootService.log("${faculty.name} treibt Forschung voran: $card → ${card.suit.displayName}.")

        if (game.drawStack.isNotEmpty()) {
            if (game.centralLiterature.size >= GameService.LITERATURE_SIZE) {
                val oldest = game.centralLiterature.removeAt(0)
                game.discardStack += oldest
                rootService.log("  Älteste Literatur $oldest wird abgelegt.")
            }
            val newCard = game.drawStack.removeLast()
            game.centralLiterature += newCard
            rootService.log("  Neue Literatur: $newCard.")
        }
        finishAction(faculty, cost = 1)
    }

    /** Neue Ideen gewinnen (1 AP): eine Karte nachziehen. Nur mit höchstens 5 Handkarten möglich. */
    fun gainNewIdea() {
        val (game, faculty) = startAction(cost = 1)
        check(faculty.handCards.size <= MAX_HAND_SIZE - 1) { "Handkartenlimit (6) erreicht." }
        check(game.drawStack.isNotEmpty()) { "Der Nachziehstapel ist leer." }

        faculty.handCards += game.drawStack.removeLast()
        rootService.log("${faculty.name} gewinnt eine neue Idee (zieht 1 Karte).")
        finishAction(faculty, cost = 1)
    }

    /**
     * Strategie anpassen (1 AP), Teil 1: [discardCard] abwerfen und 2 Karten ziehen. Danach muss
     * mit [keepStrategyCard] eine der beiden behalten werden.
     */
    fun adjustStrategy(discardCard: ResearchCard) {
        val (game, faculty) = startAction(cost = 1)
        require(discardCard in faculty.handCards) { "Diese Karte ist nicht auf deiner Hand." }
        check(game.drawStack.size >= 2) { "Dafür müssen noch mindestens 2 Karten im Nachziehstapel sein." }

        faculty.handCards.remove(discardCard)
        game.discardStack += discardCard
        pendingStrategyCards = listOf(game.drawStack.removeLast(), game.drawStack.removeLast())
        faculty.actionPoints -= 1
        rootService.log("${faculty.name} passt die Strategie an: wirft $discardCard ab und zieht 2 Karten.")
        onAllRefreshables { refreshAfterAction() }
        onAllRefreshables { refreshAfterStrategyCardsDrawn(pendingStrategyCards) }
    }

    /** Strategie anpassen, Teil 2: [card] behalten, die andere gezogene Karte abwerfen. */
    fun keepStrategyCard(card: ResearchCard) {
        val game = checkNotNull(rootService.currentGame) { "Kein laufendes Spiel." }
        check(isStrategyChoicePending) { "Es gibt keine Karte zum Auswählen." }
        require(card in pendingStrategyCards) { "Diese Karte wurde nicht gezogen." }

        val faculty = game.currentFaculty
        faculty.handCards += card
        game.discardStack += pendingStrategyCards.filter { it !== card }
        pendingStrategyCards = emptyList()
        rootService.log("  ${faculty.name} behält eine der beiden Karten.")
        finishAction(faculty, cost = 0)
    }

    /**
     * Publizieren (2 AP): alle Karten des Stapels [area] (mind. 2) auf den Publikationsstapel legen
     * und Punkte erhalten (+1, falls erste Publikation aller Fakultäten in diesem Teilgebiet).
     */
    fun publish(area: ResearchArea) {
        val (game, faculty) = startAction(cost = 2)
        val stack = faculty.researchActivity.getValue(area)
        check(stack.size >= 2) { "Zum Publizieren müssen mindestens 2 Karten auf dem Stapel liegen." }

        val isFirstInArea = game.faculties.none { f -> f.publications.any { it.suit == area } }
        val cardCount = stack.size
        val (kind, points) = publicationPoints(cardCount)
        val bonus = if (isFirstInArea) 1 else 0

        faculty.publications += stack
        stack.clear()
        faculty.researchPoints += points + bonus
        rootService.log(
            "${faculty.name} publiziert in ${area.displayName}: $kind mit $cardCount Karten → " +
                "$points Punkte" + (if (isFirstInArea) " +1 (erste Publikation im Gebiet)" else "") + "."
        )
        finishAction(faculty, cost = 2)
    }

    /** Konkurrenz ausbremsen (2 AP): oberste Karte vom Stapel [area] der Fakultät [target] ablegen. */
    fun slowDownCompetition(target: Faculty, area: ResearchArea) {
        val (game, faculty) = startAction(cost = 2)
        require(target in game.faculties) { "Unbekannte Fakultät." }
        require(target !== faculty) { "Du kannst nur andere Fakultäten ausbremsen." }
        val stack = target.researchActivity.getValue(area)
        check(stack.isNotEmpty()) { "Dieser Stapel ist leer." }

        val removed = stack.pop()
        game.discardStack += removed
        rootService.log("${faculty.name} bremst ${target.name} aus: $removed wird abgelegt.")
        finishAction(faculty, cost = 2)
    }

    /** Literatur nutzen (2 AP): [card] aus der zentralen Literatur auf den eigenen Stapel legen. */
    fun useLiterature(card: ResearchCard) {
        val (game, faculty) = startAction(cost = 2)
        require(card in game.centralLiterature) { "Diese Karte liegt nicht in der zentralen Literatur." }

        game.centralLiterature.remove(card) // die übrigen Karten rücken automatisch zusammen
        faculty.addResearch(card)
        rootService.log("${faculty.name} nutzt Literatur: $card → ${card.suit.displayName}.")
        finishAction(faculty, cost = 2)
    }

    /** Prüft die gemeinsamen Vorbedingungen einer Aktion mit Kosten [cost]. */
    private fun startAction(cost: Int): Pair<FacultyGame, Faculty> {
        val game = checkNotNull(rootService.currentGame) { "Kein laufendes Spiel." }
        check(game.endgameTurnsLeft != 0) { "Das Spiel ist bereits beendet." }
        check(!isStrategyChoicePending) { "Zuerst muss eine der gezogenen Karten behalten werden." }
        val faculty = game.currentFaculty
        check(faculty.actionPoints >= cost) { "Nicht genug Aktionspunkte (benötigt: $cost)." }
        return game to faculty
    }

    /** Zieht die AP ab, informiert die View und beendet ggf. automatisch den Zug. */
    private fun finishAction(faculty: Faculty, cost: Int) {
        faculty.actionPoints -= cost
        onAllRefreshables { refreshAfterAction() }
        if (faculty.actionPoints == 0) rootService.gameService.endTurn()
    }

    companion object {
        const val MAX_HAND_SIZE = 6

        /** Art der Publikation und Punkte abhängig von der Kartenanzahl. */
        fun publicationPoints(cardCount: Int): Pair<String, Int> = when {
            cardCount >= 5 -> "Journal-Artikel" to 9
            cardCount == 4 -> "Regular Conference Paper" to 7
            cardCount == 3 -> "Short Conference Paper" to 5
            cardCount == 2 -> "Poster-Präsentation" to 3
            else -> throw IllegalArgumentException("Mindestens 2 Karten nötig.")
        }
    }
}
