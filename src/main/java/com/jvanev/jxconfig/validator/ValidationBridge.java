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
 * Implementations of this interface can tap into the constraint validation process of JXConfig.
 */
@FunctionalInterface
public interface ValidationBridge {
    /**
     * Validates the specified value against the constraints of the specified annotation.
     *
     * @param annotations All declared annotations; might contain non-constraint annotations
     * @param value       The value to be validated against the constraints
     *
     * @return {@code true} if the value has been successfully validated, {@code false} otherwise.
     */
    boolean validate(Annotation[] annotations, Object value);
}
