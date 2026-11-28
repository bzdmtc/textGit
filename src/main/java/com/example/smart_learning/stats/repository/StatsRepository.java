package com.example.smart_learning.stats.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据看板聚合查询。管理员看板要求与订单/用户强一致，因此统一走主库。
 */
@Repository
public class StatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public Object[] orderTotals(LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        return (Object[]) entityManager.createNativeQuery("""
                        SELECT COUNT(*),
                               SUM(CASE WHEN status IN ('PAID','COMPLETED') THEN 1 ELSE 0 END),
                               COALESCE(SUM(CASE WHEN status IN ('PAID','COMPLETED') THEN pay_amount ELSE 0 END), 0),
                               SUM(CASE WHEN status IN ('PAID','COMPLETED')
                                             AND paid_at >= :todayStart AND paid_at < :tomorrowStart
                                        THEN 1 ELSE 0 END),
                               COALESCE(SUM(CASE WHEN status IN ('PAID','COMPLETED')
                                                  AND paid_at >= :todayStart AND paid_at < :tomorrowStart
                                             THEN pay_amount ELSE 0 END), 0)
                        FROM orders
                        """)
                .setParameter("todayStart", todayStart)
                .setParameter("tomorrowStart", tomorrowStart)
                .getSingleResult();
    }

    public Object[] userTotals(LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        return (Object[]) entityManager.createNativeQuery("""
                        SELECT COUNT(*),
                               SUM(CASE WHEN created_at >= :todayStart AND created_at < :tomorrowStart
                                        THEN 1 ELSE 0 END)
                        FROM sys_user
                        """)
                .setParameter("todayStart", todayStart)
                .setParameter("tomorrowStart", tomorrowStart)
                .getSingleResult();
    }

    public Object[] enrollmentTotals(LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        return (Object[]) entityManager.createNativeQuery("""
                        SELECT COUNT(*),
                               SUM(CASE WHEN enrolled_at >= :todayStart AND enrolled_at < :tomorrowStart
                                        THEN 1 ELSE 0 END)
                        FROM user_course
                        WHERE status = 'ACTIVE'
                        """)
                .setParameter("todayStart", todayStart)
                .setParameter("tomorrowStart", tomorrowStart)
                .getSingleResult();
    }

    public long pendingRefunds() {
        Object value = entityManager.createNativeQuery("""
                        SELECT COUNT(*) FROM refund_request WHERE status = 'PENDING'
                        """)
                .getSingleResult();
        return value == null ? 0 : ((Number) value).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> ordersByDay(LocalDateTime start, LocalDateTime end) {
        return entityManager.createNativeQuery("""
                        SELECT CAST(DATEADD(HOUR, 8, paid_at) AS DATE) AS stat_date,
                               COUNT(*) AS order_count,
                               COALESCE(SUM(pay_amount), 0) AS sales_amount
                        FROM orders
                        WHERE status IN ('PAID','COMPLETED')
                          AND paid_at >= :start AND paid_at < :end
                        GROUP BY CAST(DATEADD(HOUR, 8, paid_at) AS DATE)
                        ORDER BY stat_date
                        """)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> usersByDay(LocalDateTime start, LocalDateTime end) {
        return entityManager.createNativeQuery("""
                        SELECT CAST(DATEADD(HOUR, 8, created_at) AS DATE) AS stat_date,
                               COUNT(*) AS user_count
                        FROM sys_user
                        WHERE created_at >= :start AND created_at < :end
                        GROUP BY CAST(DATEADD(HOUR, 8, created_at) AS DATE)
                        ORDER BY stat_date
                        """)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> enrollmentsByDay(LocalDateTime start, LocalDateTime end) {
        return entityManager.createNativeQuery("""
                        SELECT CAST(DATEADD(HOUR, 8, enrolled_at) AS DATE) AS stat_date,
                               COUNT(*) AS enrollment_count
                        FROM user_course
                        WHERE status = 'ACTIVE'
                          AND enrolled_at >= :start AND enrolled_at < :end
                        GROUP BY CAST(DATEADD(HOUR, 8, enrolled_at) AS DATE)
                        ORDER BY stat_date
                        """)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> hotCourses(LocalDateTime start, LocalDateTime end) {
        return entityManager.createNativeQuery("""
                        SELECT TOP 6 oi.course_id,
                               MAX(oi.course_title_snapshot) AS title,
                               MAX(c.subject) AS subject,
                               COUNT(*) AS sales_count,
                               COALESCE(SUM(oi.price_snapshot), 0) AS sales_amount
                        FROM order_item oi
                        JOIN orders o ON o.id = oi.order_id
                        LEFT JOIN course c ON c.id = oi.course_id
                        WHERE o.status IN ('PAID','COMPLETED')
                          AND o.paid_at >= :start AND o.paid_at < :end
                        GROUP BY oi.course_id
                        ORDER BY sales_amount DESC
                        """)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> orderStatusCounts(LocalDateTime start, LocalDateTime end) {
        return entityManager.createNativeQuery("""
                        SELECT status, COUNT(*) AS total
                        FROM orders
                        WHERE created_at >= :start AND created_at < :end
                        GROUP BY status
                        ORDER BY total DESC
                        """)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
    }
}
