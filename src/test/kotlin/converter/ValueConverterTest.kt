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

import org.junit.jupiter.api.Test
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

    class ExampleType(
        val list: List<Int>,
        val map: Map<String, Int>,
    )

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
}
