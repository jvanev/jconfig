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
package com.jvanev.jxconfig.converter.internal;

import com.jvanev.jxconfig.exception.UnsupportedTypeConversionException;
import com.jvanev.jxconfig.exception.ValueConversionException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueConverterTest {
    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(Collections.emptyMap());
    }

    static Stream<Arguments> primitivesAndBoxedPrimitivesProvider() {
        return Stream.of(
            Arguments.of(byte.class, "127", (byte) 127),
            Arguments.of(Byte.class, "127", (byte) 127),
            Arguments.of(short.class, "16325", (short) 16325),
            Arguments.of(Short.class, "16325", (short) 16325),
            Arguments.of(int.class, "1000001", 1000001),
            Arguments.of(Integer.class, "1000001", 1000001),
            Arguments.of(long.class, "1000000001", 1000000001L),
            Arguments.of(Long.class, "1000000001", 1000000001L),
            Arguments.of(float.class, "1.23", 1.23f),
            Arguments.of(Float.class, "1.23", 1.23f),
            Arguments.of(double.class, "1.23", 1.23),
            Arguments.of(Double.class, "1.23", 1.23),
            Arguments.of(boolean.class, "True", true),
            Arguments.of(Boolean.class, "True", true),
            Arguments.of(char.class, "a", 'a'),
            Arguments.of(Character.class, "a", 'a')
        );
    }

    @ParameterizedTest
    @MethodSource("primitivesAndBoxedPrimitivesProvider")
    void shouldConvertPrimitives(Class<?> primitiveType, String input, Object expectedPrimitive) {
        Object actualArray = converter.convert(primitiveType, input);

        assertEquals(expectedPrimitive, actualArray);
    }

    static Stream<Arguments> hexPrimitivesProvider() {
        return Stream.of(
            Arguments.of(byte.class, "0x07", (byte) 7),
            Arguments.of(short.class, "0x08", (short) 8),
            Arguments.of(int.class, "0x09", 9),
            Arguments.of(long.class, "0x0A", 10L)
        );
    }

    @ParameterizedTest
    @MethodSource("hexPrimitivesProvider")
    void shouldConvertHexStringToNumeric(Class<?> primitiveHexType, String input, Object expectedHexPrimitive) {
        Object actualHexPrimitive = converter.convert(primitiveHexType, input);

        assertEquals(expectedHexPrimitive, actualHexPrimitive);
    }

    static Stream<Arguments> primitiveArraysProvider() {
        return Stream.of(
            Arguments.of(byte[].class, "1, 2, 3", new byte[]{1, 2, 3}),
            Arguments.of(Byte[].class, "1, 2, 3", new Byte[]{1, 2, 3}),
            Arguments.of(short[].class, "100, 200, 300", new short[]{100, 200, 300}),
            Arguments.of(Short[].class, "100, 200, 300", new Short[]{100, 200, 300}),
            Arguments.of(int[].class, "", new int[0]),
            Arguments.of(Integer[].class, "", new Integer[0]),
            Arguments.of(int[].class, "1, 2, 0x03, #4", new int[]{1, 2, 3, 4}),
            Arguments.of(Integer[].class, "1, 2, 0x03, #4", new Integer[]{1, 2, 3, 4}),
            Arguments.of(long[].class, "10000000000, 20000000000", new long[]{10000000000L, 20000000000L}),
            Arguments.of(Long[].class, "10000000000, 20000000000", new Long[]{10000000000L, 20000000000L}),
            Arguments.of(float[].class, "1.1, 2.2, 3.3", new float[]{1.1f, 2.2f, 3.3f}),
            Arguments.of(Float[].class, "1.1, 2.2, 3.3", new Float[]{1.1f, 2.2f, 3.3f}),
            Arguments.of(double[].class, "1.1, 2.2, 3.3", new double[]{1.1, 2.2, 3.3}),
            Arguments.of(Double[].class, "1.1, 2.2, 3.3", new Double[]{1.1, 2.2, 3.3}),
            Arguments.of(boolean[].class, "True, false, TRUE , FALSe", new boolean[]{true, false, true, false}),
            Arguments.of(Boolean[].class, "True, false, TRUE , FALSe", new Boolean[]{true, false, true, false}),
            Arguments.of(char[].class, "a, b, c,d", new char[]{'a', 'b', 'c', 'd'}),
            Arguments.of(Character[].class, "a, b, c,d", new Character[]{'a', 'b', 'c', 'd'})
        );
    }

    @ParameterizedTest
    @MethodSource("primitiveArraysProvider")
    void shouldConvertToPrimitiveArrays(Class<?> arrayType, String input, Object expectedArray) {
        Object actualArray = converter.convert(arrayType, input);

        if (expectedArray instanceof byte[]) {
            assertArrayEquals((byte[]) expectedArray, (byte[]) actualArray);
        } else if (expectedArray instanceof short[]) {
            assertArrayEquals((short[]) expectedArray, (short[]) actualArray);
        } else if (expectedArray instanceof int[]) {
            assertArrayEquals((int[]) expectedArray, (int[]) actualArray);
        } else if (expectedArray instanceof long[]) {
            assertArrayEquals((long[]) expectedArray, (long[]) actualArray);
        } else if (expectedArray instanceof float[]) {
            assertArrayEquals((float[]) expectedArray, (float[]) actualArray);
        } else if (expectedArray instanceof double[]) {
            assertArrayEquals((double[]) expectedArray, (double[]) actualArray);
        } else if (expectedArray instanceof boolean[]) {
            assertArrayEquals((boolean[]) expectedArray, (boolean[]) actualArray);
        } else if (expectedArray instanceof char[]) {
            assertArrayEquals((char[]) expectedArray, (char[]) actualArray);
        } else {
            assertArrayEquals((Object[]) expectedArray, (Object[]) actualArray);
        }
    }

    @Test
    void shouldThrowOnInvalidCharArrayElement() {
        var invalidCharValues = "a, bc, d"; // 'bc' is not a single character

        assertThrows(
            IllegalArgumentException.class,
            () -> converter.convert(char[].class, invalidCharValues)
        );
    }

    enum LogLevel {
        DEBUG, INFO
    }

    @ParameterizedTest
    @EnumSource(LogLevel.class)
    void shouldConvertEnums(LogLevel expectedLogLevel) {
        var inputString = expectedLogLevel.toString();
        var actualLogLevel = converter.convert(LogLevel.class, inputString);

        assertEquals(expectedLogLevel, actualLogLevel);
    }

    static class ExampleType {
        List<Integer> list;
        Map<String, Integer> map;
        Stack<Integer> stack;
        Map<BigInteger, Integer> unsupportedTypeArg;
    }

    static class Stack<T> {
        private final List<T> items = new LinkedList<>();

        int getSize() {
            return items.size();
        }

        void push(T item) {
            items.add(item);
        }

        T pop() {
            return items.remove(items.size() - 1);
        }

        List<T> all() {
            return items.stream().toList();
        }
    }

    @Nested
    class CollectionTypeConversionTests {
        @Test
        void shouldConvertToCollection() throws NoSuchFieldException {
            var entries = "1, 2, 3, 4";
            var type = ExampleType.class.getDeclaredField("list").getGenericType();

            assertIterableEquals(List.of(1, 2, 3, 4), (List<?>) converter.convert(type, entries));
        }

        @Test
        void shouldConvertToMap() throws NoSuchFieldException {
            var entries = "Skill1: 7200, Skill2 :3600, Skill3 : 1800, , Skill4:900";
            var type = ExampleType.class.getDeclaredField("map").getGenericType();
            var expectedResult = new LinkedHashMap<String, Integer>();

            expectedResult.put("Skill1", 7200);
            expectedResult.put("Skill2", 3600);
            expectedResult.put("Skill3", 1800);
            expectedResult.put("Skill4", 900);

            assertAll(
                () -> assertEquals(expectedResult, converter.convert(type, entries)),
                () -> assertEquals(Map.of(), converter.convert(type, ""))
            );
        }
    }

    @Nested
    class CustomTypeConversionTests {
        @Test
        void shouldUseCustomConvertorWithAssignableReturnValue_IfNoDirectTypeMatchFound() {
            var converter = new Converter(
                Map.of(
                    Temporal.class, (conv, type, typeArguments, value) -> LocalDateTime.parse(value)
                )
            );

            var value = "2025-07-31T17:55:12";

            assertEquals(LocalDateTime.parse(value), converter.convert(LocalDateTime.class, value));
        }

        @Test
        void shouldSupportCustomGenericTypes() throws NoSuchFieldException {
            var converter = new Converter(
                Map.of(
                    Stack.class, (conv, type, typeArguments, value) -> {
                        var array = value.split("\\s*,\\s*");
                        var stack = new Stack<>();

                        for (var item : array) {
                            stack.push(conv.convert(typeArguments[0], item));
                        }

                        return stack;
                    }
                )
            );

            var entries = "1, 2, 3, 4";
            var type = ExampleType.class.getDeclaredField("stack").getGenericType();
            var stack = (Stack<?>) converter.convert(type, entries);

            assertEquals(List.of(1, 2, 3, 4), stack.all());
        }
    }

    @Nested
    class UnsupportedConversionTests {
        static class TypeHolder<T> {
            T typeVariableField;
        }

        @Test
        void shouldThrowOnUnsupportedType() throws NoSuchFieldException {
            var field = TypeHolder.class.getDeclaredField("typeVariableField");
            var typeVariable = field.getGenericType();

            assertThrows(
                UnsupportedTypeConversionException.class,
                () -> converter.convert(typeVariable, "test")
            );
        }

        @Test
        void shouldThrowOnUnsupportedPrimitiveType() {
            assertThrows(
                IllegalArgumentException.class,
                () -> PrimitiveTypeUtil.toPrimitive(Map.class, "1")
            );
        }

        @Test
        void shouldThrowOnUnsupportedCollectionType() {
            assertThrows(
                UnsupportedTypeConversionException.class,
                () -> AggregateTypeUtil.toCollection(converter, Map.class, int.class, "")
            );
        }

        @Test
        void shouldThrowOnMalformedMapEntries() throws NoSuchFieldException {
            var type = ExampleType.class.getDeclaredField("map").getGenericType();

            assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> converter.convert(type, "Skill1 ; 7200")),
                () -> assertThrows(IllegalArgumentException.class, () -> converter.convert(type, "Skill1, : 7200"))
            );
        }

        @Test
        void shouldThrowOnUnsupportedMapTypeArgument() throws NoSuchFieldException {
            var type = ExampleType.class.getDeclaredField("unsupportedTypeArg").getGenericType();

            assertThrows(IllegalArgumentException.class, () -> converter.convert(type, "Skill1 : 7200"));
        }
    }

    @Nested
    class ValueOfMethodTests {
        public record TestType(int value) {
            public static TestType valueOf(String value) {
                return new TestType(Integer.parseInt(value));
            }
        }

        @Test
        void shouldConvertTypesWithValueOfMethod() {
            var value = "127";

            assertEquals(new TestType(127), converter.convert(TestType.class, value));
        }

        public record NonStaticValueOfContainer(int value) {
            public NonStaticValueOfContainer valueOf(String value) {
                return new NonStaticValueOfContainer(Integer.parseInt(value));
            }
        }

        @Test
        void shouldThrowOnUnsupportedTypeWithNonStaticValueOfMethod() {
            // Also register an unrelated converter that doesn't support NonStaticValueOfContainer
            // for test coverage purposes
            var converter = new Converter(
                Map.of(
                    DateTimeFormatter.class, (conv, type, argTypes, value) -> DateTimeFormatter.ofPattern(value)
                )
            );

            assertThrows(
                UnsupportedTypeConversionException.class,
                () -> converter.convert(NonStaticValueOfContainer.class, "")
            );
        }

        public record StaticValueOfReturningDifferentType() {
            public static String valueOf(String value) {
                return "empty";
            }
        }

        @Test
        void shouldThrowOnUnsupportedTypeWithStaticValueOfReturningDifferentType() {
            assertThrows(
                UnsupportedTypeConversionException.class,
                () -> converter.convert(StaticValueOfReturningDifferentType.class, "")
            );
        }

        public record TestTypeWithThrowingValueOf() {
            public static TestTypeWithThrowingValueOf valueOf(String value) {
                throw new RuntimeException();
            }
        }

        @Test
        void shouldThrowOnValueOfInvocationException() {
            assertThrows(
                ValueConversionException.class,
                () -> converter.convert(TestTypeWithThrowingValueOf.class, "test")
            );
        }
    }
}
