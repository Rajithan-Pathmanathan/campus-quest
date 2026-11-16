package com.campusquest

import com.campusquest.story.*
import org.junit.Assert.*
import org.junit.Test

class StoryRulesTest {
    @Test fun `new story config creates a bounded QR token`() {
        val c = LightTheWay.newConfig("g")
        assertEquals("g", c.gameId)
        assertEquals(c.cardId, c.backupCode)
        assertTrue(c.cardId.matches(Regex("[A-Z0-9]{8}")))
    }

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
    @Test fun `maze requires the switchback route and cannot cut through walls`() {
        val box=KeyBox()
        box.move(0f,-.8f)
        assertTrue(box.y >= .80f + KeyBox.RADIUS - .001f)
        fun to(x:Float,y:Float){box.move(x-box.x,y-box.y)}
        to(.89f,.90f);to(.89f,.70f);to(.11f,.70f)
        to(.11f,.56f);to(.89f,.56f)
        to(.89f,.38f);to(.11f,.38f)
        to(.11f,.22f);to(.89f,.22f)
        assertFalse(box.complete)
        to(.89f,.07f)
        assertTrue(box.complete)
    }
    @Test fun `short barriers block diagonal shortcuts and invalid input cannot corrupt position`() {
        val box=KeyBox()
        box.move(Float.NaN,0f);assertEquals(.12f,box.x,0f)
        box.move(.77f,0f);box.move(0f,-.18f)
        box.move(-.7f,0f)
        assertTrue("Short barrier must block crossing at its height",box.x>.52f)
    }
}
