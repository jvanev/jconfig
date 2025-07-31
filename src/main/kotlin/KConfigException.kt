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

import com.jvanev.kconfig.resolver.ConfigParameter
import java.lang.reflect.Type

/**
 * Base class for all KConfig-specific exceptions.
 */
sealed class KConfigException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Thrown when an error occurs during the configuration building process.
 */
class ConfigurationBuildException(message: String, cause: Throwable) : KConfigException(message, cause)

/**
 * Thrown when an invalid configuration declaration has been found.
 */
class InvalidDeclarationException(message: String) : KConfigException(message)

/**
 * Thrown when an error occurs during configuration value conversion.
 */
class ValueConversionException(message: String, cause: Throwable) : KConfigException(message, cause)

/**
 * Thrown when a configuration type contains a property in its primary constructor
 * whose data type cannot be instantiated from a [String] value.
 *
 * This typically occurs when a type lacks a default conversion rule and no custom
 * converter has been registered for it.
 */
class UnsupportedTypeConversionException internal constructor(type: Type, value: String? = null) : KConfigException(
    if (value == null) {
        "Conversions to type ${type.typeName} are not supported. " +
                "Consider registering a custom converter via ConfigFactory.addValueConverter"
    } else {
        "Cannot convert '$value' to type ${type.typeName}. " +
                "Consider registering a custom converter via ConfigFactory.addValueConverter"
    }
)

/**
 * Thrown when a circular dependency chain is detected (e.g., A depends on B -> B depends on C -> C depends on A).
 */
class CircularDependencyException internal constructor(chain: List<ConfigParameter>) : KConfigException(
    "Circular dependency chain: " + chain.joinToString(" depends on -> ")
)
