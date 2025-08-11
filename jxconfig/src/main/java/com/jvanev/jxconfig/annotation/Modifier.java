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

import com.jvanev.jxconfig.modifier.ValueModifier;
import com.jvanev.jxconfig.modifier.internal.Modifiers;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Modifies the value that will be passed to the target parameter using the specified modifier.
 * This annotation can be applied multiple times.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Modifiers.class)
public @interface Modifier {
    /**
     * The modifier that will be used to modify the value that will be passed to the parameter.
     *
     * @return The value modifier.
     */
    Class<? extends ValueModifier> value();
}
