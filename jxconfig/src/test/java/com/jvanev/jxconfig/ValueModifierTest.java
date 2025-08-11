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
import com.jvanev.jxconfig.annotation.Modifier;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import com.jvanev.jxconfig.modifier.ValueModifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueModifierTest {
    private static final String TEST_PATH = "config";

    private final ConfigFactory factory = ConfigFactory.builder()
        .withClasspathDir(TEST_PATH)
        .build();

    public static class MillisecondModifier implements ValueModifier {
        @Override
        public Object modify(Object value) {
            return ((long) value) * 1000;
        }
    }

    public static class AdditionModifier implements ValueModifier {
        @Override
        public Object modify(Object value) {
            return ((long) value) + 1000;
        }
    }

    @ConfigFile(filename = "BaseTestConfiguration.properties")
    public record MillisecondConfiguration(
        @ConfigProperty(key = "NonExistent", defaultValue = "5")
        @Modifier(MillisecondModifier.class)
        @Modifier(AdditionModifier.class)
        long milliseconds
    ) {
    }

    @Test
    void appliedModifiers_ShouldModifyTheValueInOrderOfApplication() {
        var config = factory.createConfig(MillisecondConfiguration.class);

        assertEquals(6000, config.milliseconds());
    }

    public static class MillisecondModifierWithNonDefaultConstructor implements ValueModifier {
        public MillisecondModifierWithNonDefaultConstructor(String unused) {
        }

        @Override
        public Object modify(Object value) {
            return ((long) value) * 1000;
        }
    }

    @ConfigFile(filename = "BaseTestConfiguration.properties")
    public record SecondMillisecondConfiguration(
        @ConfigProperty(key = "NonExistent", defaultValue = "5")
        @Modifier(MillisecondModifierWithNonDefaultConstructor.class)
        long milliseconds
    ) {
    }

    @Test
    void onModifierInstantiationFailure_ShouldThrow() {
        assertThrows(
            ConfigurationBuildException.class,
            () -> factory.createConfig(SecondMillisecondConfiguration.class)
        );
    }
}
