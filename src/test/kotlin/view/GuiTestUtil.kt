package view

import tools.aqua.bgw.components.ComponentView
import tools.aqua.bgw.event.MouseButtonType
import tools.aqua.bgw.event.MouseEvent

/** Simuliert einen Linksklick auf [component], ohne dass ein Fenster geöffnet wird. */
fun click(component: ComponentView) {
    checkNotNull(component.onMouseClicked) { "Komponente hat keinen Klick-Handler" }
        .invoke(MouseEvent(MouseButtonType.LEFT_BUTTON, 0, 0))
}
