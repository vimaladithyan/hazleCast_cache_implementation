package org.example.ecommerce;

import com.hazelcast.core.Hazelcast;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Builds a SessionFactory from the Hazelcast-specific Hibernate configuration.
 *
 * <p>This is separate from {@code org.example.HibernateUtil} so the original
 * Ehcache example in this project keeps working unchanged.</p>
 */
public final class HazelcastHibernateUtil {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HazelcastHibernateUtil() {
    }

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration()
                    .configure("hibernate-hazelcast.cfg.xml")
                    .buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Initial SessionFactory creation failed: " + ex.getMessage());
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return SESSION_FACTORY;
    }

    public static void shutdown() {
        if (SESSION_FACTORY != null && !SESSION_FACTORY.isClosed()) {
            SESSION_FACTORY.close();
        }
        // Safety net: close any embedded Hazelcast member still running.
        Hazelcast.shutdownAll();
    }
}
