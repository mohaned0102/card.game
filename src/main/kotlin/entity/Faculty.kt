package entity

import java.util.Stack

/**
 * Eine Fakultät, d.h. ein*e Spieler*in.
 *
 * @property name Name zur Unterscheidung der Spielenden
 */
class Faculty(val name: String) {

    /** Verbleibende Aktionspunkte im aktuellen Zug. */
    var actionPoints: Int = 0

    /** Durch Publikationen während des Spiels erzielte Punkte. */
    var researchPoints: Int = 0

    /** Die vier offenen Forschungsstapel, einer pro Teilgebiet. Oberste Karte = `peek()`. */
    val researchActivity: Map<ResearchArea, Stack<ResearchCard>> =
        ResearchArea.entries.associateWith { Stack<ResearchCard>() }

    /** Handkarten (maximal 6). */
    val handCards: MutableList<ResearchCard> = mutableListOf()

    /** Offener Publikationsstapel. */
    val publications: MutableList<ResearchCard> = mutableListOf()

    /** Setzt die Aktionspunkte für einen neuen Zug auf 2. */
    fun resetActionPoints() {
        actionPoints = ACTION_POINTS_PER_TURN
    }

    /** Legt [researchCard] auf den Stapel ihres Teilgebiets. */
    fun addResearch(researchCard: ResearchCard) {
        researchActivity.getValue(researchCard.suit).push(researchCard)
    }

    override fun toString() = name

    companion object {
        /** Aktionspunkte, die eine Fakultät zu Beginn jedes Zuges erhält. */
        const val ACTION_POINTS_PER_TURN = 2
    }
}
