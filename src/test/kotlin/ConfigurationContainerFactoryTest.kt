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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals

class ConfigurationContainerFactoryTest {
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

    @ConfigFile("GroupTestConfiguration.properties")
    data class GroupConfiguration(
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
            @ConfigProperty(name = "LogTag", defaultValue = "I")
            val logTag: Char,

            @ConfigProperty(name = "EnableCallTraces", defaultValue = "False")
            val enableCallTraces: Boolean,

            @ConfigProperty(name = "ConnectionTimeout", defaultValue = "8")
            val timeout: Int,
        )
    }

    @ConfigFile("ValueConversionsTestConfiguration.properties")
    data class ValueConversionsConfiguration(
        @ConfigProperty("BooleanTrueProperty")
        val booleanTrueProperty: Boolean,

        @ConfigProperty("BooleanFalseProperty")
        val booleanFalseProperty: Boolean,

        @ConfigProperty("ByteProperty")
        val byteProperty: Byte,

        @ConfigProperty("ShortProperty")
        val shortProperty: Short,

        @ConfigProperty("IntegerProperty")
        val intProperty: Int,

        @ConfigProperty("HexIntegerProperty")
        val hexIntProperty: Int,

        @ConfigProperty("LongProperty")
        val longProperty: Long,

        @ConfigProperty("FloatProperty")
        val floatProperty: Float,

        @ConfigProperty("DoubleProperty")
        val doubleProperty: Double,

        @ConfigProperty("CharProperty")
        val charProperty: Char,
    )

    @ConfigFile("DependencyTestConfiguration.properties")
    data class DependencyConfiguration(
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

    data class ConfigurationContainer(
        val baseConfiguration: BaseConfiguration,
        val groupConfiguration: GroupConfiguration,
        val valueConversionsConfiguration: ValueConversionsConfiguration,
        val dependencyConfiguration: DependencyConfiguration,
    )

    @Test
    fun correctlyConfiguredContainer_ShouldNotThrowOnInitialization() {
        assertDoesNotThrow {
            factory.createConfigContainer(ConfigurationContainer::class.java)
        }
    }

    @Test
    fun correctlyConfiguredContainer_ShouldBeInitializedCorrectly() {
        val container = factory.createConfigContainer(ConfigurationContainer::class.java)

        assertTrue(container.baseConfiguration.booleanProperty)
        assertEquals(0xFF, container.baseConfiguration.integerProperty)
        assertTrue(container.groupConfiguration.enabledDevMode)
        assertEquals('D', container.groupConfiguration.enabledConfig.logTag)
        assertEquals(1234567890L, container.valueConversionsConfiguration.longProperty)
        assertEquals(3.14f, container.valueConversionsConfiguration.floatProperty)
        assertEquals(65535, container.dependencyConfiguration.integerPropertyWithSatisfiedDependency)
        assertEquals(0, container.dependencyConfiguration.integerPropertyWithUnsatisfiedDependency)
    }

    data class UnannotatedTypeInConfigurationContainer(
        val unannotatedConfiguration: ConfigurationContainer,
        val baseConfiguration: BaseConfiguration,
        val groupConfiguration: GroupConfiguration,
        val valueConversionsConfiguration: ValueConversionsConfiguration,
        val dependencyConfiguration: DependencyConfiguration,
    )

    @Test
    fun incorrectlyConfiguredContainer_ShouldThrow() {
        assertThrows<IllegalArgumentException> {
            factory.createConfigContainer(UnannotatedTypeInConfigurationContainer::class.java)
        }
    }

    data class DuplicateConfigFileNameDeclarationContainer(
        val baseConfiguration: BaseConfiguration,
        val anotherBaseConfiguration: BaseConfiguration,
    )

    @Test
    fun multipleTypesDeclaringTheSameFilename_ShouldThrow() {
        assertThrows<IllegalArgumentException> {
            factory.createConfigContainer(DuplicateConfigFileNameDeclarationContainer::class.java)
        }
    }
}

private val TEST_RESOURCES_DIR = System.getProperty("user.dir") + "/src/test/resources/"
