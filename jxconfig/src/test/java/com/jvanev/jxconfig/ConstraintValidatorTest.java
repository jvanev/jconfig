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
package com.jvanev.jxconfig;

import com.jvanev.jxconfig.annotation.ConfigFile;
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import com.jvanev.jxconfig.validator.ConfigurationValidator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintValidatorTest {
    private static final String TEST_PATH = "config";

    private ConfigFactory factory;

    @BeforeEach
    void setUp() {
        factory = ConfigFactory.builder()
            .withClasspathDir(TEST_PATH)
            .withConfigurationValidator(new ExternalValidator())
            .build();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Range {
        int min();

        int max();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface IntSet {
        int[] value();
    }

    static class ExternalValidator implements ConfigurationValidator {
        @Override
        public void validate(Object config) {
            var components = config.getClass().getRecordComponents();

            for (var component : components) {
                for (var annotation : component.getDeclaredAnnotations()) {
                    if (annotation.annotationType() == Range.class) {
                        try {
                            var intVal = (int) component.getAccessor().invoke(config);
                            var range = (Range) annotation;

                            if (intVal <= range.min() || intVal >= range.max()) {
                                throw new RuntimeException(
                                    "Range constraint violation: The value must be between " +
                                        range.min() + " and " + range.max() + ", found " + intVal
                                );
                            }

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (annotation.annotationType() == IntSet.class) {
                        try {
                            var intArray = (int[]) component.getAccessor().invoke(config);
                            var intSet = (IntSet) annotation;

                            if (Arrays.equals(intArray, intSet.value())) {
                                throw new IllegalArgumentException(
                                    "IntSet constraint violation: " + Arrays.toString(intSet.value()) +
                                        " does not match the required " + Arrays.toString(intSet.value())
                                );
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
    public record ValidatableConfiguration(
        @ConfigProperty(key = "ByteProperty")
        @Range(min = 1, max = 127)
        int byteProperty,

        @ConfigProperty(key = "IntegerSetProperty")
        @IntSet({1, 3, 5, 7, 9})
        int[] shortProperty
    ) {
    }

    @Test
    void afterSuccessfulRegistration_ShouldBeUsedForValidation() {
        assertDoesNotThrow(() -> factory.createConfig(ValidatableConfiguration.class));
    }

    @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
    public record NonValidatableConfiguration(
        @ConfigProperty(key = "ByteProperty")
        @Range(min = 127, max = 256)
        int byteProperty,

        @ConfigProperty(key = "IntegerSetProperty")
        @IntSet({1, 3, 5, 7, 9})
        int[] shortProperty
    ) {
    }

    @Test
    void onBooleanFailure_ShouldThrowGenericException() {
        assertThrows(
            ConfigurationBuildException.class,
            () -> factory.createConfig(NonValidatableConfiguration.class)
        );
    }

    static class SecondExternalValidator implements ConfigurationValidator {
        @Override
        public void validate(Object config) {
        }
    }

    @Test
    void registeringMultipleValidators_ShouldThrow() {
        var builder = ConfigFactory.builder()
            .withClasspathDir(TEST_PATH)
            .withConfigurationValidator(new ExternalValidator());

        assertThrows(
            IllegalArgumentException.class,
            () -> builder.withConfigurationValidator(new SecondExternalValidator())
        );
    }
}
