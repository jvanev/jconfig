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
import com.jvanev.jxconfig.validator.ValidationBridge;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

/**
 * A mechanism for annotation-based constraint validation.
 */
public final class Validator {
    /**
     * A registry of bridges to external validation services.
     * Might be {@code null}, which indicates that we're dealing with user-defined validators only.
     */
    private final Set<ValidationBridge> bridges;

    /**
     * A registry of validators mapped to the annotation they use to validate their values.
     * Might be {@code null}, which indicates that the validation has been delegated to an external service.
     */
    private final Map<Class<? extends Annotation>, ValidationPair> validators;

    private final boolean hasBridges;

    private final boolean hasValidators;

    /**
     * Creates a new Validator.
     *
     * @param bridges    The bridges to services handling the constraint validations
     * @param validators The constraint validators to be used during the validation process
     */
    public Validator(Set<ValidationBridge> bridges, Map<Class<? extends Annotation>, ValidationPair> validators) {
        this.bridges = bridges;
        this.validators = validators;
        this.hasBridges = bridges != null && !bridges.isEmpty();
        this.hasValidators = validators != null && !validators.isEmpty();
    }

    /**
     * Validates the specified value against all constraint annotations in the specified array.
     *
     * @param annotations The array containing potential constraints
     * @param value       The value to be validated
     *
     * @throws ConstraintViolationException If the value is constrained and the constraints have been violated.
     */
    public void validateConstraints(Annotation[] annotations, Object value) {
        if (hasValidators) {
            withValidators(annotations, value);
        }

        if (hasBridges) {
            withValidationBridges(annotations, value);
        }
    }

    /**
     * Uses the registered validators to validate the specified value against the specified annotations.
     *
     * @param annotations The array containing potential constraints
     * @param value       The value to be validated
     *
     * @throws ConstraintViolationException If the value is constrained and the constraints have been violated.
     */
    @SuppressWarnings("unchecked")
    private void withValidators(Annotation[] annotations, Object value) {
        for (Annotation annotation : annotations) {
            var pair = validators.get(annotation.annotationType());

            if (pair != null) {
                if (pair.valueType().isAssignableFrom(value.getClass())) {
                    if (!((ConstraintValidator<Annotation, Object>) pair.validator()).validate(annotation, value)) {
                        throw new ConstraintViolationException(
                            "Value '" + value + "' of type " + value.getClass() +
                                " violates the constraints set by " + annotation.annotationType().getSimpleName()
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

    /**
     * Delegates the constraint validations to all registered bridges.
     *
     * @param annotations The array containing potential constraints
     * @param value       The value to be validated
     *
     * @throws ConstraintViolationException If the external service decides to return a constraint violation flag,
     *                                      instead of throwing an exception.
     */
    private void withValidationBridges(Annotation[] annotations, Object value) {
        for (var bridge : bridges) {
            if (!bridge.validate(annotations, value)) {
                throw new ConstraintViolationException(
                    "Value '" + value + "' of type " + value.getClass() +
                        " violates the constraints set by bridge " + bridge.getClass().getSimpleName()
                );
            }
        }
    }
}
