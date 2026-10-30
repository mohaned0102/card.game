package service

import entity.Faculty
import entity.ResearchCard

/**
 * Schnittstelle zwischen Service- und View-Schicht. Die Services rufen nach jeder Zustandsänderung
 * die passende Methode auf; die View aktualisiert sich daraufhin. Alle Methoden haben eine leere
 * Standardimplementierung, damit jede View nur das überschreibt, was sie braucht.
 */
interface Refreshable {

    /** Ein neues Spiel wurde gestartet. */
    fun refreshAfterStartNewGame() {}

    /** Eine neue Fakultät ist am Zug. */
    fun refreshAfterStartTurn() {}

    /** Eine Aktion wurde ausgeführt (Hand, Stapel, Literatur, Punkte o.ä. haben sich geändert). */
    fun refreshAfterAction() {}

    /** Bei "Strategie anpassen" wurden [drawnCards] gezogen; eine davon muss behalten werden. */
    fun refreshAfterStrategyCardsDrawn(drawnCards: List<ResearchCard>) {}

    /** Das Spiel ist beendet. [scores] enthält die Endwertung jeder Fakultät. */
    fun refreshAfterGameEnd(scores: List<ScoreResult>) {}

    /** Eine neue Zeile im Spiel-Log. */
    fun refreshAfterLog(message: String) {}
}

/**
 * Endwertung einer Fakultät.
 *
 * @property faculty Die bewertete Fakultät
 * @property publicationPoints Punkte aus Publikationen während des Spiels
 * @property visibilityBonus Mehrheiten-Bonus (+5 / +2 pro Teilgebiet)
 * @property breadthBonus Bonus für Publikationen in allen 4 Teilgebieten
 * @property focusBonus Bonus für 8+ publizierte Karten in einem Teilgebiet
 * @property unfinishedResearch +1 pro noch ausliegender (nicht publizierter) Karte
 */
data class ScoreResult(
    val faculty: Faculty,
    val publicationPoints: Int,
    val visibilityBonus: Int,
    val breadthBonus: Int,
    val focusBonus: Int,
    val unfinishedResearch: Int,
) {
    /** Gesamtpunktzahl. */
    val total: Int
        get() = publicationPoints + visibilityBonus + breadthBonus + focusBonus + unfinishedResearch
}
