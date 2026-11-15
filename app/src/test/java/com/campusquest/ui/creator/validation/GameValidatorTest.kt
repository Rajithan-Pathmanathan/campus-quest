package com.campusquest.ui.creator.validation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameValidatorTest {
    @Test fun blankTitle_isRejected() = assertNotNull(GameValidator.validateTitle(""))
    @Test fun whitespaceTitle_isRejected() = assertNotNull(GameValidator.validateTitle("    "))
    @Test fun validTitle_isAccepted() = assertNull(GameValidator.validateTitle("Campus Quest 1"))
    @Test fun emptyDescription_okForDraft() = assertNull(GameValidator.validateDescription("", forPublish = false))
    @Test fun emptyDescription_blockedForPublish() = assertNotNull(GameValidator.validateDescription("", forPublish = true))
}