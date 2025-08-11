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
package com.jvanev.jxconfig.resolver.internal;

import com.jvanev.jxconfig.annotation.ConfigNamespace;
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.annotation.DependsOnProperty;
import com.jvanev.jxconfig.exception.CircularDependencyException;
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import com.jvanev.jxconfig.internal.ReflectionUtil;
import com.jvanev.jxconfig.resolver.DependencyChecker;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A mechanism for resolving the runtime values of configuration properties scoped to a given namespace.
 * This resolver efficiently handles both direct and indirect (chained) dependencies during the resolution process,
 * including detection of circular dependencies.
 */
public final class ValueResolver {
    private final Properties properties;

    private final Class<?> container;

    private final DependencyChecker dependencyChecker;

    /**
     * Contains the metadata of constructor parameters annotated with {@link ConfigProperty}, keyed by their
     * {@link ConfigProperty#key()}.
     */
    private final Map<String, ConfigParameter> parameters = new HashMap<>();

    /**
     * A cache for already resolved values, mapped to their fully qualified key in the configuration file.
     */
    private final Map<String, String> resolvedValues = new HashMap<>();

    /**
     * A cache for already resolved default values, mapped to their fully qualified key in the configuration file.
     */
    private final Map<String, String> resolvedDefaultValues = new HashMap<>();

    /**
     * Creates a new ValueResolver.
     *
     * @param properties The {@link Properties} instance containing the raw configuration key-value pairs
     * @param container  The {@link Class} whose configuration values will be resolved
     * @param namespace  The namespace from which configuration values will be retrieved
     * @param parameters The parameters for which value resolution will be performed
     * @param checker    The custom dependency condition checking mechanism
     */
    public ValueResolver(
        Properties properties,
        Class<?> container,
        String namespace,
        Parameter[] parameters,
        DependencyChecker checker
    ) {
        this.properties = properties;
        this.container = container;
        this.dependencyChecker = checker;

        for (Parameter parameter : parameters) {
            if (!ReflectionUtil.isConfigNamespace(parameter)) {
                var property = ReflectionUtil.getConfigProperty(container, parameter);
                var configParameter = new ConfigParameter(container, parameter, namespace);

                this.parameters.put(property.key(), configParameter);
            }

            var dependency = ReflectionUtil.getDependencyInfo(container, parameter);

            // Create a virtual configuration parameter if the parameter depends on a key in the config file
            if (dependency != null) {
                if (dependency.isKeyDependency()) {
                    this.parameters.computeIfAbsent(
                        dependency.name(), key -> {
                            var virtualConfigParameter = new ConfigParameter(key, namespace);

                            // Trigger a check for existence
                            // This method will throw if the key doesn't exist in the configuration file
                            getConfigValue(virtualConfigParameter);

                            return virtualConfigParameter;
                        }
                    );
                }
            }
        }
    }

    /**
     * Resolves and returns the final configuration value for the specified parameter.
     * <p>
     * The resolution process respects {@link DependsOnProperty} annotations:
     * <ul>
     *     <li>
     *         If the property has no dependency, its value is directly read from the configuration {@code .properties}
     *         file (or its {@link ConfigProperty#defaultValue()} if the key is not found).
     *     </li>
     *     <li>
     *         If the property defines a dependency using {@link DependsOnProperty}, the value from the configuration
     *         file will be loaded if, and only if, the dependency condition is satisfied (i.e., its resolved
     *         value matches the {@link DependsOnProperty#value()}).
     *         Otherwise, {@link ConfigProperty#defaultValue()} will be returned.
     *     </li>
     * </ul>
     *
     * @param parameter The parameter whose value should be resolved.
     *
     * @return The resolved value.
     *
     * @throws InvalidDeclarationException If the parameter is not declared properly.
     * @throws CircularDependencyException If a circular dependency is detected in
     *                                     the dependency chain of the parameter.
     */
    public String resolveValue(Parameter parameter) {
        var configParameter = getConfigParameter(parameter);

        return !configParameter.hasDependency || isDependencyChainSatisfied(configParameter, new LinkedHashSet<>())
            ? getConfigValue(configParameter)
            : getDefaultValue(configParameter);
    }

    /**
     * Returns the default value for the specified parameter.
     *
     * @param parameter The parameter whose default value should be retrieved
     *
     * @return The parameter's default value.
     *
     * @throws InvalidDeclarationException If the parameter is not declared properly.
     */
    public String getDefaultValue(Parameter parameter) {
        return getDefaultValue(getConfigParameter(parameter));
    }

    /**
     * Determines whether the dependency condition for the specified parameter is satisfied.
     * <p>
     * The condition is satisfied if, and only if:
     * <ul>
     *     <li>The namespace doesn't have a dependency</li>
     *     <li>The resolved value of the declared dependency satisfies the dependency condition</li>
     * </ul>
     *
     * @param parameter The parameter whose condition should be checked; must be annotated with {@link ConfigNamespace}
     *
     * @return {@code true} if the namespace's dependency condition is satisfied, {@code false} otherwise.
     *
     * @throws InvalidDeclarationException If the namespace depends on an unknown parameter.
     * @throws CircularDependencyException If a circular dependency is detected (e.g., A -> B -> A).
     */
    public boolean isNamespaceDependencySatisfied(Parameter parameter) {
        var dependencyInfo = ReflectionUtil.getDependencyInfo(container, parameter);

        if (dependencyInfo == null) {
            return true;
        }

        var dependency = parameters.get(dependencyInfo.name());
        var dependencyValue = !dependency.hasDependency || isDependencyChainSatisfied(dependency, new LinkedHashSet<>())
            ? getConfigValue(dependency)
            : getDefaultValue(dependency);
        var requiredValue = dependencyInfo.value();
        var operator = dependencyInfo.operator();

        return operator.isEmpty()
            ? requiredValue.equals(dependencyValue)
            : compareWithChecker(parameter, dependencyInfo, dependencyValue, operator, requiredValue);
    }

    /**
     * Determines whether the dependency of the specified parameter matches the required value,
     * traversing the whole dependency chain (e.g., A depends on B, B depends on C).
     *
     * @param dependentParameter The parameter whose dependency should be checked
     * @param checkedLinks       A register of visited links during the current dependency chain traversal
     *
     * @return {@code true} if the dependency condition for the current {@code dependentParameter} and its
     * entire upstream chain is satisfied, {@code false} otherwise.
     *
     * @throws InvalidDeclarationException If a parameter in the dependency chain depends on an unknown parameter.
     * @throws CircularDependencyException If a circular dependency is detected (e.g., A -> B -> A).
     */
    private boolean isDependencyChainSatisfied(ConfigParameter dependentParameter, Set<String> checkedLinks) {
        if (!checkedLinks.add(dependentParameter.propertyKey)) {
            var links = checkedLinks.stream().map(parameters::get).map(ConfigParameter::toString).toList();
            var chain = String.join(" depends on -> ", links) + " depends on -> " + dependentParameter;

            throw new CircularDependencyException("Circular dependency chain detected: " + chain);
        }

        var dependency = getDependency(dependentParameter);
        var dependencyValue = !dependency.hasDependency || isDependencyChainSatisfied(dependency, checkedLinks)
            ? getConfigValue(dependency)
            : getDefaultValue(dependency);
        var requiredValue = dependentParameter.dependencyValue;
        var operator = dependentParameter.checkOperator;

        return operator.isEmpty()
            ? dependentParameter.dependencyValue.equals(dependencyValue)
            : compareWithChecker(dependentParameter, dependencyValue, operator, requiredValue);
    }

    /**
     * Uses the specified operator to compare the specified dependency's value against its required value.
     *
     * @param dependentParameter The parameter to be used to build a debug message if the check fails
     * @param dependencyValue    The resolved value of the dependency
     * @param operator           The operator to be used for the comparison
     * @param requiredValue      The value to compare the dependency's value against
     *
     * @return {@code true} if the values are equal, {@code false} otherwise.
     *
     * @throws NullPointerException If {@link #dependencyChecker} is {@code null};
     */
    private boolean compareWithChecker(
        Parameter dependentParameter,
        ReflectionUtil.DependencyInfo dependencyInfo,
        String dependencyValue,
        String operator,
        String requiredValue
    ) {
        if (dependencyChecker == null) {
            var fullName = container.getSimpleName() + "." + dependentParameter.getName();
            var identity = fullName + "(operator " + dependencyInfo.operator() + ")";

            throw new NullPointerException("No custom dependency checker found for " + identity);
        }

        return dependencyChecker.check(dependencyValue, operator, requiredValue);
    }

    /**
     * Uses the specified operator to compare the specified dependency's value against its required value.
     *
     * @param dependentParameter The parameter to be used to build a debug message if the check fails
     * @param dependencyValue    The resolved value of the dependency
     * @param operator           The operator to be used for the comparison
     * @param requiredValue      The value to compare the dependency's value against
     *
     * @return {@code true} if the values are equal, {@code false} otherwise.
     *
     * @throws NullPointerException If {@link #dependencyChecker} is {@code null};
     */
    private boolean compareWithChecker(
        ConfigParameter dependentParameter,
        String dependencyValue,
        String operator,
        String requiredValue
    ) {
        if (dependencyChecker == null) {
            var identity = dependentParameter + "(operator " + dependentParameter.checkOperator + ")";

            throw new NullPointerException("No custom dependency checker found for " + identity);
        }

        return dependencyChecker.check(dependencyValue, operator, requiredValue);
    }

    /**
     * Returns the {@link ConfigParameter} associated with the specified parameter.
     *
     * @param parameter The parameter whose {@link ConfigParameter} representation should be retrieved
     *
     * @return The {@link ConfigParameter} corresponding to the specified parameter.
     *
     * @throws InvalidDeclarationException If the specified parameter is not associated with a {@link ConfigParameter}.
     */
    private ConfigParameter getConfigParameter(Parameter parameter) {
        var property = ReflectionUtil.getConfigProperty(container, parameter);

        return parameters.get(property.key());
    }

    /**
     * Returns the {@link ConfigParameter} the specified parameter depends on.
     *
     * @param parameter The parameter whose dependency should be retrieved
     *
     * @return The {@link ConfigParameter} dependency.
     *
     * @throws InvalidDeclarationException If the specified parameter doesn't declare a dependency,
     *                                     or the dependency is either in a different namespace or doesn't exist.
     */
    private ConfigParameter getDependency(ConfigParameter parameter) {
        var dependency = parameters.get(parameter.dependencyName);

        if (dependency == null) {
            throw new InvalidDeclarationException(
                "Cannot resolve dependency " + parameter.dependencyName + " declared on " + parameter
            );
        }

        return dependency;
    }

    /**
     * Attempts to retrieve the configuration value corresponding to {@link ConfigParameter#fileKey}
     * from the configuration source.
     *
     * @param parameter The parameter whose associated configuration value should be retrieved
     *
     * @return The value of the key specified by {@link ConfigParameter#fileKey},
     * or {@link ConfigParameter#defaultValue} if no such key is defined in the source.
     *
     * @throws InvalidDeclarationException If the specified parameter is virtual
     *                                     and its configuration value cannot be found
     */
    private String getConfigValue(ConfigParameter parameter) {
        return resolvedValues.computeIfAbsent(
            parameter.fileKey, key -> {
                var value = properties.getProperty(key, parameter.isVirtual ? null : getDefaultValue(parameter));

                // Configuration property depends on nonexistent key in the configuration file
                if (value == null) {
                    throw new InvalidDeclarationException(
                        "Property key '%s' of %s cannot be found in the configuration file"
                            .formatted(parameter.fileKey, parameter)
                    );
                }

                return value;
            }
        );
    }

    /**
     * Returns the default value for the specified parameter.
     *
     * @param parameter The parameter whose default value should be retrieved
     *
     * @return The default value for the parameter.
     *
     * @throws InvalidDeclarationException If the parameter specifies a default property
     *                                     that's missing in the configuration file.
     */
    private String getDefaultValue(ConfigParameter parameter) {
        return resolvedDefaultValues.computeIfAbsent(
            parameter.fileKey, key -> {
                String defaultValue;

                if (parameter.propertyDefaultKey.isBlank()) {
                    defaultValue = parameter.defaultValue;
                } else {
                    var defaultKeyValue = properties.getProperty(parameter.propertyDefaultKey);

                    if (defaultKeyValue == null) {
                        throw new InvalidDeclarationException(
                            "Default property has been set for %s but is not defined in the configuration file"
                                .formatted(parameter)
                        );
                    } else {
                        defaultValue = defaultKeyValue;
                    }
                }

                return defaultValue;
            }
        );
    }
}
