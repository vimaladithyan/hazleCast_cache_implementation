# Hazelcast E-Commerce Second-Level Cache

This project now contains two second-level cache examples:

| Example | Package | Cache provider | Entry point |
|---|---|---|---|
| Original reference | `org.example` | Ehcache | `org.example.CacheDemo` |
| E-commerce demo | `org.example.ecommerce` | Hazelcast | `org.example.ecommerce.HazelcastECommerceDemo` |
| Hazle package demo | `org.example.hazle` | Hazelcast | `org.example.hazle.HazleCacheDemo` |

The original Ehcache files and configuration were left unchanged.

## E-commerce example

A very small product catalog with four attributes: product name, category,
price, and stock. The demo:

1. Seeds three products only if `ecom_product` is empty.
2. Loads the same product in two separate Hibernate `Session`s.
3. Shows that the first load runs SQL and the second is served by Hazelcast.
4. Runs the same cached query twice to show the query cache.
5. Prints Hibernate cache statistics and the Hazelcast JCache regions.

## New files

```text
src/main/java/org/example/ecommerce/
├── Product.java
├── HazelcastHibernateUtil.java
└── HazelcastECommerceDemo.java

src/main/resources/
├── hibernate-hazelcast.cfg.xml
└── hazelcast.xml
```

## Dependencies added

- `org.hibernate:hibernate-jcache:5.6.15.Final`
- `com.hazelcast:hazelcast:5.4.0`
- `javax.cache:cache-api:1.1.1`
- `org.slf4j:slf4j-simple:1.7.36`

Hibernate 5.6 uses its standard JCache bridge, configured as:

```xml
<property name="hibernate.cache.region.factory_class">
    org.hibernate.cache.jcache.JCacheRegionFactory
</property>
<property name="hibernate.javax.cache.provider">
    com.hazelcast.cache.impl.HazelcastServerCachingProvider
</property>
```

## Database

The Hazelcast configuration uses the same MySQL database as the reference:

```text
URL      jdbc:mysql://localhost:3306/hibernate
Username root
Password Mass
```

Change those values in `src/main/resources/hibernate-hazelcast.cfg.xml` if
needed. The demo uses its own `ecom_product` table, so it does not collide with
the reference `user` table or other tables in the schema.

## Run the Hazelcast demo

Use JDK 17 rather than the machine's default Java 26:

```powershell
$env:JAVA_HOME = 'C:\Users\vvima\.jdks\ms-17.0.21.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$mvn = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd'
& $mvn clean compile exec:java
```

`exec:java` is configured to run `HazelcastECommerceDemo` by default.

## Run the original Ehcache demo

```powershell
& $mvn exec:java "-Dexec.mainClass=org.example.CacheDemo"
```

The original demo still uses `hibernate.cfg.xml` and `ehcache.xml`.

## Expected output

The first product load prints a `select ... from ecom_product ...` statement.
The second load prints no SQL. The query cache test runs SQL once and then
reuses the cached result. Statistics should show second-level cache and query
cache hits.

At the end, the Hazelcast section lists the JCache regions, for example:

```text
[Hazelcast JCache regions]
  default-query-results-region                  entries=1
  default-update-timestamps-region              entries=0
  productCache                                  entries=1
```

## Cache regions

| Region | Purpose | Strategy |
|---|---|---|
| `productCache` | `Product` entity | `READ_WRITE` |
| `default-query-results-region` | Cached query results | Hibernate query cache |
| `default-update-timestamps-region` | Query-cache timestamps | Hibernate query cache |

## Notes

- `Product` implements `Serializable` because Hazelcast stores cache values in
  serialized form.
- The demo is intentionally small: one entity, one cached query, and one
  Hazelcast member.
- TCP/IP join is enabled on `127.0.0.1:5701`, so a second application instance
  can start on `5702` and join the same cluster.
- Direct SQL updates bypass Hibernate and do not invalidate the Hazelcast
  cache. Use Hibernate for writes.
