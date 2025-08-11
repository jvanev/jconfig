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
 * Marks a {@link ConfigProperty} or a {@link ConfigNamespace} as dependent
 * on a specified {@link ConfigProperty}'s value.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface DependsOnProperty {
    /**
     * The {@link ConfigProperty#key()} of the property that will determine the runtime value of this parameter.
     *
     * @return The {@link ConfigProperty#key()} of the dependency.
     */
    String name() default "";

    /**
     * The operator to be used for condition check.
     * <p>
     * Defaults to an empty string, meaning the default, case-sensitive string comparison will be used.
     * <p>
     * <b>Warning:</b> If no custom {@link DependencyChecker} has been registered and non-default operator is set,
     * a {@code NullPointerException} will be thrown when performing the check.
     *
     * @return The condition check operator.
     */
    String operator() default "";

    /**
     * The value the dependency must have in order for the dependency condition to be satisfied.
     * <p>
     * Defaults to {@code true} to emulate boolean-style comparison.
     *
     * @return The value satisfying the dependency condition.
     */
    String value() default "true";
}
