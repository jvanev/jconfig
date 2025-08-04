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
import com.jvanev.jxconfig.annotation.ConfigGroup;
import com.jvanev.jxconfig.converter.IValueConverter;
import com.jvanev.jxconfig.converter.internal.ValueConverter;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import com.jvanev.jxconfig.exception.ValueConversionException;
import com.jvanev.jxconfig.internal.ReflectionUtil;
import com.jvanev.jxconfig.internal.resolver.ValueResolver;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

/**
 * A type-safe configuration factory responsible for producing fully initialized configuration objects.
 * <p>
 * This factory provides methods to create instances of <i>Configuration Types</i>
 * (classes annotated with {@link ConfigFile}) or <i>Configuration Containers</i>
 * (classes that group multiple <i>Configuration Types</i>). It automates the process of
 * mapping properties from {@code .properties} files to objects, handling type conversions,
 * default values, and value resolution.
 */
public final class ConfigFactory {
    private final Path configDir;

    private final ValueConverter converter;

    /**
     * Creates a new ConfigFactory.
     *
     * @param configDir The configuration directory where the {@code .properties} configuration files are located
     */
    public ConfigFactory(String configDir) {
        this.configDir = Path.of(configDir);
        this.converter = new ValueConverter();
    }

    /**
     * Registers a custom value converter for converting {@link String} values to a specific type not natively
     * supported by the factory's default mechanism, or to override the default conversion behavior for this type.
     * <p>
     * Custom converters take precedence over the built-in conversion logic.
     *
     * @param type      The type of values the converter supports
     * @param converter The converter for the specified type
     *
     * @return This factory's instance.
     */
    public ConfigFactory addValueConverter(Class<?> type, IValueConverter converter) {
        this.converter.addValueConverter(type, converter);

        return this;
    }

    /**
     * Creates and returns a new, fully initialized instance of the specified type.
     * <p>
     * This method's purpose is to instantiate multiple configuration types at once;
     * therefore, all parameters of the constructor must be of types annotated with {@link ConfigFile}.
     *
     * @param type The configuration container to be created
     *
     * @return A fully initialized instance of the configuration container.
     *
     * @throws InvalidDeclarationException If the specified container type is not correctly set up.
     * @throws ConfigurationBuildException If an error occurs while building the configuration container.
     */
    @SuppressWarnings("unchecked")
    public <T> T createConfigContainer(Class<T> type) {
        if (type.getDeclaredConstructors().length != 1) {
            throw new InvalidDeclarationException(
                "Configuration container " + type.getSimpleName() + " must declare exactly one constructor"
            );
        }

        var constructor = type.getDeclaredConstructors()[0];
        var parameters = constructor.getParameters();
        var arguments = new Object[parameters.length];
        var processedParameters = new HashMap<String, String>();

        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];
            var parameterType = parameter.getType();
            var config = parameterType.getDeclaredAnnotation(ConfigFile.class);

            if (config == null) {
                throw new InvalidDeclarationException(
                    "Type %s declared on parameter %s.%s is not annotated with @ConfigFile"
                        .formatted(parameterType.getSimpleName(), type.getSimpleName(), parameter.getName())
                );
            }

            if (processedParameters.putIfAbsent(config.filename(), parameter.getName()) != null) {
                throw new InvalidDeclarationException(
                    "Type %s of parameter %s.%s declares the same filename (%s) as the type of parameter %s.%s"
                        .formatted(
                            parameterType.getSimpleName(),
                            type.getSimpleName(), parameter.getName(),
                            config.filename(),
                            type.getSimpleName(), processedParameters.get(config.filename())
                        )
                );
            }

            try {
                arguments[i] = createConfig(parameterType);
            } catch (Exception e) {
                throw new ConfigurationBuildException(
                    "Failed to initialize parameter %s of configuration container %s"
                        .formatted(parameter.getName(), type.getSimpleName()),
                    e
                );
            }
        }

        try {
            return (T) constructor.newInstance(arguments);
        } catch (Exception e) {
            throw new ConfigurationBuildException(
                "Failed to create an instance of configuration container " + type.getSimpleName(), e
            );
        }
    }

    /**
     * Creates and returns a new, fully initialized instance of the specified configuration type.
     * <p>
     * This method loads property values from the configuration file specified by the {@link ConfigFile}
     * annotation on the specified type, applying default values and resolving dependencies as needed.
     *
     * @param type The configuration type to be created
     *
     * @return A fully initialized instance of the specified configuration type.
     *
     * @throws InvalidDeclarationException If the specified type is not correctly set up.
     * @throws ConfigurationBuildException If an error occurs while creating the configuration type.
     */
    public <T> T createConfig(Class<T> type) {
        var configFile = type.getDeclaredAnnotation(ConfigFile.class);

        if (configFile == null) {
            throw new InvalidDeclarationException(
                "Class " + type.getSimpleName() + " must be annotated with @ConfigFile"
            );
        }

        try {
            var properties = getProperties(configFile.filename());
            var mainContext = new BuildContext(properties, "", true);

            return buildConfigurationTree(type, mainContext);
        } catch (IOException e) {
            throw new ConfigurationBuildException(
                "An error occurred while loading configuration file " + configFile.filename(), e
            );
        } catch (Exception e) {
            throw new ConfigurationBuildException(
                "Failed to create an instance of configuration type " + type.getSimpleName(), e
            );
        }
    }

    /**
     * Constructs and populates a configuration objects tree based on the specified type and context.
     *
     * @param type    The type of the configuration object (or group) to be built
     * @param context The context within which the configuration object will be built
     *
     * @return A fully initialized instance of the specified type.
     *
     * @throws InvalidDeclarationException If the type is not correctly set up.
     * @throws ValueConversionException    If a parameter's resolved string value cannot be converted
     *                                     to its target type.
     */
    @SuppressWarnings("unchecked")
    private <T> T buildConfigurationTree(Class<T> type, BuildContext context) throws ReflectiveOperationException {
        if (type.getDeclaredConstructors().length != 1) {
            throw new InvalidDeclarationException(
                "Configuration type " + type.getSimpleName() + " must declare exactly one constructor"
            );
        }

        var constructor = type.getDeclaredConstructors()[0];
        var parameters = constructor.getParameters();
        var arguments = new Object[parameters.length];
        var valueResolver = new ValueResolver(context.properties(), type, context.namespace(), parameters);
        var processedParameters = new HashMap<String, String>();

        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];

            if (ReflectionUtil.isConfigGroup(parameter)) {
                var newContext = context.fromGroup(parameter, valueResolver.isGroupDependencySatisfied(parameter));
                arguments[i] = buildConfigurationTree(parameter.getType(), newContext);
            } else {
                var property = ReflectionUtil.getConfigProperty(type, parameter);

                if (processedParameters.putIfAbsent(property.name(), parameter.getName()) != null) {
                    throw new InvalidDeclarationException(
                        "Configuration property '%s' declared on parameter %s.%s is also declared on parameter %s.%s"
                            .formatted(
                                property.name(),
                                type.getSimpleName(), parameter.getName(),
                                type.getSimpleName(), processedParameters.get(property.name())
                            )
                    );
                }

                var resolvedValue = context.isDependencySatisfied()
                    ? valueResolver.resolveValue(parameter)
                    : property.defaultValue();

                try {
                    arguments[i] = converter.convert(parameter.getParameterizedType(), resolvedValue.trim());
                } catch (Exception e) {
                    throw new ValueConversionException(
                        "Failed to convert the resolved value for configuration property %s (%s.%s)"
                            .formatted(property.name(), type.getSimpleName(), parameter.getName()),
                        e
                    );
                }
            }
        }

        return (T) constructor.newInstance(arguments);
    }

    /**
     * Represents the current context in the recursive method responsible for building the configuration tree.
     *
     * @param properties            The key-value map of configuration properties for this context
     * @param namespace             The namespace of this context
     * @param isDependencySatisfied Whether the dependency conditions for the current context
     *                              and all of its parents are satisfied
     */
    private record BuildContext(Properties properties, String namespace, boolean isDependencySatisfied) {
        /**
         * Returns a new context based on this context and the specified arguments.
         *
         * @param parameter                  The parameter annotated with {@link ConfigGroup}
         * @param isGroupDependencySatisfied Whether the dependency of the new group is satisfied
         *
         * @return A new context built in the context of this context.
         */
        public BuildContext fromGroup(Parameter parameter, boolean isGroupDependencySatisfied) {
            var group = ReflectionUtil.getConfigGroup(parameter);
            // The new namespace (if defined) is always one level deeper than the previous
            var newNamespace = namespace.isBlank() ? group.namespace() : namespace + "." + group.namespace();
            // Satisfied if, and only if, this group's dependency and the dependencies of all parents are satisfied
            var isNewContextDependencySatisfied = isDependencySatisfied && isGroupDependencySatisfied;

            return new BuildContext(properties, newNamespace, isNewContextDependencySatisfied);
        }
    }

    /**
     * Loads and returns a {@link Properties} object populated with the contents of the file with the specified name.
     * The file is expected to be located within the {@link #configDir} directory.
     *
     * @param filename The name of the configuration file, including its extension (e.g., {@code Network.properties}).
     *
     * @return A {@link Properties} object containing the key-value pairs from the file.
     *
     * @throws IOException If the file does not exist or an I/O error occurs during loading.
     */
    private Properties getProperties(String filename) throws IOException {
        var properties = new Properties();

        try (var file = Files.newInputStream(configDir.resolve(filename))) {
            properties.load(file);
        }

        return properties;
    }
}
