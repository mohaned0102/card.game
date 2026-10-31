package view

import entity.ResearchArea
import entity.ResearchCard
import tools.aqua.bgw.components.uicomponents.Button
import tools.aqua.bgw.core.Color
import tools.aqua.bgw.util.Font
import tools.aqua.bgw.visual.ColorVisual

/** Gemeinsame Farben, Schriften und Hilfsfunktionen der Oberfläche. */
object Styles {
    val BACKGROUND = ColorVisual(Color(34, 85, 60))
    val PANEL = ColorVisual(Color(255, 255, 255, 0.15))
    val PANEL_ACTIVE = ColorVisual(Color(255, 230, 120, 0.35))
    val CARD = ColorVisual(Color.WHITE)
    val CARD_SELECTED = ColorVisual(Color(255, 220, 90))
    val BUTTON = ColorVisual(Color(230, 230, 230))
    val BUTTON_PRIMARY = ColorVisual(Color(120, 190, 255))

    val TITLE = Font(size = 32, color = Color.WHITE, fontWeight = Font.FontWeight.BOLD)
    val TEXT = Font(size = 20, color = Color.WHITE)
    val TEXT_BOLD = Font(size = 22, color = Color.WHITE, fontWeight = Font.FontWeight.BOLD)
    val BUTTON_FONT = Font(size = 18)
    val ERROR = Font(size = 20, color = Color(255, 150, 150), fontWeight = Font.FontWeight.BOLD)

    /** Schrift für eine Karte: rot für ♥/♦, schwarz für ♠/♣. */
    fun cardFont(card: ResearchCard, size: Int = 30): Font {
        val red = card.suit == ResearchArea.IT_SECURITY || card.suit == ResearchArea.SE
        return Font(size = size, color = if (red) Color(200, 0, 0) else Color.BLACK, fontWeight = Font.FontWeight.BOLD)
    }
}

/** Ein Button, der eine Karte darstellt. Ist [card] `null`, ist der Button unsichtbar. */
class CardButton(posX: Number, posY: Number, width: Number = 110, height: Number = 150) :
    Button(posX = posX, posY = posY, width = width, height = height, visual = Styles.CARD) {

    var card: ResearchCard? = null
        set(value) {
            field = value
            isVisible = value != null
            if (value != null) {
                text = "${value.value} ${value.suit.symbol}"
                font = Styles.cardFont(value)
            }
        }

    var isSelected: Boolean = false
        set(value) {
            field = value
            visual = if (value) Styles.CARD_SELECTED else Styles.CARD
        }

    init {
        isWrapText = true
        isVisible = false
    }
}

/** Einheitlicher Menü-/Aktionsbutton. */
fun menuButton(text: String, posX: Number = 0, posY: Number = 0, width: Number = 290, height: Number = 55) =
    Button(posX = posX, posY = posY, width = width, height = height, text = text,
        font = Styles.BUTTON_FONT, visual = Styles.BUTTON)
