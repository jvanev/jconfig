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
package com.jvanev.jxconfig.validator.internal;

import com.jvanev.jxconfig.exception.ConstraintViolationException;
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import com.jvanev.jxconfig.validator.ConstraintValidator;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * A mechanism for annotation-based constraint validation.
 */
public final class Validator {
    /**
     * A registry of validators mapped to the annotation they use to validate their values.
     */
    private final Map<Class<? extends Annotation>, ValidationPair> validators;

    /**
     * Creates a new Validator.
     *
     * @param validators The constraint validators to be used during the validation process
     */
    public Validator(Map<Class<? extends Annotation>, ValidationPair> validators) {
        this.validators = validators;
    }

    /**
     * Validates the specified value against all constraint annotations in the specified array.
     *
     * @param annotations The array containing potential constraints
     * @param value       The value to be validated
     *
     * @throws ConstraintViolationException If the value is constrained and the constraints have been violated.
     */
    @SuppressWarnings("unchecked")
    public void validateConstraints(Annotation[] annotations, Object value) {
        for (Annotation annotation : annotations) {
            var pair = validators.get(annotation.annotationType());

            if (pair != null) {
                if (pair.valueType().isAssignableFrom(value.getClass())) {
                    if (!((ConstraintValidator<Annotation, Object>) pair.validator()).validate(annotation, value)) {
                        throw new ConstraintViolationException(
                            "Value '" + value + "' violates the constraints set by " +
                                annotation.annotationType().getSimpleName()
                        );
                    }
                } else {
                    throw new InvalidDeclarationException(
                        "Constraint " + annotation.annotationType().getSimpleName() + " validated by " +
                            pair.validator().getClass().getSimpleName() + " is applied to a parameter of " +
                            "an incompatible type. Expected " + pair.valueType().getSimpleName() + " but was " +
                            value.getClass().getSimpleName()
                    );
                }
            }
        }
    }
}
