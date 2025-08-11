# Configuration Dependencies

A configuration dependency is a declarative rule that makes the runtime value of one configuration property
conditional upon the value of another. This allows for complex configuration logic to be defined without
writing imperative code.

The process involves several key components:

- **The Dependency:** The property or key whose value is checked. This can be another property
  within the same configuration record (using `@DependsOnProperty`) or a raw key from the
  configuration source (using `@DependsOnKey`)

- **The Dependent:** The property or namespace that is annotated with a dependency annotation.
  Its final value is determined by the outcome of the dependency check

- **The Condition:** The rule used to evaluate the dependency's value - a **case-sensitive string comparison**
  by default

- **The Resolved Value:** The final value of any property after all of its dependency conditions have been evaluated.
  This is the value ultimately used to construct the configuration object

## Resolution Process

When resolving a dependent property, **JXConfig** follows a precise logical order:

1. First, it identifies the dependency

2. It then determines the resolved value of that dependency. Crucially, if the dependency has dependencies
   of its own, they are resolved recursively first. This ensures conditions are always evaluated against the final,
   correct value

3. The dependency's resolved value is then checked against the dependent's condition (using the default string
   comparison or a custom `DependencyChecker`)

4. If the condition is satisfied, the dependent's value is loaded from its corresponding key in the configuration
   source

5. If the condition is not satisfied, the dependent falls back to its specified `defaultValue` or `defaultKey`
   reference

## Declaring Dependency

A dependency can be declared using one of two annotations - `@DependsOnKey` or `@DependsOnProperty` - but not both.

Use `@DependsOnKey` when you want to depend on a way key from the configuration source that is **not**
also a property in your current record.

Use `@DependsOnProperty` when you want to depend on the **resolved value** of another property that is
defined as a field in the same record.

> **Note:**
>
> The `@DependsOnKey` annotation is particularly useful for dependencies that act as control flags rather than actual
> configuration data that your application needs. For instance, a key like `DeveloperMode` might exist solely
> to enable or disable a group of other debugging-related properties. Your application may never need to read
> the value of `DeveloperMode` itself. By using `@DependsOnKey`, you can make your properties conditional on
> this flag without needing to add a redundant boolean `developerMode` field to your configuration record,
> keeping it clean and focused only on the data your application will actually use.

### Example

Consider the following example:

`Developer.properties`:

```properties
DeveloperMode = false

LogLevel = DEBUG
LogRequests = true
LogResponses = true
DisableEncryption = true

SuppressWarnings = false
```

`DeveloperConfiguration.java`:

```java
@ConfigFile(filename = "Developer.properties")
public record DeveloperConfiguration(
    @ConfigProperty(key = "LogLevel", defaultValue = "INFO")
    @DependsOnKey(name = "DeveloperMode", value = "true")
    System.Logger.Level logLevel,

    @ConfigProperty(key = "LogRequests", defaultValue = "false")
    @DependsOnProperty(name = "LogLevel", value = "DEBUG")
    boolean logRequests,

    @ConfigProperty(key = "LogResponses", defaultValue = "false")
    @DependsOnProperty(name = "LogLevel", value = "DEBUG")
    boolean logResponses,

    @ConfigProperty(key = "SuppressWarnings", defaultValue = "true")
    @DependsOnKey(name = "LogLevel", value = "INFO")
    boolean suppressWarnings
) {}
```

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var developerConfig = factory.createConfig(DeveloperConfiguration.class);

    System.out.println(developerConfig);
}
```

**Output:**

```
DeveloperConfiguration[
    logLevel=INFO,
    logRequests=false,
    logResponses=false,
    suppressWarnings=false
]
```

The `LogLevel` depends on a key in the configuration file - `DeveloperMode` - which determines its
runtime log level. `DeveloperMode` also determines the runtime values for `LogRequests` and `LogResponses`,
because these properties depend on `LogLevel` to have the value `DEBUG`.

This example illustrates:

- **Dependency chains** - A property can depend on another property that itself has a dependency

- **Dependency on file keys** - A property can depend on a key declared in the configuration file

- **Unsatisfied conditions** - When the dependency condition of a dependent property is not satisfied
  it falls back to its default value

- **Default values can satisfy a dependent property's conditions** - In the case of `SuppressWarnings`,
  the default value of `LogLevel` satisfies its condition

## Dependent Namespaces

A `@DependsOn*` annotation can be applied directly to a `@ConfigNamespace`. This effectively turns
the dependency into a **master switch** for the entire group of properties and namespaces within that namespace.

If the dependency condition is satisfied, all properties inside the namespace will be loaded from the configuration
source as usual. The dependency rules within the namespace still apply.

If the dependency condition is not satisfied, every single property within that namespace will fall back to its
respective default value. This also includes properties in namespaces nested within that namespace.

### Example

`Example.properties`:

```properties
PluginsEnabled = false

PluginOne.Enabled = true
PluginOne.ParameterOne = feature one - param 1
PluginOne.ParameterTwo = 128
PluginOne.Feature.ParameterOne = feature one - feature_param 1
PluginOne.Feature.ParameterTwo = 65535

PluginTwo.Enabled = true
PluginTwo.ParameterOne = feature two - param 1
PluginTwo.ParameterTwo = 256
PluginTwo.Feature.ParameterOne = feature two - feature_param 2
PluginTwo.Feature.ParameterTwo = 1048575
```

`ExampleConfiguration.properties`:

```java
@ConfigFile(filename = "Example.properties")
public record ExampleConfiguration(
    @ConfigNamespace("PluginOne")
    @DependsOnKey(name = "PluginsEnabled", value = "true")
    PluginConfiguration pluginOne,

    @ConfigNamespace("PluginTwo")
    @DependsOnKey(name = "PluginsEnabled", value = "true")
    PluginConfiguration pluginTwo
) {
    public record PluginConfiguration(
        @ConfigProperty(key = "Enabled", defaultValue = "false")
        boolean enabled,

        @ConfigProperty(key = "ParameterOne", defaultValue = "disabled plugin")
        String parameterOne,

        @ConfigProperty(key = "ParameterTwo", defaultValue = "0")
        int parameterTwo,

        @ConfigNamespace("Feature")
        FeatureConfiguration feature
    ) {
        public record FeatureConfiguration(
            @ConfigProperty(key = "ParameterOne", defaultValue = "disabled feature")
            String parameterOne,

            @ConfigProperty(key = "ParameterTwo", defaultValue = "-1")
            int parameterTwo
        ) {}
    }
}
```

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var config = factory.createConfig(ExampleConfiguration.class);

    System.out.println(config);
}
```

**Output:**

```
ExampleConfiguration[
    pluginOne=PluginConfiguration[
        enabled=false,
        parameterOne=disabled plugin,
        parameterTwo=0,
        feature=FeatureConfiguration[
            parameterOne=disabled feature,
            parameterTwo=-1
        ]
    ],
    pluginTwo=PluginConfiguration[
        enabled=false,
        parameterOne=disabled plugin,
        parameterTwo=0,
        feature=FeatureConfiguration[
            parameterOne=disabled feature,
            parameterTwo=-1
        ]
    ]
]
```

### Default Namespace

If `@ConfigNamespace` is applied without specifying a `@ConfigNamespace.value`, it creates a group of properties
within the default namespace, which is the root of the configuration file.

When multiple configuration values depend on a single configuration value, we can group them within the default
namespace and switch them all to their defaults if the dependency's value doesn't match the requirement.

#### Example

`Developer.properties`:

```properties
DeveloperMode = false
LogLevel = DEBUG
ShowDebugOverlay = true
BypassLogin = true
RateLimit = 0
```

`DeveloperConfig.java`:

```java
@ConfigFile(filename = "Developer.properties")
public record DeveloperConfig(
    @ConfigProperty(key = "DeveloperMode")
    boolean developerMode,

    @ConfigNamespace
    @DependsOnKey(name = "DeveloperMode")
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

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var developerConfig = factory.createConfig(DeveloperConfig.class);

    System.out.println(developerConfig);
}
```

**Output:**

```
DeveloperConfig[
    developerMode=false,
    dependentConfig=DependentConfig[
        logLevel=INFO,
        debugOverlay=false,
        bypassLogin=false,
        rateLimit=127
    ]
]
```

## Custom Dependency Condition Check

As explained above, a **case-sensitive string comparison** is performed to determine whether
the resolved value of the dependency matches the required value specified in `@DependsOn*.value`.

If this doesn't meet your needs, you can provide a custom comparison strategy via a `DependencyChecker`.
`@DependsOn*.operator` should be used to specify an operator supported by the custom checker.

### Example

`Example.properties`:

```properties
SomeNumber = 127
LogLevel = TRACE
ConfigurationA = true
ConfigurationB = true
ConfigurationC = true
ConfigurationD = true
```

`ExampleConfiguration.java`:

```java
@ConfigFile(filename = "Example.properties")
public record ExampleConfiguration(
    @ConfigProperty(key = "SomeNumber")
    int integerA,

    @ConfigProperty(key = "LogLevel")
    System.Logger.Level logLevel,

    @ConfigProperty(key = "ConfigurationA", defaultValue = "false")
    @DependsOnProperty(name = "SomeNumber", operator = ">", value = "126")
    boolean configurationA,

    @ConfigProperty(key = "ConfigurationB", defaultValue = "false")
    @DependsOnProperty(name = "SomeNumber", operator = ">", value = "127")
    boolean configurationB,

    @ConfigProperty(key = "ConfigurationC", defaultValue = "false")
    @DependsOnProperty(name = "LogLevel", operator = "|", value = "DEBUG|TRACE|INFO")
    boolean configurationC,

    @ConfigProperty(key = "ConfigurationD", defaultValue = "false")
    @DependsOnProperty(name = "LogLevel", operator = "|", value = "INFO|WARN|ERROR")
    boolean configurationD
) {}
```

`CustomChecker.java`:

```java
public class CustomChecker implements DependencyChecker {
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
```

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder()
        .withDependencyChecker(new CustomChecker())
        .build();
    var config = factory.createConfig(ExampleConfiguration.class);

    System.out.println(config);
}
```

**Output:**

```
ExampleConfiguration[
    integerA=127,
    logLevel=TRACE,
    configurationA=true,
    configurationB=false,
    configurationC=true,
    configurationD=false
]
```

> **Note:** Your custom checker will be invoked for every `@DependsOn*` with non-default `operator`.
> Ensure the checker is performant if you expect a high number of dependencies relying on it.
