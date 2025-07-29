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
 * Marks a primary constructor property of a *Configuration Type* as a *Configuration Property*.
 *
 * This annotation links a property in your Kotlin class to a specific key
 * within the configuration file, as defined by the containing [ConfigFile] annotation.
 *
 * ## How it Works
 *
 * During the initialization of the *Configuration Type*, the value for this property will be
 * fetched from the configuration file using its [name] as the lookup key.
 *
 * If the property's key is not found in the configuration file, or if the property
 * also declares a dependency using [DependsOn] and the dependency condition is not satisfied,
 * the [defaultValue] will be used instead.
 *
 * @property name The exact key of the property in the configuration file (e.g., "Database.Host").
 * @property defaultValue The default value to use if the property's key is not found in the
 * configuration file or if its [DependsOn] conditions are not satisfied.
 * Defaults to an empty string. While this is suitable for some types (e.g., arrays and collections),
 * other types (like primitives and enums) typically require an explicitly declared non-empty default value.
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigProperty(val name: String, val defaultValue: String = "")
