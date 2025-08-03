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

import com.jvanev.jconfig.annotation.ConfigProperty;
import com.jvanev.jconfig.annotation.DependsOn;
import com.jvanev.jconfig.internal.ReflectionUtil;
import java.lang.reflect.Parameter;

/**
 * Represents a constructor parameter designated as a configuration parameter
 * (i.e., annotated with {@link ConfigProperty}). Exposes a combined view of the parameter
 * and its annotations.
 */
final class ConfigParameter {
    /**
     * The name of the constructor parameter along with the name of its declaring class.
     */
    final String parameterName;

    /**
     * The {@link ConfigProperty#name()} for this parameter.
     */
    final String propertyName;

    /**
     * The {@link ConfigProperty#defaultValue()} for this parameter.
     */
    final String defaultValue;

    /**
     * The fully qualified name of the {@code .properties} configuration key this parameter is mapped to.
     */
    final String keyName;

    /**
     * Determines whether this parameter is annotated with {@link DependsOn}.
     */
    final boolean hasDependency;

    /**
     * The {@link DependsOn#property()} for this parameter. Might be an empty string if the parameter
     * is not annotated with {@link DependsOn}, use {@link #hasDependency} to determine if it is.
     */
    final String dependencyName;

    /**
     * The {@link DependsOn#value()} for this parameter. Might be an empty string if the parameter
     * is not annotated with {@link DependsOn}, use {@link #hasDependency} to determine if it is.
     */
    final String dependencyValue;

    /**
     * Creates a new ConfigParameter.
     *
     * @param container The declaring class of the parameter
     * @param parameter The parameter this class represents
     */
    ConfigParameter(Class<?> container, Parameter parameter, String namespace) {
        parameterName = container.getSimpleName() + "." + parameter.getName();

        var property = ReflectionUtil.getConfigProperty(container, parameter);
        propertyName = property.name();
        defaultValue = property.defaultValue();

        keyName = namespace.isBlank() ? propertyName : namespace + "." + propertyName;

        var dependency = ReflectionUtil.getDependsOn(parameter);
        hasDependency = dependency != null;
        dependencyName = hasDependency ? dependency.property() : "";
        dependencyValue = hasDependency ? dependency.value() : "";
    }

    @Override
    public String toString() {
        return propertyName + " (" + parameterName + ")";
    }
}
