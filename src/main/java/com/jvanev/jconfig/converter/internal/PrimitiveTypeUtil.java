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
package com.jvanev.jconfig.converter.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Provides methods for working with primitives and their boxed versions.
 */
final class PrimitiveTypeUtil {
    private static final Set<Class<?>> BOXED_TYPES = Set.of(
        Byte.class, Short.class, Integer.class, Long.class,
        Float.class, Double.class, Boolean.class, Character.class
    );

    /**
     * Contains string-to-primitive converters mapped to the type they produce.
     */
    private static final Map<Class<?>, Function<String, Object>> CONVERTERS;

    static {
        CONVERTERS = new HashMap<>();
        CONVERTERS.put(byte.class, Byte::decode);
        CONVERTERS.put(Byte.class, Byte::decode);
        CONVERTERS.put(short.class, Short::decode);
        CONVERTERS.put(Short.class, Short::decode);
        CONVERTERS.put(int.class, Integer::decode);
        CONVERTERS.put(Integer.class, Integer::decode);
        CONVERTERS.put(long.class, Long::decode);
        CONVERTERS.put(Long.class, Long::decode);
        CONVERTERS.put(float.class, Float::parseFloat);
        CONVERTERS.put(Float.class, Float::parseFloat);
        CONVERTERS.put(double.class, Double::parseDouble);
        CONVERTERS.put(Double.class, Double::parseDouble);
        CONVERTERS.put(boolean.class, Boolean::parseBoolean);
        CONVERTERS.put(Boolean.class, Boolean::parseBoolean);

        Function<String, Object> charConverter = value -> {
            if (value.length() != 1) {
                throw new IllegalArgumentException(
                    "Cannot convert '" + value + "' to char. Expected single character."
                );
            }

            return value.charAt(0);
        };

        CONVERTERS.put(char.class, charConverter);
        CONVERTERS.put(Character.class, charConverter);
    }

    /**
     * Determines if this type represents a boxed primitive object.
     */
    static boolean isBoxedPrimitive(Class<?> type) {
        return BOXED_TYPES.contains(type);
    }

    /**
     * Returns the specified value converted into the specified primitive type.
     *
     * @return The value converted to a primitive.
     *
     * @throws IllegalArgumentException if the specified value cannot be converted.
     */
    static Object toPrimitive(Class<?> type, String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot convert an empty string to a primitive of type " + type.getSimpleName()
            );
        }

        var converter = CONVERTERS.get(type);

        if (converter == null) {
            throw new IllegalArgumentException(
                "Cannot convert '" + value + "' to a primitive of type " + type.getSimpleName()
            );
        }

        return converter.apply(value);
    }
}
