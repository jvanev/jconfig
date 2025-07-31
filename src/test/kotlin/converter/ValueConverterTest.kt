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
package com.jvanev.kconfig.converter

import com.jvanev.kconfig.ValueConversionException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.LinkedList
import kotlin.test.assertEquals

class ValueConverterTest {
    private val converter = ValueConverter()

    @Test
    fun shouldConvertPrimitives() {
        val byte = "127"
        val short = "16325"
        val int = "1000001"
        val long = "1000000001"
        val float = "1.23"
        val double = "1.23"
        val boolean = "True"
        val char = "a"

        assertEquals(127.toByte(), converter.convert(byte, Byte::class.java))
        assertEquals(16325.toShort(), converter.convert(short, Short::class.java))
        assertEquals(1_000_001, converter.convert(int, Int::class.java))
        assertEquals(1_000_000_001L, converter.convert(long, Long::class.java))
        assertEquals(1.23f, converter.convert(float, Float::class.java))
        assertEquals(1.23, converter.convert(double, Double::class.java))
        assertEquals(true, converter.convert(boolean, Boolean::class.java))
        assertEquals('a', converter.convert(char, Char::class.java))
    }

    @Test
    fun shouldConvertHexStringToNumeric() {
        val byte = "0x07"
        val short = "0x08"
        val int = "0x09"
        val long = "0x0A"

        assertEquals(7.toByte(), converter.convert(byte, Byte::class.java))
        assertEquals(8.toShort(), converter.convert(short, Short::class.java))
        assertEquals(9, converter.convert(int, Int::class.java))
        assertEquals(10L, converter.convert(long, Long::class.java))
    }

    @Test
    fun shouldConvertToPrimitiveArrays() {
        val intValues = "1, 2, 0x03, #4"
        val expectedIntArray = intArrayOf(1, 2, 3, 4)
        assertArrayEquals(expectedIntArray, converter.convert(intValues, IntArray::class.java) as IntArray)

        val booleanValues = "True, false, TRUE , FALSe"
        val expectedBooleanArray = booleanArrayOf(true, false, true, false)
        assertArrayEquals(
            expectedBooleanArray,
            converter.convert(booleanValues, BooleanArray::class.java) as BooleanArray
        )

        val charValues = "a, b, c,d"
        val expectedCharArray = charArrayOf('a', 'b', 'c', 'd')
        assertArrayEquals(expectedCharArray, converter.convert(charValues, CharArray::class.java) as CharArray)

        val byteValues = "1, 2, 3"
        val expectedByteArray = byteArrayOf(1, 2, 3)
        assertArrayEquals(expectedByteArray, converter.convert(byteValues, ByteArray::class.java) as ByteArray)

        val shortValues = "100, 200, 300"
        val expectedShortArray = shortArrayOf(100, 200, 300)
        assertArrayEquals(expectedShortArray, converter.convert(shortValues, ShortArray::class.java) as ShortArray)

        val longValues = "10000000000, 20000000000"
        val expectedLongArray = longArrayOf(10000000000L, 20000000000L)
        assertArrayEquals(expectedLongArray, converter.convert(longValues, LongArray::class.java) as LongArray)

        val floatValues = "1.1, 2.2, 3.3"
        val expectedFloatArray = floatArrayOf(1.1f, 2.2f, 3.3f)
        assertArrayEquals(expectedFloatArray, converter.convert(floatValues, FloatArray::class.java) as FloatArray)

        val doubleValues = "1.1, 2.2, 3.3"
        val expectedDoubleArray = doubleArrayOf(1.1, 2.2, 3.3)
        assertArrayEquals(expectedDoubleArray, converter.convert(doubleValues, DoubleArray::class.java) as DoubleArray)
    }

    @Test
    fun shouldThrowOnInvalidCharArrayElement() {
        val charArrayType = CharArray::class.java
        val invalidCharValues = "a, bc, d" // 'bc' is not a single character

        assertThrows<IllegalArgumentException> {
            converter.convert(invalidCharValues, charArrayType)
        }
    }

    class ExampleType(
        val list: List<Int>,
        val map: Map<String, Int>,
        val stack: Stack<Int>,
    )

    class Stack<T> {
        private val items: MutableList<T> = LinkedList()

        val size: Int get() = items.size

        fun push(item: T) {
            items.add(item)
        }

        fun pop(): T {
            return items.removeLast()
        }

        fun all(): List<T> {
            return items.toList()
        }
    }

    @Test
    fun shouldConvertToCollection() {
        val entries = "1, 2, 3, 4"
        val type = ExampleType::class.java.getDeclaredField("list").genericType

        assertEquals(arrayListOf(1, 2, 3, 4), converter.convert(entries, type))
    }

    @Test
    fun shouldConvertToMap() {
        val entries = "Skill1: 7200, Skill2 :3600, Skill3 : 1800, Skill4:900"
        val type = ExampleType::class.java.getDeclaredField("map").genericType
        val expectedResult = mapOf(
            "Skill1" to 7200,
            "Skill2" to 3600,
            "Skill3" to 1800,
            "Skill4" to 900,
        )

        assertEquals(expectedResult, converter.convert(entries, type))
        assertEquals(emptyMap<String, Int>(), converter.convert("", type))
    }

    @Test
    fun shouldThrowOnMalformedMapEntries() {
        val type = ExampleType::class.java.getDeclaredField("map").genericType

        assertThrows<IllegalArgumentException> { converter.convert("Skill1 : 7200,", type) }
        assertThrows<IllegalArgumentException> { converter.convert("Skill1 ; 7200", type) }
        assertThrows<IllegalArgumentException> { converter.convert(",Skill1 : 7200", type) }
        assertThrows<IllegalArgumentException> { converter.convert("Skill1, : 7200", type) }
    }

    @Test
    fun shouldUseCustomConvertorWithAssignableReturnValue_IfNoDirectTypeMatchFound() {
        val converter = ValueConverter().apply {
            addValueConverter(Temporal::class.java) { type, _, _ ->
                LocalDateTime.parse(type)
            }
        }
        val value = "2025-07-31T17:55:12"

        assertEquals(LocalDateTime.parse(value), converter.convert(value, LocalDateTime::class.java))
    }

    @Test
    fun shouldSupportCustomGenericTypes() {
        val entries = "1, 2, 3, 4"
        val converter = ValueConverter().apply {
            addValueConverter(Stack::class.java) { value, _, typeArgs ->
                val entries = value.split("\\s*,\\s*".toRegex())
                val stack = Stack<Any>()

                for (entry in entries) {
                    stack.push(convert(entry, typeArgs[0]))
                }

                stack
            }
        }
        val type = ExampleType::class.java.getDeclaredField("stack").genericType
        val stack = converter.convert(entries, type) as Stack<*>

        assertEquals(listOf(1, 2, 3, 4), stack.all())
    }

    enum class LogLevel {
        DEBUG, INFO
    }

    @Test
    fun shouldConvertEnums() {
        val debug = "DEBUG"
        val info = "INFO"

        assertEquals(LogLevel.DEBUG, converter.convert(debug, LogLevel::class.java))
        assertEquals(LogLevel.INFO, converter.convert(info, LogLevel::class.java))
    }

    data class TestType(val value: Int) {
        companion object {
            @JvmStatic
            fun valueOf(value: String): TestType {
                return TestType(value.toInt())
            }
        }
    }

    @Test
    fun shouldConvertTypesWithValueOfMethod() {
        val value = "127"

        assertEquals(TestType(127), converter.convert(value, TestType::class.java))
    }

    data class TestTypeWithThrowingValueOf(val value: Int) {
        companion object {
            @JvmStatic
            fun valueOf(value: String): TestTypeWithThrowingValueOf {
                throw RuntimeException()
            }
        }
    }

    @Test
    fun shouldThrowOnValueOfInvocationException() {
        assertThrows<ValueConversionException> {
            converter.convert("test", TestTypeWithThrowingValueOf::class.java)
        }
    }
}
