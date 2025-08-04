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
import com.jvanev.jxconfig.annotation.DependsOn;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyConfigurationTest {
    private final String TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/";

    private final ConfigFactory factory = new ConfigFactory(TEST_RESOURCES_DIR + "config");

    @BeforeEach
    void ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        );
    }

    @Nested
    class ValidDependencyDeclarationTests {
        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record BasicDependencyConfiguration(
            @ConfigProperty(name = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(name = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerPropertyWithSatisfiedDependency,

            @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyWithUnsatisfiedDependency
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectSatisfiedDependencyConfiguration(
            @ConfigProperty(name = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(name = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "BooleanTrueProperty")
            boolean configB,

            @ConfigProperty(name = "ConfigurationC", defaultValue = "false")
            @DependsOn(property = "ConfigurationB")
            boolean configC
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectUnsatisfiedDependencyConfiguration(
            @ConfigProperty(name = "ConfigurationA")
            boolean configA,

            @ConfigProperty(name = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "ConfigurationA")
            boolean configB,

            @ConfigProperty(name = "ConfigurationC", defaultValue = "false")
            @DependsOn(property = "ConfigurationB")
            boolean configC
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectMixedSatisfiedDependencyConfiguration(
            @ConfigProperty(name = "ConfigurationB")
            boolean configB,

            @ConfigProperty(name = "ConfigurationC", defaultValue = "false")
            @DependsOn(property = "ConfigurationB")
            boolean configC,

            @ConfigProperty(name = "ConfigurationD", defaultValue = "false")
            @DependsOn(property = "ConfigurationC")
            boolean configD,

            @ConfigProperty(name = "ConfigurationE", defaultValue = "false")
            @DependsOn(property = "ConfigurationD")
            boolean configE,

            @ConfigProperty(name = "ConfigurationF", defaultValue = "false")
            @DependsOn(property = "ConfigurationE")
            boolean configF
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DefaultValueSatisfyingDependencyConfiguration(
            @ConfigProperty(name = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyTwo,

            @ConfigProperty(name = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "IntegerPropertyTwo", value = "0")
            boolean configB
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record TwoKeysDependingOnTheSameSatisfiedDependency(
            @ConfigProperty(name = "BooleanTrueProperty")
            boolean booleanTrue,

            @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerPropertyOne,

            @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerPropertyTwo
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record TwoKeysDependingOnTheSameUnsatisfiedDependency(
            @ConfigProperty(name = "BooleanFalseProperty")
            boolean booleanTrue,

            @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyOne,

            @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyTwo
        ) {
        }

        @Test
        void dependentDeclarationWithSatisfiedDependency_ShouldReadFromFile() {
            var config = factory.createConfig(BasicDependencyConfiguration.class);

            assertEquals(65535, config.integerPropertyWithSatisfiedDependency());
        }

        @Test
        void dependentDeclarationWithUnsatisfiedDependency_ShouldReadDefaultValue() {
            var config = factory.createConfig(BasicDependencyConfiguration.class);

            assertEquals(0, config.integerPropertyWithUnsatisfiedDependency());
        }

        @Test
        void indirectSatisfiedDependencies_ShouldReadFromFile() {
            var config = factory.createConfig(IndirectSatisfiedDependencyConfiguration.class);

            assertTrue(config.configC());
            assertTrue(config.configB());
            assertTrue(config.booleanTrueProperty());
        }

        @Test
        void indirectUnsatisfiedDependencies_ShouldReadDefaultValue() {
            var config = factory.createConfig(IndirectUnsatisfiedDependencyConfiguration.class);

            assertFalse(config.configA());
            assertFalse(config.configB());
            assertFalse(config.configC());
        }

        @Test
        void indirectMixedSatisfiedDependencies_ShouldReadFromAppropriateSource() {
            var config = factory.createConfig(IndirectMixedSatisfiedDependencyConfiguration.class);

            assertTrue(config.configB());
            assertTrue(config.configC());
            assertFalse(config.configD());
            assertFalse(config.configE());
            assertFalse(config.configF());
        }

        @Test
        void dependencySatisfiedByDefaultValue_ShouldReadFromFile() {
            var config = factory.createConfig(DefaultValueSatisfyingDependencyConfiguration.class);

            assertFalse(config.booleanFalseProperty());
            assertEquals(0, config.integerPropertyTwo());
            assertTrue(config.configB());
        }

        @Test
        void twoKeysDependingOnTheSameSatisfiedDependency_ShouldReadFromFile() {
            var config = factory.createConfig(TwoKeysDependingOnTheSameSatisfiedDependency.class);

            assertEquals(65535, config.integerPropertyOne());
            assertEquals(65535, config.integerPropertyTwo());
        }

        @Test
        void twoKeysDependingOnTheSameUnsatisfiedDependency_ShouldReadDefaultValue() {
            var config = factory.createConfig(TwoKeysDependingOnTheSameUnsatisfiedDependency.class);

            assertEquals(0, config.integerPropertyOne());
            assertEquals(0, config.integerPropertyTwo());
        }
    }

    @Nested
    class InvalidDependencyDeclarationTests {
        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependencyWithNoDefaultValue(
            @ConfigProperty(name = "UndefinedProperty", defaultValue = "false")
            boolean booleanProperty,

            @ConfigProperty(name = "IntegerPropertyOne")
            @DependsOn(property = "BooleanProperty")
            int integerProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnNonExistentConfiguration(
            @ConfigProperty(name = "BooleanProperty", defaultValue = "false")
            @DependsOn(property = "NonExistent")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record CircularDependencyGraph(
            @ConfigProperty(name = "BooleanTrueProperty", defaultValue = "false")
            @DependsOn(property = "IntegerPropertyOne", value = "65535")
            boolean booleanProperty,

            @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerProperty
        ) {
        }

        @Test
        void dependencyWithNoDefaultValue_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependencyWithNoDefaultValue.class)
            );
        }

        @Test
        void dependingOnNonExistentParameter_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependingOnNonExistentConfiguration.class)
            );
        }

        @Test
        void circularDependencyGraph_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(CircularDependencyGraph.class)
            );
        }
    }
}
