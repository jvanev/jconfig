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
package com.jvanev.jxconfig.exception;

/**
 * Thrown when the configuration value constraints have been violated.
 */
public class ConstraintViolationException extends RuntimeException {
    /**
     * Creates a new ConstraintViolationException.
     *
     * @param message A message describing the violation
     */
    public ConstraintViolationException(String message) {
        super(message);
    }
}
