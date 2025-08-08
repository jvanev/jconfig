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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Provides methods for easier access to type information.
 */
final class TypeUtil {
    // Utility class
    private TypeUtil() {
    }

    /**
     * Returns the class of the specified type.
     *
     * @param type The type whose class is to be retrieved
     *
     * @return The {@link Class} of the specified type.
     *
     * @throws UnsupportedTypeConversionException If the type is not supported by the conversion mechanism.
     */
    static Class<?> getClass(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz;
        }

        if (type instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getRawType();
        }

        throw new UnsupportedTypeConversionException(
            "Conversions to type " + type.getClass().getSimpleName() + " are not supported. " +
                "Consider registering a custom converter via ConfigFactory.Builder.withConverter"
        );
    }

    /**
     * Returns the actual type arguments of the specified generic type.
     *
     * @param type The generic type whose actual type arguments should be retrieved
     *
     * @return An array containing the actual type arguments of the specified type.
     */
    static Type[] getTypeArguments(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return parameterizedType.getActualTypeArguments();
        }

        return new Type[0];
    }
}
