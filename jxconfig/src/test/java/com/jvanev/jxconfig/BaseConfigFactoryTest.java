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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseConfigFactoryTest {
    private static final String TEST_PATH = "classpath:config";

    private final ConfigFactory factory = ConfigFactory.builder(TEST_PATH).build();

    @Nested
    class ValidConfigurations {
        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record BaseConfiguration(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty,

            @ConfigProperty(key = "MissingBooleanProperty", defaultKey = "DefaultBooleanProperty")
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
            @ConfigProperty(key = "BooleanProperty", defaultKey = "Missing")
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

    @Nested
    class FileLoaderTests {
        @ConfigFile(filename = "BaseTestConfiguration.properties")
        public record BaseConfiguration(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty,

            @ConfigProperty(key = "DefaultBooleanProperty")
            boolean anotherBooleanProperty,

            @ConfigProperty(key = "OverridableBooleanProperty")
            boolean overridableBooleanProperty
        ) {
        }

        @Test
        void shouldLoadFromDefaultClasspath() {
            var factory = ConfigFactory.builder().build();
            var config = factory.createConfig(BaseConfiguration.class);

            assertAll(
                () -> assertTrue(config.booleanProperty()),
                () -> assertTrue(config.anotherBooleanProperty()),
                () -> assertFalse(config.overridableBooleanProperty())
            );
        }

        @Test
        void shouldLoadFromExplicitlySetClasspath() {
            var factory = ConfigFactory.builder("classpath:config").build();
            var config = factory.createConfig(BaseConfiguration.class);

            assertAll(
                () -> assertTrue(config.booleanProperty()),
                () -> assertTrue(config.anotherBooleanProperty()),
                () -> assertTrue(config.overridableBooleanProperty())
            );
        }

        @Test
        void shouldLoadFromExplicitlySetDirectoryPath() {
            var dir = "dir:" + System.getProperty("user.dir") + "/src/test/resources/config";
            var factory = ConfigFactory.builder(dir).build();
            var config = factory.createConfig(BaseConfiguration.class);

            assertAll(
                () -> assertTrue(config.booleanProperty()),
                () -> assertTrue(config.anotherBooleanProperty()),
                // Implicitly tests the overriding behavior,
                // the file is also loaded from the root of the resources dir
                () -> assertTrue(config.overridableBooleanProperty())
            );
        }

        @Test
        void shouldOverrideThePropertiesInTheClasspathFile() {
            var classpath = "";
            var dir = System.getProperty("user.dir") + "/src/test/resources/config";
            var factory = ConfigFactory.builder(classpath, dir).build();
            var config = factory.createConfig(BaseConfiguration.class);

            assertAll(
                () -> assertTrue(config.booleanProperty()),
                () -> assertTrue(config.anotherBooleanProperty()),
                () -> assertTrue(config.overridableBooleanProperty())
            );
        }

        @Test
        void shouldHandleClasspathDirSeparatorSuffix() {
            var factory = ConfigFactory.builder("classpath:config/").build();
            var config = factory.createConfig(BaseConfiguration.class);

            assertAll(
                () -> assertTrue(config.booleanProperty()),
                () -> assertTrue(config.anotherBooleanProperty()),
                () -> assertTrue(config.overridableBooleanProperty())
            );
        }

        @ConfigFile(filename = "AltBaseTestConfiguration.properties")
        public record AltBaseConfiguration(
            @ConfigProperty(key = "BooleanProperty")
            boolean booleanProperty,

            @ConfigProperty(key = "DefaultBooleanProperty")
            boolean anotherBooleanProperty,

            @ConfigProperty(key = "OverridableBooleanProperty")
            boolean overridableBooleanProperty
        ) {
        }

        @Test
        void shouldNotThrowWhenClasspathFileDoesNotExist() {
            var dir = "dir:" + System.getProperty("user.dir") + "/src/test/resources/config";
            var factory = ConfigFactory.builder(dir).build();
            var config = factory.createConfig(AltBaseConfiguration.class);

            assertAll(
                () -> assertTrue(config.booleanProperty()),
                () -> assertTrue(config.anotherBooleanProperty()),
                () -> assertTrue(config.overridableBooleanProperty())
            );
        }

        @Test
        void shouldThrowOnMissingOrInvalidPrefix() {
            assertThrows(
                IllegalArgumentException.class,
                () -> ConfigFactory.builder("prefix")
            );
        }
    }
}
