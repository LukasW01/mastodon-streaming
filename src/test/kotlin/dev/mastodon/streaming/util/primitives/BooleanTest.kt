package dev.mastodon.streaming.util.primitives

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BooleanTest {
    @ParameterizedTest
    @ValueSource(strings = ["1", "true", "TRUE", "t", "yes", "on"])
    fun valuesAreTruthy(value: String) {
        assertTrue(value.isTruthy())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "0", "f", "F", "false", "False", "off", "OFF"])
    fun emptyStringsAndKnownFalseValuesAreFalsy(value: String) {
        assertFalse(value.isTruthy())
    }

    @Test
    fun `null is falsy`() {
        assertFalse(null.isTruthy())
    }
}
