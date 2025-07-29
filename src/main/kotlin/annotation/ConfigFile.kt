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
package com.jvanev.kconfig.annotation

/**
 * Marks a class as a *Configuration Type* and specifies the name of its corresponding configuration file.
 *
 * A *Configuration Type* is a class whose primary constructor properties are
 * annotated with [ConfigProperty]. These properties can be automatically
 * populated from the specified configuration file.
 *
 * @property name The name of the configuration file, including its extension (e.g., `Config.properties`).
 */
@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigFile(val name: String)
