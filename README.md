# KConfig

A lightweight, declarative configuration factory for Java and Kotlin. Provides type-safety, effortless conditional
value resolution, grouping, namespaces, and a flexible value conversion mechanism.

---

## Requirements

Built with Kotlin 2.2.0, requires Java 17 or newer.

Usage in Java projects requires `kotlin-stdlib 2.2.0`

---

## Features

- Simple and straightforward API - Just 3 simple methods to remember
- Declarative configuration types - Describe your configurations and let the factory do the work for you
- Extensible value conversion mechanism - Add your own value converter if the defaults are not enough
- Configuration dependencies - Get rid of the `if` statements for dependent configuration values,
  either individual or entire groups
- Namespaces - Reuse configuration types for groups of configurations with identical structures

---

## Basics

The most basic usage of this factory requires two steps:

1. Define your configuration type
2. Let the `ConfigFactory` instantiate it

### Example

`Database.properties`

```properties
URL = jdbc:mariadb://127.0.0.1:3306/test
User = kconfig
Password = kconfig_password
PoolSize = 7
```

`DatabaseConfig.kt`

```kotlin
@ConfigFile("Database.properties")
data class DatabaseConfig(
    @ConfigProperty("URL")
    val url: String,

    @ConfigProperty("User")
    val user: String,

    @ConfigProperty("Password")
    val password: String,

    @ConfigProperty("PoolSize")
    val poolSize: Int,

    // When no such key exists in the properties file, a defaultValue is required
    @ConfigProperty(name = "NonExistent", defaultValue = "False")
    val nonExistent: Boolean,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath")
    val dbConfig = factory.createConfig(DatabaseConfig::class.java)

    println(dbConfig)
}
```

> **Output:** DatabaseConfig(url=jdbc:mariadb://127.0.0.1:3306/test, user=kconfig, password=kconfig_password,
> poolSize=7, nonExistent=false)

> **Note:** If a configuration property is not found in the properties file and no explicit defaultValue
> is set within its `@ConfigProperty` annotation, KConfig will attempt to convert an empty string ("")
> for that property. If the conversion mechanism can successfully produce a valid value from an empty string
> (e.g., an empty list for a List<T> type, or an empty map for a Map<K, V> type), then you are not required
> to provide an explicit `defaultValue`. However, if an empty string cannot be converted to the required type
> (as is the case for `Boolean` or `Int`), an exception will be thrown, highlighting the need for an explicit
> `defaultValue` like for `NonExistent` in the example above.

---

## Value Conversion

The configuration factory provides support for converting `String` values to several
common types by default:

- **Primitive types** - `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `Boolean`, `Char`
- **Primitive arrays** - `ByteArray`, `ShortArray`, `IntArray`, `LongArray`, `FloatArray`, `DoubleArray`,
  `BooleanArray`, `CharArray`
- **Enumerations** - Any `Enum` type, by matching the string value to an enum entry's name.
- **Reference Types with `valueOf(String)`:** Any class that provides a public, static `valueOf(String)` method
  (e.g., `java.math.BigDecimal`, `java.util.UUID`). The return type of this method must be assignable
  to the target type.
- **Collections:** `Collection`, `List`, `Set`. Elements within the collection are also converted recursively based on
  their generic type argument (e.g., `List<Int>` will convert string "1,2,3" into a list of integers).
- **Maps:** `Map`. Both keys and values within the map are converted recursively based on their generic type arguments
  (e.g., `Map<String, Int>` will convert "key1=1,key2=2" into a map with string keys and integer values).

When the type of the configuration property is:

- `Collection` or `List` - An `ArrayList` will be used for its initialization (through `mutableListOf`)
- `Set` - A `LinkedHashSet` will be used for its initialization (through `mutableSetOf`)
- `Map` - A `LinkedHashMap` will be used for its initialization (through `mutableMapOf`)

### Custom Converters

Although the library cannot cover every possible type, it provides a flexible mechanism for adding new type converters
and overriding existing ones.

#### Example

We will use `DateTimeFormatter` as an example of how to add support for an unsupported property type.

`System.properties`

```properties
LogTimestampFormat = yyyy-MM-dd HH:mm:ss.SSS
```

`SystemConfig.kt`

```kotlin
@ConfigFile("System.properties")
data class SystemConfig(
    @ConfigProperty("LogTimestampFormat")
    val logTimestampFormatter: DateTimeFormatter,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath").apply {
        addValueConverter(DateTimeFormatter::class.java) { value, targetType, actualTypeArgumentsArray ->
            DateTimeFormatter.ofPattern(value)
        }
    }
    val systemConfig = factory.createConfig(SystemConfig::class.java)

    println(LocalDateTime.now().format(systemConfig.logTimestampFormatter))
}
```

> **Output:** 2025-07-30 19:51:17.943

#### Custom Converters and Overriding Behavior

Using `addValueConverter` has an important side effect - Custom value converters take precedence
over the default conversion mechanism. While adding support for additional types doesn't change the
default behavior, adding a custom converter for an already supported type effectively overrides
the default conversion mechanism for this type.

#### Example

`System.properties`

```properties
RetryDelay = 5
```

`SystemConfig.kt`

```kotlin
@ConfigFile("System.properties")
data class SystemConfig(
    @ConfigProperty("RetryDelay")
    val retryDelay: Long,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath").apply {
        addValueConverter(Long::class.java) { value, _, _ ->
            value.toLong() * 1000
        }
    }
    val systemConfig = factory.createConfig(SystemConfig::class.java)

    println(systemConfig.retryDelay)
}
```

> **Output:** 5000

---

## Configuration Dependencies

It's not uncommon for a configuration value to depend on another configuration value. For example,
the `LogLevel` should be set to `DEBUG` only in developer mode, or fall back to `INFO` otherwise. Usually,
such setup involves `if` statements determining the value at runtime.

KConfig offers another approach - Configuration Dependencies. Declaratively describe the relationships between
dependent properties, and KConfig will resolve the final runtime values for you.

If the value of the dependency matches the required value, the dependent property will be initialized with the
value from the configuration file; otherwise, its `@ConfigProperty.defaultValue` will be used.

### Example

`Developer.properties`

```properties
DeveloperMode = False
LogLevel = DEBUG
```

`DeveloperConfig.kt`

```kotlin
//import java.lang.System.Logger.Level

@ConfigFile("Developer.properties")
data class DeveloperConfig(
    @ConfigProperty("DeveloperMode")
    val developerMode: Boolean,

    @ConfigProperty(name = "LogLevel", defaultValue = "INFO")
    @DependsOn(property = "DeveloperMode", value = "True")
    val logLevel: Level,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath")
    val developerConfig = factory.createConfig(DeveloperConfig::class.java)

    println(developerConfig)
}
```

> **Output:** DeveloperConfig(developerMode=false, logLevel=INFO)

> **Note:** By default, the `value` parameter of `@DependsOn` is set to `True`, emulating a boolean-style comparison,
> so technically the declaration of `@DependsOn.value` in the example above is redundant.

> **Note 2:** A **case-sensitive string comparison** is performed to determine whether
> the value of the dependency matches the required value in `@DependsOn.value`.

> **Note 3:** The dependent value is compared to the dependency's resolved value, which means if the dependency has a
> dependency of its own, that relationship will be checked first to determine
> the final runtime value for the dependency.

---

## Configuration Groups

When multiple configuration values depend on a single configuration value, we can define a configuration group
that switches them all to their default values if the dependency's value doesn't match the requirement.
Back to the developer mode example, it usually does more than just switching the log level (e.g., turns on additional
features useful for debugging).

### Example

`Developer.properties`

```properties
DeveloperMode = False
LogLevel = DEBUG
ShowDebugOverlay = True
BypassLogin = True
RateLimit = 0
```

`DeveloperConfig.kt`

```kotlin
//import java.lang.System.Logger.Level

@ConfigFile("Developer.properties")
data class DeveloperConfig(
    @ConfigProperty("DeveloperMode")
    val developerMode: Boolean,

    @ConfigGroup
    @DependsOn("DeveloperMode")
    val dependentConfig: DependentConfig,
)

data class DependentConfig(
    @ConfigProperty(name = "LogLevel", defaultValue = "INFO")
    val logLevel: Level,

    @ConfigProperty(name = "ShowDebugOverlay", defaultValue = "False")
    val debugOverlay: Boolean,

    @ConfigProperty(name = "BypassLogin", defaultValue = "False")
    val bypassLogin: Boolean,

    @ConfigProperty(name = "RateLimit", defaultValue = "127")
    val rateLimit: Int,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath")
    val developerConfig = factory.createConfig(DeveloperConfig::class.java)

    println(developerConfig)
}
```

> **Output:** DeveloperConfig(developerMode=false, dependentConfig=DependentConfig(logLevel=INFO, debugOverlay=false,
> bypassLogin=false, rateLimit=127))

---

## Configuration Namespaces

It's not uncommon for different configurations to have a common structure. For example, plugins,
services, features, etc. Using KConfig, such configurations can be represented by a single
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

`NetworkConfig.kt`

```kotlin
@ConfigFile("Network.properties")
data class NetworkConfig(
    @ConfigGroup("LoginServer")
    val login: ServerConfig,

    @ConfigGroup("GameServer")
    val game: ServerConfig,
)

data class ServerConfig(
    @ConfigProperty("Host")
    val host: String,

    @ConfigProperty("Port")
    val port: Int,

    @ConfigProperty("AcceptorThreads")
    val acceptorThreads: Int,

    @ConfigProperty("WorkerThreads")
    val workerThreads: Int,

    @ConfigProperty("WaterMarkLow")
    val watermarkLow: Int,

    @ConfigProperty("WaterMarkHigh")
    val watermarkHigh: Int,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath")
    val networkConfig = factory.createConfig(NetworkConfig::class.java)

    println(networkConfig)
}
```

> **Output:**
>
> NetworkConfig(login=ServerConfig(host=127.0.0.1, port=4460, acceptorThreads=1, workerThreads=0,
> watermarkLow=32, watermarkHigh=64), game=ServerConfig(host=192.168.0.1, port=6543, acceptorThreads=1, workerThreads=0,
> watermarkLow=64, watermarkHigh=128))

> **Note:** The namespaces can also define `@DependsOn` and the same rules as for unnamed groups will apply.

> **Note 2:** The namespaces can be as deeply nested as needed, the `@ConfigGroup.namespace` corresponds to
> a single level of nesting (i.e., `@ConfigGroup("Database")` refers to the second level in the namespace
> `LoginServer.Database.URL`)

---

## Configuration Containers

Last but not least, calling `ConfigFactory.createConfig` for every configuration type might become cumbersome
when there are many configuration types. In this case, we can define a *configuration container* that provides
access to all configurations.

We will use a configuration container, which doesn't need any annotations for its definition, to group all previously
defined configuration types in a single POJO. The only difference when using a configuration container is the method
we use to create its instance - `ConfigFactory.createConfigContainer`.

### Example

`ConfigContainer.kt`

```kotlin
data class ConfigContainer(
    val database: DatabaseConfig,
    val network: NetworkConfig,
    val system: SystemConfig,
    val developerConfig: DeveloperConfig,
)
```

`Main.kt`

```kotlin
fun main() {
    val factory = ConfigFactory("../configDirPath")
    val container = factory.createConfigContainer(ConfigContainer::class.java)

    println(container)
}
```

> **Output:** ConfigContainer(database=DatabaseConfig(...), network=NetworkConfig(...), system=SystemConfig(...),
> developerConfig=DeveloperConfig(...))

> **Note:** Instantiation of the configuration types is exactly the same as if they were instantiated one by one using
> `ConfigFactory.createConfig`.

---

## Java Compatibility

KConfig is 100% compatible with Java - replace Kotlin's data classes with Java records, and you're ready to roll.

### Example

The following example is a compact version of all examples above.

```java
public class Example {
    public record ConfigContainer(
        DatabaseConfig database,
        NetworkConfig network,
        SystemConfig system,
        DeveloperConfig developerConfig
    ) {
        @ConfigFile(name = "Database.properties")
        public record DatabaseConfig(
            @ConfigProperty(name = "URL") String url,
            @ConfigProperty(name = "User") String user,
            @ConfigProperty(name = "Password") String password,
            @ConfigProperty(name = "PoolSize") int poolSize,

            // When no such key exists in the properties file, a defaultValue is required
            @ConfigProperty(name = "NonExistent", defaultValue = "False") boolean nonExistent
        ) {
        }

        @ConfigFile(name = "System.properties")
        public record SystemConfig(
            @ConfigProperty(name = "LogTimestampFormat") DateTimeFormatter logTimestampFormat,
            @ConfigProperty(name = "RetryDelay") long retryDelay
        ) {
        }

        @ConfigFile(name = "Network.properties")
        public record NetworkConfig(
            @ConfigGroup(namespace = "LoginServer") ServerConfig login,
            @ConfigGroup(namespace = "GameServer") ServerConfig game
        ) {
            public record ServerConfig(
                @ConfigProperty(name = "Host") String host,
                @ConfigProperty(name = "Port") int port,
                @ConfigProperty(name = "AcceptorThreads") int acceptorThreads,
                @ConfigProperty(name = "WorkerThreads") int workerThreads,
                @ConfigProperty(name = "WaterMarkLow") int watermarkLow,
                @ConfigProperty(name = "WaterMarkHigh") int watermarkHigh
            ) {
            }
        }

        @ConfigFile(name = "Developer.properties")
        public record DeveloperConfig(
            @ConfigProperty(name = "DeveloperMode") boolean developerMode,

            @ConfigGroup
            @DependsOn(property = "DeveloperMode")
            DependentConfig dependentConfig
        ) {
            public record DependentConfig(
                @ConfigProperty(name = "LogLevel", defaultValue = "INFO") System.Logger.Level logLevel,
                @ConfigProperty(name = "ShowDebugOverlay", defaultValue = "False") boolean debugOverlay,
                @ConfigProperty(name = "BypassLogin", defaultValue = "False") boolean bypassLogin,
                @ConfigProperty(name = "RateLimit", defaultValue = "127") int rateLimit
            ) {
            }
        }
    }

    public static void main(String[] args) {
        var factory = new ConfigFactory("../configDirPath");
        factory.addValueConverter(
            DateTimeFormatter.class,
            (value, type, typeArgs) -> DateTimeFormatter.ofPattern(value)
        );
        factory.addValueConverter(long.class, (value, type, typeArgs) -> Long.parseLong(value) * 1000);

        var database = factory.createConfig(ConfigContainer.DatabaseConfig.class);
        System.out.println(database);

        var systemConfig = factory.createConfig(ConfigContainer.SystemConfig.class);
        System.out.println(LocalDateTime.now().format(systemConfig.logTimestampFormat));
        System.out.println(systemConfig.retryDelay);

        var developer =  factory.createConfig(ConfigContainer.DeveloperConfig.class);
        System.out.println(developer);

        var network = factory.createConfig(ConfigContainer.NetworkConfig.class);
        System.out.println(network);

        var container = factory.createConfigContainer(ConfigContainer.class);
        System.out.println(container);
    }
}
```

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
