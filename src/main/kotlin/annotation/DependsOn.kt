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
 * Marks a *Configuration Property* as conditionally resolved based on another property's value.
 *
 * This annotation is applied to a *Configuration Property* to specify that its value at runtime
 * depends on the resolved value of a declared *Dependency*.
 *
 * ## Terms
 *
 * - **Configuration Property:** A property declared in the primary constructor of a
 * *Configuration Type*, annotated with [ConfigProperty].
 *
 * - **Dependency:** Another *Configuration Property* within the same *Configuration Type*
 * whose [ConfigProperty.name] matches the [property] value.
 *
 * ## How it Works
 *
 * If the *Dependency*'s resolved value matches the [value] specified for *this* property,
 * *this* property's runtime value will be fetched from the `.properties` file.
 * Otherwise, its [ConfigProperty.defaultValue] will be used.
 *
 * All comparisons for dependency conditions are case-sensitive.
 *
 * @property property The name of the *Dependency* property.
 * @property value The expected value of the *Dependency* property for *this* configuration property to be active.
 * Defaults to `True` for boolean-style activation.
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class DependsOn(val property: String, val value: String = "True")
