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
 * Thrown when a circular dependency chain is detected (e.g., A depends on B -> B depends on C -> C depends on A).
 */
public class CircularDependencyException extends RuntimeException {
    /**
     * Creates a new CircularDependencyException.
     *
     * @param message A detailed description of the dependency circle.
     */
    public CircularDependencyException(String message) {
        super(message);
    }
}
