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
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseConfigFactoryTest {
    private static final String TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/";

    private final ConfigFactory factory = ConfigFactory.builder(TEST_RESOURCES_DIR + "config").build();

    @BeforeEach
    void ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        );
    }

    @Nested
    class ValidConfigurations {
        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record BaseConfiguration(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty,

            @ConfigProperty(key = "MissingBooleanProperty", fallbackKey = "DefaultBooleanProperty")
            boolean anotherBooleanProperty,

            @ConfigProperty(key = "MissingProperty", defaultValue = "0xFF")
            int integerProperty
        ) {
        }

        @Test
        void existingProperties_ShouldReadFromPropertiesFile() {
            var config = factory.createConfig(BaseConfiguration.class);

            assertTrue(config.booleanProperty());
        }

        @Test
        void missingPropertyWithDefaultProperty_ShouldReadFromPropertiesFile() {
            var config = factory.createConfig(BaseConfiguration.class);

            assertTrue(config.anotherBooleanProperty());
        }

        @Test
        void missingPropertiesWithDefaultValue_ShouldReadDefaultValue() {
            var config = factory.createConfig(BaseConfiguration.class);

            assertEquals(0xFF, config.integerProperty);
        }
    }

    @Nested
    class InvalidConfigurations {
        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record InvalidBaseConfiguration(
            @ConfigProperty(key = "MissingProperty")
            int integerProperty
        ) {
        }

        @Test
        void missingPropertiesWithNoDefaultValue_ShouldThrow() {
            assertThrows(ConfigurationBuildException.class, () -> factory.createConfig(InvalidBaseConfiguration.class));
        }

        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record MultipleDeclarationsOfTheSameKeyName(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty,

            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty2
        ) {
        }

        @Test
        void multipleDeclarationsOfSameKeyName_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MultipleDeclarationsOfTheSameKeyName.class)
            );
        }

        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record MissingConfigurationParameterAnnotation(
            boolean booleanProperty
        ) {
        }

        @Test
        void missingConfigPropertyAnnotation_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MissingConfigurationParameterAnnotation.class)
            );
        }

        public record MissingConfigFileAnnotation(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty
        ) {
        }

        @Test
        void missingConfigFileAnnotation_ShouldThrow() {
            assertThrows(
                InvalidDeclarationException.class,
                () -> factory.createConfig(MissingConfigFileAnnotation.class)
            );
        }

        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record MultipleConstructorsConfiguration(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty,

            @ConfigProperty(key = "MissingProperty", defaultValue = "0xFF")
            int integerProperty
        ) {
            public MultipleConstructorsConfiguration(int integerProperty) {
                this(false, integerProperty);
            }
        }

        @Test
        void configurationWithMultipleConstructors_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MultipleConstructorsConfiguration.class)
            );
        }

        @ConfigFile(filename = "BaseTestConfiguration")
        public record MissingFileConfiguration(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty
        ) {
        }

        @Test
        void onMissingConfigurationFile_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MissingFileConfiguration.class)
            );
        }

        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record MissingDefaultPropertyConfiguration(
            @ConfigProperty(key = "BooleanProperty", fallbackKey = "Missing")
            boolean booleanProperty
        ) {
        }

        @Test
        void onMissingDefaultProperty_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MissingDefaultPropertyConfiguration.class)
            );
        }
    }
}
