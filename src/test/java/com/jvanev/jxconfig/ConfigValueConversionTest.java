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
package com.jvanev.jxconfig;

import com.jvanev.jxconfig.annotation.ConfigFile;
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValueConversionTest {
    private final String TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/";

    private final ConfigFactory factory = ConfigFactory.builder(TEST_RESOURCES_DIR + "config").build();

    @BeforeEach
    void ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        );
    }

    enum LogLevel {
        DEBUG, INFO
    }

    @Nested
    class CorrectlyFormattedPropertyTests {
        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record CorrectPrimitiveConfiguration(
            @ConfigProperty(name = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(name = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(name = "ByteProperty")
            byte byteProperty,

            @ConfigProperty(name = "ShortProperty")
            short shortProperty,

            @ConfigProperty(name = "IntegerProperty")
            int intProperty,

            @ConfigProperty(name = "HexIntegerProperty")
            int hexIntProperty,

            @ConfigProperty(name = "LongProperty")
            long longProperty,

            @ConfigProperty(name = "FloatProperty")
            float floatProperty,

            @ConfigProperty(name = "DoubleProperty")
            double doubleProperty,

            @ConfigProperty(name = "CharProperty")
            char charProperty
        ) {
        }

        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record CorrectReferenceConfiguration(
            @ConfigProperty(name = "StringProperty")
            String stringProperty,

            @ConfigProperty(name = "EnumProperty")
            LogLevel enumProperty,

            @ConfigProperty(name = "StringArrayProperty")
            List<String> stringListProperty,

            @ConfigProperty(name = "IntegerListProperty")
            List<Integer> integerListProperty,

            @ConfigProperty(name = "IntegerSetProperty")
            Set<Integer> integerSetProperty
        ) {
        }

        @Test
        void shouldSupportPrimitives() {
            var config = factory.createConfig(CorrectPrimitiveConfiguration.class);

            assertTrue(config.booleanTrueProperty());
            assertFalse(config.booleanFalseProperty());
            assertEquals(126, config.byteProperty());
            assertEquals(16584, config.shortProperty());
            assertEquals(65534, config.intProperty());
            assertEquals(255, config.hexIntProperty());
            assertEquals(1234567890, config.longProperty());
            assertEquals(3.14f, config.floatProperty());
            assertEquals(3.14, config.doubleProperty());
            assertEquals('T', config.charProperty());
        }

        @Test
        void shouldSupportBoxedPrimitivesAndOtherReferenceTypes() {
            var config = factory.createConfig(CorrectReferenceConfiguration.class);

            assertEquals("This is a test", config.stringProperty);
            assertEquals(LogLevel.DEBUG, config.enumProperty);
            assertEquals(List.of("this", "is", "a", "test"), config.stringListProperty);
            assertEquals(List.of(1, 2, 3, 4, 56), config.integerListProperty);
            assertEquals(Set.of(1, 2, 3, 4, 56), config.integerSetProperty);
        }
    }

    @Nested
    class IncorrectlyFormattedPropertyTests {
        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record IncorrectPropertyConfiguration(
            @ConfigProperty(name = "MixedCaseTrailingSpaceBooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(name = "MixedCaseTrailingSpaceBooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(name = "TrailingSpaceStringProperty")
            String stringProperty,

            @ConfigProperty(name = "EmptyIntegerArray")
            List<Integer> emptyIntegerArray,

            @ConfigProperty(name = "EmptyStringArray")
            List<String> emptyStringArray
        ) {
        }

        @Test
        void incorrectlyFormattedPropertyValuesShouldStillWork() {
            var config = factory.createConfig(IncorrectPropertyConfiguration.class);

            assertTrue(config.booleanTrueProperty());
            assertFalse(config.booleanFalseProperty());
            assertEquals("This is a test", config.stringProperty());
            assertEquals(List.of(), config.emptyIntegerArray());
            assertEquals(List.of(), config.emptyStringArray());
        }
    }

    @Nested
    class CustomTypeSupportTests {
        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record DateTimeFormatterConfigurationParameter(
            @ConfigProperty(name = "DateTimeFormat")
            DateTimeFormatter formatter
        ) {
        }

        @Test
        void shouldThrowOnUnsupportedReferenceType() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DateTimeFormatterConfigurationParameter.class)
            );
        }

        @Test
        void onAddedSupporter_ShouldSupportCustomReferenceType() {
            var factory = ConfigFactory.builder(TEST_RESOURCES_DIR + "config")
                .withConverter(
                    DateTimeFormatter.class,
                    (type, typeArguments, value) -> DateTimeFormatter.ofPattern(value)
                )
                .build();

            assertDoesNotThrow(() -> {
                factory.createConfig(DateTimeFormatterConfigurationParameter.class);
            });
        }

        @Test
        void onAddedSupporter_ShouldContainUsableCustomReferenceType() {
            var factory = ConfigFactory.builder(TEST_RESOURCES_DIR + "config")
                .withConverter(
                    DateTimeFormatter.class,
                    (type, typeArguments, value) -> DateTimeFormatter.ofPattern(value)
                )
                .withConverter(
                    URL.class,
                    (type, typeArguments, value) -> {
                        try {
                            return URI.create(value).toURL();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    }
                )
                .build();

            var config = factory.createConfig(DateTimeFormatterConfigurationParameter.class);
            var dateTime = LocalDateTime.of(2025, 7, 5, 13, 17);

            assertEquals("05.07.2025 13:17", dateTime.format(config.formatter));
        }
    }

    @Nested
    class InvalidPropertyTests {
        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record InvalidIntegerConfiguration(
            @ConfigProperty(name = "InvalidIntegerProperty")
            int integerProperty
        ) {
        }

        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record InvalidCharConfiguration(
            @ConfigProperty(name = "InvalidCharacterProperty")
            char charProperty
        ) {
        }

        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record InvalidEnumCasingConfiguration(
            @ConfigProperty(name = "EnumUCFirstProperty")
            LogLevel logLevel
        ) {
        }

        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record InvalidIntegerArrayConfiguration(
            @ConfigProperty(name = "InvalidIntegerArrayProperty")
            List<Integer> integerArrayProperty
        ) {
        }

        @Test
        void invalidStringToIntegerConversion_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(InvalidIntegerConfiguration.class)
            );
        }

        @Test
        void invalidStringToCharacterConversion_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(InvalidCharConfiguration.class)
            );
        }

        @Test
        void invalidEnumCasing_shouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(InvalidEnumCasingConfiguration.class)
            );
        }

        @Test
        void invalidStringToIntegerArrayConversion_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(InvalidIntegerArrayConfiguration.class)
            );
        }
    }
}
