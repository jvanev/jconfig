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
package com.jvanev.jxconfig.resolver;

/**
 * Implementations of this interface provide custom logic for checking
 * whether a dependency's value matches the required value.
 */
@FunctionalInterface
public interface DependencyChecker {
    /**
     * Returns the result of checking the dependency's value against the required value using the specified operator.
     * <p>
     * <b>Note:</b> This method will only be invoked for non-default operators. The library's
     * built-in mechanism handles the check automatically if the operator is an empty string.
     *
     * @param dependencyValue The resolved value of the dependency
     * @param operator        The comparison operator to be used
     * @param requiredValue   The value that the dependency must have in order for the check to be successful
     *
     * @return {@code true} if the check is successful, {@code false} otherwise.
     */
    boolean check(String dependencyValue, String operator, String requiredValue);
}
