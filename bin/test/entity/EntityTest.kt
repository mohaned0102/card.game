package entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EntityTest {

    @Test
    fun `faculty starts empty and resets to two action points`() {
        val faculty = Faculty("A")
        assertEquals(0, faculty.actionPoints)
        assertEquals(4, faculty.researchActivity.size)
        assertTrue(faculty.researchActivity.values.all { it.isEmpty() })

        faculty.resetActionPoints()
        assertEquals(2, faculty.actionPoints)
    }

    @Test
    fun `addResearch puts card on the stack of its suit`() {
        val faculty = Faculty("A")
        val first = ResearchCard(ResearchTopic.ACE, ResearchArea.SE)
        val second = ResearchCard(ResearchTopic.KING, ResearchArea.SE)
        faculty.addResearch(first)
        faculty.addResearch(second)

        val stack = faculty.researchActivity.getValue(ResearchArea.SE)
        assertEquals(2, stack.size)
        assertSame(second, stack.peek())
        assertTrue(faculty.researchActivity.getValue(ResearchArea.AI).isEmpty())
    }

    @Test
    fun `nextFaculty wraps around clockwise`() {
        val game = FacultyGame(listOf(Faculty("A"), Faculty("B"), Faculty("C")))
        assertEquals("A", game.currentFaculty.name)
        game.nextFaculty()
        game.nextFaculty()
        assertEquals("C", game.currentFaculty.name)
        game.nextFaculty()
        assertEquals("A", game.currentFaculty.name)
    }

    @Test
    fun `game needs two to four faculties`() {
        assertFailsWith<IllegalArgumentException> { FacultyGame(listOf(Faculty("A"))) }
        assertFailsWith<IllegalArgumentException> { FacultyGame(List(5) { Faculty("F$it") }) }
    }

    @Test
    fun `cards are displayed with suit symbol and value`() {
        assertEquals("♥Q", ResearchCard(ResearchTopic.QUEEN, ResearchArea.IT_SECURITY).toString())
        assertEquals("♣10", ResearchCard(ResearchTopic.TEN, ResearchArea.THEORETICAL).toString())
    }
}
