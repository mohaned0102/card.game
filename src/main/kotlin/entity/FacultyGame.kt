package entity

/**
 * Der gesamte Spielzustand.
 *
 * @property faculties Die 2–4 Fakultäten in Spielreihenfolge
 */
class FacultyGame(val faculties: List<Faculty>) {

    init {
        require(faculties.size in 2..4) { "Es werden 2 bis 4 Fakultäten benötigt." }
    }

    /** Index der Fakultät, die am Zug ist. */
    var facultyAtTurn: Int = 0

    /**
     * Anzahl der Züge, die nach dem Leerwerden des Nachziehstapels noch gespielt werden.
     * -1 bedeutet, dass die Endphase noch nicht begonnen hat.
     */
    var endgameTurnsLeft: Int = -1

    /** Nachziehstapel (0..52 Karten). Die oberste Karte ist das letzte Element. */
    val drawStack: MutableList<ResearchCard> = mutableListOf()

    /** Ablagestapel. */
    val discardStack: MutableList<ResearchCard> = mutableListOf()

    /** Zentrale Literatur (0..6 Karten). Index 0 ist die älteste Karte. */
    val centralLiterature: MutableList<ResearchCard> = mutableListOf()

    /** Die Fakultät, die gerade am Zug ist. */
    val currentFaculty: Faculty
        get() = faculties[facultyAtTurn]

    /** Gibt den Zug an die nächste Fakultät im Uhrzeigersinn weiter. */
    fun nextFaculty() {
        facultyAtTurn = (facultyAtTurn + 1) % faculties.size
    }
}
