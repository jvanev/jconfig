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
package com.jvanev.jxconfig.modifier;

/**
 * Implementations of this interface are responsible for modifying configuration values after type conversion.
 */
public interface ValueModifier {
    /**
     * Applies a transformation to the specified value.
     * <p>
     * Implementations are free to modify mutable objects (like a {@code List}) in-place
     * or return new instances. The caller should always use the returned object
     * as the result of the modification.
     *
     * @param value The configuration value after type conversion
     *
     * @return The transformed value. It may be a new instance or the modified input instance.
     */
    Object modify(Object value);
}
