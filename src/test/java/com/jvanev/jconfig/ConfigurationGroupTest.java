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
package com.jvanev.jconfig;

import com.jvanev.jconfig.annotation.ConfigFile;
import com.jvanev.jconfig.annotation.ConfigGroup;
import com.jvanev.jconfig.annotation.ConfigProperty;
import com.jvanev.jconfig.annotation.DependsOn;
import com.jvanev.jconfig.exception.ConfigurationBuildException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationGroupTest {
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
    class TopLevelGroupTests {
        enum LogLevel {
            DEBUG, INFO
        }

        @ConfigFile(filename = "GroupTestConfiguration.properties")
        public record TopLevelConfiguration(
            @ConfigProperty(name = "EnabledDeveloperMode")
            boolean enabledDevMode,

            @ConfigProperty(name = "DisabledDeveloperMode")
            boolean disabledDevMode,

            @ConfigGroup
            @DependsOn(property = "EnabledDeveloperMode")
            NestedConfiguration enabledConfig,

            @ConfigGroup
            @DependsOn(property = "DisabledDeveloperMode")
            NestedConfiguration disabledConfig
        ) {
            public record NestedConfiguration(
                @ConfigProperty(name = "LogLevel", defaultValue = "INFO")
                LogLevel logLevel,

                @ConfigProperty(name = "LogTag", defaultValue = "I")
                char logTag,

                @ConfigProperty(name = "EnableCallTraces", defaultValue = "false")
                boolean enableCallTraces,

                @ConfigProperty(name = "ConnectionTimeout", defaultValue = "8")
                int timeout
            ) {
            }
        }

        @Test
        void enabledToplevelGroup_ShouldReadFromConfigFile() {
            TopLevelConfiguration config = factory.createConfig(TopLevelConfiguration.class);

            assertEquals(LogLevel.DEBUG, config.enabledConfig().logLevel());
            assertEquals('D', config.enabledConfig().logTag());
            assertTrue(config.enabledConfig().enableCallTraces());
            assertEquals(25, config.enabledConfig().timeout());
        }

        @Test
        void disabledToplevelGroup_ShouldReadDefaultValues() {
            TopLevelConfiguration config = factory.createConfig(TopLevelConfiguration.class);

            assertEquals(LogLevel.INFO, config.disabledConfig().logLevel());
            assertEquals('I', config.disabledConfig().logTag());
            assertFalse(config.disabledConfig().enableCallTraces());
            assertEquals(8, config.disabledConfig().timeout());
        }
    }

    @Nested
    class NamespacedGroupTests {
        enum EncryptionAlgorithm {
            PLAIN_TEXT, ARGON2
        }

        @ConfigFile(filename = "GroupTestConfiguration.properties")
        public record NamespacedConfiguration(
            @ConfigProperty(name = "EnabledDeveloperMode")
            boolean enabledDevMode,

            @ConfigProperty(name = "DisabledDeveloperMode")
            boolean disabledDevMode,

            @ConfigGroup(namespace = "DevService")
            @DependsOn(property = "EnabledDeveloperMode")
            ServiceConfiguration devService,

            @ConfigGroup(namespace = "ClientService")
            @DependsOn(property = "DisabledDeveloperMode")
            ServiceConfiguration clientService
        ) {
            public record ServiceConfiguration(
                @ConfigProperty(name = "EncryptPassword", defaultValue = "true")
                boolean encryptPassword,

                @ConfigProperty(name = "DisableSecurity", defaultValue = "false")
                boolean disableSecurity,

                @ConfigProperty(name = "EncryptionAlgorithm", defaultValue = "ARGON2")
                EncryptionAlgorithm encryptionAlgorithm,

                @ConfigGroup(namespace = "Network")
                NetworkConfiguration networkConfig
            ) {
                public record NetworkConfiguration(
                    @ConfigProperty(name = "LogRequest", defaultValue = "false")
                    boolean logRequest,

                    @ConfigProperty(name = "LogResponse", defaultValue = "true")
                    boolean logResponse,

                    @ConfigProperty(name = "RequestLimit", defaultValue = "100")
                    int requestLimit,

                    @ConfigProperty(name = "ContentType", defaultValue = "application/json")
                    String contentType,

                    @ConfigProperty(name = "LogPassword", defaultValue = "false")
                    @DependsOn(property = "LogResponse")
                    boolean logPassword
                ) {
                }
            }
        }

        @Test
        void enabledNamespacedGroup_ShouldReadFromConfigFile() {
            NamespacedConfiguration config = factory.createConfig(NamespacedConfiguration.class);

            assertFalse(config.devService().encryptPassword());
            assertTrue(config.devService().disableSecurity());
            assertEquals(EncryptionAlgorithm.PLAIN_TEXT, config.devService().encryptionAlgorithm());
            assertTrue(config.devService().networkConfig().logRequest());
            assertFalse(config.devService().networkConfig().logResponse());
            assertEquals(3, config.devService().networkConfig().requestLimit());
            assertEquals("text/html", config.devService().networkConfig().contentType());
            assertFalse(config.devService().networkConfig().logPassword());
        }

        @Test
        void disabledNamespacedGroup_ShouldReadDefaultValues() {
            NamespacedConfiguration config = factory.createConfig(NamespacedConfiguration.class);

            assertTrue(config.clientService().encryptPassword());
            assertFalse(config.clientService().disableSecurity());
            assertEquals(EncryptionAlgorithm.ARGON2, config.clientService().encryptionAlgorithm());
            assertFalse(config.clientService().networkConfig().logRequest());
            assertTrue(config.clientService().networkConfig().logResponse());
            assertEquals(100, config.clientService().networkConfig().requestLimit());
            assertEquals("application/json", config.clientService().networkConfig().contentType());
            assertFalse(config.clientService().networkConfig().logPassword());
        }
    }

    @Nested
    class IncorrectGroupSetupTests {
        @ConfigFile(filename = "GroupTestConfiguration.properties")
        public record MissingDefaultValueOnDependentParameter(
            @ConfigProperty(name = "DisabledDeveloperMode")
            boolean enabledDevMode,

            @ConfigGroup
            @DependsOn(property = "DisabledDeveloperMode")
            Configuration invalidConfiguration
        ) {
            public record Configuration(
                @ConfigProperty(name = "LogTag")
                char logTag
            ) {
            }
        }

        @Test
        void missingDefaultValueInDependentGroup_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MissingDefaultValueOnDependentParameter.class)
            );
        }
    }
}
