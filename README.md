# JXConfig

A lightweight, declarative configuration factory for Java. Provides type-safety, effortless conditional
value resolution, grouping, namespaces, and a flexible value conversion mechanism.

## Requirements

Requires Java 17+.

## Features

- Minimal API surface
- Declarative configuration types - Describe your configurations and let the factory do the work for you
- Extensible value conversion mechanism - Add your own value converter if the defaults are not enough
- Customizable validation - Define constraints for your configuration values and catch violations early
- Configuration dependencies - Get rid of the `if` statements for individual dependent configuration
  values or entire groups
- Namespaces - Reuse configuration types for groups of configurations with identical structures
- Fail-fast loading - Invalid configurations are caught early

## Table of Content

1. [JXConfig Basics](/docs/basics.md)
2. [Type Conversions](/docs/conversions.md)
3. [Value Modifiers](/docs/modifiers.md)
4. [Configuration Namespaces](/docs/namespaces.md)
5. [Configuration Dependencies](/docs/dependencies.md)
6. [Configuration Validators](/docs/validators.md)

## Basic Example

The most basic usage of this factory requires two steps:

1. Define your configuration type
2. Let the `ConfigFactory` instantiate it

`Database.properties`:

```properties
URL = jdbc:mariadb://127.0.0.1:3306/test
User = JXConfig
Password = JXConfig_Password
PoolSize = 7
```

`DatabaseConfig.java`:

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

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var dbConfig = factory.createConfig(DatabaseConfig.class);

    System.out.println(dbConfig);
}
```

**Output:**

```
DatabaseConfig[
    url=jdbc:mariadb://127.0.0.1:3306/test,
    user=JXConfig,
    password=JXConfig_Password,
    poolSize=7,
    nonExistent=false
]
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
