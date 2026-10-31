package org.example.hazle;

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
 * Single-record e-commerce Hazelcast cache demo for the {@code hazle} package.
 *
 * <p>The demo creates one product if the table is empty, then runs one session
 * that loads that product once and executes one cached query once. There are no
 * repeated reads or repeated queries.</p>
 */
public final class HazleCacheDemo {

    private static final String HAZELCAST_INSTANCE = "ecommerce-hazelcast";

    private HazleCacheDemo() {
    }

    public static void main(String[] args) {
        SessionFactory sessionFactory = HazleHibernateUtil.getSessionFactory();

        try {
            System.out.println("============================================================");
            System.out.println(" Hazle Package - Single Product Hazelcast Cache");
            System.out.println("============================================================");

            Long productId = ensureSingleProduct(sessionFactory);
            System.out.println("Using hazle product id: " + productId);

            // Start with a cold L2 cache and clean statistics.
            sessionFactory.getCache().evictAll();
            sessionFactory.getStatistics().clear();

            runSingleProcess(sessionFactory, productId);
            hazelcastView(sessionFactory);
        } finally {
            System.out.println();
            System.out.println("Closing SessionFactory and Hazelcast instance...");
            HazleHibernateUtil.shutdown();
            System.out.println("Done.");
        }
    }

    /**
     * Creates exactly one demo product when the table is empty. Existing data
     * is never changed or deleted.
     */
    private static Long ensureSingleProduct(SessionFactory sessionFactory) {
        return inTransaction(sessionFactory, session -> {
            List<Long> ids = session
                    .createQuery("select p.id from HazleProduct p order by p.id", Long.class)
                    .setMaxResults(1)
                    .getResultList();

            if (!ids.isEmpty()) {
                return ids.get(0);
            }

            HazleProduct product = new HazleProduct(
                    "Hazle Laptop 14",
                    "Electronics",
                    new BigDecimal("1199.99"),
                    20);

            session.persist(product);
            return product.getId();
        });
    }

    /**
     * One session performs one product load and one cached query. No operation
     * is repeated, so the demo runs each process exactly once.
     */
    private static void runSingleProcess(SessionFactory sessionFactory, Long productId) {
        System.out.println();
        System.out.println("--- Single session: one product load and one query ----------");

        inTransaction(sessionFactory, session -> {
            HazleProduct product = session.get(HazleProduct.class, productId);
            System.out.println("  Product loaded from database: " + product);

            List<String> productNames = session
                    .createQuery("select p.name from HazleProduct p " +
                            "where p.category = :category order by p.price", String.class)
                    .setParameter("category", product.getCategory())
                    .setCacheable(true)
                    .getResultList();

            System.out.println("  Cached query result: " + productNames);
            return null;
        });

        printStatistics(sessionFactory, "After the single run");
    }

    private static void hazelcastView(SessionFactory sessionFactory) {
        System.out.println();
        System.out.println("--- Hazelcast view ----------------------------------------");
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
