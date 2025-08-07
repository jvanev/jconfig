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
package com.jvanev.jxconfig.converter.internal;

import com.jvanev.jxconfig.converter.ValueConverter;
import com.jvanev.jxconfig.exception.UnsupportedTypeConversionException;
import com.jvanev.jxconfig.exception.ValueConversionException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a flexible mechanism for converting {@link String} values to common Java types.
 * <p>
 * This converter offers robust support for automatically converting string representations
 * into the following types:
 *
 * <h3>Supported Conversions:</h3>
 * <ul>
 *     <li>
 *         <b>Primitive Types:</b> {@code byte}, {@code short}, {@code int}, {@code long}, {@code float},
 *         {@code double}, {@code boolean}, {@code char}.
 *     </li>
 *     <li>
 *         <b>Primitive Arrays:</b> {@code byte[]}, {@code short[]}, {@code int[]}, {@code long[]}, {@code float[]},
 *         {@code double[]}, {@code boolean[]}, {@code char[]}.
 *     </li>
 *     <li>
 *         <b>Enumerations:</b> Any {@link Enum} type, by matching the string value to an enum entry's name.
 *     </li>
 *     <li>
 *         <b>Reference Types with {@code valueOf(String)}:</b> Any class that provides a public,
 *         static <b>valueOf(String)</b> method. The return type of this method must be assignable to the target type.
 *     </li>
 *     <li>
 *         <b>Collections:</b> {@link List} and {@link Set}. Elements within the collection are also converted
 *         recursively based on their generic type argument (e.g., {@code List<Integer>}
 *         will convert string "1,2,3" into a list of integers).
 *     </li>
 *     <li>
 *         <b>Maps:</b> Both keys and values within the map are converted recursively based on their generic
 *         type arguments (e.g., {@code Map<String, Integer>} will convert "Key1 = 1, Key2 = 2" into a map with
 *         string keys and integer values).
 *     </li>
 * </ul>
 *
 * <b>Note:</b> The default implementation returns an {@link ArrayList} for {@link List},
 * and a {@link LinkedHashSet} for {@link Set}.
 *
 * <h3>Custom Converters and Overriding Behavior:</h3>
 * Additional type conversion support can be seamlessly integrated using the {@link #addValueConverter} method.
 * Custom converters registered via this method take precedence over the default conversion logic
 * for direct matches. If the newly registered converter's supported type doesn't match the target
 * type directly, a more specific conversion mechanism will be looked up first; if no direct match exists
 * the converters register will be reviewed in insertion order for a converter that can provide an assignable value.
 * <p>
 * This allows you to override existing behaviors or introduce support for new, complex types.
 * For instance, if you register a converter for {@code int.class}, the default string-to-integer
 * conversion provided by this class will be skipped, and your custom converter will be invoked instead.
 */
public final class Converter {
    private final Map<Class<?>, ValueConverter> converters = new LinkedHashMap<>();

    /**
     * A cache of references to static valueOf methods mapped to their declaring type.
     * It might contain a key with {@code null} value, which implies that the type was
     * already checked and no static valueOf method was found.
     */
    private final Map<Class<?>, ValueOfMethod> valueOfMethods = new ConcurrentHashMap<>();

    /**
     * Implementations of this interface represent a static valueOf method
     * accepting a string value and returning an instance of a specific type.
     */
    @FunctionalInterface
    private interface ValueOfMethod {
        /**
         * Returns the value produced by the actual implementation.
         *
         * @param instance The instance this method will be invoked on;
         *                 since we're looking for static methods, this value is always {@code null}
         * @param value    The value to be passed to the actual implementation
         *
         * @return The return value depends on the specific implementation of this method.
         *
         * @throws ReflectiveOperationException If an error occurs during the reflective call.
         */
        Object valueOf(Object instance, String value) throws ReflectiveOperationException;
    }

    /**
     * Registers a custom converter for a specific target type.
     * <p>
     * When a conversion is requested for the specified type, the specified converter will be invoked
     * to perform the string-to-object conversion. Custom converters override default behavior.
     *
     * @param type      The {@link Class} representing the target type this converter can produce
     * @param converter The {@code String} to {@code type} conversion mechanism
     */
    public void addValueConverter(Class<?> type, ValueConverter converter) {
        converters.put(type, converter);
    }

    /**
     * Converts the specified string value into an instance of the specified type.
     * <p>
     * This method attempts conversion using the following precedence:
     * <ul>
     *     <li>
     *         <b>Custom Converters:</b> Checks if a custom converter has been registered for the specified type
     *         or any of its supertypes/interfaces using {@link #addValueConverter}.
     *     </li>
     *     <li>
     *         <b>Built-in Conversions:</b> If no custom converter is found, it proceeds with its
     *         default conversion logic for supported types (primitives, enums, collections, maps,
     *         and types with {@code valueOf(String)} methods).
     *     </li>
     * </ul>
     *
     * @param type  The target type to convert the string into
     * @param value The string value to convert
     *
     * @return An instance of the specified type containing the converted value.
     *
     * @throws UnsupportedTypeConversionException If the conversion to the target type is not supported
     *                                            by default or by any registered custom converters.
     */
    public Object convert(Type type, String value) {
        var rawType = TypeUtil.getClass(type);

        if (converters.containsKey(rawType)) {
            return converters.get(rawType).convert(type, TypeUtil.getTypeArguments(type), value);
        }

        if (ReferenceTypeUtil.isString(rawType)) {
            return value;
        }

        if (rawType.isEnum()) {
            return ReferenceTypeUtil.toEnum(rawType, value);
        }

        if (AggregateTypeUtil.isCollection(rawType)) {
            return AggregateTypeUtil.toCollection(this, rawType, TypeUtil.getTypeArguments(type)[0], value);
        }

        if (AggregateTypeUtil.isMap(rawType)) {
            var typeArguments = TypeUtil.getTypeArguments(type);

            return AggregateTypeUtil.toMap(this, typeArguments[0], typeArguments[1], value);
        }

        if (AggregateTypeUtil.isPrimitiveArray(rawType)) {
            return AggregateTypeUtil.toPrimitiveArray(rawType, value);
        }

        if (rawType.isPrimitive() || PrimitiveTypeUtil.isBoxedPrimitive(rawType)) {
            return PrimitiveTypeUtil.toPrimitive(rawType, value);
        }

        Object result = null;

        try {
            var valueOfMethod = valueOfMethods.computeIfAbsent(
                rawType, key -> {
                    try {
                        var method = key.getMethod("valueOf", String.class);

                        if (Modifier.isStatic(method.getModifiers()) && key.isAssignableFrom(method.getReturnType())) {
                            return method::invoke;
                        }
                    } catch (NoSuchMethodException e) {
                        // Continue to the custom converters
                    }

                    return null;
                }
            );

            if (valueOfMethod != null) {
                result = valueOfMethod.valueOf(null, value);
            }
        } catch (Exception e) {
            throw new ValueConversionException(
                "An error occurred while converting '" + value + "' to " + rawType.getSimpleName() +
                    " using valueOf method.",
                e
            );
        }

        if (result == null) {
            for (var set : converters.entrySet()) {
                if (set.getKey().isAssignableFrom(rawType)) {
                    result = set.getValue().convert(type, TypeUtil.getTypeArguments(type), value);

                    break;
                }
            }
        }

        if (result == null) {
            throw new UnsupportedTypeConversionException(
                "Cannot convert " + value + " to type " + rawType + ". " +
                    "Consider registering a custom converter via ConfigFactory.addValueConverter"
            );
        }

        return result;
    }
}
