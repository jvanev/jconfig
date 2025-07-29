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
import java.lang.reflect.Type
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * A type-safe configuration factory responsible for producing fully initialized configuration objects.
 *
 * This factory provides methods to create instances of *Configuration Types*
 * (classes annotated with [ConfigFile]) or *Configuration Containers*
 * (classes that group multiple *Configuration Types*). It automates the process of
 * mapping properties from `.properties` files to Kotlin objects, handling type conversions,
 * default values, and conditional dependencies.
 *
 * ---
 *
 * ## Terms
 *
 * - **Configuration Type:** A class annotated with [ConfigFile]. Its primary constructor declares
 * properties, each annotated with [ConfigProperty] or [ConfigGroup] (and optionally [DependsOn]),
 * whose values are initialized by mapping them from a `.properties` configuration file.
 * - **Configuration Container:** A Plain Old Java Object (POJO) whose primary constructor declares
 * properties, where each property's type is itself a [ConfigFile]-annotated class (a *Configuration Type*).
 * This serves as a convenient means to group and instantiate related *Configuration Type* objects
 * from various configuration files.
 *
 * @param configDir The directory where the configuration `.properties` files are located.
 * This directory is used as the base path for resolving configuration files specified by [ConfigFile] annotations.
 */
class ConfigFactory(configDir: String) {
    private val configDir = Path.of(configDir)

    /**
     * Registers a custom value [converter] for [String] to [type] conversions not natively
     * supported by the factory's default mechanism, or to override existing default conversions.
     *
     * Custom converters take precedence over the built-in conversion logic.
     *
     * @param type The [Class] representing the target type (e.g., `MyCustomClass::class`)
     * for which this converter will be invoked.
     * @param converter A lambda function that accepts two arguments:
     * 1. The [String] value to be converted.
     * 2. The target [Type] the value must be converted to (useful for generic types).
     * The function must return the successfully converted value; returning `null` is not allowed.
     */
    fun addValueConverter(type: Class<*>, converter: (String, Type) -> Any) {
        TODO("Not implemented yet")
    }

    /**
     * Creates and returns a new, fully initialized instance of the specified *Configuration Container* [type].
     *
     * This method recursively instantiates all *Configuration Types* declared as properties
     * within the container, loading their values from their respective configuration files.
     *
     * @param type The [KClass] of the *Configuration Container* to create.
     *
     * @return A fully initialized instance of the *Configuration Container*.
     *
     * @throws IllegalArgumentException If the provided [type] is not a valid *Configuration Container*
     * (e.g., its properties are not *Configuration Types*).
     */
    fun <T : Any> createConfigContainer(type: KClass<T>): T {
        TODO("Not implemented yet")
    }

    /**
     * Creates and returns a new, fully initialized instance of the specified *Configuration Type* [type].
     *
     * This method loads property values from the configuration file specified by the [ConfigFile]
     * annotation on the provided [type], applying default values and resolving dependencies as needed.
     *
     * @param type The [KClass] of the *Configuration Type* to create.
     *
     * @return A fully initialized instance of the *Configuration Type*.
     *
     * @throws IllegalArgumentException If the provided [type] is not a valid *Configuration Type*
     * (i.e., not annotated with [ConfigFile]).
     */
    fun <T : Any> createConfig(type: KClass<T>): T {
        TODO("Not implemented yet")
    }
}
