package org.example.hazle;

import com.hazelcast.core.Hazelcast;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * SessionFactory for the {@code hazle} package demo.
 *
 * <p>It loads the Hazelcast-specific Hibernate configuration and is separate
 * from the original Ehcache utility and the e-commerce utility.</p>
 */
public final class HazleHibernateUtil {

    private static final SessionFactory SESSION_FACTORY = buildSessionFactory();

    private HazleHibernateUtil() {
    }

    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration()
                    .configure("hibernate-hazle.cfg.xml")
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
