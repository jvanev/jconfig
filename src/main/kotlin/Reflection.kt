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
import java.lang.reflect.Parameter

/**
 * Returns the [annotationClass] annotation of this class.
 *
 * @throws InvalidDeclarationException If this class is not annotated with [annotationClass].
 * The exception will be thrown with the specified [message].
 */
internal inline fun <A : Annotation> Class<*>.requireAnnotation(annotationClass: Class<A>, message: () -> String): A =
    getDeclaredAnnotation(annotationClass) ?: throw InvalidDeclarationException(message())

/**
 * Returns the [ConfigProperty] annotation of this parameter.
 *
 * @param container The [Class] type in which this parameter is declared.
 *
 * @throws InvalidDeclarationException If this parameter is not annotated with [ConfigProperty].
 */
internal fun Parameter.requireConfigProperty(container: Class<*>) = requireAnnotation(ConfigProperty::class.java) {
    "Parameter '${this.name}' in '${container.simpleName}' is not annotated with @ConfigProperty."
}

/**
 * Determines whether this parameter is annotated with [ConfigGroup].
 */
internal val Parameter.isGroup get() = isAnnotationPresent(ConfigGroup::class.java)

/**
 * Returns the [ConfigGroup] annotation of this parameter.
 *
 * @param container The [Class] type in which this parameter is declared.
 *
 * @throws InvalidDeclarationException If this parameter is not annotated with [ConfigGroup].
 */
internal fun Parameter.requireConfigGroup(container: Class<*>) = requireAnnotation(ConfigGroup::class.java) {
    "Parameter '${this.name}' in '${container.simpleName}' is not annotated with @ConfigGroup."
}

/**
 * Determines whether this parameter is annotated with [DependsOn].
 */
internal val Parameter.hasDependency get() = isAnnotationPresent(DependsOn::class.java)

/**
 * Returns the [DependsOn] annotation of this parameter, or `null` if it is not present.
 */
internal fun Parameter.getDependsOn() = getDeclaredAnnotation(DependsOn::class.java)

/**
 * Returns the [DependsOn] annotation of this parameter.
 *
 * @param container The [Class] type in which this parameter is declared.
 *
 * @throws InvalidDeclarationException If this parameter is not annotated with [DependsOn].
 */
internal fun Parameter.requireDependsOn(container: Class<*>) = requireAnnotation(DependsOn::class.java) {
    "Parameter '${this.name}' in '${container.simpleName}' is not annotated with @DependsOn."
}

/**
 * Returns the [annotationClass] annotation of this parameter.
 *
 * @throws InvalidDeclarationException If this parameter is not annotated with [annotationClass].
 * The exception will be thrown with the specified [message].
 */
private inline fun <A : Annotation> Parameter.requireAnnotation(annotationClass: Class<A>, message: () -> String): A =
    getDeclaredAnnotation(annotationClass) ?: throw InvalidDeclarationException(message())
