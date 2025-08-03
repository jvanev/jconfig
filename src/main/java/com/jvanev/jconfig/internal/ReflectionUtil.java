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
package com.jvanev.jconfig.internal;

import com.jvanev.jconfig.annotation.ConfigGroup;
import com.jvanev.jconfig.annotation.ConfigProperty;
import com.jvanev.jconfig.annotation.DependsOn;
import com.jvanev.jconfig.exception.InvalidDeclarationException;
import java.lang.reflect.Parameter;

/**
 * Provides a set of convenience methods for working with Java reflection.
 */
public final class ReflectionUtil {
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
     * Determines whether the specified parameter is annotated with {@link DependsOn}.
     *
     * @param parameter The parameter to be checked
     *
     * @return {@code true} if the annotation is present, {@code false} otherwise.
     */
    public static boolean hasDependency(Parameter parameter) {
        return parameter.isAnnotationPresent(DependsOn.class);
    }

    /**
     * Returns the {@link DependsOn} annotation of the specified parameter.
     *
     * @param parameter The annotated parameter
     *
     * @return The {@link DependsOn} annotation of the parameter if present, {@code null} otherwise.
     */
    public static DependsOn getDependsOn(Parameter parameter) {
        return parameter.getDeclaredAnnotation(DependsOn.class);
    }
}
