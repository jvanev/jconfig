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
import com.jvanev.kconfig.annotation.DependsOn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths

class DependencyConfigurationTest {
    private val factory = ConfigFactory(TEST_RESOURCES_DIR + "config")

    @BeforeEach
    fun ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        )
    }

    @ConfigFile("DependencyTestConfiguration.properties")
    data class BasicDependencyConfiguration(
        @ConfigProperty("BooleanTrueProperty")
        val booleanTrueProperty: Boolean,

        @ConfigProperty("BooleanFalseProperty")
        val booleanFalseProperty: Boolean,

        @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
        @DependsOn("BooleanTrueProperty")
        val integerPropertyWithSatisfiedDependency: Int,

        @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
        @DependsOn("BooleanFalseProperty")
        val integerPropertyWithUnsatisfiedDependency: Int,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class IndirectSatisfiedDependencyConfiguration(
        @ConfigProperty("BooleanTrueProperty")
        val booleanTrueProperty: Boolean,

        @ConfigProperty(name = "ConfigurationB", defaultValue = "False")
        @DependsOn("BooleanTrueProperty")
        val configB: Boolean,

        @ConfigProperty(name = "ConfigurationC", defaultValue = "False")
        @DependsOn("ConfigurationB")
        val configC: Boolean,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class IndirectUnsatisfiedDependencyConfiguration(
        @ConfigProperty("ConfigurationA")
        val configA: Boolean,

        @ConfigProperty(name = "ConfigurationB", defaultValue = "False")
        @DependsOn("ConfigurationA")
        val configB: Boolean,

        @ConfigProperty(name = "ConfigurationC", defaultValue = "False")
        @DependsOn("ConfigurationB")
        val configC: Boolean,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class IndirectMixedSatisfiedDependencyConfiguration(
        @ConfigProperty("ConfigurationB")
        val configB: Boolean,

        @ConfigProperty(name = "ConfigurationC", defaultValue = "False")
        @DependsOn("ConfigurationB")
        val configC: Boolean,

        @ConfigProperty(name = "ConfigurationD", defaultValue = "False")
        @DependsOn("ConfigurationC")
        val configD: Boolean,

        @ConfigProperty(name = "ConfigurationE", defaultValue = "False")
        @DependsOn("ConfigurationD")
        val configE: Boolean,

        @ConfigProperty(name = "ConfigurationF", defaultValue = "False")
        @DependsOn("ConfigurationE")
        val configF: Boolean,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class DefaultValueSatisfyingDependencyConfiguration(
        @ConfigProperty("BooleanFalseProperty")
        val booleanFalseProperty: Boolean,

        @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
        @DependsOn("BooleanFalseProperty")
        val integerPropertyTwo: Int,

        @ConfigProperty(name = "ConfigurationB", defaultValue = "False")
        @DependsOn(property = "IntegerPropertyTwo", value = "0")
        val configB: Boolean,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class TwoKeysDependingOnTheSameSatisfiedDependency(
        @ConfigProperty("BooleanTrueProperty")
        val booleanTrue: Boolean,

        @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
        @DependsOn("BooleanTrueProperty")
        val integerPropertyOne: Int,

        @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
        @DependsOn("BooleanTrueProperty")
        val integerPropertyTwo: Int,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class TwoKeysDependingOnTheSameUnsatisfiedDependency(
        @ConfigProperty("BooleanFalseProperty")
        val booleanTrue: Boolean,

        @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
        @DependsOn("BooleanFalseProperty")
        val integerPropertyOne: Int,

        @ConfigProperty(name = "IntegerPropertyTwo", defaultValue = "0")
        @DependsOn("BooleanFalseProperty")
        val integerPropertyTwo: Int,
    )

    @Nested
    inner class ValidDependencyDeclarationTests {
        @Test
        fun dependentDeclarationWithSatisfiedDependency_ShouldReadFromFile() {
            val config = factory.createConfig(BasicDependencyConfiguration::class)

            assertEquals(65535, config.integerPropertyWithSatisfiedDependency)
        }

        @Test
        fun dependentDeclarationWithUnsatisfiedDependency_ShouldReadDefaultValue() {
            val config = factory.createConfig(BasicDependencyConfiguration::class)

            assertEquals(0, config.integerPropertyWithUnsatisfiedDependency)
        }

        @Test
        fun indirectSatisfiedDependencies_ShouldReadFromFile() {
            val config = factory.createConfig(IndirectSatisfiedDependencyConfiguration::class)

            assertTrue(config.configC)
            assertTrue(config.configB)
            assertTrue(config.booleanTrueProperty)
        }

        @Test
        fun indirectUnsatisfiedDependencies_ShouldReadDefaultValue() {
            val config = factory.createConfig(IndirectUnsatisfiedDependencyConfiguration::class)

            assertFalse(config.configA)
            assertFalse(config.configB)
            assertFalse(config.configC)
        }

        @Test
        fun indirectMixedSatisfiedDependencies_ShouldReadFromAppropriateSource() {
            val config = factory.createConfig(IndirectMixedSatisfiedDependencyConfiguration::class)

            assertTrue(config.configB)
            assertTrue(config.configC)
            assertFalse(config.configD)
            assertFalse(config.configE)
            assertFalse(config.configF)
        }

        @Test
        fun dependencySatisfiedByDefaultValue_ShouldReadFromFile() {
            val config = factory.createConfig(DefaultValueSatisfyingDependencyConfiguration::class)

            assertFalse(config.booleanFalseProperty)
            assertEquals(0, config.integerPropertyTwo)
            assertTrue(config.configB)
        }

        @Test
        fun twoKeysDependingOnTheSameSatisfiedDependency_ShouldReadFromFile() {
            val config = factory.createConfig(TwoKeysDependingOnTheSameSatisfiedDependency::class)

            assertEquals(65535, config.integerPropertyOne)
            assertEquals(65535, config.integerPropertyTwo)
        }

        @Test
        fun twoKeysDependingOnTheSameUnsatisfiedDependency_ShouldReadDefaultValue() {
            val config = factory.createConfig(TwoKeysDependingOnTheSameUnsatisfiedDependency::class)

            assertEquals(0, config.integerPropertyOne)
            assertEquals(0, config.integerPropertyTwo)
        }
    }

    @ConfigFile("DependencyTestConfiguration.properties")
    data class DependencyWithNoDefaultValue(
        @ConfigProperty(name = "UndefinedProperty", defaultValue = "False")
        val booleanProperty: Boolean = false,

        @ConfigProperty(name = "IntegerPropertyOne")
        @DependsOn("BooleanProperty")
        val integerProperty: Int,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class DependingOnNonExistentConfiguration(
        @ConfigProperty(name = "BooleanProperty", defaultValue = "False")
        @DependsOn("NonExistent")
        val booleanProperty: Boolean,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class CircularDependencyGraph(
        @ConfigProperty(name = "BooleanTrueProperty", defaultValue = "False")
        @DependsOn(property = "IntegerPropertyOne", value = "65535")
        val booleanProperty: Boolean,

        @ConfigProperty(name = "IntegerPropertyOne", defaultValue = "0")
        @DependsOn("BooleanTrueProperty")
        val integerProperty: Int,
    )

    @Nested
    inner class InvalidDependencyDeclarationTests {
        @Test
        fun dependencyWithNoDefaultValue_ShouldThrow() {
            assertThrows<IllegalArgumentException> {
                factory.createConfig(DependencyWithNoDefaultValue::class)
            }
        }

        @Test
        fun dependingOnNonExistentParameter_ShouldThrow() {
            assertThrows<RuntimeException> {
                factory.createConfig(DependingOnNonExistentConfiguration::class)
            }
        }

        @Test
        fun circularDependencyGraph_ShouldThrow() {
            assertThrows<IllegalArgumentException> {
                factory.createConfig(CircularDependencyGraph::class)
            }
        }
    }
}

private val TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/"
