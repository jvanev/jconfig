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
import com.jvanev.jxconfig.annotation.ConfigGroup;
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.annotation.DependsOn;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import com.jvanev.jxconfig.resolver.DependencyChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyConfigurationTest {
    private static final String TEST_PATH = "classpath:config";

    private final ConfigFactory factory = ConfigFactory.builder(TEST_PATH).build();

    @Nested
    class ValidDependencyDeclarationTests {
        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record BasicDependencyConfiguration(
            // Tests whether the value will be correctly read from te fallback key when the dependency is not satisfied
            @ConfigProperty(key = "ProdURL", fallbackKey = "DevURL")
            @DependsOn(key = "Environment", value = "prod")
            String prodUrl,

            // Tests whether the value will be correctly read from the primary key when the dependency is satisfied
            @ConfigProperty(key = "DevURL", fallbackKey = "ProdURL")
            @DependsOn(key = "Environment", value = "dev")
            String devUrl,

            @ConfigProperty(key = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(key = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerPropertyWithSatisfiedDependency,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyWithUnsatisfiedDependency
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectSatisfiedDependencyConfiguration(
            @ConfigProperty(key = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "BooleanTrueProperty")
            boolean configB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOn(property = "ConfigurationB")
            boolean configC
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectUnsatisfiedDependencyConfiguration(
            @ConfigProperty(key = "ConfigurationA")
            boolean configA,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "ConfigurationA")
            boolean configB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOn(property = "ConfigurationB")
            boolean configC
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectMixedSatisfiedDependencyConfiguration(
            @ConfigProperty(key = "ConfigurationB")
            boolean configB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOn(property = "ConfigurationB")
            boolean configC,

            @ConfigProperty(key = "ConfigurationD", defaultValue = "false")
            @DependsOn(property = "ConfigurationC")
            boolean configD,

            @ConfigProperty(key = "ConfigurationE", defaultValue = "false")
            @DependsOn(property = "ConfigurationD")
            boolean configE,

            @ConfigProperty(key = "ConfigurationF", defaultValue = "false")
            @DependsOn(property = "ConfigurationE")
            boolean configF
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DefaultValueSatisfyingDependencyConfiguration(
            @ConfigProperty(key = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyTwo,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "IntegerPropertyTwo", value = "0")
            boolean configB
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record TwoKeysDependingOnTheSameSatisfiedDependency(
            @ConfigProperty(key = "BooleanTrueProperty")
            boolean booleanTrue,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerPropertyOne,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanTrueProperty")
            int integerPropertyTwo
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record TwoKeysDependingOnTheSameUnsatisfiedDependency(
            @ConfigProperty(key = "BooleanFalseProperty")
            boolean booleanTrue,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyOne,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOn(property = "BooleanFalseProperty")
            int integerPropertyTwo
        ) {
        }

        @Test
        void dependentDeclarationWithSatisfiedDependency_ShouldReadFromFile() {
            var config = factory.createConfig(BasicDependencyConfiguration.class);

            assertAll(
                () -> assertEquals(65535, config.integerPropertyWithSatisfiedDependency()),
                () -> assertEquals("https://dev.example.com/", config.devUrl())
            );
        }

        @Test
        void dependentDeclarationWithUnsatisfiedDependency_ShouldReadDefaultValue() {
            var config = factory.createConfig(BasicDependencyConfiguration.class);

            assertAll(
                () -> assertEquals(0, config.integerPropertyWithUnsatisfiedDependency()),
                () -> assertEquals("https://dev.example.com/", config.prodUrl())
            );
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
            @ConfigProperty(key = "UndefinedProperty", defaultValue = "false")
            boolean booleanProperty,

            @ConfigProperty(key = "IntegerPropertyOne")
            @DependsOn(property = "BooleanProperty")
            int integerProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnNonExistentConfigurationProperty(
            @ConfigProperty(key = "BooleanProperty", defaultValue = "false")
            @DependsOn(property = "NonExistent")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnNonExistentConfigurationKey(
            @ConfigProperty(key = "BooleanProperty", defaultValue = "false")
            @DependsOn(key = "NonExistent")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnConfigurationPropertyAndConfigurationKey(
            @ConfigProperty(key = "IntegerPropertyOne")
            int integerProperty,

            @ConfigProperty(key = "BooleanProperty", defaultValue = "false")
            @DependsOn(key = "IntegerPropertyTwo", property = "IntegerPropertyOne")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record CircularDependencyGraph(
            @ConfigProperty(key = "BooleanTrueProperty", defaultValue = "false")
            @DependsOn(property = "IntegerPropertyOne", value = "65535")
            boolean booleanProperty,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
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
        void dependingOnNonExistentProperty_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependingOnNonExistentConfigurationProperty.class)
            );
        }

        @Test
        void dependingOnNonExistentKey_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependingOnNonExistentConfigurationKey.class)
            );
        }

        @Test
        void dependingOnConfigurationPropertyAndConfigurationKey_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependingOnConfigurationPropertyAndConfigurationKey.class)
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

    @Nested
    class CustomDependencyCheckerTests {
        private ConfigFactory configFactory;

        static class CustomChecker implements DependencyChecker {
            @Override
            public boolean check(String dependencyValue, String operator, String requiredValue) {
                return switch (operator) {
                    case ">" -> Integer.parseInt(dependencyValue) > Integer.parseInt(requiredValue);
                    case "<" -> Integer.parseInt(dependencyValue) < Integer.parseInt(requiredValue);
                    case "|" -> {
                        for (var entry : requiredValue.split("\\|")) {
                            if (dependencyValue.equals(entry)) {
                                yield true;
                            }
                        }

                        yield false;
                    }
                    default -> false;
                };
            }
        }

        @BeforeEach
        void setUp() {
            configFactory = ConfigFactory.builder(TEST_PATH)
                .withDependencyChecker(new CustomChecker())
                .build();
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependencyConfiguration(
            @ConfigProperty(key = "IntegerPropertyOne")
            int integerPropertyOne,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOn(property = "IntegerPropertyOne", operator = ">", value = "65534")
            boolean configurationB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOn(key = "IntegerPropertyTwo", operator = "<", value = "65536")
            boolean configurationC,

            @ConfigProperty(key = "ConfigurationE", defaultValue = "false")
            @DependsOn(key = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
            boolean configurationE,

            @ConfigProperty(key = "ConfigurationF", defaultValue = "false")
            @DependsOn(key = "LogLevel", operator = "|", value = "INFO|WARN|ERROR")
            boolean configurationF,

            @ConfigGroup
            @DependsOn(key = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
            ConfigurationGroup configGroup1,

            @ConfigGroup
            @DependsOn(key = "LogLevel", operator = "|", value = "INFO|WARN|ERROR")
            ConfigurationGroup configGroup2
        ) {
            public record ConfigurationGroup(
                @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
                boolean configurationB,

                @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
                boolean configurationC
            ) {
            }
        }

        @Test
        void onCustomOperator_ShouldUseCustomChecker() {
            var config = configFactory.createConfig(DependencyConfiguration.class);

            assertTrue(config.configurationB());
            assertTrue(config.configurationC());
            assertTrue(config.configurationE());
            assertFalse(config.configurationF());
            assertTrue(config.configGroup1().configurationB());
            assertTrue(config.configGroup1().configurationC());
            assertFalse(config.configGroup2().configurationB());
            assertFalse(config.configGroup2().configurationC());
        }

        @Test
        void onCustomOperatorAndMissingCustomChecker_ShouldThrow() {
            var factory = ConfigFactory.builder(TEST_PATH).build();

            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependencyConfiguration.class)
            );
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependencyConfigurationAlt(
            @ConfigProperty(key = "LogLevel")
            System.Logger.Level logLevel,

            @ConfigGroup
            @DependsOn(property = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
            ConfigurationGroup configGroup1
        ) {
            public record ConfigurationGroup(
                @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
                boolean configurationB
            ) {
            }
        }

        @Test
        void onCustomOperatorAndMissingCustomChecker_GroupVersion_ShouldThrow() {
            var factory = ConfigFactory.builder(TEST_PATH).build();

            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependencyConfigurationAlt.class)
            );
        }
    }
}
