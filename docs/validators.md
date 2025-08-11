# Configuration Validators

Type-safety alone is not always enough. While **JXConfig** ensures a property is converted
to the correct Java type (like an `int`), it can't guarantee the value is
**semantically correct**. For example, a port number could be `-1`, or a username string
could be blank.

To solve this, **JXConfig** provides a validation step that runs **after** your
configuration object has been fully created, allowing you to check its final state.

You have complete control over the implementation. You can write your own custom validation
logic or integrate a powerful, established framework.

## Registering a Validator

Validation is an optional step that is skipped by default. To enable it, you must register
your validator instance with the `ConfigFactory` builder using the
`withConfigurationValidator` method:

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder()
        .withConfigurationValidator(new MyCustomValidator())
        .build();
}
```

## The `ConfigurationValidator` Interface

The `ConfigurationValidator` is a functional interface that you can implement to create
your validation logic.

Its single method, `validate(Object)`, is called for every configuration object immediately
after it is created by the factory. This allows you to inspect the final, fully constructed
object and throw an exception if its state is invalid.

### Example

The following example demonstrates how you can implement a custom, annotation-based
validator for your needs.

**1. Create the annotations:**

`Range.java`:

```java
@Retention(RetentionPolicy.RUNTIME)
public @interface Range {
    int min();
    int max();
}
```

`NotBlank.java`:

```java
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlank {
}
```

**2. Create the validator:**

`SimpleValidator.java`:

```java
public class SimpleValidator implements ConfigurationValidator {
    @Override
    public void validate(Object config) {
        var components = config.getClass().getRecordComponents();

        for (var component : components) {
            for (var annotation : component.getDeclaredAnnotations()) {
                if (annotation.annotationType() == Range.class) {
                    try {
                        var intVal = (int) component.getAccessor().invoke(config);
                        var range = (Range) annotation;

                        if (intVal < range.min() || intVal > range.max()) {
                            throw new IllegalArgumentException(
                                "Range constraint violation for '" + component.getName() + "': The value must be between " +
                                    range.min() + " and " + range.max() + ", found " + intVal
                            );
                        }
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (annotation.annotationType() == NotBlank.class) {
                    try {
                        var strVal = (String) component.getAccessor().invoke(config);

                        if (strVal == null || strVal.isBlank()) {
                            throw new IllegalArgumentException(
                                "NotBlank constraint violation for '" + component.getName() + "'"
                            );
                        }
                    } catch (ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }
}
```

**3. Apply the constraints:**

`ExampleConfiguration.java`:

```java
@ConfigFile(filename = "Example.properties")
public record ExampleConfiguration(
    @ConfigProperty(key = "IntProperty")
    @Range(min = 0, max = 128)
    int intProperty,

    @ConfigProperty(key = "StrProperty")
    @NotBlank
    String strProperty
) {}
```

**4. Register the validator:**

`Main.java`:

```java
public static void main(String[] args) {
    var factory = ConfigFactory.builder()
        .withConfigurationValidator(new SimpleValidator())
        .build();

    factory.createConfig(ExampleConfiguration.class);
}
```

If the configuration file contains an invalid value, an exception will be thrown
when creating the configuration instance:

`Example.properties`:

```properties
IntProperty = 256
StrProperty = test
```

**Output:**

```
Exception in thread "main" com.jvanev.jxconfig.exception.ConfigurationBuildException: Failed to create an instance of configuration type ExampleConfiguration
	... stack trace
Caused by: java.lang.IllegalArgumentException: Range constraint violation: The value must be between 0 and 128, found 256
	... stack trace
```
