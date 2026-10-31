# Hazle Package - E-Commerce Hazelcast Cache

The `secondlevel_cache` project now contains a dedicated package named
`org.example.hazle` (package folder: `hazle`). It mirrors the existing
e-commerce Hazelcast demo with slightly modified class names.

## Classes

| Class | Purpose |
|---|---|
| `org.example.hazle.HazleProduct` | Product entity cached in `hazleProductCache` |
| `org.example.hazle.HazleHibernateUtil` | Builds the Hazelcast `SessionFactory` |
| `org.example.hazle.HazleCacheDemo` | Runs the entity-cache and query-cache demo |

## Configuration

- `src/main/resources/hibernate-hazle.cfg.xml` maps `HazleProduct`
- `src/main/resources/hazelcast.xml` contains the `hazleProductCache` region
- Database table: `hazle_product`
- Cache region: `hazleProductCache`
- Cache strategy: `READ_WRITE`

The demo uses Hibernate's JCache bridge with Hazelcast:

```xml
<property name="hibernate.cache.region.factory_class">
    org.hibernate.cache.jcache.JCacheRegionFactory
</property>
<property name="hibernate.javax.cache.provider">
    com.hazelcast.cache.impl.HazelcastServerCachingProvider
</property>
```

## What the demo does

1. Creates one `HazleProduct` row only if `hazle_product` is empty.
2. Opens one Hibernate session.
3. Loads that product once.
4. Runs one cached query once.
5. Prints Hibernate statistics and the Hazelcast JCache regions.

Each operation runs exactly once; there are no repeated reads or repeated
queries.

## Run

Use JDK 17:

```powershell
$env:JAVA_HOME = 'C:\Users\vvima\.jdks\ms-17.0.21.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$mvn = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $mvn clean compile exec:java "-Dexec.mainClass=org.example.hazle.HazleCacheDemo"
```

The default `mvn exec:java` still runs the earlier
`org.example.ecommerce.HazelcastECommerceDemo`.

## Expected result

The single product load shows one SQL statement, and the single query shows one
SQL statement. Hibernate statistics show the L2 cache miss/put and the query
cache miss from that single run. The Hazelcast region list confirms that the
product and query result were stored.

The Hazelcast output should include:

```text
hazleProductCache
default-query-results-region
default-update-timestamps-region
```
