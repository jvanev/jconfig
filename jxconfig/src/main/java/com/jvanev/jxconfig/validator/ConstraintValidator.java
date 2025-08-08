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
package com.jvanev.jxconfig.validator;

import java.lang.annotation.Annotation;

/**
 * Implementations of this interface provide annotation-based validation
 * for configuration values of a specific type.
 *
 * @param <A> The actual annotation defining the value constraints
 * @param <T> The type of the value
 */
public interface ConstraintValidator<A extends Annotation, T> {
    /**
     * Validates the specified value against the constraints of the specified annotation.
     *
     * @param annotation The constraint annotation specifying the bounds of a valid value
     * @param value      The value to be checked against the constraints of the annotation
     *
     * @return {@code true} if the value is valid, {@code false} otherwise.
     */
    boolean validate(A annotation, T value);
}
