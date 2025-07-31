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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class BaseConfigFactoryTest {
    private val factory: ConfigFactory = ConfigFactory(TEST_RESOURCES_DIR + "config")

    @BeforeEach
    fun ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        )
    }

    @ConfigFile("BaseTestConfiguration.properties")
    data class BaseConfiguration(
        @ConfigProperty("BooleanProperty")
        val booleanProperty: Boolean,

        @ConfigProperty(name = "MissingProperty", defaultValue = "0xFF")
        val integerProperty: Int,
    )

    @Test
    fun existingProperties_ShouldReadFromPropertiesFile() {
        val config = factory.createConfig(BaseConfiguration::class.java)

        assertTrue(config.booleanProperty)
    }

    @Test
    fun missingPropertiesWithDefaultValue_ShouldReadDefaultValue() {
        val config = factory.createConfig(BaseConfiguration::class.java)

        assertEquals(0xFF, config.integerProperty)
    }

    @ConfigFile("BaseTestConfiguration.properties")
    data class InvalidBaseConfiguration(
        @ConfigProperty("MissingProperty")
        val integerProperty: Int,
    )

    @Test
    fun missingPropertiesWithNoDefaultValue_ShouldThrow() {
        assertThrows<ConfigurationBuildException> {
            factory.createConfig(InvalidBaseConfiguration::class.java)
        }
    }

    @ConfigFile("BaseTestConfiguration.properties")
    data class MultipleDeclarationsOfTheSameKeyName(
        @ConfigProperty("BooleanProperty")
        val booleanProperty: Boolean,

        @ConfigProperty("BooleanProperty")
        val booleanProperty2: Boolean,
    )

    @Test
    fun multipleDeclarationsOfSameKeyName_ShouldThrow() {
        assertThrows<ConfigurationBuildException> {
            factory.createConfig(MultipleDeclarationsOfTheSameKeyName::class.java)
        }
    }

    @ConfigFile("BaseTestConfiguration.properties")
    data class MissingConfigurationParameterAnnotation(
        val booleanProperty: Boolean,
    )

    @Test
    fun missingConfigPropertyAnnotation_ShouldThrow() {
        assertThrows<ConfigurationBuildException> {
            factory.createConfig(MissingConfigurationParameterAnnotation::class.java)
        }
    }

    data class MissingConfigFileAnnotation(
        @ConfigProperty("BooleanProperty")
        val booleanProperty: Boolean,
    )

    @Test
    fun missingConfigFileAnnotation_ShouldThrow() {
        assertThrows<InvalidDeclarationException> {
            factory.createConfig(MissingConfigFileAnnotation::class.java)
        }
    }
}

private val TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/"
