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
import java.lang.reflect.Parameter
import java.util.Properties

/**
 * Represents the current resolution context (scope) within the configuration build tree.
 * It encapsulates the relevant configuration data and state for a specific level of the hierarchy.
 *
 * @property container The [Class] of the primary configuration type (the top-level class annotated with [ConfigFile])
 * that defines the base `.properties` file for this entire tree.
 * @property properties The [Properties] object containing the raw key-value pairs loaded from the
 * top-level configuration file associated with `container`.
 * @property namespace The current string prefix (e.g., "LoginServer.Network") for properties being
 * resolved in this context. An empty string signifies the root or default namespace.
 * @property isDependent Indicates whether this namespace itself (or any of its parent groups)
 * has a [DependsOn] annotation, meaning its activation is conditional.
 * @property dependenciesSatisfied Reflects whether all [DependsOn] conditions for this namespace
 * and all its parent groups in the hierarchy have been successfully met. If `false`, properties
 * within this namespace will typically resolve to their [ConfigProperty.defaultValue].
 */
internal data class Namespace(
    val container: Class<*>,
    val properties: Properties,
    val namespace: String = "",
    val isDependent: Boolean = false,
    val dependenciesSatisfied: Boolean = true,
) {
    /**
     * Creates a new [Namespace] instance that represents a nested configuration group,
     * inheriting properties and updating the namespace prefix and dependency state.
     *
     * @param group The [Parameter] representing the [ConfigGroup] that defines this nested namespace.
     * @param groupDependencySatisfied A [Boolean] indicating whether the [DependsOn] condition
     * specifically for this `group` parameter is met.
     *
     * @return A new [Namespace] instance for the nested group.
     */
    fun fromGroup(group: Parameter, groupDependencySatisfied: Boolean): Namespace {
        val configGroup = group.requireConfigGroup(container)

        return Namespace(
            container = container,
            properties = properties,
            namespace = if (namespace.isBlank()) configGroup.namespace else "$namespace.${configGroup.namespace}",
            isDependent = isDependent || group.hasDependency,
            dependenciesSatisfied = dependenciesSatisfied && groupDependencySatisfied,
        )
    }
}
