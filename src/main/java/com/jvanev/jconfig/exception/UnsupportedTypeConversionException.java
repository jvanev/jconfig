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
package com.jvanev.jconfig.exception;

/**
 * Thrown when a configuration type contains a property in its primary constructor
 * whose data type cannot be instantiated from a {@link String} value.
 */
public class UnsupportedTypeConversionException extends RuntimeException {
    /**
     * Creates a new UnsupportedTypeConversionException.
     *
     * @param message A brief description of the failure
     */
    public UnsupportedTypeConversionException(String message) {
        super(message);
    }
}
