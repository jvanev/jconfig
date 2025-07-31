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
import com.jvanev.kconfig.annotation.ConfigGroup
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

class ConfigurationGroupTest {
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

    @ConfigFile("GroupTestConfiguration.properties")
    data class TopLevelConfiguration(
        @ConfigProperty("EnabledDeveloperMode")
        val enabledDevMode: Boolean,

        @ConfigProperty("DisabledDeveloperMode")
        val disabledDevMode: Boolean,

        @ConfigGroup
        @DependsOn("EnabledDeveloperMode")
        val enabledConfig: NestedConfiguration,

        @ConfigGroup
        @DependsOn("DisabledDeveloperMode")
        val disabledConfig: NestedConfiguration,
    ) {
        data class NestedConfiguration(
            @ConfigProperty(name = "LogLevel", defaultValue = "INFO")
            val logLevel: LogLevel,

            @ConfigProperty(name = "LogTag", defaultValue = "I")
            val logTag: Char,

            @ConfigProperty(name = "EnableCallTraces", defaultValue = "False")
            val enableCallTraces: Boolean,

            @ConfigProperty(name = "ConnectionTimeout", defaultValue = "8")
            val timeout: Int,
        )
    }

    @Nested
    inner class TopLevelGroupTests {
        @Test
        fun enabledToplevelGroup_ShouldReadFromConfigFile() {
            val config = factory.createConfig(TopLevelConfiguration::class.java)

            assertEquals(LogLevel.DEBUG, config.enabledConfig.logLevel)
            assertEquals('D', config.enabledConfig.logTag)
            assertTrue(config.enabledConfig.enableCallTraces)
            assertEquals(25, config.enabledConfig.timeout)
        }

        @Test
        fun disabledToplevelGroup_ShouldReadDefaultValues() {
            val config = factory.createConfig(TopLevelConfiguration::class.java)

            assertEquals(LogLevel.INFO, config.disabledConfig.logLevel)
            assertEquals('I', config.disabledConfig.logTag)
            assertFalse(config.disabledConfig.enableCallTraces)
            assertEquals(8, config.disabledConfig.timeout)
        }
    }

    enum class EncryptionAlgorithm {
        PLAIN_TEXT, ARGON2
    }

    @ConfigFile("GroupTestConfiguration.properties")
    data class NamespacedConfiguration(
        @ConfigProperty("EnabledDeveloperMode")
        val enabledDevMode: Boolean,

        @ConfigProperty("DisabledDeveloperMode")
        val disabledDevMode: Boolean,

        @ConfigGroup("DevService")
        @DependsOn("EnabledDeveloperMode")
        val devService: ServiceConfiguration,

        @ConfigGroup("ClientService")
        @DependsOn("DisabledDeveloperMode")
        val clientService: ServiceConfiguration,
    ) {
        data class ServiceConfiguration(
            @ConfigProperty(name = "EncryptPassword", defaultValue = "True")
            val encryptPassword: Boolean,

            @ConfigProperty(name = "DisableSecurity", defaultValue = "False")
            val disableSecurity: Boolean,

            @ConfigProperty(name = "EncryptionAlgorithm", defaultValue = "ARGON2")
            val encryptionAlgorithm: EncryptionAlgorithm,

            @ConfigGroup("Network")
            val networkConfig: NetworkConfiguration,
        ) {
            data class NetworkConfiguration(
                @ConfigProperty(name = "LogRequest", defaultValue = "False")
                val logRequest: Boolean,

                @ConfigProperty(name = "LogResponse", defaultValue = "True")
                val logResponse: Boolean,

                @ConfigProperty(name = "RequestLimit", defaultValue = "100")
                val requestLimit: Int,

                @ConfigProperty(name = "ContentType", defaultValue = "application/json")
                val contentType: String,

                @ConfigProperty(name = "LogPassword", defaultValue = "False")
                @DependsOn("LogResponse")
                val logPassword: Boolean,
            )
        }
    }

    @Nested
    inner class NamespacedGroupTests {
        @Test
        fun enabledNamespacedGroup_ShouldReadFromConfigFile() {
            val config = factory.createConfig(NamespacedConfiguration::class.java)

            assertFalse(config.devService.encryptPassword)
            assertTrue(config.devService.disableSecurity)
            assertEquals(EncryptionAlgorithm.PLAIN_TEXT, config.devService.encryptionAlgorithm)
            assertTrue(config.devService.networkConfig.logRequest)
            assertFalse(config.devService.networkConfig.logResponse)
            assertEquals(3, config.devService.networkConfig.requestLimit)
            assertEquals("text/html", config.devService.networkConfig.contentType)
            assertFalse(config.devService.networkConfig.logPassword)
        }

        @Test
        fun disabledNamespacedGroup_ShouldReadDefaultValues() {
            val config = factory.createConfig(NamespacedConfiguration::class.java)

            assertTrue(config.clientService.encryptPassword)
            assertFalse(config.clientService.disableSecurity)
            assertEquals(EncryptionAlgorithm.ARGON2, config.clientService.encryptionAlgorithm)
            assertFalse(config.clientService.networkConfig.logRequest)
            assertTrue(config.clientService.networkConfig.logResponse)
            assertEquals(100, config.clientService.networkConfig.requestLimit)
            assertEquals("application/json", config.clientService.networkConfig.contentType)
            assertFalse(config.clientService.networkConfig.logPassword)
        }
    }

    @ConfigFile("GroupTestConfiguration.properties")
    data class MissingDefaultValueOnDependentParameter(
        @ConfigProperty("DisabledDeveloperMode")
        val enabledDevMode: Boolean,

        @ConfigGroup
        @DependsOn("DisabledDeveloperMode")
        val invalidConfiguration: Configuration,
    ) {
        data class Configuration(
            @ConfigProperty("LogTag")
            val logTag: Char,
        )
    }

    @Nested
    inner class IncorrectGroupSetupTests {
        @Test
        fun missingDefaultValueInDependentGroup_ShouldThrow() {
            assertThrows<ConfigurationBuildException> {
                factory.createConfig(MissingDefaultValueOnDependentParameter::class.java)
            }
        }
    }
}

private val TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/"
