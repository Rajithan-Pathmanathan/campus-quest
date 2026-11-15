package com.campusquest.ui.creator.validation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CheckpointValidatorTest {

    @Test fun validRadius_isAccepted() = assertNull(CheckpointValidator.validateRadius("20"))
    @Test fun negativeRadius_isRejected() = assertNotNull(CheckpointValidator.validateRadius("-5"))
    @Test fun textRadius_isRejected() = assertNotNull(CheckpointValidator.validateRadius("abc"))

    @Test fun validLux_isAccepted() = assertNull(CheckpointValidator.validateLux("100", "350"))
    @Test fun minGreaterThanMax_isRejected() = assertNotNull(CheckpointValidator.validateLux("500", "100"))
    @Test fun negativeLux_isRejected() = assertNotNull(CheckpointValidator.validateLux("-1", "100"))

    @Test fun latitudeOutOfRange_isRejected() = assertNotNull(CheckpointValidator.validateLatitude("95"))
}