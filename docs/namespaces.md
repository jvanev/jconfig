# Configuration Namespaces

A configuration namespace is a group of configurations with a specified name.
It allows for configurations with identical structures, but in different namespaces,
to be represented by a single, reusable type.

For example, we can have multiple database configurations:

`Database.properties`:

```properties
Production.Host = jdbc:mariadb://127.0.0.1:3306/jxconfig
Production.Port = 3306
Production.Pool.Size = 5
Production.Pool.Timeout = 10000
Production.Pool.LeakDetectionThreshold = 0
Production.Pool.Just.A.Name = Production Database Pool

Development.Host = jdbc:mariadb://127.0.0.1:3306/test
Development.Port = 3306
Development.Pool.Size = 5
Development.Pool.Timeout = 10000
Development.Pool.LeakDetectionThreshold = 10000
Development.Pool.Just.A.Name = Development Database Pool
```

`DatabaseConfiguration.java`:

```java
@ConfigFile(filename = "Database.properties")
public record DatabaseConfiguration(
    @ConfigNamespace("Production")
    DatabaseConnection production,

    @ConfigNamespace("Development")
    DatabaseConnection development
) {
    public record DatabaseConnection(
        @ConfigProperty(key = "Host")
        String host,

        @ConfigProperty(key = "Port")
        int port,

        @ConfigNamespace("Pool")
        PoolConfiguration pool
    ) {}

    public record PoolConfiguration(
        @ConfigProperty(key = "Size")
        int size,

        @ConfigProperty(key = "Timeout")
        long timeout,

        @ConfigProperty(key = "LeakDetectionThreshold")
        long leakDetectionThreshold,

        @ConfigProperty(key = "Just.A.Name")
        String poolName
    ) {}
}
```

**Output:**

```
DatabaseConfiguration[
    production=DatabaseConnection[
        host=jdbc:mariadb://127.0.0.1:3306/jxconfig,
        port=3306,
        pool=PoolConfiguration[
            size=5,
            timeout=10000,
            leakDetectionThreshold=0,
            poolName=Production Database Pool
        ]
    ],
    development=DatabaseConnection[
        host=jdbc:mariadb://127.0.0.1:3306/test,
        port=3306,
        pool=PoolConfiguration[
            size=5,
            timeout=10000,
            leakDetectionThreshold=10000,
            poolName=Development Database Pool
        ]
    ]
]
```

Notice that property keys and nested namespaces are **relative** to their parent namespace.
For example, inside the `DatabaseConnection` record, the pool's namespace is just `Pool`,
not `Production.Pool`. Likewise, in `PoolConfiguration`, the key for the size is
just `Size`, not `Production.Pool.Size`.

The last declaration, `PoolConfiguration.poolName`, demonstrates that a namespace is not
defined merely by dot-separation. In order for dot-separated components of a key to be considered
as components of a namespace, the components must be declared explicitly using `@ConfigNamespace.value`.
