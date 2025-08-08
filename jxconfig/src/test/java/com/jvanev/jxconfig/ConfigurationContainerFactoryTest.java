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
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationContainerFactoryTest {
    private static final String TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/";

    private final ConfigFactory factory = ConfigFactory.builder(TEST_RESOURCES_DIR + "config").build();

    @BeforeEach
    void ensureTestConfigurationDirectoryExists() {
        assertTrue(
            Files.isDirectory(Paths.get(TEST_RESOURCES_DIR + "config")),
            "Test configurations directory does not exist"
        );
    }

    @ConfigFile(filename = "BaseTestConfiguration.properties")
    public record BaseConfiguration(
        @ConfigProperty(key = "BooleanProperty")
        boolean booleanProperty,

        @ConfigProperty(key = "MissingProperty", defaultValue = "0xFF")
        int integerProperty
    ) {
    }

    @ConfigFile(filename = "GroupTestConfiguration.properties")
    public record GroupConfiguration(
        @ConfigProperty(key = "EnabledDeveloperMode")
        boolean enabledDevMode,

        @ConfigProperty(key = "DisabledDeveloperMode")
        boolean disabledDevMode,

        @ConfigGroup
        @DependsOn(property = "EnabledDeveloperMode")
        NestedConfiguration enabledConfig,

        @ConfigGroup
        @DependsOn(property = "DisabledDeveloperMode")
        NestedConfiguration disabledConfig
    ) {
        public record NestedConfiguration(
            @ConfigProperty(key = "LogTag", defaultValue = "I")
            char logTag,

            @ConfigProperty(key = "EnableCallTraces", defaultValue = "false")
            boolean enableCallTraces,

            @ConfigProperty(key = "ConnectionTimeout", defaultValue = "8")
            int timeout
        ) {
        }
    }

    @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
    public record ValueConversionsConfiguration(
        @ConfigProperty(key = "BooleanTrueProperty")
        boolean booleanTrueProperty,

        @ConfigProperty(key = "BooleanFalseProperty")
        boolean booleanFalseProperty,

        @ConfigProperty(key = "ByteProperty")
        byte byteProperty,

        @ConfigProperty(key = "ShortProperty")
        short shortProperty,

        @ConfigProperty(key = "IntegerProperty")
        int intProperty,

        @ConfigProperty(key = "HexIntegerProperty")
        int hexIntProperty,

        @ConfigProperty(key = "LongProperty")
        long longProperty,

        @ConfigProperty(key = "FloatProperty")
        float floatProperty,

        @ConfigProperty(key = "DoubleProperty")
        double doubleProperty,

        @ConfigProperty(key = "CharProperty")
        char charProperty
    ) {
    }

    @ConfigFile(filename = "DependencyTestConfiguration.properties")
    public record DependencyConfiguration(
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

    public record ConfigurationContainer(
        BaseConfiguration baseConfiguration,
        GroupConfiguration groupConfiguration,
        ValueConversionsConfiguration valueConversionsConfiguration,
        DependencyConfiguration dependencyConfiguration
    ) {
    }

    @Test
    void correctlyConfiguredContainer_ShouldNotThrowOnInitialization() {
        assertDoesNotThrow(() -> factory.createConfigContainer(ConfigurationContainer.class));
    }

    @Test
    void correctlyConfiguredContainer_ShouldBeInitializedCorrectly() {
        var container = factory.createConfigContainer(ConfigurationContainer.class);

        assertTrue(container.baseConfiguration().booleanProperty());
        assertEquals(0xFF, container.baseConfiguration().integerProperty());
        assertTrue(container.groupConfiguration().enabledDevMode());
        assertEquals('D', container.groupConfiguration().enabledConfig().logTag());
        assertEquals(1234567890L, container.valueConversionsConfiguration().longProperty());
        assertEquals(3.14f, container.valueConversionsConfiguration().floatProperty());
        assertEquals(65535, container.dependencyConfiguration().integerPropertyWithSatisfiedDependency());
        assertEquals(0, container.dependencyConfiguration().integerPropertyWithUnsatisfiedDependency());
    }

    public record UnannotatedTypeInConfigurationContainer(
        ConfigurationContainer unannotatedConfiguration,
        BaseConfiguration baseConfiguration,
        GroupConfiguration groupConfiguration,
        ValueConversionsConfiguration valueConversionsConfiguration,
        DependencyConfiguration dependencyConfiguration
    ) {
    }

    @Test
    void incorrectlyConfiguredContainer_ShouldThrow() {
        assertThrows(
            InvalidDeclarationException.class,
            () -> factory.createConfigContainer(UnannotatedTypeInConfigurationContainer.class)
        );
    }

    public record DuplicateConfigFileNameDeclarationContainer(
        BaseConfiguration baseConfiguration,
        BaseConfiguration anotherBaseConfiguration
    ) {
    }

    @Test
    void multipleTypesDeclaringTheSameFilename_ShouldThrow() {
        assertThrows(
            InvalidDeclarationException.class,
            () -> factory.createConfigContainer(DuplicateConfigFileNameDeclarationContainer.class)
        );
    }

    public record MultipleConstructorsContainer(
        BaseConfiguration baseConfiguration
    ) {
        public MultipleConstructorsContainer(Boolean test, BaseConfiguration baseConfiguration) {
            this(baseConfiguration);
        }
    }

    @Test
    void containerWithMultipleConstructors_ShouldThrow() {
        assertThrows(
            InvalidDeclarationException.class,
            () -> factory.createConfigContainer(MultipleConstructorsContainer.class)
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

    public record MultipleConstructorsConfigurationContainer(
        MultipleConstructorsConfiguration baseConfiguration
    ) {
    }

    @Test
    void containerContainingConfigurationWithMultipleConstructors_ShouldThrow() {
        assertThrows(
            ConfigurationBuildException.class,
            () -> factory.createConfigContainer(MultipleConstructorsConfigurationContainer.class)
        );
    }

    private record InaccessibleConfigurationContainer() {
    }

    @Test
    void onFailedConfigurationInstantiation_ShouldThrow() {
        assertThrows(
            ConfigurationBuildException.class,
            () -> factory.createConfigContainer(InaccessibleConfigurationContainer.class)
        );
    }
}
