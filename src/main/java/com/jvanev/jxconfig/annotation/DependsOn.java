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
package com.jvanev.jxconfig.annotation;

import com.jvanev.jxconfig.resolver.DependencyChecker;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link ConfigProperty} or a {@link ConfigGroup} as dependent on a specified {@link ConfigProperty}'s value.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsOn {
    /**
     * The {@link ConfigProperty#name()} of the property that will determine the runtime value of this parameter.
     *
     * @return The {@link ConfigProperty#name()} of the dependency.
     */
    String property();

    /**
     * The operator to be used for condition check.
     * <p>
     * Defaults to {@link DependencyChecker#DEFAULT_OPERATOR}, meaning the default,
     * case-sensitive string comparison is used.
     * <p>
     * <b>Warning:</b> If no custom {@link DependencyChecker} has been registered,
     * a {@code NullPointerException} will be thrown when performing the check.
     *
     * @return The condition check operator.
     */
    String operator() default DependencyChecker.DEFAULT_OPERATOR;

    /**
     * The value the dependency must have in order for the dependency condition to be satisfied.
     * <p>
     * Defaults to {@code true} to emulate boolean-style comparison.
     *
     * @return The value satisfying the dependency condition.
     */
    String value() default "true";
}
