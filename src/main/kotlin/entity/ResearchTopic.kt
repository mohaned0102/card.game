package entity

/**
 * Die 13 Werte einer Karte (Ass bis König).
 *
 * @property shortName Kurzschreibweise für die Anzeige
 */
enum class ResearchTopic(val shortName: String) {
    ACE("A"),
    TWO("2"),
    THREE("3"),
    FOUR("4"),
    FIVE("5"),
    SIX("6"),
    SEVEN("7"),
    EIGHT("8"),
    NINE("9"),
    TEN("10"),
    JACK("J"),
    QUEEN("Q"),
    KING("K");

    override fun toString() = shortName
}
