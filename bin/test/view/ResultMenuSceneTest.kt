package view

import entity.Faculty
import service.RootService
import service.ScoreResult
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultMenuSceneTest {

    private fun score(name: String, points: Int) = ScoreResult(Faculty(name), points, 0, 0, 0, 0)

    @Test
    fun `shows single winner and all rows`() {
        val scene = ResultMenuScene(RootService())
        scene.showScores(listOf(score("A", 20), score("B", 10), score("C", 5)))
        assertTrue(scene.title.text.contains("A gewinnt"))
        assertTrue(scene.rows[1].text.startsWith("2. B: 10 Punkte"))
        assertFalse(scene.rows[3].isVisible)
    }

    @Test
    fun `shows tie`() {
        val scene = ResultMenuScene(RootService())
        scene.showScores(listOf(score("A", 10), score("B", 10)))
        assertTrue(scene.title.text.contains("Gleichstand"))
    }

    @Test
    fun `turn switch names next faculty`() {
        val scene = TurnSwitchMenuScene()
        scene.showFaculty("Bochum", "")
        assertTrue(scene.infoLabel.text.startsWith("Bochum ist am Zug"))
    }
}
