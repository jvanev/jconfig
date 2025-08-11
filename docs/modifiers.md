# Value Modifiers

Value modifiers allow you to apply transformations to a configuration value **after** it has been
converted to the appropriate Java type, but **before** it is passed to the *configuration type*'s
constructor for initialization. This is useful for tasks like forcing a string to lowercase,
sorting a list, or clamping a numeric value within a specific range.

You can apply one or more modifiers to a configuration parameter using the repeatable `@Modifier`
annotation. Each annotation points to a class that implements the `ValueModifier` interface,
which contains your custom transformation logic.

## Implementation

The `ValueModifier` interface has a single method, `modify`, that needs to be implemented.
It accepts a converted configuration value and is expected to return its transformed version.

For better performance, **JXConfig** instantiates each modifier class only once upon its
first use. This single instance is then cached and reused for every configuration property
that applies it.

Because of this caching, it is strongly recommended that all `ValueModifier`
implementations be stateless to avoid unpredictable side effects.

## Usage

Let's create a *value modifier* that converts a timeout value from seconds to milliseconds.

**1. Implement the** `ValueModifier`

The modifier's logic takes the converted `long` value and multiplies it by 1000.

`ToMilliseconds.java`:

```java
public class ToMilliseconds implements ValueModifier {
    @Override
    public Object modify(Object value) {
        return ((long) value) * 1000;
    }
}
```

**2. Define the Configuration**

In the properties file, the value is defined in seconds. The `@Modifier` annotation is applied to
the record component to transform the value.

`Network.properties`:

```properties
Timeout = 15
```

`NetworkConfiguration.java`:

```java
@ConfigFile(filename = "Network.properties")
public record NetworkConfiguration(
    @ConfigProperty(key = "Timeout")
    @Modifier(ToMilliseconds.class)
    long timeout
) {}
```

**3. See the Result**

When the configuration object is created, the modifier is automatically applied, and the
`timeout` method returns the value in milliseconds.

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder().build();
    var config = factory.createConfig(NetworkConfiguration.class);

    System.out.printf("Timeout in milliseconds: %d%n", config.timeout());
}
```

**Output:**

```
Timeout in milliseconds: 15000
```
