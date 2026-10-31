package view

import service.RootService
import service.TestRefreshable
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StartMenuSceneTest {

    private lateinit var root: RootService
    private lateinit var scene: StartMenuScene

    @BeforeTest
    fun setUp() {
        root = RootService()
        root.addRefreshable(TestRefreshable())
        scene = StartMenuScene(root)
    }

    private fun enterNames(vararg names: String) =
        scene.nameFields.forEachIndexed { i, field -> field.text = names.getOrElse(i) { "" } }

    @Test
    fun `start button creates game in entered order`() {
        enterNames("Dortmund", "Bochum", "", "Essen")
        click(scene.startButton)
        assertEquals(listOf("Dortmund", "Bochum", "Essen"), root.currentGame!!.faculties.map { it.name })
        assertEquals("", scene.errorLabel.text)
    }

    @Test
    fun `move up button swaps order`() {
        enterNames("A", "B", "C")
        click(scene.moveUpButtons[1]) // Feld 3 nach oben
        assertEquals(listOf("A", "C", "B"), scene.nameFields.take(3).map { it.text })
    }

    @Test
    fun `shuffle keeps all names and moves empty fields to the end`() {
        enterNames("A", "", "B", "C")
        click(scene.shuffleButton)
        assertEquals(setOf("A", "B", "C"), scene.nameFields.take(3).map { it.text }.toSet())
        assertEquals("", scene.nameFields[3].text)
    }

    @Test
    fun `too few players shows error and starts no game`() {
        enterNames("Allein")
        click(scene.startButton)
        assertNull(root.currentGame)
        assertTrue(scene.errorLabel.text.isNotEmpty())
    }

    @Test
    fun `duplicate names show error`() {
        enterNames("A", "A")
        click(scene.startButton)
        assertNull(root.currentGame)
        assertTrue(scene.errorLabel.text.contains("verschieden"))
    }

    @Test
    fun `random order checkbox still uses all names`() {
        enterNames("A", "B", "C", "D")
        scene.randomAtStart.isChecked = true
        click(scene.startButton)
        assertEquals(setOf("A", "B", "C", "D"), root.currentGame!!.faculties.map { it.name }.toSet())
    }
}
