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
package com.jvanev.jconfig.internal.resolver;

import com.jvanev.jconfig.annotation.ConfigGroup;
import com.jvanev.jconfig.annotation.ConfigProperty;
import com.jvanev.jconfig.annotation.DependsOn;
import com.jvanev.jconfig.exception.CircularDependencyException;
import com.jvanev.jconfig.exception.InvalidDeclarationException;
import com.jvanev.jconfig.internal.ReflectionUtil;
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

    /**
     * Contains the metadata of constructor parameters annotated with {@link ConfigProperty}, keyed by their
     * {@link ConfigProperty#name()}.
     */
    private final Map<String, ConfigParameter> parameters;

    /**
     * A cache for already resolved values, mapped to their fully qualified key in the configuration file.
     */
    private final Map<String, String> resolvedValues;

    /**
     * Creates a new ValueResolver.
     *
     * @param properties The {@link Properties} instance containing the raw configuration key-value pairs
     * @param container  The {@link Class} whose configuration values will be resolved
     * @param namespace  The namespace from which configuration values will be retrieved
     * @param parameters The parameters for which value resolution will be performed
     */
    public ValueResolver(Properties properties, Class<?> container, String namespace, Parameter[] parameters) {
        this.properties = properties;
        this.container = container;
        this.parameters = new HashMap<>();
        this.resolvedValues = new HashMap<>();

        for (Parameter parameter : parameters) {
            if (!ReflectionUtil.isConfigGroup(parameter)) {
                var property = ReflectionUtil.getConfigProperty(container, parameter);
                var configParameter = new ConfigParameter(container, parameter, namespace);

                this.parameters.put(property.name(), configParameter);
            }
        }
    }

    /**
     * Resolves and returns the final configuration value for the specified parameter.
     * <p>
     * The resolution process respects {@link DependsOn} annotations:
     * <ul>
     *     <li>
     *         If the property has no dependency, its value is directly read from the configuration {@code .properties}
     *         file (or its {@link ConfigProperty#defaultValue()} if the key is not found).
     *     </li>
     *     <li>
     *         If the property defines a dependency using {@link DependsOn}, the value from the configuration
     *         file will be loaded if, and only if, the dependency condition is satisfied (i.e., its resolved
     *         value matches the {@link DependsOn#value()}).
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

        return !configParameter.hasDependency || isDependencySatisfied(configParameter)
            ? getConfigValue(configParameter)
            : configParameter.defaultValue;

    }

    /**
     * Determines whether the dependency condition for the specified parameter is satisfied.
     * <p>
     * The condition is satisfied if, and only if:
     * <ul>
     *     <li>The group doesn't have a dependency</li>
     *     <li>The resolved value of the declared dependency satisfies the dependency condition</li>
     * </ul>
     *
     * @param parameter The parameter whose condition should be checked; must be annotated with {@link ConfigGroup}
     *
     * @return {@code true} if the group's dependency condition is satisfied, {@code false} otherwise.
     *
     * @throws InvalidDeclarationException If the group depends on an unknown parameter.
     * @throws CircularDependencyException If a circular dependency is detected (e.g., A -> B -> A).
     */
    public boolean isGroupDependencySatisfied(Parameter parameter) {
        if (!ReflectionUtil.hasDependency(parameter)) {
            return true;
        }

        var dependencyInfo = ReflectionUtil.getDependsOn(parameter);
        var dependency = parameters.get(dependencyInfo.property());
        var dependencyValue = !dependency.hasDependency || isDependencySatisfied(dependency)
            ? getConfigValue(dependency)
            : dependency.defaultValue;

        return dependencyInfo.value().equals(dependencyValue);
    }

    /**
     * Determines whether the dependency condition for the specified parameter is satisfied.
     * <p>
     * The condition is satisfied if, and only if:
     * <ul>
     *     <li>The parameter doesn't have a dependency</li>
     *     <li>The resolved value of the declared dependency satisfies the dependency condition</li>
     * </ul>
     *
     * @param parameter The parameter whose dependency should be checked
     *
     * @return {@code true} if the dependency condition is satisfied, {@code false} otherwise.
     *
     * @throws InvalidDeclarationException If the parameter depends on an unknown parameter.
     * @throws CircularDependencyException If a circular dependency is detected (e.g., A -> B -> A).
     */
    private boolean isDependencySatisfied(ConfigParameter parameter) {
        var dependency = getDependency(parameter);
        var dependencyValue = !dependency.hasDependency || isDependencyChainSatisfied(dependency, new LinkedHashSet<>())
            ? getConfigValue(dependency)
            : dependency.defaultValue;

        return parameter.dependencyValue.equals(dependencyValue);
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
        if (!checkedLinks.add(dependentParameter.propertyName)) {
            var links = checkedLinks.stream().map(parameters::get).toList();
            var chain = new StringBuilder();

            for (int i = 0; i < checkedLinks.size(); i++) {
                if (i > 0) {
                    chain.append(" depends on -> ");
                }

                chain.append(links.get(i));
            }

            throw new CircularDependencyException("Circular dependency chain detected: " + chain);
        }

        var dependency = getDependency(dependentParameter);
        var dependencyValue = !dependency.hasDependency || isDependencyChainSatisfied(dependency, checkedLinks)
            ? getConfigValue(dependency)
            : dependency.defaultValue;

        return dependentParameter.dependencyValue.equals(dependencyValue);
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
        var configParameter = parameters.get(property.name());

        if (configParameter == null) {
            throw new InvalidDeclarationException(
                "Parameter %s.%s is not a configuration parameter"
                    .formatted(container.getSimpleName(), parameter.getName())
            );
        }

        return configParameter;
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
     * Attempts to retrieve the configuration value corresponding to {@link ConfigParameter#keyName}
     * from the configuration source.
     *
     * @param parameter The parameter whose associated configuration value should be retrieved
     *
     * @return The value of the key specified by {@link ConfigParameter#keyName},
     * or {@link ConfigParameter#defaultValue} if no such key is defined in the source.
     */
    private String getConfigValue(ConfigParameter parameter) {
        return resolvedValues
            .computeIfAbsent(parameter.keyName, key -> properties.getProperty(key, parameter.defaultValue));
    }
}
