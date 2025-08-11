# Basics

## Configuration Factory

The entrypoint into **JXConfig** is the `ConfigFactory` class, responsible for producing
fully initialized configuration instances. You can obtain an instance of this class through its *builder*.

To get a builder instance, use the static `ConfigFactory.builder()` method.

The builder provides several method to set up the `ConfigFactory` instance, these are the basic ones:

- `Builder.withClasspathDir(String)` - Specifies the directory on the classpath where the configuration files
  are located. If not set, the root of the classpath will be used

- `Builder.withFilesystemDir(String)` - Specifies the directory in the filesystem where the configuration files
  are located. If not set, the current working directory (i.e., `./`) will be used

- `Builder.build()` - Returns a new, fully configured instance of the `ConfigFactory`

The factory exposes two methods for producing configuration objects:

- `ConfigFactory.createConfig(Class)` - Produces fully initialized *[configuration types](#configuration-types)*

- `ConfigFactory.createConfigContainer(Class)` - Produces fully initialized
  *[configuration containers](#configuration-containers)*

## Configuration Types

A *configuration type* is a `class` or `record` annotated with `@ConfigFile`. In **JXConfig**, it serves as the runtime
representation of a configuration file, allowing you to work with configuration data through strongly typed objects.

For a declaration to be a valid *configuration type*, two conditions must be met:

1. It must define **exactly one** constructor
2. **Every constructor parameter** must be annotated with either `@ConfigProperty` or `@ConfigGroup` (but not both)

> **Note:** `ConfigGroup` is a more advanced feature and is not covered on this page.

### Declaring a Configuration Type

Consider the following `Network.properties` file:

```properties
Host = 127.0.0.1
Port = 6000
LogLevel = INFO
```

A corresponding *configuration type* can be declared as follows.
When the factory instantiates this type, it retrieves the value for each specified key from the `.properties` file,
converts it to the appropriate constructor parameter type, and passes it to the constructor.

`NetworkConfiguration.java`:

```java
@ConfigFile(filename = "Network.properties")
public record NetworkConfiguration(
    @ConfigProperty(key = "Host")
    String host,

    @ConfigProperty(key = "Port")
    int port,

    @ConfigProperty(key = "LogLevel")
    System.Logger.Level transportType
) {}
```

Passing this *configuration type* to the configuration factory using
`ConfigFactory.createConfig(NetworkConfiguration.class)` will produce an instance initialized
with the values from the specified configuration file.

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var config = factory.createConfig(NetworkConfiguration.class);

    System.out.println(config);
}
```

**Output:**

```
NetworkConfiguration[
    host=127.0.0.1,
    port=6000,
    transportType=INFO
]
```

This example illustrates:

- **File-to-type mapping** - The `filename` in `@ConfigFile` must match the configuration file name
- **Direct property mapping** - Each `@ConfigProperty.key` corresponds to a specific key in the `.properties` file
- **Type flexibility** - **JXConfig** supports mapping to a variety of Java types,
  including enums (`System.Logger.Level` in this example)

### Configuration Property

A *configuration property*, as shown in the previous example, provides a direct mapping to a key
in the `.properties` file.

If the specified key doesn't exist in the file, **an empty string will be used as the default value**.
While this can produce valid runtime defaults for certain types (e.g., empty arrays and collections),
it is insufficient for most others (such as primitive types, enums, and most non-collection reference types).

When a meaningful default value cannot be derived automatically, you must define it explicitly.
`@ConfigProperty` offers two ways to do this - you may choose **only one**:

- `@ConfigProperty.defaultKey` - Specifies another key in the configuration file whose value will be used
  to initialize the parameter

- `@ConfigProperty.defaultValue` - Specifies a hardcoded value to be used as the parameter's default

#### Example

`NetworkConfiguration.java`:

```java
@ConfigFile(filename = "Network.properties")
public record NetworkConfiguration(
    @ConfigProperty(key = "Host")
    String host,

    // Not declared in Network.properties
    @ConfigProperty(key = "DevHost", defaultKey = "Host")
    String devHost,

    @ConfigProperty(key = "Port")
    int port,

    @ConfigProperty(key = "LogLevel")
    System.Logger.Level transportType,

    // Not declared in Network.properties
    @ConfigProperty(key = "AcceptorThreads", defaultValue = "1")
    int acceptorThreads
) {}
```

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var config = factory.createConfig(NetworkConfiguration.class);

    System.out.println(config);
}
```

**Output:**

```
NetworkConfiguration[
    host=127.0.0.1,
    devHost=127.0.0.1,
    port=6000,
    transportType=INFO,
    acceptorThreads=1
]
```

### Loading Configuration Files

The `@ConfigFile` specifies the name of the configuration file that the *configuration type* represents at runtime.
When `ConfigFactory.createConfig` is called, it will look up this file in the configured directories on the classpath
and in the filesystem.

The configuration file must exist in at least one of the configuration directories,
otherwise an exception wil be thrown.

If a file exists in both directories, both files will be loaded and their contents will be merged.
If a key is defined in both files, the value from the file in the filesystem will be used for the
*configuration type* initialization, effectively overriding the value from the classpath file.

## Configuration Containers

Manually creating individual instances of *configuration types* is manageable for one or two configurations,
but becomes inefficient as their number grows.

For large projects, a *configuration container* groups all *configuration types* into a single object, making them
easier to manage and initialize at once.

The only requirement for a *configuration container* is that all of its members must be *configuration types*
(i.e., types annotated with `@ConfigFile`).

`ConfigurationContainer.java`:

```java
public record ConfigurationContainer(
    DatabaseConfiguration database,
    NetworkConfiguration network,
    ServerConfiguration server,
    SomeFeatureConfiguration someFeature
    // ...
) {}
```

Passing this *configuration container* to the configuration factory using
`ConfigFactory.createConfigContainer(ConfigurationContainer.class)` will produce a fully initialized instances
of the container and all of its members.

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var config = factory.createConfigContainer(ConfigurationContainer.class);

    System.out.println(config);
}
```

**Output:**

```
ConfigurationContainer[
    database=...,
    network=NetworkConfiguration[
        host=127.0.0.1,
        devHost=127.0.0.1,
        port=6000,
        transportType=INFO,
        acceptorThreads=1
    ],
    server=...,
    someFeature=...,
]
```
