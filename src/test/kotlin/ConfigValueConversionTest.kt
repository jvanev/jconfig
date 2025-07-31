/*
 * Copyright 2025 Georgi Vanev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jvanev.kconfig

import com.jvanev.kconfig.annotation.ConfigFile
import com.jvanev.kconfig.annotation.ConfigProperty
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ConfigValueConversionTest {
    private val factory = ConfigFactory(TEST_RESOURCES_DIR + "config")

    @BeforeEach
    fun ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        )
    }

    enum class LogLevel {
        DEBUG, INFO
    }

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class CorrectPrimitiveConfiguration(
        @ConfigProperty("BooleanTrueProperty")
        val booleanTrueProperty: Boolean,

        @ConfigProperty("BooleanFalseProperty")
        val booleanFalseProperty: Boolean,

        @ConfigProperty("ByteProperty")
        val byteProperty: Byte,

        @ConfigProperty("ShortProperty")
        val shortProperty: Short,

        @ConfigProperty("IntegerProperty")
        val intProperty: Int,

        @ConfigProperty("HexIntegerProperty")
        val hexIntProperty: Int,

        @ConfigProperty("LongProperty")
        val longProperty: Long,

        @ConfigProperty("FloatProperty")
        val floatProperty: Float,

        @ConfigProperty("DoubleProperty")
        val doubleProperty: Double,

        @ConfigProperty("CharProperty")
        val charProperty: Char,
    )

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class CorrectReferenceConfiguration(
        @ConfigProperty("StringProperty")
        val stringProperty: String,

        @ConfigProperty("EnumProperty")
        val enumProperty: LogLevel,

        @ConfigProperty("StringArrayProperty")
        val stringListProperty: List<String>,

        @ConfigProperty("IntegerListProperty")
        val integerListProperty: List<Int>,

        @ConfigProperty("IntegerSetProperty")
        val integerSetProperty: Set<Int>,
    )

    @Nested
    inner class CorrectlyFormattedPropertyTests {
        @Test
        fun shouldSupportPrimitives() {
            val config = factory.createConfig(CorrectPrimitiveConfiguration::class.java)

            assertTrue(config.booleanTrueProperty)
            assertFalse(config.booleanFalseProperty)
            assertEquals(126, config.byteProperty)
            assertEquals(16584, config.shortProperty)
            assertEquals(65534, config.intProperty)
            assertEquals(255, config.hexIntProperty)
            assertEquals(1234567890, config.longProperty)
            assertEquals(3.14f, config.floatProperty)
            assertEquals(3.14, config.doubleProperty)
            assertEquals('T', config.charProperty)
        }

        @Test
        fun shouldSupportBoxedPrimitivesAndOtherReferenceTypes() {
            val config = factory.createConfig(CorrectReferenceConfiguration::class.java)

            assertEquals("This is a test", config.stringProperty)
            assertEquals(LogLevel.DEBUG, config.enumProperty)
            assertEquals(listOf("this", "is", "a", "test"), config.stringListProperty)
            assertEquals(listOf(1, 2, 3, 4, 56), config.integerListProperty)
            assertEquals(setOf(1, 2, 3, 4, 56), config.integerSetProperty)
        }
    }

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class IncorrectPropertyConfiguration(
        @ConfigProperty("MixedCaseTrailingSpaceBooleanTrueProperty")
        val booleanTrueProperty: Boolean,

        @ConfigProperty("MixedCaseTrailingSpaceBooleanFalseProperty")
        val booleanFalseProperty: Boolean,

        @ConfigProperty("TrailingSpaceStringProperty")
        val stringProperty: String,

        @ConfigProperty("EmptyIntegerArray")
        val emptyIntegerArray: List<Int>,

        @ConfigProperty("EmptyStringArray")
        val emptyStringArray: List<String>,
    )

    @Nested
    inner class IncorrectlyFormattedPropertyTests {
        @Test
        fun incorrectlyFormattedPropertyValuesShouldStillWork() {
            val config = factory.createConfig(IncorrectPropertyConfiguration::class.java)

            assertTrue(config.booleanTrueProperty)
            assertFalse(config.booleanFalseProperty)
            assertEquals("This is a test", config.stringProperty)
            assertEquals(emptyList<Int>(), config.emptyIntegerArray)
            assertEquals(emptyList<String>(), config.emptyStringArray)
        }
    }

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class DateTimeFormatterConfigurationParameter(
        @ConfigProperty("DateTimeFormat")
        val formatter: DateTimeFormatter,
    )

    @Nested
    inner class CustomTypeSupportTests {
        @Test
        fun shouldThrowOnUnsupportedReferenceType() {
            assertThrows<ConfigurationBuildException> {
                factory.createConfig(DateTimeFormatterConfigurationParameter::class.java)
            }
        }

        @Test
        fun onAddedSupporter_ShouldSupportCustomReferenceType() {
            val factory = ConfigFactory(TEST_RESOURCES_DIR + "config")
            factory.addValueConverter(DateTimeFormatter::class.java) { value, type, _ ->
                DateTimeFormatter.ofPattern(value)
            }

            assertDoesNotThrow {
                factory.createConfig(DateTimeFormatterConfigurationParameter::class.java)
            }
        }

        @Test
        fun onAddedSupporter_ShouldContainUsableCustomReferenceType() {
            val factory = ConfigFactory(TEST_RESOURCES_DIR + "config")
            factory.addValueConverter(DateTimeFormatter::class.java) { value, type, _ ->
                DateTimeFormatter.ofPattern(value)
            }
            factory.addValueConverter(URL::class.java) { value, type, _ ->
                URI.create(value).toURL()
            }

            val config = factory.createConfig(DateTimeFormatterConfigurationParameter::class.java)
            val dateTime = LocalDateTime.of(2025, 7, 5, 13, 17)

            assertEquals("05.07.2025 13:17", dateTime.format(config.formatter))
        }
    }

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class InvalidIntegerConfiguration(
        @ConfigProperty("InvalidIntegerProperty")
        val integerProperty: Int,
    )

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class InvalidCharConfiguration(
        @ConfigProperty("InvalidCharacterProperty")
        val charProperty: Char,
    )

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class InvalidEnumCasingConfiguration(
        @ConfigProperty("EnumUCFirstProperty")
        val logLevel: LogLevel,
    )

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class InvalidIntegerArrayConfiguration(
        @ConfigProperty("InvalidIntegerArrayProperty")
        val integerArrayProperty: List<Int>,
    )

    @Nested
    inner class InvalidPropertyTests {
        @Test
        fun invalidStringToIntegerConversion_ShouldThrow() {
            assertThrows<ConfigurationBuildException> {
                factory.createConfig(InvalidIntegerConfiguration::class.java)
            }
        }

        @Test
        fun invalidStringToCharacterConversion_ShouldThrow() {
            assertThrows<ConfigurationBuildException> {
                factory.createConfig(InvalidCharConfiguration::class.java)
            }
        }

        @Test
        fun invalidEnumCasing_shouldThrow() {
            assertThrows<ConfigurationBuildException> {
                factory.createConfig(InvalidEnumCasingConfiguration::class.java)
            }
        }

        @Test
        fun invalidStringToIntegerArrayConversion_ShouldThrow() {
            assertThrows<ConfigurationBuildException> {
                factory.createConfig(InvalidIntegerArrayConfiguration::class.java)
            }
        }
    }
}

private val TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/"
