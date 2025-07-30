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
import com.jvanev.kconfig.converter.ValueConverter
import com.jvanev.kconfig.exception.UnsupportedTypeConversionException
import com.jvanev.kconfig.resolver.ValueResolver
import java.io.IOException
import java.lang.reflect.Type
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType
import kotlin.reflect.jvm.jvmErasure

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

    private val valueConverter = ValueConverter()

    /**
     * Registers a custom value [converter] for [String] to [type] conversions not natively
     * supported by the factory's default mechanism, or to override existing default conversions.
     *
     * Custom converters take precedence over the built-in conversion logic.
     *
     * @param type The [Class] representing the target type (e.g., `MyCustomClass::class.java`)
     * for which this converter will be invoked.
     * @param converter A lambda function that accepts two arguments:
     * 1. The [String] value to be converted.
     * 2. The target [Type] the value must be converted to (useful for generic types).
     * 3. An array of the actual [Type] arguments; the array will be empty if the target [Type] is not generic.
     * The function must return the successfully converted value; returning `null` is not allowed.
     */
    fun addValueConverter(type: Class<*>, converter: (String, Type, Array<Type>) -> Any) {
        valueConverter.addValueConverter(type, converter)
    }

    /**
     * Creates and returns a new, fully initialized instance of the specified *Configuration Container* [type].
     *
     * This method recursively instantiates all *Configuration Types* declared as properties
     * within the container, loading their values from their respective configuration files.
     * Each property in the container's primary constructor must be of a type annotated with [ConfigFile].
     *
     * @param type The [KClass] of the *Configuration Container* to create.
     *
     * @return A fully initialized instance of the *Configuration Container*.
     *
     * @throws IllegalArgumentException If the provided [type] does not declare a primary constructor,
     * if any of its primary constructor parameters are not valid **Configuration Types** (missing [ConfigFile]),
     * or if multiple properties within the container reference the same configuration file name.
     * @throws IOException If there's an issue loading a `.properties` file from `configDir`.
     */
    fun <T : Any> createConfigContainer(type: KClass<T>): T {
        val constructor = requireNotNull(type.primaryConstructor) {
            "${type.simpleName} must declare a primary constructor"
        }
        val arguments = mutableMapOf<KParameter, Any?>()
        val processedConfigurations = mutableMapOf<String, String>()

        for (parameter in constructor.parameters) {
            val parameterType = parameter.type.jvmErasure
            val annotation = requireNotNull(parameterType.findAnnotation<ConfigFile>()) {
                "Type ${parameterType.simpleName} declared on parameter " +
                "${type.simpleName}.${parameter.name} is not annotated with @ConfigFile"
            }

            if (processedConfigurations.containsKey(annotation.name)) {
                throw IllegalArgumentException(
                    "Configuration type ${parameterType.simpleName} of property " +
                    "${type.simpleName}.${parameter.name} declares the same filename " +
                    "(${annotation.name}) as the type of property " +
                    "${type.simpleName}.${processedConfigurations[annotation.name]}"
                )
            }

            processedConfigurations[annotation.name] = parameter.name ?: ""
            arguments[parameter] = try {
                createConfig(parameterType)
            } catch (e: Exception) {
                System.err.println(
                    "Failed to initialize configuration container property ${type.simpleName}.${parameter.name}"
                )

                throw e
            }
        }

        return constructor.callBy(arguments)
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
        val container = requireNotNull(type.findAnnotation<ConfigFile>()) {
            "Class ${type.simpleName} must be annotated with @ConfigFile"
        }
        val properties = getProperties(container.name)
        val namespace = Namespace(type, properties)

        return try {
            buildConfigTree(type, namespace)
        } catch (e: Exception) {
            System.err.println("Failed to create an instance of configuration type ${type.simpleName}")

            throw e
        }
    }

    /**
     * Recursively constructs and populates a configuration object tree based on the provided
     * container [KClass] and its current [namespace] context.
     *
     * This internal method iterates through the primary constructor parameters of the `container`:
     * - If a parameter is a [ConfigGroup], it creates a new [Namespace] for the group and
     * recursively calls itself to build the nested group.
     * - If a parameter is a [ConfigProperty], it uses [ValueResolver] to determine its runtime
     * value, applying dependency checks and default values, and then converts the string value
     * to the appropriate Kotlin type using [ValueConverter].
     *
     * @param container The [KClass] of the configuration object (or group) currently being built.
     * @param namespace The [Namespace] object defining the current scope, properties, and dependency state.
     *
     * @return A fully initialized instance of the `container` type.
     *
     * @throws IllegalArgumentException If the `container` lacks a primary constructor, contains
     * duplicate [ConfigProperty.name]s, or if dependency chains are invalid (e.g., circular).
     * @throws UnsupportedTypeConversionException If a property's resolved string value cannot be
     * converted to its target Kotlin type.
     */
    @OptIn(ExperimentalStdlibApi::class)
    private fun <T : Any> buildConfigTree(container: KClass<T>, namespace: Namespace): T {
        val constructor = requireNotNull(container.primaryConstructor) {
            "${container.simpleName} must declare a primary constructor"
        }
        val arguments = mutableMapOf<KParameter, Any?>()
        val processedKeys = mutableMapOf<String, String>()
        val valueResolver = ValueResolver(namespace.properties, container, namespace.namespace, constructor.parameters)

        for (parameter in constructor.parameters) {
            if (parameter.isGroup) {
                val groupScope = namespace.fromGroup(parameter, valueResolver.isGroupDependencySatisfied(parameter))
                arguments[parameter] = buildConfigTree(parameter.type.jvmErasure, groupScope)
            } else {
                val configProperty = parameter.requireConfigProperty(container)

                if (processedKeys.containsKey(configProperty.name)) {
                    throw IllegalArgumentException(
                        "Configuration property '${configProperty.name}' declared on parameter " +
                        "${container.simpleName}.${parameter.name} is also declared on parameter " +
                        "${container.simpleName}.${processedKeys[configProperty.name]}"
                    )
                }

                val resolvedValue = if (namespace.dependenciesSatisfied) {
                    valueResolver.resolveValue(parameter)
                } else {
                    configProperty.defaultValue
                }
                val type = parameter.type.javaType
                processedKeys[configProperty.name] = parameter.name ?: ""
                arguments[parameter] = try {
                    valueConverter.convert(resolvedValue.trim(), type)
                } catch (e: Exception) {
                    System.err.println(
                        "Failed to initialize configuration property ${container.simpleName}.${parameter.name}"
                    )

                    throw e
                }
            }
        }

        return constructor.callBy(arguments)
    }

    /**
     * Loads and returns a [Properties] object populated with the contents of the file with the given name.
     * The file is expected to be located within the [configDir].
     *
     * @param fileName The base name of the `.properties` file (e.g., "application" for "application.properties").
     *
     * @return A [Properties] object containing the key-value pairs from the file.
     *
     * @throws IOException If the file does not exist or an I/O error occurs during loading.
     */
    private fun getProperties(fileName: String): Properties {
        return Files.newInputStream(configDir.resolve(fileName)).use { propertiesFile ->
            Properties().apply { load(propertiesFile) }
        }
    }
}
