# Value Conversions

**JXConfig** provides `String` conversions for common Java types by default. These converters are used to convert the
resolved string values to the types expected by the constructor parameters of the *configuration types*.

The following is a list of built-in conversions:

- **Primitive types** - `byte`, `short`, `int`, `long`, `float`, `double`, `boolean`, `char`
- **Boxed primitive types** - `Byte`, `Short`, `Integer`, `Long`, `Float`, `Double`, `Boolean`, `Character`
- **Primitive arrays** - `byte[]`, `short[]`, `int[]`, `long[]`, `float[]`, `double[]`, `boolean[]`, `char[]`
- **Boxed primitive arrays** - `Byte[]`, `Short[]`, `Integer[]`, `Long[]`, `Float[]`, `Double[]`, `Boolean[]`,
  `Character[]`
- **Enumerations** - Any `Enum` type, by matching the string value to an enum entry's name
- **Reference Types with `valueOf(String)`:** Any class that provides a public, static `valueOf(String)` method
  The return type of this method must be assignable to the target type
- **Collections:** `List` and `Set`. Elements within the collection are also converted recursively based on
  their generic type argument (e.g., `List<Integer>` will convert string "1,2,3" into a list of integers)
- **Maps:** For `Map<K, V>`, both keys and values are converted recursively based on their generic type arguments
  (e.g., `Map<String, Integer>` will convert "key1=1,key2=2" into a map of string keys and integer values)

When the type of the configuration parameter is:

- `List` - An `ArrayList` will be used for its initialization
- `Set` - A `LinkedHashSet` will be used for its initialization
- `Map` - A `LinkedHashMap` will be used for its initialization

> **Note:** The values for Arrays, Lists, and Sets are parsed by splitting the string by a comma (`,`).
> Maps are parsed by first splitting entries by comma and then splitting each key-value pair by an
> equals sign (`=`). Whitespace around delimiters is automatically trimmed.

## Custom Converters

If the built-in set of converters doesn't cover your needs, you can register custom converters to add support for
additional types.

Custom converters are registered once, during the `ConfigFactory` instantiation, using the builder's
`withValueConverter` method.

### Example

We will use `DateTimeFormatter` as an example of how to add support for an unsupported parameter type.

`System.properties`:

```properties
LogTimestampFormat = yyyy-MM-dd HH:mm:ss.SSS
```

`SystemConfig.java`:

```java
@ConfigFile(filename = "System.properties")
public record SystemConfig(
    @ConfigProperty(key = "LogTimestampFormat")
    DateTimeFormatter logTimestampFormatter
) {}
```

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder()
        .withValueConverter(
            DateTimeFormatter.class,
            (converter, type, typeArgs, value) -> DateTimeFormatter.ofPattern(value)
        )
        .build();
    var systemConfig = factory.createConfig(SystemConfig.class);
    String dateTime = LocalDateTime.parse("2025-08-03T10:15:30").format(systemConfig.logTimestampFormatter());

    System.out.printf("Time: %s%n", dateTime);
}
```

**Output:**

```
Time: 2025-08-03 10:15:30.000
```

## Custom Converters and Overriding Behavior

Registering a custom converter using `withValueConverter` has an important side effect - Custom value
converters take precedence over the default conversion mechanism for direct target matches.
While adding support for additional types doesn't change the default behavior, adding a custom
converter for an already supported type effectively overrides the default conversion mechanism for this type.

If no converter matches the target type directly, the custom converters will be checked in insertion order;
the first one that can produce an assignable value will be used.

### Example

`System.properties`:

```properties
RetryDelay = 5
```

`SystemConfig.java`:

```java
@ConfigFile(filename = "System.properties")
public record SystemConfig(
    @ConfigProperty(key = "RetryDelay") long retryDelay
) {}
```

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder()
        .withValueConverter(
            long.class,
            (converter, type, typeArgs, value) -> Long.parseLong(value) * 1000
        )
        .build();
    var systemConfig = factory.createConfig(SystemConfig.class);

    System.out.printf("Retry delay: %dms%n", systemConfig.retryDelay());
}
```

**Output:**

```
Retry delay: 5000ms
```
