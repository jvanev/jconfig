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
import com.jvanev.jxconfig.annotation.ConfigProperty;
import com.jvanev.jxconfig.converter.ValueConverter;
import com.jvanev.jxconfig.converter.internal.Converter;
import com.jvanev.jxconfig.exception.ConfigurationBuildException;
import com.jvanev.jxconfig.exception.InvalidDeclarationException;
import com.jvanev.jxconfig.exception.ValidationException;
import com.jvanev.jxconfig.exception.ValueConversionException;
import com.jvanev.jxconfig.internal.ReflectionUtil;
import com.jvanev.jxconfig.resolver.DependencyChecker;
import com.jvanev.jxconfig.resolver.internal.ValueResolver;
import com.jvanev.jxconfig.validator.ConstraintValidator;
import com.jvanev.jxconfig.validator.ValidationBridge;
import com.jvanev.jxconfig.validator.internal.ValidationPair;
import com.jvanev.jxconfig.validator.internal.Validator;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;

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
    private static final String DEFAULT_DIR = "./";

    private static final String DEFAULT_CLASS_PATH = "";

    private final String classpath;

    private final Path configurationDirectory;

    private final Converter converter;

    /**
     * The constraints validation mechanism. Might be {@code null}, indicating that no
     * validators has been registered.
     */
    private final Validator validator;

    /**
     * The additional dependency checking mechanism. Might be {@code null}, indicating that no
     * checker has been registered.
     */
    private final DependencyChecker checker;

    // Instances of the factory are obtained through the dedicated builder
    private ConfigFactory(
        String classpath,
        String configurationDirectory,
        Converter converter,
        Validator validator,
        DependencyChecker checker
    ) {
        this.classpath = classpath.isBlank() ? classpath : classpath.endsWith("/")
            ? classpath
            : classpath + "/";
        this.configurationDirectory = Path.of(configurationDirectory);
        this.converter = converter;
        this.validator = validator;
        this.checker = checker;
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
                "An error occurred while loading configuration file " + configFile.filename() +
                    ". Attempted to load the file from classpath '" + classpath + "' and directory '" +
                    configurationDirectory + "'",
                e
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
        var valueResolver = new ValueResolver(context.properties(), type, context.namespace(), parameters, checker);
        var processedParameters = new HashMap<String, String>();

        for (var i = 0; i < parameters.length; i++) {
            var parameter = parameters[i];

            if (ReflectionUtil.isConfigGroup(parameter)) {
                var newContext = context.fromGroup(parameter, valueResolver.isGroupDependencySatisfied(parameter));
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

                try {
                    arguments[i] = converter.convert(parameter.getParameterizedType(), resolvedValue.trim());
                } catch (Exception e) {
                    throw new ValueConversionException(
                        "Failed to convert the resolved value for configuration property %s (%s.%s)"
                            .formatted(property.key(), type.getSimpleName(), parameter.getName()),
                        e
                    );
                }

                if (validator != null) {
                    try {
                        validator.validateConstraints(parameter.getDeclaredAnnotations(), arguments[i]);
                    } catch (Exception e) {
                        throw new ValidationException(
                            "Failed to validate the converted value for configuration property %s (%s.%s)"
                                .formatted(property.key(), type.getSimpleName(), parameter.getName()),
                            e
                        );
                    }
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
     * The file is expected to be located within the {@link #classpath} and/or
     * {@link #configurationDirectory}.
     * <p>
     * If the file exists in both locations, both files will be loaded and their contents will be merged.
     * The content of the file from the filesystem will override the matching content of the file from the classpath.
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

        try (var stream = ClassLoader.getSystemClassLoader().getResourceAsStream(classpath + filename)) {
            if (stream != null) {
                defaultConfigFound = true;

                properties.load(stream);
            }
        }

        try (var file = Files.newInputStream(configurationDirectory.resolve(filename))) {
            properties.load(file);
        } catch (IOException e) {
            if (!defaultConfigFound) {
                // No default, no override - we have no other option
                throw e;
            }
        }

        return properties;
    }

    /**
     * Returns a new builder object responsible for building a new instance of {@link ConfigFactory}.
     * <p>
     * The new factory instance will be configured to load files from the default sources:
     * <ul>
     *     <li>The root of the application's {@code resources} directory for classpath lookup</li>
     *     <li>The current working directory (i.e., {@code ./}) for filesystem lookup</li>
     * </ul>
     * <p>
     * To configure the source paths, use {@link #builder(String)} or {@link #builder(String, String)}.
     *
     * @return A new {@link Builder} for constructing a {@link ConfigFactory}.
     */
    public static Builder builder() {
        return new Builder(DEFAULT_CLASS_PATH, DEFAULT_DIR);
    }

    /**
     * Returns a new builder object responsible for building a new instance of {@link ConfigFactory}.
     * <p>
     * The new factory instance will be configured to load files from the specified path,
     * which must include one of the following prefixes, indicating the source type:
     * <ul>
     *     <li>
     *         {@code classpath} - defines a subdirectory in the classpath where the configuration files are located.
     *         For example: {@code classpath:config}, {@code classpath:someDir/someSubDir/...}, etc.
     *     </li>
     *     <li>
     *         {@code dir} - defines a directory in the local filesystem.
     *         For example: {@code dir:/var/config}, {@code dir:./dir/config}, etc.
     *     </li>
     * </ul>
     * <p>
     * Using this method explicitly sets only one of the sources, while leaving the other at its default.
     * For example, specifying {@code dir:/var/config} will load files from both {@code /var/config} and
     * the root of the classpath.
     * <p>
     * The default classpath is the root of the application's {@code resources} directory.<br />
     * The default filesystem directory is the current working directory (i.e., {@code ./}).
     *
     * @param path The source path for {@code .properties} configuration files
     *
     * @return A new {@link Builder} for constructing a {@link ConfigFactory}.
     */
    public static Builder builder(String path) {
        String classpath = DEFAULT_CLASS_PATH;
        String dir = DEFAULT_DIR;

        if (path.startsWith(Builder.CLASSPATH_PREFIX)) {
            classpath = path.substring(Builder.CLASSPATH_PREFIX.length());
        } else if (path.startsWith(Builder.DIR_PREFIX)) {
            dir = path.substring(Builder.DIR_PREFIX.length());
        } else {
            throw new IllegalArgumentException(
                "Missing or invalid source type prefix. Use " + Builder.CLASSPATH_PREFIX + " or " + Builder.DIR_PREFIX
            );
        }

        return new Builder(classpath, dir);
    }

    /**
     * Returns a new builder object responsible for building a new instance of {@link ConfigFactory}.
     * The new factory instance will be configured to load files from the specified paths.
     *
     * @param classpath The base path within the classpath, relative to the application's {@code resources} directory
     * @param dir       The path to the filesystem directory containing configuration files
     *
     * @return A new {@link Builder} for constructing a {@link ConfigFactory}.
     */
    public static Builder builder(String classpath, String dir) {
        return new Builder(classpath, dir);
    }

    /**
     * This class is responsible for building immutable instances of {@link ConfigFactory}.
     */
    public static class Builder {
        private static final String CLASSPATH_PREFIX = "classpath:";

        private static final String DIR_PREFIX = "dir:";

        /**
         * A predicate to find the {@link ConstraintValidator} interface of a validator during registration.
         */
        private static final Predicate<Type> VALIDATOR_INTERFACE_PREDICATE = type ->
            type instanceof ParameterizedType parameterizedType &&
                parameterizedType.getRawType() == ConstraintValidator.class;

        private final String classpath;

        private final String configurationDirectory;

        private final Map<Class<?>, ValueConverter> converters = new LinkedHashMap<>();

        private Map<Class<? extends Annotation>, ValidationPair> validators = null;

        private Set<ValidationBridge> bridges = null;

        private DependencyChecker dependencyChecker = null;

        // Instantiable by the builder method only
        private Builder(String classpath, String configurationDirectory) {
            this.classpath = classpath;
            this.configurationDirectory = configurationDirectory;
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
        public Builder withConverter(Class<?> type, ValueConverter converter) {
            if (converters.containsKey(type)) {
                throw new IllegalArgumentException("Duplicate converter found for type " + type.getSimpleName());
            }

            converters.put(type, converter);

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
            this.dependencyChecker = dependencyChecker;

            return this;
        }

        /**
         * Registers a validator that will be used to validate the resolved values of configuration
         * properties annotated with its supported annotation. The validator is invoked after the
         * resolved value has been converted to the type of the {@link ConfigProperty}-annotated
         * parameter.
         *
         * @param validator The constraint validator to be registered
         *
         * @return This builder
         */
        @SuppressWarnings("unchecked")
        public Builder withConstraintValidator(ConstraintValidator<? extends Annotation, ?> validator) {
            if (validators == null) {
                validators = new LinkedHashMap<>();
            }

            var constraintValidator = Arrays.stream(validator.getClass().getGenericInterfaces())
                .filter(VALIDATOR_INTERFACE_PREDICATE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    validator.getClass().getSimpleName() + " does not implement ConstraintValidator"
                ));
            var typeArguments = ((ParameterizedType) constraintValidator).getActualTypeArguments();
            var constraint = (Class<Annotation>) typeArguments[0];
            var valueType = (Class<?>) typeArguments[1];
            var pair = new ValidationPair(validator, valueType);

            if (validators.putIfAbsent(constraint, pair) != null) {
                throw new IllegalArgumentException(
                    "Duplicate constraint registration for %s by %s. %s is already registered by %s."
                        .formatted(
                            constraint.getSimpleName(), validator.getClass().getSimpleName(),
                            constraint.getSimpleName(), validators.get(constraint).getClass().getSimpleName()
                        )
                );
            }

            return this;
        }

        /**
         * Registers a validation bridge that delegates the constraint validations to an external service.
         *
         * @param bridge The bridge to an external constraints validator
         *
         * @return This builder.
         */
        public Builder withValidationBridge(ValidationBridge bridge) {
            if (bridges == null) {
                bridges = new HashSet<>();
            }

            if (!bridges.add(bridge)) {
                throw new IllegalArgumentException(
                    "Bridge " + bridge.getClass().getSimpleName() + " is already registered"
                );
            }

            return this;
        }

        /**
         * Builds a new instance of {@link ConfigFactory} set up in the context of this builder.
         *
         * @return The fully initialized {@link ConfigFactory} object.
         */
        public ConfigFactory build() {
            var converter = new Converter();

            for (var set : converters.entrySet()) {
                converter.addValueConverter(set.getKey(), set.getValue());
            }

            Validator validator = null;

            if (validators != null || bridges != null) {
                validator = new Validator(bridges, validators);
            }

            return new ConfigFactory(classpath, configurationDirectory, converter, validator, dependencyChecker);
        }
    }
}
