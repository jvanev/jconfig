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

import com.jvanev.kconfig.CircularDependencyException
import com.jvanev.kconfig.InvalidDeclarationException
import com.jvanev.kconfig.annotation.ConfigGroup
import com.jvanev.kconfig.annotation.ConfigProperty
import com.jvanev.kconfig.annotation.DependsOn
import com.jvanev.kconfig.hasDependency
import com.jvanev.kconfig.isGroup
import com.jvanev.kconfig.requireConfigProperty
import com.jvanev.kconfig.requireDependsOn
import java.lang.reflect.Parameter
import java.util.Properties

/**
 * A mechanism for resolving the runtime values of configuration properties scoped to a given namespace.
 * This resolver efficiently handles both direct and indirect (chained) dependencies during the resolution process,
 * including detection of circular dependencies.
 *
 * @param properties The [Properties] instance containing the raw configuration key-value pairs.
 * @param container The [Class] representing the configuration type (or group) being resolved.
 * @param namespace The namespace (prefix) for the properties being resolved within this context.
 * An empty string indicates the root or default namespace.
 * @param parameters The list of [Parameter]s representing the properties declared in the
 * primary constructor of the [container] class.
 */
internal class ValueResolver(
    private val properties: Properties,
    private val container: Class<*>,
    private val namespace: String,
    parameters: Array<Parameter>,
) {
    /**
     * Internal map storing [ConfigParameter] instances, keyed by their [ConfigProperty.name].
     * This provides quick access to parameter metadata for resolution and dependency checking.
     */
    private val configParameters: Map<String, ConfigParameter> = mutableMapOf<String, ConfigParameter>().apply {
        for (parameter in parameters) {
            val configProperty = if (parameter.isGroup) {
                // Configuration groups are not actual configurations
                continue
            } else {
                parameter.requireConfigProperty(container)
            }
            this[configProperty.name] = ConfigParameter(container, parameter)
        }
    }

    /**
     * Cache for already resolved string values, mapped by their fully qualified property name
     * (including namespace). This prevents redundant lookups in the [properties] object.
     */
    private val resolvedValues = mutableMapOf<String, String>()

    /**
     * Resolves and returns the final runtime [String] value for the given configuration [parameter].
     *
     * The resolution process respects [DependsOn] annotations:
     * - If the property has no dependency, its value is directly read from the configuration [properties]
     * file (or its [ConfigProperty.defaultValue] if the key is not found).
     * - If the property defines a dependency using [DependsOn], the value from the configuration
     * file will be loaded *only if* the dependency condition is satisfied (i.e., its resolved
     * value matches the [DependsOn.value]). Otherwise, [ConfigProperty.defaultValue] will be returned.
     *
     * @param parameter The [Parameter] representing the configuration property to resolve.
     *
     * @return The resolved runtime [String] value for the parameter.
     *
     * @throws IllegalArgumentException If the parameter is not a valid configuration property
     * or if its dependencies are malformed.
     */
    fun resolveValue(parameter: Parameter): String {
        val configParameter = parameter.asConfigParameter()

        return if (!configParameter.hasDependency || isDependencySatisfied(configParameter)) {
            configParameter.getValue()
        } else {
            configParameter.defaultValue
        }
    }

    /**
     * Determines whether the dependency condition for the given configuration group [parameter] is satisfied.
     *
     * The condition is satisfied if:
     * - The group doesn't have a [DependsOn] annotation, or
     * - The resolved value of its declared dependency matches the [DependsOn.value].
     * The dependency chain for the dependent property is also evaluated.
     *
     * @param parameter The [Parameter] representing the [ConfigGroup] to check.
     *
     * @return `true` if the group's dependency condition is satisfied, `false` otherwise.
     *
     * @throws InvalidDeclarationException If the group depends on a non-existent property
     * or if a circular dependency is detected in the chain.
     */
    fun isGroupDependencySatisfied(parameter: Parameter): Boolean {
        if (!parameter.hasDependency) {
            return true
        }

        val dependencyInfo = parameter.requireDependsOn(container)
        val dependency = configParameters[dependencyInfo.property]!!
        val dependencyValue = if (!dependency.hasDependency || isDependencySatisfied(dependency)) {
            dependency.getValue()
        } else {
            dependency.defaultValue
        }

        return dependencyInfo.value == dependencyValue
    }

    /**
     * Determines whether the dependency condition for the given [configParameter] is satisfied.
     * This method is called internally when resolving an individual property's value.
     *
     * The condition is satisfied if:
     * - The property doesn't have a [DependsOn] annotation, or
     * - The resolved value of its declared dependency matches the [DependsOn.value].
     * The dependency chain for the dependent property is also evaluated recursively.
     *
     * @param configParameter The [ConfigParameter] representing the property whose dependency is to be checked.
     *
     * @return `true` if the dependency condition is satisfied, `false` otherwise.
     *
     * @throws InvalidDeclarationException If the property depends on a non-existent property
     * or if a circular dependency is detected in the chain.
     */
    private fun isDependencySatisfied(configParameter: ConfigParameter): Boolean {
        val dependency = configParameter.getDependency()
        val dependencyValue = if (!dependency.hasDependency || isDependencyChainSatisfied(dependency)) {
            dependency.getValue()
        } else {
            dependency.defaultValue
        }

        return configParameter.dependencyValue == dependencyValue
    }

    /**
     * Recursively checks whether the dependency of the given [dependentParameter] matches the required value.
     * This function traverses the dependency chain (e.g., A depends on B, B depends on C).
     *
     * @param dependentParameter The [ConfigParameter] whose dependency is currently being checked.
     * @param checkedLinks A [MutableSet] used to track properties already visited in the current
     * dependency chain, preventing infinite recursion in case of circular dependencies.
     *
     * @return `true` if the dependency condition for the current `dependentParameter` and its
     * entire upstream chain is satisfied, `false` otherwise.
     *
     * @throws CircularDependencyException If a circular dependency is detected (e.g., A -> B -> A).
     * @throws InvalidDeclarationException If a property in the dependency chain depends on a non-existent property.
     */
    private fun isDependencyChainSatisfied(
        dependentParameter: ConfigParameter,
        checkedLinks: MutableSet<String> = LinkedHashSet(),
    ): Boolean {
        if (!checkedLinks.add(dependentParameter.propertyName)) {
            val parameters = mutableListOf<ConfigParameter>().apply {
                for (link in checkedLinks) {
                    add(configParameters[link]!!)
                }

                add(dependentParameter)
            }

            throw CircularDependencyException(parameters)
        }

        val dependency = dependentParameter.getDependency()
        val dependencyValue = if (!dependency.hasDependency || isDependencyChainSatisfied(dependency, checkedLinks)) {
            dependency.getValue()
        } else {
            dependency.defaultValue
        }

        return dependentParameter.dependencyValue == dependencyValue
    }

    /**
     * Converts a [Parameter] into its corresponding [ConfigParameter] representation.
     *
     * @throws InvalidDeclarationException If the parameter is not a valid configuration property
     * (i.e., not found in the [configParameters] map, or missing [ConfigProperty] annotation).
     */
    private fun Parameter.asConfigParameter(): ConfigParameter {
        val configProperty = requireConfigProperty(container)

        return configParameters[configProperty.name] ?: throw InvalidDeclarationException(
            "Parameter ${container.simpleName}.$name is not a configuration parameter"
        )
    }

    /**
     * Retrieves the [ConfigParameter] that this parameter's dependency refers to.
     *
     * @throws InvalidDeclarationException If this parameter does not declare a dependency
     * or if the declared dependency property does not exist within the current container.
     */
    private fun ConfigParameter.getDependency(): ConfigParameter = configParameters[dependencyName]
        ?: throw InvalidDeclarationException("Cannot resolve dependency $dependencyName declared on $this")

    /**
     * Retrieves the configuration value for this [ConfigParameter] from the underlying
     * [properties] file, applying the current [namespace].
     *
     * If the property's fully qualified name is not defined in the configuration file,
     * its [ConfigProperty.defaultValue] will be returned instead.
     * Resolved values are cached to prevent redundant file lookups.
     */
    private fun ConfigParameter.getValue(): String {
        // Construct the full property key, including the namespace if present
        val propertyName = if (namespace.isBlank()) propertyName else "$namespace.$propertyName"

        // Cache the result for later
        return resolvedValues.getOrPut(propertyName) {
            properties.getProperty(propertyName, defaultValue)
        }
    }
}
