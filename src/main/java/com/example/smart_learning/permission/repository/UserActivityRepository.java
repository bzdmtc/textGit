package com.example.smart_learning.permission.repository;

import com.example.smart_learning.permission.dto.UserActivityDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class UserActivityRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public UserActivityDto activity(Long userId) {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
                        SELECT
                            (SELECT COUNT(*) FROM orders o WHERE o.user_id = :userId),
                            (SELECT COUNT(*) FROM orders o
                              WHERE o.user_id = :userId AND o.status IN ('PAID','COMPLETED')),
                            COALESCE((SELECT SUM(o.pay_amount) FROM orders o
                                       WHERE o.user_id = :userId AND o.status IN ('PAID','COMPLETED')), 0),
                            (SELECT COUNT(*) FROM user_course uc WHERE uc.user_id = :userId),
                            (SELECT COUNT(*) FROM exam_record er WHERE er.user_id = :userId),
                            (SELECT COUNT(*) FROM wrong_question wq
                              WHERE wq.user_id = :userId AND wq.mastered = 0)
                        """)
                .setParameter("userId", userId)
                .getSingleResult();
        return new UserActivityDto(userId,
                asLong(row[0]), asLong(row[1]), asDecimal(row[2]),
                asLong(row[3]), asLong(row[4]), asLong(row[5]));
    }

    private long asLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
