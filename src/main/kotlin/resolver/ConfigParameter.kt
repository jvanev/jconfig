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
package com.jvanev.kconfig.resolver

import com.jvanev.kconfig.annotation.ConfigProperty
import com.jvanev.kconfig.annotation.DependsOn
import com.jvanev.kconfig.getDependsOn
import com.jvanev.kconfig.hasDependency
import com.jvanev.kconfig.requireConfigProperty
import java.lang.reflect.Parameter

/**
 * Represents a single configuration parameter, encapsulating its metadata for resolution.
 * This class serves as an internal data carrier for [ValueResolver].
 *
 * @param container The [Class] of the configuration type (or group) where this parameter is declared.
 * @param parameter The [Parameter] representing the actual Kotlin property.
 */
internal class ConfigParameter(container: Class<*>, parameter: Parameter) {
    /**
     * The fully qualified name of the [Parameter] this config parameter represents,
     * including its parent container's simple name (e.g., "MyConfig.myProperty").
     */
    val parameterName = "${container.simpleName}.${parameter.name}"

    /**
     * The exact key of the property in the `.properties` configuration file,
     * as defined by [ConfigProperty.name].
     */
    val propertyName: String

    /**
     * The default value defined in [ConfigProperty.defaultValue], used if the property
     * is not found in the configuration or its dependency is not satisfied.
     */
    val defaultValue: String

    /**
     * Determines whether this configuration parameter has a declared dependency
     * via the [DependsOn] annotation.
     */
    val hasDependency = parameter.hasDependency

    /**
     * The [ConfigProperty.name] of the property this parameter depends on.
     * This field is only relevant if [hasDependency] is `true`.
     */
    val dependencyName: String

    /**
     * The expected value of the [dependencyName] property required to activate
     * this configuration parameter, as defined by [DependsOn.value].
     * This field is only relevant if [hasDependency] is `true`.
     */
    val dependencyValue: String

    init {
        val configProperty = parameter.requireConfigProperty(container)
        propertyName = configProperty.name
        defaultValue = configProperty.defaultValue

        val dependencyInfo = parameter.getDependsOn()
        dependencyName = dependencyInfo?.property ?: ""
        dependencyValue = dependencyInfo?.value ?: ""
    }

    override fun toString(): String {
        return "$propertyName ($parameterName)"
    }
}
