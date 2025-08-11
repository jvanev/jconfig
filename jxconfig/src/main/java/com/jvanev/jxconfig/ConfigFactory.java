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
import com.jvanev.jxconfig.annotation.ConfigNamespace;
import com.jvanev.jxconfig.annotation.Modifier;
import com.jvanev.jxconfig.converter.ValueConverter;
import com.jvanev.jxconfig.converter.internal.Converter;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import com.jvanev.jxconfig.exception.ModifierInstantiationException;
import com.jvanev.jxconfig.exception.ValueConversionException;
import com.jvanev.jxconfig.internal.ReflectionUtil;
import com.jvanev.jxconfig.modifier.ValueModifier;
import com.jvanev.jxconfig.resolver.DependencyChecker;
import com.jvanev.jxconfig.resolver.internal.ValueResolver;
import com.jvanev.jxconfig.validator.ConfigurationValidator;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

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
    private final String classpathDirectory;

    private final Path configurationDirectory;

    private final Converter valueConverter;

    private final DependencyChecker dependencyChecker;

    private final ConfigurationValidator configurationValidator;

    private final Map<Class<?>, ValueModifier> valueModifiers = new ConcurrentHashMap<>();

    // Instances of the factory are obtained through the dedicated builder
    private ConfigFactory(
        String classpathDirectory,
        String configurationDirectory,
        Converter valueConverter,
        DependencyChecker dependencyChecker,
        ConfigurationValidator configurationValidator
    ) {
        this.classpathDirectory = classpathDirectory.isBlank() ? classpathDirectory : classpathDirectory.endsWith("/")
            ? classpathDirectory
            : classpathDirectory + "/";
        this.configurationDirectory = Path.of(configurationDirectory);
        this.valueConverter = valueConverter;
        this.dependencyChecker = dependencyChecker;
        this.configurationValidator = configurationValidator;
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
        Objects.requireNonNull(type, "The configuration container type must not be null");

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
        Objects.requireNonNull(type, "The configuration type must not be null");

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
        } catch (Exception e) {
            throw new ConfigurationBuildException(
                "Failed to create an instance of configuration type " + type.getSimpleName(), e
            );
        }
    }

    /**
     * Constructs and populates a configuration objects tree based on the specified type and context.
     *
     * @param type    The type of the configuration object (or namespace) to be built
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
        var valueResolver = new ValueResolver(
            context.properties(),
            type,
            context.namespace(),
            parameters,
            dependencyChecker
        );
        var processedParameters = new HashMap<String, String>();

        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];

            if (ReflectionUtil.isConfigNamespace(parameter)) {
                var newContext = context.fromNamespace(
                    parameter,
                    valueResolver.isNamespaceDependencySatisfied(parameter)
                );
                arguments[i] = buildConfigurationTree(parameter.getType(), newContext);
            } else {
                var property = ReflectionUtil.getConfigProperty(type, parameter);

                if (processedParameters.putIfAbsent(property.key(), parameter.getName()) != null) {
                    throw new InvalidDeclarationException(
                        "Configuration property '%s' declared on parameter %s.%s is also declared on parameter %s.%s"
                            .formatted(
                                property.key(),
                                type.getSimpleName(), parameter.getName(),
                                type.getSimpleName(), processedParameters.get(property.key())
                            )
                    );
                }

                var resolvedValue = context.isDependencySatisfied()
                    ? valueResolver.resolveValue(parameter)
                    : valueResolver.getDefaultValue(parameter);
                Object convertedValue;

                try {
                    convertedValue = valueConverter.convert(parameter.getParameterizedType(), resolvedValue.trim());
                } catch (Exception e) {
                    throw new ValueConversionException(
                        "Failed to convert the resolved value for configuration property %s (%s.%s)"
                            .formatted(property.key(), type.getSimpleName(), parameter.getName()),
                        e
                    );
                }

                arguments[i] = modify(type, parameter, convertedValue);
            }
        }

        var configurationObject = (T) constructor.newInstance(arguments);

        // Use the registered validator, if exists, to validate the product
        if (configurationValidator != null) {
            configurationValidator.validate(configurationObject);
        }

        return configurationObject;
    }

    /**
     * Returns the specified value with all modifiers of the specified parameter applied to it.
     *
     * @param type      The type declaring the specified parameter
     * @param parameter The parameter to which the specified value will be passed
     * @param value     The value to be modified
     *
     * @return The specified value after all modifiers the specified parameter is annotated with
     * have been applied to it.
     */
    private Object modify(Class<?> type, Parameter parameter, Object value) {
        var modifiedValue = value;

        for (var modifier : parameter.getAnnotationsByType(Modifier.class)) {
            var valueModifier = valueModifiers.computeIfAbsent(
                modifier.value(), key -> {
                    try {
                        return modifier.value().getConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        var property = ReflectionUtil.getConfigProperty(type, parameter);

                        throw new ModifierInstantiationException(
                            "Failed to instantiate modifier %s applied to parameter %s.%s (%s)"
                                .formatted(
                                    key.getSimpleName(),
                                    type.getSimpleName(),
                                    parameter.getName(),
                                    property.key()
                                ),
                            e
                        );
                    }
                }
            );
            modifiedValue = valueModifier.modify(modifiedValue);
        }

        return modifiedValue;
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
         * @param parameter                      The parameter annotated with {@link ConfigNamespace}
         * @param isNamespaceDependencySatisfied Whether the dependency of the new namespace is satisfied
         *
         * @return A new context built in the context of this context.
         */
        BuildContext fromNamespace(Parameter parameter, boolean isNamespaceDependencySatisfied) {
            var parameterNamespace = ReflectionUtil.getConfigNamespace(parameter);

            // The new namespace (if defined) is always one level deeper than the previous
            var newNamespace = parameterNamespace.value().isBlank() ? namespace : namespace.isBlank()
                ? parameterNamespace.value()
                : namespace + "." + parameterNamespace.value();

            // Satisfied if, and only if, this namespace's dependency
            // and the dependencies of all upstream context entries are satisfied
            var isNewContextDependencySatisfied = isDependencySatisfied && isNamespaceDependencySatisfied;

            return new BuildContext(properties, newNamespace, isNewContextDependencySatisfied);
        }
    }

    /**
     * Loads and returns a {@link Properties} object populated with the contents of the file with the specified name.
     * The file is expected to be located in the {@link #classpathDirectory} and/or {@link #configurationDirectory}.
     * <p>
     * If the file exists in both locations, both files will be loaded and their contents will be merged.
     * The content of the file in the filesystem will override the matching keys in the file on the classpath.
     *
     * @param filename The name of the configuration file, including its extension (e.g., {@code Network.properties}).
     *
     * @return A {@link Properties} object containing the key-value pairs from the file.
     *
     * @throws IOException If the file does not exist or an I/O error occurs during loading.
     */
    private Properties getProperties(String filename) throws IOException {
        var properties = new Properties();
        var defaultConfigFound = false;

        var classpathConfig = classpathDirectory + filename;

        try (var stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathConfig)) {
            if (stream != null) {
                defaultConfigFound = true;

                properties.load(stream);
            }
        }

        var filesystemConfig = configurationDirectory.resolve(filename);

        if (Files.isRegularFile(filesystemConfig)) {
            try (var stream = Files.newInputStream(filesystemConfig)) {
                properties.load(stream);
            }
        } else if (!defaultConfigFound) {
            throw new FileNotFoundException(
                "Could not find configuration file " + filename + ". Attempted classpath lookup for " +
                    classpathConfig + ", and filesystem lookup for " + configurationDirectory
            );
        }

        return properties;
    }

    /**
     * Returns a new builder object responsible for building a new instance of {@link ConfigFactory}.
     *
     * @return A new {@link Builder} for constructing a {@link ConfigFactory}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * This class is responsible for building immutable instances of {@link ConfigFactory}.
     */
    public static class Builder {
        private String classpathDirectory = "";

        private String configurationDirectory = "./";

        private DependencyChecker dependencyChecker;

        private ConfigurationValidator configurationValidator;

        private final Map<Class<?>, ValueConverter> valueConverters = new LinkedHashMap<>();

        // Instantiable by the builder method only
        private Builder() {
        }

        /**
         * Specifies the directory on the classpath where the configuration files are located.
         *
         * @param directory The configuration files directory on the classpath
         *
         * @return This builder.
         */
        public Builder withClasspathDir(String directory) {
            this.classpathDirectory = Objects.requireNonNull(directory, "The classpath directory cannot be null");

            return this;
        }

        /**
         * Specifies the directory in the filesystem where the configuration files are located.
         *
         * @param directory The configuration files directory in the filesystem
         *
         * @return This builder.
         */
        public Builder withFilesystemDir(String directory) {
            this.configurationDirectory = Objects.requireNonNull(directory, "The filesystem directory cannot be null");

            return this;
        }

        /**
         * Registers a custom value converter for converting {@link String} values to a specific type not natively
         * supported by the factory's default mechanism, or to override the default conversion behavior for this type.
         * <p>
         * Custom converters take precedence over the built-in conversion logic for exact matches of the specified type.
         *
         * @param type      The type of values the converter supports
         * @param converter The converter for the specified type
         *
         * @return This builder.
         */
        public Builder withValueConverter(Class<?> type, ValueConverter converter) {
            Objects.requireNonNull(type, "The target type cannot be null");
            Objects.requireNonNull(converter, "The converter cannot be null");

            if (valueConverters.containsKey(type)) {
                throw new IllegalArgumentException("Duplicate converter found for type " + type.getSimpleName());
            }

            valueConverters.put(type, converter);

            return this;
        }

        /**
         * Registers a custom dependency condition checking mechanism that complements the default,
         * case-sensitive {@code String} comparison.
         *
         * @param dependencyChecker The dependency condition checker
         *
         * @return This builder.
         */
        public Builder withDependencyChecker(DependencyChecker dependencyChecker) {
            this.dependencyChecker = Objects.requireNonNull(dependencyChecker, "The dependency checker cannot be null");

            return this;
        }

        /**
         * Registers a validator that will be used to validate the individual configuration objects
         * produced by the configuration factory.
         *
         * @param validator The constraint validator to be registered
         *
         * @return This builder.
         */
        public Builder withConfigurationValidator(ConfigurationValidator validator) {
            if (this.configurationValidator != null) {
                throw new IllegalArgumentException("A configuration validator is already registered");
            }

            this.configurationValidator = Objects
                .requireNonNull(validator, "The configuration validator cannot be null");

            return this;
        }

        /**
         * Builds a new instance of {@link ConfigFactory} set up in the context of this builder.
         *
         * @return The fully initialized {@link ConfigFactory} object.
         */
        public ConfigFactory build() {
            var valueConverter = new Converter(valueConverters);

            return new ConfigFactory(
                classpathDirectory,
                configurationDirectory,
                valueConverter,
                dependencyChecker,
                configurationValidator
            );
        }
    }
}
