package entity

/**
 * Eine Spielkarte. Die Farbe ([suit]) bestimmt das Teilgebiet, der Wert ([value]) ist nur Anzeige.
 *
 * @property value Wert der Karte
 * @property suit Teilgebiet der Karte
 */
class ResearchCard(val value: ResearchTopic, val suit: ResearchArea) {
    override fun toString() = "$suit$value"
}
