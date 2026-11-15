package com.campusquest

import com.campusquest.story.*
import org.junit.Assert.*
import org.junit.Test

class StoryRulesTest {
    @Test fun `card input is scoped versioned and bounded`() {
        val c = StoryConfigEntity("g", "card", "AB12CD34", 0f)
        assertTrue(LightTheWay.matchesCard(LightTheWay.qr(c), c))
        assertFalse(LightTheWay.matchesCard("https://example.com", c))
        assertFalse(LightTheWay.matchesCard(LightTheWay.qr(c.copy(gameId="other")), c))
        assertFalse(LightTheWay.matchesCard("x".repeat(1000), c))
        assertTrue(LightTheWay.matchesCode(" ab12cd34 ", c))
        assertFalse(LightTheWay.matchesCode("wrong", c))
    }
    @Test fun `hold resets on interruption`() {
        val hold = ContinuousHold(1000)
        assertEquals(0f, hold.update(true, 0), 0f)
        assertEquals(0.5f, hold.update(true, 500), 0f)
        assertEquals(0f, hold.update(false, 600), 0f)
        assertEquals(0f, hold.update(true, 900), 0f)
        assertEquals(1f, hold.update(true, 1900), 0f)
    }
    @Test fun `maze walls cannot be crossed and route reaches exit`() {
        val box = KeyBox()
        box.move(0.7f,0f)
        assertTrue(box.x < 0.62f)
        box.move(-1f,0f)
        box.move(0f,-0.05f)
        box.move(0.42f,0f)
        box.move(0f,-0.72f)
        box.move(0.45f,0f)
        assertTrue(box.complete)
    }
}
