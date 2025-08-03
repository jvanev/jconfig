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
package com.jvanev.jconfig.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a constructor parameter to a key in a configuration file.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperty {
    /**
     * The name of the key in the configuration file.
     * <p>
     * If this annotation is used on a parameter of a {@link ConfigFile} or a type representing a group
     * of configurations in the default namespace, the name matches the key in the configuration file
     * exactly (e.g., {@code DeveloperMode}).
     * <p>
     * If this annotation is used on a parameter of a type representing a group of configurations in
     * a namespace, the name of the key is relative to that namespace (e.g., {@code Host} for {@code LoginServer.Host}).
     *
     * @return The name of the corresponding key.
     */
    String name();

    /**
     * The default value to be used if the configuration file doesn't contain a key
     * with the specified {@link #name()}, or if {@link DependsOn} is declared and its condition is not satisfied.
     * <p>
     * Defaults to an empty string. While this is suitable for some types (e.g., arrays and collections),
     * other types (like primitives and enums) require an explicitly specified default value.
     *
     * @return The default value for this property.
     */
    String defaultValue() default "";
}
