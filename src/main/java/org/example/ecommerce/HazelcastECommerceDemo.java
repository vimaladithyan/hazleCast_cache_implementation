package org.example.ecommerce;

import com.hazelcast.cache.ICache;
import com.hazelcast.cluster.Member;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Very small e-commerce demo for Hibernate's second-level cache with Hazelcast.
 *
 * <p>It seeds three products only when the table is empty, then loads the same
 * product in two separate Hibernate Sessions. The first load runs SQL; the
 * second is served from Hazelcast. A cached query demonstrates the query cache
 * as well.</p>
 */
public final class
HazelcastECommerceDemo {

    private static final String DEMO_CATEGORY = "Electronics";
    private static final String HAZELCAST_INSTANCE = "ecommerce-hazelcast";

    private HazelcastECommerceDemo() {
    }

    public static void main(String[] args) {
        SessionFactory sessionFactory = HazelcastHibernateUtil.getSessionFactory();

        try {
            System.out.println("============================================================");
            System.out.println(" Simple E-Commerce - Hazelcast Second-Level Cache");
            System.out.println("============================================================");

            Long productId = ensureDemoData(sessionFactory);
            System.out.println("Using product id: " + productId);

            // Start with a cold L2 cache so the first read is a real miss.
            sessionFactory.getCache().evictAll();
            sessionFactory.getStatistics().clear();

            entityCacheDemo(sessionFactory, productId);
            queryCacheDemo(sessionFactory);
            hazelcastView(sessionFactory);
        } finally {
            System.out.println();
            System.out.println("Closing SessionFactory and Hazelcast instance...");
            HazelcastHibernateUtil.shutdown();
            System.out.println("Done.");
        }
    }

    /**
     * Inserts demo products only if the table is empty. Existing data is never
     * changed or deleted.
     */
    private static Long ensureDemoData(SessionFactory sessionFactory) {
        return inTransaction(sessionFactory, session -> {
            List<Long> ids = session
                    .createQuery("select p.id from Product p order by p.id", Long.class)
                    .setMaxResults(1)
                    .getResultList();

            if (!ids.isEmpty()) {
                return ids.get(0);
            }

            Product laptop = new Product("Laptop Pro 14", "Electronics",
                    new BigDecimal("1299.99"), 25);
            Product mouse = new Product("Wireless Mouse", "Electronics",
                    new BigDecimal("29.99"), 100);
            Product book = new Product("Clean Code", "Books",
                    new BigDecimal("39.99"), 40);

            session.persist(laptop);
            session.persist(mouse);
            session.persist(book);

            return laptop.getId();
        });
    }

    /**
     * First read runs SQL and fills Hazelcast. A new Session then reads the same
     * product from the distributed second-level cache.
     */
    private static void entityCacheDemo(SessionFactory sessionFactory, Long productId) {
        System.out.println();
        System.out.println("--- 1. Product cache across two separate sessions ---------");
        System.out.println("Expect one SELECT for Session 1 and no SELECT for Session 2.");

        Product first = inTransaction(sessionFactory,
                session -> session.get(Product.class, productId));
        System.out.println("  Session 1 (database / cache miss): " + first);

        Product second = inTransaction(sessionFactory,
                session -> session.get(Product.class, productId));
        System.out.println("  Session 2 (Hazelcast / cache hit): " + second);

        printStatistics(sessionFactory, "After product cache test");
    }

    /**
     * Query results are cached separately from entities. A scalar projection
     * makes the query-cache hit visible even when the entity is already cached.
     */
    private static void queryCacheDemo(SessionFactory sessionFactory) {
        System.out.println();
        System.out.println("--- 2. Product query cache --------------------------------");
        System.out.println("Expect SQL for the first query and no SQL for the second.");

        List<String> first = runProductQuery(sessionFactory);
        System.out.println("  Session 3 (query cache miss): " + first);

        List<String> second = runProductQuery(sessionFactory);
        System.out.println("  Session 4 (query cache hit) : " + second);

        printStatistics(sessionFactory, "After query cache test");
    }

    private static List<String> runProductQuery(SessionFactory sessionFactory) {
        return inTransaction(sessionFactory, session -> session
                .createQuery("select p.name from Product p " +
                        "where p.category = :category order by p.price", String.class)
                .setParameter("category", DEMO_CATEGORY)
                .setCacheable(true)
                .getResultList());
    }

    private static void hazelcastView(SessionFactory sessionFactory) {
        System.out.println();
        System.out.println("--- 3. Hazelcast view -------------------------------------");
        printStatistics(sessionFactory, "Final Hibernate counters");
        printHazelcastInfo();
    }

    private static void printStatistics(SessionFactory sessionFactory, String label) {
        Statistics stats = sessionFactory.getStatistics();

        System.out.println();
        System.out.println("  [" + label + "]");
        System.out.printf("    L2 cache    : hits=%d, misses=%d, puts=%d%n",
                stats.getSecondLevelCacheHitCount(),
                stats.getSecondLevelCacheMissCount(),
                stats.getSecondLevelCachePutCount());
        System.out.printf("    Query cache : hits=%d, misses=%d%n",
                stats.getQueryCacheHitCount(),
                stats.getQueryCacheMissCount());
        System.out.printf("    SQL queries : %d%n", stats.getQueryExecutionCount());
    }

    private static void printHazelcastInfo() {
        HazelcastInstance hazelcast = findHazelcastInstance();
        if (hazelcast == null) {
            System.out.println("  Hazelcast instance was not found.");
            return;
        }

        System.out.println();
        System.out.println("  [Hazelcast cluster]");
        System.out.println("    Cluster name : " + hazelcast.getConfig().getClusterName());
        System.out.println("    Instance     : " + hazelcast.getName());
        System.out.println("    Members      : " + hazelcast.getCluster().getMembers().size());

        for (Member member : hazelcast.getCluster().getMembers()) {
            System.out.println("      - " + member.getAddress());
        }

        List<ICache<?, ?>> caches = new ArrayList<>();
        for (DistributedObject object : hazelcast.getDistributedObjects()) {
            if (object instanceof ICache<?, ?> cache) {
                caches.add(cache);
            }
        }
        caches.sort(Comparator.comparing(ICache::getName));

        System.out.println();
        System.out.println("  [Hazelcast JCache regions]");
        for (ICache<?, ?> cache : caches) {
            System.out.printf("    %-45s entries=%d%n", cache.getName(), cache.size());
        }
    }

    private static HazelcastInstance findHazelcastInstance() {
        HazelcastInstance named = Hazelcast.getHazelcastInstanceByName(HAZELCAST_INSTANCE);
        if (named != null) {
            return named;
        }

        for (HazelcastInstance instance : Hazelcast.getAllHazelcastInstances()) {
            return instance;
        }
        return null;
    }

    private static <T> T inTransaction(SessionFactory sessionFactory, Function<Session, T> action) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                T result = action.apply(session);
                transaction.commit();
                return result;
            } catch (RuntimeException ex) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw ex;
            }
        }
    }
}
