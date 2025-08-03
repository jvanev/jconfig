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
package com.jvanev.jconfig.converter;

import java.lang.reflect.Type;

/**
 * Implementations of this interface provide {@link String} value conversions.
 */
@FunctionalInterface
public interface IValueConverter {
    /**
     * Converts a {@link String} value to the specified target type.
     *
     * @param targetType    The type to which the value must be converted
     * @param typeArguments The actual type arguments of the target type (if any)
     * @param value         The value to be converted
     *
     * @return The converted value.
     */
    Object convert(Type targetType, Type[] typeArguments, String value);
}
