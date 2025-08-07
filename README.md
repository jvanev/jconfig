# JXConfig

A lightweight, declarative configuration factory for Java. Provides type-safety, effortless conditional
value resolution, grouping, namespaces, and a flexible value conversion mechanism.

## Requirements

Requires Java 17 or newer.

## Features

- Minimal API surface
- Declarative configuration types - Describe your configurations and let the factory do the work for you
- Extensible value conversion mechanism - Add your own value converter if the defaults are not enough
- Customizable validation - Define constraints for your configuration values and catch violations early
- Configuration dependencies - Get rid of the `if` statements for individual dependent configuration
  values or entire groups
- Namespaces - Reuse configuration types for groups of configurations with identical structures
- Fail-fast loading - Invalid configurations are caught early

## Basics

The most basic usage of this factory requires two steps:

1. Define your configuration type
2. Let the `ConfigFactory` instantiate it

### Example

`Database.properties`

```properties
URL = jdbc:mariadb://127.0.0.1:3306/test
User = JXConfig
Password = JXConfig_Password
PoolSize = 7
```

`DatabaseConfig.java`

```java
@ConfigFile(filename = "Database.properties")
public record DatabaseConfig(
    @ConfigProperty(key = "URL") String url,
    @ConfigProperty(key = "User") String user,
    @ConfigProperty(key = "Password") String password,
    @ConfigProperty(key = "PoolSize") int poolSize,

    // When no such key exists in the properties file, a defaultValue is required
    @ConfigProperty(key = "NonExistent", defaultValue = "false") boolean nonExistent
) {}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir").build();
    var dbConfig = factory.createConfig(DatabaseConfig.class);

    System.out.println(dbConfig);

    // Output:
    // DatabaseConfig[
    //     url=jdbc:mariadb://127.0.0.1:3306/test,
    //     user=JXConfig,
    //     password=JXConfig_Password,
    //     poolSize=7,
    //     nonExistent=false
    // ]
}
```

> **Note:** If a configuration property is not found in the properties file and no explicit defaultValue
> is set within its `@ConfigProperty` annotation, JXConfig will attempt to convert an empty string ("")
> for that property. If the conversion mechanism can successfully produce a valid value from an empty string
> (e.g., an empty list for a `List<T>` type, or an empty map for a `Map<K, V>` type), then you are not required
> to provide an explicit `defaultValue`. However, if an empty string cannot be converted to the required type
> (as is the case for `boolean` or `int`), an exception will be thrown, highlighting the need for an explicit
> `defaultValue` like for `NonExistent` in the example above.

## Value Conversion

The configuration factory provides support for converting `String` values to several
common types by default:

- **Primitive types** - `byte`, `short`, `int`, `long`, `float`, `double`, `boolean`, `char`
- **Primitive arrays** - `byte[]`, `short[]`, `int[]`, `long[]`, `float[]`, `double[]`, `boolean[]`, `char[]`
- **Enumerations** - Any `Enum` type, by matching the string value to an enum entry's name.
- **Reference Types with `valueOf(String)`:** Any class that provides a public, static `valueOf(String)` method.
  The return type of this method must be assignable to the target type.
- **Collections:** `List` and `Set`. Elements within the collection are also converted recursively based on
  their generic type argument (e.g., `List<Integer>` will convert string "1,2,3" into a list of integers).
- **Maps:** For `Map<K, V>`, both keys and values are converted recursively based on their generic type arguments
  (e.g., `Map<String, Integer>` will convert "key1=1,key2=2" into a map with string keys and integer values).

When the type of the configuration property is:

- `List` - An `ArrayList` will be used for its initialization
- `Set` - A `LinkedHashSet` will be used for its initialization
- `Map` - A `LinkedHashMap` will be used for its initialization

### Custom Converters

Although the library cannot cover every possible type, it provides a flexible mechanism for adding new type converters
and overriding existing ones.

#### Example

We will use `DateTimeFormatter` as an example of how to add support for an unsupported property type.

`System.properties`

```properties
LogTimestampFormat = yyyy-MM-dd HH:mm:ss.SSS
```

`SystemConfig.java`

```java
@ConfigFile(filename = "System.properties")
public record SystemConfig(
    @ConfigProperty(key = "LogTimestampFormat")
    DateTimeFormatter logTimestampFormatter
) {}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir")
        .withConverter(
            DateTimeFormatter.class,
            (type, typeArgs, value) -> DateTimeFormatter.ofPattern(value)
        )
        .build();
    var systemConfig = factory.createConfig(SystemConfig.class);

    System.out.println(LocalDateTime.parse("2025-08-03T10:15:30").format(systemConfig.logTimestampFormatter()));

    // Output: 2025-08-03 10:15:30.000
}
```

#### Custom Converters and Overriding Behavior

Registering a custom converter using `withConverter` has an important side effect - Custom value
converters take precedence over the default conversion mechanism for direct target matches.
While adding support for additional types doesn't change the default behavior, adding a custom
converter for an already supported type effectively overrides the default conversion mechanism for this type.

If no converter matches the target type directly, the custom converters will be checked in insertion order;
the first one that can produce an assignable value will be used.

#### Example

`System.properties`

```properties
RetryDelay = 5
```

`SystemConfig.java`

```java
@ConfigFile(filename = "System.properties")
public record SystemConfig(
    @ConfigProperty(key = "RetryDelay") long retryDelay
) {}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir")
        .withConverter(
            long.class,
            (type, typeArgs, value) -> Long.parseLong(value) * 1000
        )
        .build();
    var systemConfig = factory.createConfig(SystemConfig.class);

    System.out.println(systemConfig.retryDelay());

    // Output: 5000
}
```

## Value Constraints

All types of applications rely on proper configuration; incorrect configuration might cause
hard-to-find bugs, or even worse - serious security vulnerabilities.

JXConfig offers a flexible solution to validate your configurations at startup, without making
any decision on what sort of constraints should be used and how they should be validated.
You're free to create your own constraints and validators, use your favorite validation framework, or both.

### Example

In order to apply constraints to your configurations, you must create the constraints first.
We'll create a `Range` constraint that will be used to restrict the range of values of a configuration value.

```java
@Retention(RetentionPolicy.RUNTIME) // The annotation must be retained in order to be available at runtime
public @interface Range {
    int min();
    int max();
}
```

Next, you must create a validator providing the logic that checks for constraints violations:

```java
public class RangeValidator implements ConstraintValidator<Range, Integer> {
    @Override
    public boolean validate(Range annotation, Integer value) {
        return value >= annotation.min() && value <= annotation.max();
    }
}
```

You just created a configuration value constraint. Now, let's test it:

`Application.properties`

```properties
# Violated constraint
Port = -1
ThreadPoolSize = 1
```

Apply your brand-new constraint to your configuration:

```java
@ConfigFile(filename = "Application.properties")
public record ApplicationConfig(
    @ConfigProperty(key = "Port")
    @Range(min = 0, max = 65535)
    int port,

    @ConfigProperty(key = "ThreadPoolSize")
    @Range(min = 1, max = 100)
    int poolSize
) {}
```

Let JXConfig know that your validator exists:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir")
        // Register your brand-new validator
        .withConstraintValidator(new RangeValidator())
        .build();
    
    // This line will throw because its constraints have been violated
    // by the Port property in the config file
    var appConfig = factory.createConfig(ApplicationConfig.class);
}
```

You can now be sure that your configurations are properly set up.
Any violation will throw an exception preventing all kinds of issues from sneaking in to the runtime.

> **Note:** You can apply as many constraints as you need.

## Configuration Dependencies

It's not uncommon for a configuration value to depend on another configuration value. For example,
the `LogLevel` should be set to `DEBUG` only in developer mode, or fall back to `INFO` otherwise. Usually,
such setup involves `if` statements determining the value at runtime.

JXConfig offers another approach - Configuration Dependencies. Declaratively describe the relationships between
dependent properties, and JXConfig will resolve the final runtime values for you.

If the value of the dependency matches the required value, the dependent property will be initialized with the
value from the configuration file.
If the value values don't match, the property will be initialized using either `ConfigProperty.fallbackKey`,
or `ConfigProperty.defaultValue` - only one of these members should be defined.

`ConfigProperty.fallbackKey` refers to a key in the `.properties` file, while `ConfigProperty.defaultValue`
is a hardcoded value in your configuration type.

### Example

`Developer.properties`

```properties
DeveloperMode = false
LogLevel = DEBUG
```

`DeveloperConfig.java`

```java
@ConfigFile(filename = "Developer.properties")
public record DeveloperConfig(
    @ConfigProperty(key = "DeveloperMode") boolean developerMode,

    @ConfigProperty(key = "LogLevel", defaultValue = "INFO")
    @DependsOn(property = "DeveloperMode", value = "true")
    System.Logger.Level logLevel
) {}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir").build();
    var developerConfig = factory.createConfig(DeveloperConfig.class);

    System.out.println(developerConfig);

    // Output:
    // DeveloperConfig[
    //     developerMode=false,
    //     logLevel=INFO
    // ]
}
```

> **DependsOn.key:** If `DeveloperConfig.developerMode` is not used anywhere else in your project, you can safely
> remove it and use `@DependsOn(key = "DeveloperMode")` instead. In this case, `DeveloperMode`'s value will be read
> from the configuration file without the need for an intermediate parameter declaration.

> **Note:** By default, the `value` parameter of `@DependsOn` is set to `true`, emulating a boolean-style comparison,
> so technically the declaration of `@DependsOn.value` in the example above is redundant.

> **Note 2:** The dependent value is compared to the dependency's resolved value, which means if the dependency has a
> dependency of its own, that relationship will be checked first to determine
> the final runtime value for the dependency.

### Custom Dependency Condition Check

By default, a **case-sensitive string comparison** is performed to determine whether
the resolved value of the dependency matches the required value specified in `@DependsOn.value`.

If this doesn't meet your needs, you can provide a custom comparison strategy via a `DependencyChecker`.
`DependsOn.operator` should be used to specify operators supported by the custom checker.

#### Example

`Example.properties`

```properties
SomeNumber = 127
LogLevel = TRACE

ConfigurationA = true
ConfigurationB = true
ConfigurationC = true
ConfigurationD = true
```

`Main.java`

```java
public class Main {
    private static class CustomChecker implements DependencyChecker {
        @Override
        public boolean check(String dependencyValue, String operator, String requiredValue) {
            return switch (operator) {
                case ">" -> Integer.parseInt(dependencyValue) > Integer.parseInt(requiredValue);
                case "|" -> {
                    for (var entry : requiredValue.split("\\|")) {
                        if (dependencyValue.equals(entry)) {
                            yield true;
                        }
                    }

                    yield false;
                }
                default -> false;
            };
        }
    }

    @ConfigFile(filename = "Example.properties")
    public record ExampleConfiguration(
        @ConfigProperty(key = "SomeNumber")
        int integerA,

        @ConfigProperty(key = "LogLevel")
        System.Logger.Level logLevel,

        @ConfigProperty(key = "ConfigurationA", defaultValue = "false")
        @DependsOn(property = "SomeNumber", operator = ">", value = "126")
        boolean configurationA,

        @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
        @DependsOn(property = "SomeNumber", operator = ">", value = "127")
        boolean configurationB,

        @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
        @DependsOn(property = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
        boolean configurationC,

        @ConfigProperty(key = "ConfigurationD", defaultValue = "false")
        @DependsOn(property = "LogLevel", operator = "|", value = "INFO|WARN|ERROR")
        boolean configurationD
    ) {}

    public static void main(String[] args) {
        var factory = ConfigFactory.builder("../configDir")
            .withDependencyChecker(new CustomChecker())
            .build();
        var config = factory.createConfig(ExampleConfiguration.class);

        System.out.println(config);

        // Output:
        // ExampleConfiguration[
        //     integerA=127,
        //     logLevel=TRACE,
        //     configurationA=true,
        //     configurationB=false,
        //     configurationC=true,
        //     configurationD=false
        // ]
    }
}
```

> **Note:** Your custom checker will be invoked for every configuration property that declares
> a dependency with a non-default `@DependsOn.operator`.
> Ensure the checker is performant if you expect a high number of dependencies relying on it.

## Configuration Groups

When multiple configuration values depend on a single configuration value, we can define a configuration group
that switches them all to their default values if the dependency's value doesn't match the requirement.
Back to the developer mode example, it usually does more than just switching the log level (e.g., turns on additional
features useful for debugging).

### Example

`Developer.properties`

```properties
DeveloperMode = false
LogLevel = DEBUG
ShowDebugOverlay = true
BypassLogin = true
RateLimit = 0
```

`DeveloperConfig.java`

```java
@ConfigFile(filename = "Developer.properties")
public record DeveloperConfig(
    @ConfigProperty(key = "DeveloperMode") boolean developerMode,

    @ConfigGroup
    @DependsOn(property = "DeveloperMode")
    DependentConfig dependentConfig
) {
    public record DependentConfig(
        @ConfigProperty(key = "LogLevel", defaultValue = "INFO")
        System.Logger.Level logLevel,

        @ConfigProperty(key = "ShowDebugOverlay", defaultValue = "false")
        boolean debugOverlay,

        @ConfigProperty(key = "BypassLogin", defaultValue = "false")
        boolean bypassLogin,

        @ConfigProperty(key = "RateLimit", defaultValue = "127")
        int rateLimit
  ) {}
}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir").build();
    var developerConfig = factory.createConfig(DeveloperConfig.class);

    System.out.println(developerConfig);

    // Output:
    // DeveloperConfig[
    //     developerMode=false,
    //     dependentConfig=DependentConfig[
    //         logLevel=INFO,
    //         debugOverlay=false,
    //         bypassLogin=false,
    //         rateLimit=127
    //     ]
    // ]
}
```

## Configuration Namespaces

It's not uncommon for different configurations to have a common structure. For example, plugins,
services, features, etc. Using JXConfig, such configurations can be represented by a single
configuration type we call a *Namespace*. A namespace is simply a `@ConfigGroup` with a specified name.

### Example

`Network.properties`

```properties
LoginServer.Host = 127.0.0.1
LoginServer.Port = 4460
LoginServer.AcceptorThreads = 1
LoginServer.WorkerThreads = 0
LoginServer.WaterMarkLow = 32
LoginServer.WaterMarkHigh = 64

GameServer.Host = 192.168.0.1
GameServer.Port = 6543
GameServer.AcceptorThreads = 1
GameServer.WorkerThreads = 0
GameServer.WaterMarkLow = 64
GameServer.WaterMarkHigh = 128
```

`NetworkConfig.java`

```java
@ConfigFile(filename = "Network.properties")
public record NetworkConfig(
    @ConfigGroup(namespace = "LoginServer") ServerConfig login,
    @ConfigGroup(namespace = "GameServer") ServerConfig game
) {
    public record ServerConfig(
        @ConfigProperty(key = "Host") String host,
        @ConfigProperty(key = "Port") int port,
        @ConfigProperty(key = "AcceptorThreads") int acceptorThreads,
        @ConfigProperty(key = "WorkerThreads") int workerThreads,
        @ConfigProperty(key = "WaterMarkLow") int watermarkLow,
        @ConfigProperty(key = "WaterMarkHigh") int watermarkHigh
    ) {}
}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir").build();
    var networkConfig = factory.createConfig(NetworkConfig.class);

    System.out.println(networkConfig);

    // Output:
    // NetworkConfig[
    //     login=ServerConfig[
    //         host=127.0.0.1,
    //         port=4460,
    //         acceptorThreads=1,
    //         workerThreads=0,
    //         watermarkLow=32,
    //         watermarkHigh=64
    //     ],
    //     game=ServerConfig[
    //         host=192.168.0.1,
    //         port=6543,
    //         acceptorThreads=1,
    //         workerThreads=0,
    //         watermarkLow=64,
    //         watermarkHigh=128
    //     ]
    // ]
}
```

> **Note:** The namespaces can also define `@DependsOn` and the same rules as for unnamed groups will apply.

> **Note 2:** The namespaces can be as deeply nested as needed, the `@ConfigGroup.namespace` corresponds to
> a single level of nesting (i.e., `@ConfigGroup("Database")` refers to the second level in the namespace
> `LoginServer.Database.URL`)

## Configuration Containers

Last but not least, calling `ConfigFactory.createConfig` for every configuration type might become cumbersome
when there are many configuration types. In this case, we can define a *configuration container* that provides
access to all configurations.

We will use a configuration container, which doesn't need any annotations for its definition, to group all previously
defined configuration types in a single POJO. The only difference when using a configuration container is the method
we use to create its instance - `ConfigFactory.createConfigContainer`.

### Example

`ConfigContainer.java`

```java
public record ConfigContainer(
    DatabaseConfig database,
    NetworkConfig network,
    SystemConfig system,
    DeveloperConfig developer
) {}
```

`Main.java`

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder("../configDir")
        .withConverter(
            DateTimeFormatter.class,
            (type, typeArgs, value) -> DateTimeFormatter.ofPattern(value)
        )
        .build();
    var config = factory.createConfigContainer(ConfigContainer.class);

    System.out.println(config);

    // Output:
    // ConfigContainer[
    //     database=DatabaseConfig[
    //         url=jdbc:mariadb://127.0.0.1:3306/test,
    //         user=JXConfig,
    //         password=JXConfig_Password,
    //         poolSize=7,
    //         nonExistent=false
    //     ],
    //     network=NetworkConfig[
    //         login=ServerConfig[
    //             host=127.0.0.1,
    //             port=4460,
    //             acceptorThreads=1,
    //             workerThreads=0,
    //             watermarkLow=32,
    //             watermarkHigh=64
    //         ],
    //         game=ServerConfig[
    //             host=192.168.0.1,
    //             port=6543,
    //             acceptorThreads=1,
    //             workerThreads=0,
    //             watermarkLow=64,
    //             watermarkHigh=128
    //         ]
    //     ],
    //     system=SystemConfig[
    //         logTimestampFormatter=...,
    //         retryDelay=5
    //     ],
    //     developer=DeveloperConfig[
    //         developerMode=false,
    //         dependentConfig=DependentConfig[
    //             logLevel=INFO,
    //             debugOverlay=false,
    //             bypassLogin=false,
    //             rateLimit=127
    //         ]
    //     ]
    // ]
}
```

> **Note:** Instantiation of the configuration types is exactly the same as if they were instantiated one by one using
> `ConfigFactory.createConfig`.

## License

```
Copyright 2025 Georgi Vanev

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
