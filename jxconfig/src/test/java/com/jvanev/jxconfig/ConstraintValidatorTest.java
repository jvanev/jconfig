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
import com.jvanev.jxconfig.validator.ConstraintValidator;
import com.jvanev.jxconfig.validator.ValidationBridge;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConstraintValidatorTest {
    private static final String TEST_PATH = "classpath:config";

    private final ConfigFactory.Builder builder = ConfigFactory.builder(TEST_PATH);

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Min {
        int value();
    }

    public static class MinValidator implements ConstraintValidator<Min, Integer> {
        @Override
        public boolean validate(Min annotation, Integer value) {
            return value >= annotation.value();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Max {
        int value();
    }

    public static class MaxValidator implements ConstraintValidator<Max, Integer> {
        @Override
        public boolean validate(Max annotation, Integer value) {
            return value <= annotation.value();
        }
    }

    @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
    public record ValidatableConfiguration(
        @ConfigProperty(key = "ByteProperty")
        @Min(100)
        int byteProperty,

        @ConfigProperty(key = "ShortProperty")
        @Max(65535)
        int shortProperty
    ) {
    }

    @Test
    void validConfigurations_ShouldBeCreatedSuccessfully() {
        var factory = builder
            .withConstraintValidator(new MinValidator())
            .withConstraintValidator(new MaxValidator())
            .build();
        var config = factory.createConfig(ValidatableConfiguration.class);

        assertEquals(126, config.byteProperty());
        assertEquals(16584, config.shortProperty());
    }

    @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
    public record ConstraintViolatingConfiguration(
        @ConfigProperty(key = "ByteProperty")
        @Min(127)
        int byteProperty
    ) {
    }

    @Test
    void onViolatedConstraints_ShouldThrow() {
        var factory = builder.withConstraintValidator(new MinValidator()).build();

        assertThrows(
            ConfigurationBuildException.class,
            () -> factory.createConfig(ConstraintViolatingConfiguration.class)
        );
    }

    @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
    public record InvalidConstraintApplicationConfiguration(
        @ConfigProperty(key = "StringProperty")
        @Min(100)
        String stringProperty
    ) {
    }

    @Test
    void onInvalidConstraintApplication_ShouldThrow() {
        var factory = builder.withConstraintValidator(new MinValidator()).build();

        assertThrows(
            ConfigurationBuildException.class,
            () -> factory.createConfig(InvalidConstraintApplicationConfiguration.class)
        );
    }

    @Test
    void onDuplicateValidatorRegistration_ShouldThrow() {
        builder.withConstraintValidator(new MinValidator());

        assertThrows(
            IllegalArgumentException.class,
            () -> builder.withConstraintValidator(new MinValidator())
        );
    }

    public interface NonValidatorInterface<T> {
    }

    public static class NonValidatorType extends MinValidator implements NonValidatorInterface<String> {
    }

    @Test
    void onRegisteringNonValidatorType_ShouldThrow() {
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.withConstraintValidator(new NonValidatorType())
        );
    }

    @SuppressWarnings("rawtypes")
    public static class RawValidatorType implements ConstraintValidator {
        @Override
        public boolean validate(Annotation annotation, Object value) {
            return false;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void onRegisteringRawValidatorType_ShouldThrow() {
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.withConstraintValidator(new RawValidatorType())
        );
    }

    @Nested
    class ValidationBridgeTests {
        private ConfigFactory factory;

        @BeforeEach
        void setUp() {
            factory = ConfigFactory.builder(TEST_PATH)
                .withValidationBridge(new ExternalValidator())
                .withValidationBridge(new SecondExternalValidator())
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

        static class ExternalValidator implements ValidationBridge {
            @Override
            public boolean validate(Annotation[] annotations, Object value) {
                for (var annotation : annotations) {
                    if (annotation.annotationType() == Range.class) {
                        var intVal = (int) value;
                        var range = (Range) annotation;

                        return intVal >= range.min() && intVal <= range.max();
                    }

                    if (annotation.annotationType() == IntSet.class) {
                        var intArray = (int[]) value;
                        var inSet = (IntSet) annotation;

                        for (var intElement : intArray) {
                            for (var setElement : inSet.value()) {
                                if (intElement == setElement) {
                                    return true;
                                }
                            }
                        }

                        throw new IllegalArgumentException("Not in set");
                    }
                }

                return true;
            }
        }

        static class SecondExternalValidator implements ValidationBridge {
            @Override
            public boolean validate(Annotation[] annotations, Object value) {
                return true;
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

        @ConfigFile(filename = "ValueConversionsTestConfiguration.properties")
        public record SecondNonValidatableConfiguration(
            @ConfigProperty(key = "ByteProperty")
            @Range(min = 0, max = 256)
            int byteProperty,

            @ConfigProperty(key = "IntegerSetProperty")
            @IntSet({-1, -2, -3, -4, -5})
            int[] shortProperty
        ) {
        }

        @Test
        void onValidationException_ShouldRethrow() {
            assertThrows(
                ConfigurationBuildException.class,
                () -> factory.createConfig(SecondNonValidatableConfiguration.class)
            );
        }

        @Test
        void registeringAlreadyRegisteredValidator_ShouldThrow() {
            var validator = new ExternalValidator();
            var builder = ConfigFactory.builder(TEST_PATH)
                .withValidationBridge(validator);

            assertThrows(
                IllegalArgumentException.class,
                () -> builder.withValidationBridge(validator)
            );
        }
    }
}
