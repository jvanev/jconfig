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

import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.annotation.DependsOnKey;
import com.jvanev.jxconfig.annotation.DependsOnProperty;
import com.jvanev.jxconfig.internal.ReflectionUtil;
import java.lang.reflect.Parameter;

/**
 * Represents an entity designated as a configuration parameter. It can be based on a parameter
 * annotated with {@link ConfigProperty}, in which case it's a representation of a constructor parameter;
 * or, it can be based on a parameter annotated with {@link DependsOnKey}, in which case it's
 * a representation of a key in the configuration file.
 */
final class ConfigParameter {
    /**
     * The name of the constructor parameter along with the name of its declaring class.
     */
    final String parameterName;

    /**
     * The {@link ConfigProperty#key()} for this parameter.
     */
    final String propertyKey;

    /**
     * The {@link ConfigProperty#defaultKey()} for this parameter.
     */
    final String propertyDefaultKey;

    /**
     * The {@link ConfigProperty#defaultValue()} for this parameter.
     */
    final String defaultValue;

    /**
     * The fully qualified name of the {@code .properties} configuration key this parameter is mapped to.
     */
    final String fileKey;

    /**
     * Determines whether this parameter is virtual (i.e., it's a representation of a key in the config file).
     */
    final boolean isVirtual;

    /**
     * Determines whether this parameter is annotated with {@link DependsOnProperty} or {@link DependsOnKey}.
     */
    final boolean hasDependency;

    /**
     * The operator used to check the dependency condition for this parameter.
     */
    final String checkOperator;

    /**
     * The {@link DependsOnProperty#name()} or {@link DependsOnKey#name()} for this parameter.
     * Might be an empty string if the parameter doesn't declare a dependency,
     * use {@link #hasDependency} to determine if it does.
     */
    final String dependencyName;

    /**
     * The {@link DependsOnProperty#value()} or {@link DependsOnKey#value()} for this parameter.
     * Might be an empty string if the parameter doesn't declare a dependency,
     * use {@link #hasDependency} to determine if it does.
     */
    final String dependencyValue;

    /**
     * Creates a new ConfigParameter.
     *
     * @param container The declaring class of the parameter
     * @param parameter The parameter this class represents
     * @param namespace The namespace within which the parameter is declared
     */
    ConfigParameter(Class<?> container, Parameter parameter, String namespace) {
        parameterName = container.getSimpleName() + "." + parameter.getName();

        var property = ReflectionUtil.getConfigProperty(container, parameter);
        propertyKey = property.key();
        propertyDefaultKey = property.defaultKey();
        defaultValue = property.defaultValue();

        fileKey = namespace.isBlank() ? propertyKey : namespace + "." + propertyKey;
        isVirtual = false;

        var dependency = ReflectionUtil.getDependencyInfo(container, parameter);
        hasDependency = dependency != null;
        checkOperator = hasDependency ? dependency.operator() : "";
        dependencyName = hasDependency ? dependency.name() : "";
        dependencyValue = hasDependency ? dependency.value() : "";
    }

    /**
     * Creates a new virtual ConfigParameter (i.e., not declared as a parameter in the configuration type).
     *
     * @param key       The name of the key in the configuration file
     * @param namespace The namespace within which the key is declared
     */
    ConfigParameter(String key, String namespace) {
        // Virtual configuration parameters are not constructor parameters
        parameterName = "";

        propertyKey = key;

        // Virtual configuration parameters cannot have fallbacks
        propertyDefaultKey = "";
        defaultValue = "";

        fileKey = namespace.isBlank() ? propertyKey : namespace + "." + propertyKey;
        isVirtual = true;

        // Virtual configuration parameters cannot have dependencies
        hasDependency = false;
        checkOperator = "";
        dependencyName = "";
        dependencyValue = "";
    }

    @Override
    public String toString() {
        return propertyKey + " (" + (isVirtual ? "Virtual Parameter" : parameterName) + ")";
    }
}
