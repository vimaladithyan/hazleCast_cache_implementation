package org.example;




import org.hibernate.Session;
import org.hibernate.Transaction;

public class  CacheDemo {
    public static void main(String[] args) {
        // First session: Load user (cache miss - hits DB)
        Session session1 = HibernateUtil.getSessionFactory().openSession();
        Transaction tx1 = session1.beginTransaction();
        User user1 = session1.get(User.class, 2L);  // SQL query executed
        System.out.println("Session 1 - User from DB: " + user1);
        tx1.commit();
        session1.close();

        System.out.println("--- Closing first session ---");

        // Second session: Load same user (cache hit - no SQL query)
        Session session2 = HibernateUtil.getSessionFactory().openSession();
        Transaction tx2 = session2.beginTransaction();
        User user2 = session2.get(User.class, 2L);  // No SQL - from second-level cache
        System.out.println("Session 2 - User from Cache: " + user2);
        tx2.commit();
        session2.close();

        // Clean up
        HibernateUtil.getSessionFactory().close();
    }
}
