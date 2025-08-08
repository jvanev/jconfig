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
package com.jvanev.jxconfig.internal;

import com.jvanev.jxconfig.annotation.ConfigGroup;
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.annotation.DependsOnKey;
import com.jvanev.jxconfig.annotation.DependsOnProperty;
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import java.lang.reflect.Parameter;

/**
 * Provides a set of convenience methods for working with Java reflection.
 */
public final class ReflectionUtil {
    // Utility class
    private ReflectionUtil() {
    }

    /**
     * Returns the {@link ConfigProperty} annotation of the specified parameter.
     *
     * @param clazz     The type declaring the annotated parameter
     * @param parameter The annotated parameter
     *
     * @return The {@link ConfigProperty} annotation of the specified parameter.
     *
     * @throws InvalidDeclarationException If the parameter is not annotated with {@link ConfigProperty}.
     */
    public static ConfigProperty getConfigProperty(Class<?> clazz, Parameter parameter) {
        var annotation = parameter.getDeclaredAnnotation(ConfigProperty.class);

        if (annotation == null) {
            throw new InvalidDeclarationException(
                "Parameter '%s' declared in %s is not annotated with @ConfigProperty."
                    .formatted(parameter.getName(), clazz.getName())
            );
        }

        return annotation;
    }

    /**
     * Determines whether the specified parameter is annotated with {@link ConfigGroup}.
     *
     * @param parameter The parameter to be checked
     *
     * @return {@code true} if the annotation is present, {@code false} otherwise.
     */
    public static boolean isConfigGroup(Parameter parameter) {
        return parameter.isAnnotationPresent(ConfigGroup.class);
    }

    /**
     * Returns the {@link ConfigGroup} annotation of the specified parameter.
     *
     * @param parameter The annotated parameter
     *
     * @return The {@link ConfigGroup} annotation of the parameter if present, {@code null} otherwise.
     */
    public static ConfigGroup getConfigGroup(Parameter parameter) {
        return parameter.getDeclaredAnnotation(ConfigGroup.class);
    }

    /**
     * Returns dependency information for the specified parameter.
     *
     * @param parameter The parameter whose dependency information should be retrieved
     *
     * @return The {@link DependencyInfo} if the parameter is annotated with a dependency annotation,
     * {@code null} otherwise.
     */
    public static DependencyInfo getDependencyInfo(Class<?> type, Parameter parameter) {
        var propertyInfo = parameter.getDeclaredAnnotation(DependsOnProperty.class);
        var keyInfo = parameter.getDeclaredAnnotation(DependsOnKey.class);

        if (propertyInfo != null && keyInfo != null) {
            throw new InvalidDeclarationException(
                "Parameter %s.%s cannot be annotated with @DependsOnKey and @DependsOnProperty at the same time"
                    .formatted(type.getSimpleName(), parameter.getName())
            );
        }

        if (propertyInfo != null) {
            return new DependencyInfo(propertyInfo.name(), propertyInfo.operator(), propertyInfo.value(), false);
        }

        if (keyInfo != null) {
            return new DependencyInfo(keyInfo.name(), keyInfo.operator(), keyInfo.value(), true);
        }

        return null;
    }

    /**
     * A unified view that contains the information of either {@link DependsOnProperty} or {@link DependsOnKey}.
     *
     * @param name             The name of the entity the annotated parameter depends on
     * @param operator         The operator to be used to compare the value of the dependency to the required value
     * @param value            The required value to satisfy the dependency condition
     * @param isDependentOnKey Determines whether the dependency is a key in the configuration file
     */
    public record DependencyInfo(String name, String operator, String value, boolean isDependentOnKey) {
    }
}
