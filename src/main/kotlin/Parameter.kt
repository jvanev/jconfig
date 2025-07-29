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
package com.jvanev.kconfig

import com.jvanev.kconfig.annotation.ConfigGroup
import com.jvanev.kconfig.annotation.ConfigProperty
import com.jvanev.kconfig.annotation.DependsOn
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

/**
 * Returns the [ConfigProperty] annotation of this parameter.
 *
 * @param container The [KClass] type in which this parameter is declared.
 *
 * @return The [ConfigProperty] annotation found on this parameter.
 *
 * @throws IllegalArgumentException If this parameter is not annotated with [ConfigProperty].
 */
internal fun KParameter.requireConfigProperty(container: KClass<*>) = requireNotNull(findAnnotation<ConfigProperty>()) {
    "Parameter '${this.name}' in '${container.simpleName}' is not annotated with @ConfigProperty."
}

/**
 * Determines whether this parameter is annotated with [ConfigGroup].
 */
internal val KParameter.isGroup get() = hasAnnotation<ConfigGroup>()

/**
 * Returns the [ConfigGroup] annotation of this parameter.
 *
 * @param container The [KClass] type in which this parameter is declared.
 *
 * @return The [ConfigGroup] annotation found on this parameter.
 *
 * @throws IllegalArgumentException If this parameter is not annotated with [ConfigGroup].
 */
internal fun KParameter.requireConfigGroup(container: KClass<*>) = requireNotNull(findAnnotation<ConfigGroup>()) {
    "Parameter '${this.name}' in '${container.simpleName}' is not annotated with @ConfigGroup."
}

/**
 * Determines whether this parameter is annotated with [DependsOn].
 */
internal val KParameter.hasDependency get() = hasAnnotation<DependsOn>()

/**
 * Returns the [DependsOn] annotation of this parameter, or `null` if it is not present.
 */
internal fun KParameter.getDependsOn() = findAnnotation<DependsOn>()

/**
 * Returns the [DependsOn] annotation of this parameter.
 *
 * @param container The [KClass] type in which this parameter is declared.
 *
 * @return The [DependsOn] annotation found on this parameter.
 *
 * @throws IllegalArgumentException If this parameter is not annotated with [DependsOn].
 */
internal fun KParameter.requireDependsOn(container: KClass<*>) = requireNotNull(findAnnotation<DependsOn>()) {
    "Parameter '${this.name}' in '${container.simpleName}' is not annotated with @DependsOn."
}
