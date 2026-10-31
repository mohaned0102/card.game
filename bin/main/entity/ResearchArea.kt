package entity

/**
 * Die vier Teilgebiete der Informatik. Sie entsprechen den Farben eines Standard-Kartendecks.
 *
 * @property symbol Kartensymbol der Farbe
 * @property displayName Lesbarer Name des Teilgebiets
 */
enum class ResearchArea(val symbol: String, val displayName: String) {
    AI("♠", "Künstliche Intelligenz"),
    IT_SECURITY("♥", "IT-Sicherheit"),
    SE("♦", "Software Engineering"),
    THEORETICAL("♣", "Theoretische Informatik");

    override fun toString() = symbol
}
