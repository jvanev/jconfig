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
package com.jvanev.kconfig.annotation

/**
 * Designates a primary constructor property as a *Configuration Group*, allowing it to encapsulate
 * a related set of [ConfigProperty] definitions from a *Configuration Type*.
 *
 * A *Configuration Group* defines a logical grouping of configuration properties,
 * typically mapping to a subsection or a set of conditionally active properties within a *Configuration Type*.
 *
 * ## How It Works
 *
 * Properties within a *Configuration Group* are resolved relative to its defined [namespace].
 *
 * - **If [namespace] is provided (e.g., `LoginServer.Network`):** All [ConfigProperty] annotations
 * declared within the *Configuration Group*'s type will be resolved using this namespace
 * as a prefix (e.g., `LoginServer.Network.Host`).
 *
 * - **If [namespace] is omitted (empty string):** The properties within this group's type
 * will be resolved directly from the *parent* configuration's scope (i.e., without any additional namespace prefix).
 * This effectively places the group's properties in the *default namespace*.
 *
 * Any **Configuration Group** can also be made conditionally active by applying a [DependsOn] annotation
 * to the same property. If the [DependsOn] condition is not satisfied, the *Configuration Group* will not be active,
 * and its properties will resolve to their respective [ConfigProperty.defaultValue].
 *
 * ## Important Considerations
 * - The type of the property annotated with `@ConfigGroup` must itself be a class
 * whose primary constructor properties are annotated with [ConfigProperty].
 * - The `defaultValue` of properties within a **Configuration Group**'s type will be used
 * if the specific property key is not found in the configuration, or if the group itself
 * is inactive due to an unsatisfied [DependsOn] condition.
 *
 * @property namespace An optional string that serves as a prefix for all properties
 * within this group in the configuration file. An empty string (default) means properties
 * are resolved from the parent configuration's scope (the *default namespace*).
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigGroup(val namespace: String = "")
