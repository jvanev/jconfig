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

/**
 * Provides methods for working with reference types.
 */
final class ReferenceTypeUtil {
    // Utility class
    private ReferenceTypeUtil() {
    }

    /**
     * Determines if this type represents a [String] object.
     */
    static boolean isString(Class<?> type) {
        return type == String.class;
    }

    /**
     * Returns the specified value converted to an enumeration of the specified type.
     *
     * @return The enumeration corresponding to the specified type and value.
     */
    @SuppressWarnings("unchecked")
    static Enum<?> toEnum(Class<?> type, String value) {
        var enumClass = type.asSubclass(Enum.class);

        return Enum.valueOf(enumClass, value);
    }
}
