package view

import entity.ResearchArea
import entity.ResearchCard
import entity.ResearchTopic
import service.RootService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** GUI-Tests: Klicks auf Komponenten der [GameScene] und Prüfung von Anzeige und Spielzustand. */
class GameSceneTest {

    private lateinit var root: RootService
    private lateinit var scene: GameScene
    private val game get() = root.currentGame!!

    @BeforeTest
    fun setUp() {
        root = RootService()
        scene = GameScene(root)
        root.addRefreshable(scene)
        root.gameService.startNewGame(listOf("A", "B"))
        scene.revealHand()
    }

    @Test
    fun `hand is hidden at turn start and shown after reveal`() {
        root.gameService.endTurn()
        assertTrue(scene.handButtons.none { it.isVisible })
        assertEquals("Hand verdeckt", scene.handTitle.text)

        scene.revealHand()
        assertEquals(5, scene.handButtons.count { it.isVisible })
        assertTrue(scene.handTitle.text.contains("B"))
    }

    @Test
    fun `status shows current faculty and action points`() {
        assertTrue(scene.statusLabel.text.contains("Am Zug: A"))
        assertTrue(scene.statusLabel.text.contains("Aktionspunkte: 2"))
    }

    @Test
    fun `literature shows six cards`() {
        assertEquals(6, scene.literatureButtons.count { it.isVisible })
        assertSame(game.centralLiterature[0], scene.literatureButtons[0].card)
    }

    @Test
    fun `selecting a hand card highlights it and toggles off again`() {
        click(scene.handButtons[0])
        assertTrue(scene.handButtons[0].isSelected)
        click(scene.handButtons[0])
        assertFalse(scene.handButtons[0].isSelected)
    }

    @Test
    fun `advance research via clicks`() {
        val card = scene.handButtons[0].card!!
        click(scene.handButtons[0])
        click(scene.advanceButton)

        assertSame(card, game.faculties[0].researchActivity.getValue(card.suit).peek())
        assertTrue(scene.statusLabel.text.contains("Aktionspunkte: 1"))
        assertEquals(4, scene.handButtons.count { it.isVisible })
        assertTrue(scene.handButtons.none { it.isSelected }) // Auswahl zurückgesetzt
    }

    @Test
    fun `action without selection shows error message`() {
        click(scene.advanceButton)
        assertTrue(scene.messageLabel.text.contains("Handkarte"))
        assertEquals(2, game.currentFaculty.actionPoints)
    }

    @Test
    fun `rule violation is shown instead of crashing`() {
        click(scene.panels[0].stackButtons.getValue(ResearchArea.AI)) // eigener, leerer Stapel
        click(scene.publishButton)
        assertTrue(scene.messageLabel.text.contains("mindestens 2"))
    }

    @Test
    fun `publish own stack via clicks`() {
        repeat(2) { game.faculties[0].addResearch(ResearchCard(ResearchTopic.ACE, ResearchArea.SE)) }
        click(scene.panels[0].stackButtons.getValue(ResearchArea.SE))
        click(scene.publishButton)

        assertEquals(4, game.faculties[0].researchPoints) // Poster 3 + 1 Erstpublikation
        assertEquals("B", game.currentFaculty.name)        // 2 AP verbraucht → Zugwechsel
    }

    @Test
    fun `cannot publish stack of another faculty`() {
        repeat(2) { game.faculties[1].addResearch(ResearchCard(ResearchTopic.ACE, ResearchArea.SE)) }
        click(scene.panels[1].stackButtons.getValue(ResearchArea.SE))
        click(scene.publishButton)
        assertTrue(scene.messageLabel.text.contains("eigene"))
    }

    @Test
    fun `slow down competition via clicks`() {
        game.faculties[1].addResearch(ResearchCard(ResearchTopic.KING, ResearchArea.AI))
        scene.refreshAfterAction()
        click(scene.panels[1].stackButtons.getValue(ResearchArea.AI))
        click(scene.slowDownButton)
        assertTrue(game.faculties[1].researchActivity.getValue(ResearchArea.AI).isEmpty())
    }

    @Test
    fun `use literature via clicks`() {
        val card = scene.literatureButtons[3].card!!
        click(scene.literatureButtons[3])
        click(scene.literatureButton)
        assertSame(card, game.faculties[0].researchActivity.getValue(card.suit).peek())
        assertEquals(5, scene.literatureButtons.count { it.isVisible })
    }

    @Test
    fun `strategy shows two cards to choose from`() {
        click(scene.handButtons[0])
        click(scene.strategyButton)
        assertTrue(scene.strategyTitle.isVisible)
        assertEquals(2, scene.strategyButtons.count { it.isVisible })

        val kept = scene.strategyButtons[1].card!!
        click(scene.strategyButtons[1])
        assertTrue(kept in game.currentFaculty.handCards)
        assertFalse(scene.strategyTitle.isVisible)
    }

    @Test
    fun `end turn button passes turn to next faculty`() {
        click(scene.endTurnButton)
        assertTrue(scene.statusLabel.text.contains("Am Zug: B"))
        assertTrue(scene.panels[1].nameLabel.text.startsWith("▶"))
    }

    @Test
    fun `stack buttons show cards in order`() {
        game.faculties[0].addResearch(ResearchCard(ResearchTopic.TWO, ResearchArea.AI))
        game.faculties[0].addResearch(ResearchCard(ResearchTopic.KING, ResearchArea.AI))
        scene.refreshAfterAction()
        assertTrue(scene.panels[0].stackButtons.getValue(ResearchArea.AI).text.endsWith("(2): 2 K"))
    }

    @Test
    fun `only panels of existing faculties are visible`() {
        assertTrue(scene.panels[1].nameLabel.isVisible)
        assertFalse(scene.panels[2].nameLabel.isVisible)
    }

    @Test
    fun `log shows latest actions`() {
        click(scene.ideaButton)
        assertTrue(scene.logLabels.any { it.text.contains("A gewinnt eine neue Idee") })
    }
}
