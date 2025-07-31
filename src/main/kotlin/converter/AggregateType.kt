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

import com.jvanev.kconfig.UnsupportedTypeConversionException
import java.lang.reflect.Type

/**
 * Regex to split strings by commas (e.g., 1, 2, 3). Any space around the comma will be trimmed.
 */
internal val ARRAY_SPLIT_REGEX = "\\s*,\\s*".toRegex()

/**
 * Regex to split strings by colons (e.g., Key:Value). Any space around the colon will be trimmed.
 */
internal val KEY_VALUE_SPLIT_REGEX = "\\s*:\\s*".toRegex()

/**
 * Determines if this type represents an array of primitive type.
 */
internal val Class<*>.isPrimitiveArray: Boolean
    get() = this == ByteArray::class.java ||
            this == ShortArray::class.java ||
            this == IntArray::class.java ||
            this == LongArray::class.java ||
            this == FloatArray::class.java ||
            this == DoubleArray::class.java ||
            this == BooleanArray::class.java ||
            this == CharArray::class.java

/**
 * Returns the given [value] as an array of primitives of this type.
 *
 * Throws [UnsupportedOperationException] if conversions to this type are not supported.
 */
internal fun Class<*>.toPrimitiveArray(value: String): Any {
    val array = value.split(ARRAY_SPLIT_REGEX).filter { it.isNotBlank() }

    return when (this) {
        ByteArray::class.java -> ByteArray(array.size) { Integer.decode(array[it]).toByte() }
        ShortArray::class.java -> ShortArray(array.size) { Integer.decode(array[it]).toShort() }
        IntArray::class.java -> IntArray(array.size) { Integer.decode(array[it]) }
        LongArray::class.java -> LongArray(array.size) { java.lang.Long.decode(array[it]) }
        FloatArray::class.java -> FloatArray(array.size) { array[it].toFloat() }
        DoubleArray::class.java -> DoubleArray(array.size) { array[it].toDouble() }
        BooleanArray::class.java -> BooleanArray(array.size) { array[it].toBoolean() }
        CharArray::class.java -> CharArray(array.size) {
            val char = array[it]

            if (char.length != 1) {
                throw IllegalArgumentException("Cannot convert '$char' to char. Expected single character.")
            }

            array[it][0]
        }

        else -> throw UnsupportedTypeConversionException(this, value)
    }
}

/**
 * Determines if this type represents a collection object.
 */
internal val Class<*>.isCollection
    get() = this == Collection::class.java ||
            this == List::class.java ||
            this == Set::class.java

/**
 * Returns the given [value] converted to a collection of this type.
 *
 * Throws [UnsupportedOperationException] if conversions to the given collection type are not supported.
 */
internal fun Class<*>.toCollection(converter: ValueConverter, value: String, valueType: Type): Collection<Any> {
    val entries = if (value.isBlank()) emptyList() else value.split(ARRAY_SPLIT_REGEX)
    val collection: MutableCollection<Any> = when (this) {
        Collection::class.java, List::class.java -> mutableListOf()

        Set::class.java -> mutableSetOf()

        else -> throw UnsupportedTypeConversionException(this, value)
    }

    for (entry in entries) {
        collection.add(converter.convert(entry, valueType))
    }

    return collection
}

/**
 * Determines if this type represents a key-value map object.
 */
internal val Class<*>.isMap get() = this == Map::class.java

/**
 * Returns the given [value] converted to a key-value map.
 */
internal fun Class<*>.toMap(converter: ValueConverter, value: String, keyType: Type, valueType: Type): Map<Any, Any> {
    val entries = if (value.isBlank()) emptyList() else value.split(ARRAY_SPLIT_REGEX)
    val map = mutableMapOf<Any, Any>()

    for (entry in entries) {
        val keyValue = entry.split(KEY_VALUE_SPLIT_REGEX).also { pair ->
            if (pair.size != 2) {
                throw IllegalArgumentException("$this only supports key:value formatted pairs")
            }
        }
        val entryKey = converter.convert(keyValue[0], keyType)
        val entryValue = converter.convert(keyValue[1], valueType)
        map[entryKey] = entryValue
    }

    return map
}
