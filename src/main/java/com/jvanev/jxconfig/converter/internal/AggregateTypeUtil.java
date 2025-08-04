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

import com.jvanev.jxconfig.exception.UnsupportedTypeConversionException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Provides methods for working with aggregate types (e.g., arrays and collections).
 */
final class AggregateTypeUtil {
    /**
     * Contains string-to-primitive array converters mapped to the type of array they produce.
     */
    private static final Map<Class<?>, Function<String, Object>> PRIMITIVE_ARRAY_CONVERTERS = Map.of(
        byte[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(byte.class, value),
        short[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(short.class, value),
        int[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(int.class, value),
        long[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(long.class, value),
        float[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(float.class, value),
        double[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(double.class, value),
        boolean[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(boolean.class, value),
        char[].class, value -> AggregateTypeUtil.convertToPrimitiveArray(char.class, value)
    );

    /**
     * Regex to split strings by commas (e.g., 1, 2, 3). Any space around the comma will be trimmed.
     */
    private static final Pattern ARRAY_SPLIT_REGEX = Pattern.compile("\\s*,\\s*");

    /**
     * Regex to split strings by colons (e.g., Key:Value). Any space around the colon will be trimmed.
     */
    private static final Pattern KEY_VALUE_SPLIT_REGEX = Pattern.compile("\\s*:\\s*");

    // Utility class
    private AggregateTypeUtil() {
    }

    /**
     * Determines if the specified type represents an array of primitive type.
     *
     * @param type The type to be checked
     *
     * @return {@code true} if the specified type is a primitive array, {@code false} otherwise.
     */
    static boolean isPrimitiveArray(Class<?> type) {
        return PRIMITIVE_ARRAY_CONVERTERS.containsKey(type);
    }

    /**
     * Returns the specified value as a primitive array of the specified type.
     *
     * @param type  The type of the array
     * @param value The value to be converted
     *
     * @return A primitive array of the specified type.
     */
    static Object toPrimitiveArray(Class<?> type, String value) {
        return PRIMITIVE_ARRAY_CONVERTERS.get(type).apply(value);
    }

    /**
     * Converts the specified value into an array of elements of the specified component type.
     *
     * @param value         The value to be converted into an array
     * @param componentType The type of the elements in the array
     *
     * @return The value converted into a primitive array of the component type.
     */
    private static Object convertToPrimitiveArray(Class<?> componentType, String value) {
        String[] entries = value.isBlank() ? new String[0] : ARRAY_SPLIT_REGEX.split(value);
        Object array = Array.newInstance(componentType, entries.length);

        for (int i = 0; i < entries.length; i++) {
            Array.set(array, i, PrimitiveTypeUtil.toPrimitive(componentType, entries[i]));
        }

        return array;
    }

    /**
     * Determines if the specified type represents a collection object.
     *
     * @param type The type to be checked
     *
     * @return {@code true} if the specified type is a collection, {@code false} otherwise.
     */
    static boolean isCollection(Class<?> type) {
        return type == List.class || type == Set.class;
    }

    /**
     * Returns the specified value converted to a collection of the specified type.
     *
     * @param converter The converter to be used to convert the collection's entries
     * @param type      The type of the collection
     * @param valueType The type of the collection entries
     * @param value     The value to be converted into collection
     *
     * @return A collection of the specified type containing the elements of the value.
     *
     * @throws UnsupportedTypeConversionException If conversions to the specified collection type are not supported.
     */
    static Collection<Object> toCollection(ValueConverter converter, Class<?> type, Type valueType, String value) {
        if (type != List.class && type != Set.class) {
            throw new UnsupportedTypeConversionException(
                "Cannot convert value '" + value + "' to type " + type.getSimpleName() +
                "<" + valueType.getTypeName() + ">. " +
                "Consider registering a custom converter via ConfigFactory.addValueConverter"
            );
        }

        String[] entries = value.isBlank() ? new String[0] : ARRAY_SPLIT_REGEX.split(value);
        Collection<Object> collection = type == List.class
            ? new ArrayList<>(entries.length)
            : new LinkedHashSet<>(entries.length);

        for (var entry : entries) {
            collection.add(converter.convert(valueType, entry));
        }

        return collection;
    }

    /**
     * Determines if the specified type represents a key-value map object.
     *
     * @return {@code true} if type is a key-value map, {@code false} otherwise.
     */
    static boolean isMap(Class<?> type) {
        return type == Map.class;
    }

    /**
     * Returns the specified value converted to a key-value map.
     *
     * @param converter The converter to convert the map's keys and values
     * @param keyType   The type of the map's keys
     * @param valueType The type of the map's values
     * @param value     The value to be converted into map
     *
     * @return A map of the specified type containing the elements of the value.
     *
     * @throws IllegalArgumentException If the value contains malformed tokens.
     */
    static Map<Object, Object> toMap(ValueConverter converter, Type keyType, Type valueType, String value) {
        var entries = value.isBlank() ? new String[0] : ARRAY_SPLIT_REGEX.split(value);
        var map = new LinkedHashMap<>();

        for (var entry : entries) {
            if (entry.isBlank()) {
                continue;
            }

            var pair = KEY_VALUE_SPLIT_REGEX.split(entry);

            if (pair.length != 2) {
                throw new IllegalArgumentException("Maps support only key:value pairs, " + entry + " given");
            }

            try {
                var entryKey = converter.convert(keyType, pair[0]);
                var entryValue = converter.convert(valueType, pair[1]);

                map.put(entryKey, entryValue);
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to convert map entry '" + entry + "'", e);
            }
        }

        return map;
    }
}
