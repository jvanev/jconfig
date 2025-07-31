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
package com.jvanev.kconfig.converter

/**
 * Determines if this type represents a boxed primitive object.
 */
internal val Class<*>.isBoxedPrimitive
    get() = this == java.lang.Byte::class.java ||
            this == java.lang.Short::class.java ||
            this == Integer::class.java ||
            this == java.lang.Long::class.java ||
            this == java.lang.Float::class.java ||
            this == java.lang.Double::class.java ||
            this == java.lang.Boolean::class.java ||
            this == Character::class.java

/**
 * Returns the given [value] converted into the appropriate primitive.
 *
 * Throws an [IllegalArgumentException] if the [value] cannot be converted.
 */
internal fun Class<*>.toPrimitive(value: String): Any {
    if (value.isEmpty()) {
        throw IllegalArgumentException("Cannot convert an empty value to primitive type ${this.simpleName}")
    }

    return when (this) {
        Byte::class.java, java.lang.Byte::class.java -> Integer.decode(value).toByte()
        Short::class.java, java.lang.Short::class.java -> Integer.decode(value).toShort()
        Int::class.java, Integer::class.java -> Integer.decode(value)
        Long::class.java, java.lang.Long::class.java -> java.lang.Long.decode(value)
        Float::class.java, java.lang.Float::class.java -> value.toFloat()
        Double::class.java, java.lang.Double::class.java -> value.toDouble()
        Boolean::class.java, java.lang.Boolean::class.java -> value.toBoolean()

        else -> {
            if (value.length != 1) {
                throw IllegalArgumentException("Cannot convert '$value' to char. Expected single character.")
            }

            value.first()
        }
    }
}
