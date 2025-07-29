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

import com.jvanev.kconfig.exception.UnsupportedTypeConversionException
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Provides a flexible mechanism for converting **String** values to various common Kotlin types.
 *
 * This converter offers robust support for automatically converting string representations
 * into the following types:
 *
 * ### Supported Conversions:
 * - **Primitive Types:** [Byte], [Short], [Int], [Long], [Float], [Double], [Boolean], [Char].
 * - **Primitive Arrays:** [ByteArray], [ShortArray], [IntArray], [LongArray], [FloatArray],
 * [DoubleArray], [BooleanArray], [CharArray].
 * - **Enumerations:** Any [Enum] type, by matching the string value to an enum entry's name.
 * - **Reference Types with `valueOf(String)`:** Any class that provides a public, static `valueOf(String)` method
 * (e.g., [java.math.BigDecimal], [java.util.UUID]). The return type of this method must be assignable
 * to the target type.
 * - **Collections:** [Collection], [List], [Set]. Elements within the collection are also converted
 * recursively based on their generic type argument
 * (e.g., `List<Int>` will convert string "1,2,3" into a list of integers).
 * - **Maps:** [Map]. Both keys and values within the map are converted recursively based on their
 * generic type arguments
 * (e.g., `Map<String, Int>` will convert "key1=1,key2=2" into a map with string keys and integer values).
 *
 * ### Custom Converters and Overriding Behavior:
 * Additional type conversion support can be seamlessly integrated using the [addValueConverter] method.
 * Custom converters registered via this method take precedence over the default conversion logic.
 * This allows you to override existing behaviors or introduce support for new, complex types.
 *
 * For instance, if you register a converter for `Int::class.java`, the default string-to-integer
 * conversion provided by this class will be skipped, and your custom converter will be invoked instead.
 */
internal class ValueConverter {
    /**
     * Stores additional, user-defined value converters, mapped by the target type they can convert to.
     * These converters take precedence over the built-in conversion mechanisms.
     */
    private val valueConverters = mutableMapOf<Class<*>, (String, Type) -> Any>()

    /**
     * Registers a custom converter for a specific target type.
     *
     * When a conversion is requested for `type`, this `converter` function will be invoked
     * to perform the string-to-object conversion. Custom converters override default behavior.
     *
     * @param type The [Class] representing the target type this converter can produce.
     * @param converter A lambda function that takes the string `value` to convert and the `type`
     * to convert to, returning the converted object.
     */
    fun addValueConverter(type: Class<*>, converter: (String, Type) -> Any) {
        valueConverters[type] = converter
    }

    /**
     * Converts a given [String] value into an instance of the specified [type].
     *
     * This method attempts conversion using the following precedence:
     * 1. **Custom Converters:** Checks if a custom converter has been registered for `type`
     * or any of its supertypes/interfaces using [addValueConverter].
     * 2. **Built-in Conversions:** If no custom converter is found, it proceeds with its
     * default conversion logic for supported types (primitives, enums, collections, maps,
     * and types with `valueOf(String)` methods).
     *
     * If the conversion is not possible, an [UnsupportedTypeConversionException] is thrown.
     *
     * @param value The [String] value to convert.
     * @param type The target [Type] to convert the string into. This can be a [Class],
     * or a [ParameterizedType] for collections and maps.
     *
     * @return An instance of the specified `type` containing the converted value.
     *
     * @throws UnsupportedTypeConversionException If the conversion to the target `type` is not supported
     * by default or by any registered custom converters.
     */
    fun convert(value: String, type: Type): Any {
        val rawType = type.asClass()

        return when {
            valueConverters.containsKey(rawType) -> valueConverters[rawType]!!(value, type)

            rawType.isString -> value

            rawType.isEnum -> rawType.toEnum(value)

            rawType.isCollection -> rawType.toCollection(this, value, type.getTypeParameter())

            rawType.isMap -> rawType.toMap(this, value, type.getKeyTypeParameter(), type.getValueTypeParameter())

            rawType.isPrimitiveArray -> rawType.toPrimitiveArray(value)

            rawType.isPrimitive || rawType.isBoxedPrimitive -> rawType.toPrimitive(value)

            else -> {
                var result: Any? = null

                try {
                    val method = rawType.getMethod("valueOf", String::class.java)

                    if (Modifier.isStatic(method.modifiers) && rawType.isAssignableFrom(method.returnType)) {
                        result = method.invoke(null, value)
                    }
                } catch (_: NoSuchMethodException) {
                    // Continue to the custom converters
                }

                if (result == null) {
                    for ((key, converter) in valueConverters.entries.toList()) {
                        if (key.isAssignableFrom(rawType)) {
                            result = converter(value, rawType)

                            break
                        }
                    }
                }

                if (result == null) {
                    throw UnsupportedTypeConversionException(type)
                }

                result
            }
        }
    }
}

/**
 * Returns the class representing this type.
 *
 * Throws [UnsupportedTypeConversionException] if conversions into this type are not supported.
 */
private fun Type.asClass() = when (this) {
    is Class<*> -> this

    is ParameterizedType -> when (val classType = rawType as Class<*>) {
        Collection::class.java, List::class.java, Set::class.java, Map::class.java -> classType

        else -> throw UnsupportedTypeConversionException(
            "Unsupported parameterized type: $typeName. " +
            "Only Collection, List, Set, and Map types are supported by default. " +
            "Use addValueConverter(...) to add support for additional types."
        )
    }

    else -> throw UnsupportedTypeConversionException(this)
}

/**
 * Returns the actual type parameter of this type.
 *
 * Throws [IllegalArgumentException] if this type is not parameterized.
 */
private fun Type.getTypeParameter() = if (this is ParameterizedType) {
    actualTypeArguments[0]
} else {
    throw IllegalArgumentException("Target type $this is not a parameterized type")
}

/**
 * Returns the actual key type parameter of this type.
 *
 * Throws [IllegalArgumentException] if this type is not parameterized.
 */
private fun Type.getKeyTypeParameter() = if (this is ParameterizedType) {
    actualTypeArguments[0]
} else {
    throw IllegalArgumentException("Target type $this is not a parameterized type")
}

/**
 * Returns the actual value type parameter of this type.
 *
 * Throws [IllegalArgumentException] if this type is not parameterized.
 */
private fun Type.getValueTypeParameter() = if (this is ParameterizedType) {
    actualTypeArguments[1]
} else {
    throw IllegalArgumentException("Target type $this is not a parameterized type")
}
