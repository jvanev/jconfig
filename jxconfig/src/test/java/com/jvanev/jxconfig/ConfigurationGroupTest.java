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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationGroupTest {
    private static final String TEST_PATH = "classpath:config";

    private final ConfigFactory factory = ConfigFactory.builder(TEST_PATH).build();

    @Nested
    class TopLevelGroupTests {
        enum LogLevel {
            DEBUG, INFO
        }

        @ConfigFile(filename = "GroupTestConfiguration.properties")
        public record TopLevelConfiguration(
            @ConfigProperty(key = "EnabledDeveloperMode", defaultValue = "false")
            @DependsOn(key = "Environment", value = "dev")
            boolean enabledDevMode,

            @ConfigProperty(key = "DisabledDeveloperMode", defaultValue = "false")
            @DependsOn(key = "Environment", value = "prod")
            boolean disabledDevMode,

            @ConfigGroup
            @DependsOn(property = "EnabledDeveloperMode")
            NestedConfiguration enabledConfig,

            @ConfigGroup
            @DependsOn(property = "DisabledDeveloperMode")
            NestedConfiguration disabledConfig
        ) {
            public record NestedConfiguration(
                @ConfigProperty(key = "LogLevel", defaultValue = "INFO")
                LogLevel logLevel,

                @ConfigProperty(key = "LogTag", defaultValue = "I")
                char logTag,

                @ConfigProperty(key = "EnableCallTraces", defaultValue = "false")
                boolean enableCallTraces,

                @ConfigProperty(key = "ConnectionTimeout", defaultValue = "8")
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
            @ConfigGroup(namespace = "DevService")
            @DependsOn(key = "EnabledDeveloperMode")
            ServiceConfiguration devService,

            @ConfigGroup(namespace = "ClientService")
            @DependsOn(key = "DisabledDeveloperMode")
            ServiceConfiguration clientService
        ) {
            public record ServiceConfiguration(
                @ConfigProperty(key = "EncryptPassword", defaultValue = "true")
                boolean encryptPassword,

                @ConfigProperty(key = "DisableSecurity", defaultValue = "false")
                boolean disableSecurity,

                @ConfigProperty(key = "EncryptionAlgorithm", defaultValue = "ARGON2")
                EncryptionAlgorithm encryptionAlgorithm,

                @ConfigGroup(namespace = "Network")
                NetworkConfiguration networkConfig
            ) {
                public record NetworkConfiguration(
                    @ConfigProperty(key = "LogRequest", defaultValue = "false")
                    boolean logRequest,

                    @ConfigProperty(key = "LogResponse", defaultValue = "true")
                    boolean logResponse,

                    @ConfigProperty(key = "RequestLimit", defaultValue = "100")
                    int requestLimit,

                    @ConfigProperty(key = "ContentType", defaultValue = "application/json")
                    String contentType,

                    @ConfigProperty(key = "LogPassword", defaultValue = "false")
                    @DependsOn(property = "LogResponse")
                    boolean logPassword,

                    @ConfigProperty(key = "LogPasswordFormat", defaultValue = "base64")
                    @DependsOn(key = "LogRaw")
                    String passwordLogFormat
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
            assertEquals("plain", config.devService().networkConfig().passwordLogFormat());
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
            assertEquals("base64", config.clientService().networkConfig().passwordLogFormat());
        }
    }

    @Nested
    class IncorrectGroupSetupTests {
        @ConfigFile(filename = "GroupTestConfiguration.properties")
        public record MissingDefaultValueOnDependentParameter(
            @ConfigGroup
            @DependsOn(key = "DisabledDeveloperMode")
            Configuration invalidConfiguration
        ) {
            public record Configuration(
                @ConfigProperty(key = "LogTag")
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

        @ConfigFile(filename = "GroupTestConfiguration.properties")
        public record MissingConfigurationDependency(
            @ConfigGroup
            @DependsOn(key = "NonExistent")
            Configuration invalidConfiguration
        ) {
            public record Configuration(
                @ConfigProperty(key = "LogTag", defaultValue = "T")
                char logTag
            ) {
            }
        }

        @Test
        void missingDependencyInDependentGroup_ShouldThrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(MissingConfigurationDependency.class)
            );
        }
    }
}
