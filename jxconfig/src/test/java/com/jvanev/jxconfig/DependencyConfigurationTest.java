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
import com.jvanev.jxconfig.annotation.ConfigNamespace;
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.annotation.DependsOnKey;
import com.jvanev.jxconfig.annotation.DependsOnProperty;
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
    private static final String TEST_PATH = "config";

    private final ConfigFactory factory = ConfigFactory.builder()
        .withClasspathDir(TEST_PATH)
        .build();

    @Nested
    class ValidDependencyDeclarationTests {
        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record BasicDependencyConfiguration(
            // Tests whether the value will be correctly read from te fallback key when the dependency is not satisfied
            @ConfigProperty(key = "ProdURL", defaultKey = "DevURL")
            @DependsOnProperty(name = "Environment", value = "prod")
            String prodUrl,

            // Tests whether the value will be correctly read from the primary key when the dependency is satisfied
            @ConfigProperty(key = "DevURL", defaultKey = "ProdURL")
            @DependsOnKey(name = "Environment", value = "dev")
            String devUrl,

            @ConfigProperty(key = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(key = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOnProperty(name = "BooleanTrueProperty")
            int integerPropertyWithSatisfiedDependency,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOnProperty(name = "BooleanFalseProperty")
            int integerPropertyWithUnsatisfiedDependency
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectSatisfiedDependencyConfiguration(
            @ConfigProperty(key = "BooleanTrueProperty")
            boolean booleanTrueProperty,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOnProperty(name = "BooleanTrueProperty")
            boolean configB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationB")
            boolean configC
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectUnsatisfiedDependencyConfiguration(
            @ConfigProperty(key = "ConfigurationA")
            boolean configA,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationA")
            boolean configB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationB")
            boolean configC
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record IndirectMixedSatisfiedDependencyConfiguration(
            @ConfigProperty(key = "ConfigurationB")
            boolean configB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationB")
            boolean configC,

            @ConfigProperty(key = "ConfigurationD", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationC")
            boolean configD,

            @ConfigProperty(key = "ConfigurationE", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationD")
            boolean configE,

            @ConfigProperty(key = "ConfigurationF", defaultValue = "false")
            @DependsOnProperty(name = "ConfigurationE")
            boolean configF
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DefaultValueSatisfyingDependencyConfiguration(
            @ConfigProperty(key = "BooleanFalseProperty")
            boolean booleanFalseProperty,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOnProperty(name = "BooleanFalseProperty")
            int integerPropertyTwo,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOnProperty(name = "IntegerPropertyTwo", value = "0")
            boolean configB
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record TwoKeysDependingOnTheSameSatisfiedDependency(
            @ConfigProperty(key = "BooleanTrueProperty")
            boolean booleanTrue,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOnProperty(name = "BooleanTrueProperty")
            int integerPropertyOne,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOnProperty(name = "BooleanTrueProperty")
            int integerPropertyTwo
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record TwoKeysDependingOnTheSameUnsatisfiedDependency(
            @ConfigProperty(key = "BooleanFalseProperty")
            boolean booleanTrue,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOnProperty(name = "BooleanFalseProperty")
            int integerPropertyOne,

            @ConfigProperty(key = "IntegerPropertyTwo", defaultValue = "0")
            @DependsOnProperty(name = "BooleanFalseProperty")
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

            assertAll(
                () -> assertTrue(config.configC()),
                () -> assertTrue(config.configB()),
                () -> assertTrue(config.booleanTrueProperty())
            );
        }

        @Test
        void indirectUnsatisfiedDependencies_ShouldReadDefaultValue() {
            var config = factory.createConfig(IndirectUnsatisfiedDependencyConfiguration.class);

            assertAll(
                () -> assertFalse(config.configA()),
                () -> assertFalse(config.configB()),
                () -> assertFalse(config.configC())
            );
        }

        @Test
        void indirectMixedSatisfiedDependencies_ShouldReadFromAppropriateSource() {
            var config = factory.createConfig(IndirectMixedSatisfiedDependencyConfiguration.class);

            assertAll(
                () -> assertTrue(config.configB()),
                () -> assertTrue(config.configC()),
                () -> assertFalse(config.configD()),
                () -> assertFalse(config.configE()),
                () -> assertFalse(config.configF())
            );
        }

        @Test
        void dependencySatisfiedByDefaultValue_ShouldReadFromFile() {
            var config = factory.createConfig(DefaultValueSatisfyingDependencyConfiguration.class);

            assertAll(
                () -> assertFalse(config.booleanFalseProperty()),
                () -> assertEquals(0, config.integerPropertyTwo()),
                () -> assertTrue(config.configB())
            );
        }

        @Test
        void twoKeysDependingOnTheSameSatisfiedDependency_ShouldReadFromFile() {
            var config = factory.createConfig(TwoKeysDependingOnTheSameSatisfiedDependency.class);

            assertAll(
                () -> assertEquals(65535, config.integerPropertyOne()),
                () -> assertEquals(65535, config.integerPropertyTwo())
            );
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
            @DependsOnProperty(name = "BooleanProperty")
            int integerProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnNonExistentConfigurationProperty(
            @ConfigProperty(key = "BooleanProperty", defaultValue = "false")
            @DependsOnProperty(name = "NonExistent")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnNonExistentConfigurationKey(
            @ConfigProperty(key = "BooleanProperty", defaultValue = "false")
            @DependsOnKey(name = "NonExistent")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependingOnConfigurationPropertyAndConfigurationKey(
            @ConfigProperty(key = "IntegerPropertyOne")
            int integerProperty,

            @ConfigProperty(key = "BooleanProperty", defaultValue = "false")
            @DependsOnKey(name = "IntegerPropertyTwo")
            @DependsOnProperty(name = "IntegerPropertyOne")
            boolean booleanProperty
        ) {
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record CircularDependencyGraph(
            @ConfigProperty(key = "BooleanTrueProperty", defaultValue = "false")
            @DependsOnProperty(name = "IntegerPropertyOne", value = "65535")
            boolean booleanProperty,

            @ConfigProperty(key = "IntegerPropertyOne", defaultValue = "0")
            @DependsOnProperty(name = "BooleanTrueProperty")
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
            configFactory = ConfigFactory.builder()
                .withClasspathDir(TEST_PATH)
                .withDependencyChecker(new CustomChecker())
                .build();
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependencyConfiguration(
            @ConfigProperty(key = "IntegerPropertyOne")
            int integerPropertyOne,

            @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
            @DependsOnProperty(name = "IntegerPropertyOne", operator = ">", value = "65534")
            boolean configurationB,

            @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
            @DependsOnKey(name = "IntegerPropertyTwo", operator = "<", value = "65536")
            boolean configurationC,

            @ConfigProperty(key = "ConfigurationE", defaultValue = "false")
            @DependsOnKey(name = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
            boolean configurationE,

            @ConfigProperty(key = "ConfigurationF", defaultValue = "false")
            @DependsOnKey(name = "LogLevel", operator = "|", value = "INFO|WARN|ERROR")
            boolean configurationF,

            @ConfigNamespace
            @DependsOnKey(name = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
            ConfigurationNamespace configNamespace1,

            @ConfigNamespace
            @DependsOnKey(name = "LogLevel", operator = "|", value = "INFO|WARN|ERROR")
            ConfigurationNamespace configNamespace2
        ) {
            public record ConfigurationNamespace(
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

            assertAll(
                () -> assertTrue(config.configurationB()),
                () -> assertTrue(config.configurationC()),
                () -> assertTrue(config.configurationE()),
                () -> assertFalse(config.configurationF()),
                () -> assertTrue(config.configNamespace1().configurationB()),
                () -> assertTrue(config.configNamespace1().configurationC()),
                () -> assertFalse(config.configNamespace2().configurationB()),
                () -> assertFalse(config.configNamespace2().configurationC())
            );
        }

        @Test
        void onCustomOperatorAndMissingCustomChecker_ShouldThrow() {
            var factory = ConfigFactory.builder()
                .withClasspathDir(TEST_PATH)
                .build();

            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependencyConfiguration.class)
            );
        }

        @ConfigFile(filename = "DependencyTestConfiguration.properties")
        public record DependencyConfigurationAlt(
            @ConfigProperty(key = "LogLevel")
            System.Logger.Level logLevel,

            @ConfigNamespace
            @DependsOnProperty(name = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
            ConfigurationNamespace configNamespace1
        ) {
            public record ConfigurationNamespace(
                @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
                boolean configurationB
            ) {
            }
        }

        @Test
        void onCustomOperatorAndMissingCustomChecker_NamespaceVersion_ShouldThrow() {
            var factory = ConfigFactory.builder()
                .withClasspathDir(TEST_PATH)
                .build();

            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(DependencyConfigurationAlt.class)
            );
        }
    }
}
