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
package com.jvanev.jconfig.converter.internal;

import com.jvanev.jconfig.exception.ValueConversionException;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueConverterTest {
    private final ValueConverter converter = new ValueConverter();

    @Test
    void shouldConvertPrimitives() {
        var byteType = "127";
        var shortType = "16325";
        var intType = "1000001";
        var longType = "1000000001";
        var floatType = "1.23";
        var doubleType = "1.23";
        var booleanType = "True";
        var charType = "a";

        assertEquals((byte) 127, converter.convert(byte.class, byteType));
        assertEquals((short) 16325, converter.convert(short.class, shortType));
        assertEquals(1_000_001, converter.convert(int.class, intType));
        assertEquals(1_000_000_001L, converter.convert(long.class, longType));
        assertEquals(1.23f, converter.convert(float.class, floatType));
        assertEquals(1.23, converter.convert(double.class, doubleType));
        assertEquals(true, converter.convert(boolean.class, booleanType));
        assertEquals('a', converter.convert(char.class, charType));
    }

    @Test
    void shouldConvertHexStringToNumeric() {
        var byteType = "0x07";
        var shortType = "0x08";
        var intType = "0x09";
        var longType = "0x0A";

        assertEquals((byte) 7, converter.convert(byte.class, byteType));
        assertEquals((short) 8, converter.convert(short.class, shortType));
        assertEquals(9, converter.convert(int.class, intType));
        assertEquals(10L, converter.convert(long.class, longType));
    }

    @Test
    void shouldConvertToPrimitiveArrays() {
        var intValues = "1, 2, 0x03, #4";
        var expectedIntArray = new int[]{1, 2, 3, 4};
        assertArrayEquals(expectedIntArray, (int[]) converter.convert(int[].class, intValues));

        var booleanValues = "True, false, TRUE , FALSe";
        var expectedBooleanArray = new boolean[]{true, false, true, false};
        assertArrayEquals(
            expectedBooleanArray,
            (boolean[]) converter.convert(boolean[].class, booleanValues)
        );

        var charValues = "a, b, c,d";
        var expectedCharArray = new char[]{'a', 'b', 'c', 'd'};
        assertArrayEquals(expectedCharArray, (char[]) converter.convert(char[].class, charValues));

        var byteValues = "1, 2, 3";
        var expectedByteArray = new byte[]{1, 2, 3};
        assertArrayEquals(expectedByteArray, (byte[]) converter.convert(byte[].class, byteValues));

        var shortValues = "100, 200, 300";
        var expectedShortArray = new short[]{100, 200, 300};
        assertArrayEquals(expectedShortArray, (short[]) converter.convert(short[].class, shortValues));

        var longValues = "10000000000, 20000000000";
        var expectedLongArray = new long[]{10000000000L, 20000000000L};
        assertArrayEquals(expectedLongArray, (long[]) converter.convert(long[].class, longValues));

        var floatValues = "1.1, 2.2, 3.3";
        var expectedFloatArray = new float[]{1.1f, 2.2f, 3.3f};
        assertArrayEquals(expectedFloatArray, (float[]) converter.convert(float[].class, floatValues));

        var doubleValues = "1.1, 2.2, 3.3";
        var expectedDoubleArray = new double[]{1.1, 2.2, 3.3};
        assertArrayEquals(expectedDoubleArray, (double[]) converter.convert(double[].class, doubleValues));
    }

    @Test
    void shouldThrowOnInvalidCharArrayElement() {
        var invalidCharValues = "a, bc, d"; // 'bc' is not a single character

        assertThrows(
            IllegalArgumentException.class,
            () -> converter.convert(char[].class, invalidCharValues)
        );
    }

    static class ExampleType {
        List<Integer> list;
        Map<String, Integer> map;
        Stack<Integer> stack;
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

    @Test
    void shouldConvertToCollection() throws NoSuchFieldException {
        var entries = "1, 2, 3, 4";
        var type = ExampleType.class.getDeclaredField("list").getGenericType();

        assertIterableEquals(List.of(1, 2, 3, 4), (List<?>) converter.convert(type, entries));
    }

    @Test
    void shouldConvertToMap() throws NoSuchFieldException {
        var entries = "Skill1: 7200, Skill2 :3600, Skill3 : 1800, Skill4:900";
        var type = ExampleType.class.getDeclaredField("map").getGenericType();
        var expectedResult = new LinkedHashMap<String, Integer>();

        expectedResult.put("Skill1", 7200);
        expectedResult.put("Skill2", 3600);
        expectedResult.put("Skill3", 1800);
        expectedResult.put("Skill4", 900);

        assertEquals(expectedResult, converter.convert(type, entries));
        assertEquals(Map.of(), converter.convert(type, ""));
    }

    @Test
    void shouldThrowOnMalformedMapEntries() throws NoSuchFieldException {
        var type = ExampleType.class.getDeclaredField("map").getGenericType();

        assertThrows(IllegalArgumentException.class, () -> converter.convert(type, "Skill1 ; 7200"));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(type, "Skill1, : 7200"));
    }

    @Test
    void shouldUseCustomConvertorWithAssignableReturnValue_IfNoDirectTypeMatchFound() {
        var converter = new ValueConverter();
        converter.addValueConverter(
            Temporal.class,
            (type, typeArguments, value) -> LocalDateTime.parse(value)
        );

        var value = "2025-07-31T17:55:12";

        assertEquals(LocalDateTime.parse(value), converter.convert(LocalDateTime.class, value));
    }

    @Test
    void shouldSupportCustomGenericTypes() throws NoSuchFieldException {
        var entries = "1, 2, 3, 4";
        var converter = new ValueConverter();
        converter.
            addValueConverter(
                Stack.class,
                (type, typeArguments, value) -> {
                    var array = value.split("\\s*,\\s*");
                    var stack = new Stack<>();

                    for (var item : array) {
                        stack.push(converter.convert(typeArguments[0], item));
                    }

                    return stack;
                }
            );

        var type = ExampleType.class.getDeclaredField("stack").getGenericType();
        var stack = (Stack<?>) converter.convert(type, entries);

        assertEquals(List.of(1, 2, 3, 4), stack.all());
    }

    enum LogLevel {
        DEBUG, INFO
    }

    @Test
    void shouldConvertEnums() {
        var debug = "DEBUG";
        var info = "INFO";

        assertEquals(LogLevel.DEBUG, converter.convert(LogLevel.class, debug));
        assertEquals(LogLevel.INFO, converter.convert(LogLevel.class, info));
    }

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

    public record TestTypeWithThrowingValueOf(int value) {
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
