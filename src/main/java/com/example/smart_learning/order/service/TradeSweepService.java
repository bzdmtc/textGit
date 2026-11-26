package com.example.smart_learning.order.service;

import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.coupon.entity.UserCoupon;
import com.example.smart_learning.coupon.repository.UserCouponRepository;
import com.example.smart_learning.coupon.service.CouponUserService;
import com.example.smart_learning.order.domain.OrderStateMachine;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * 交易定时清扫逻辑放在独立事务服务中：调度器调用本 Bean，
 * 确保 @Master / @Transactional 经过代理生效，扫描与写入都走主库。
 */
@Service
@Master
public class TradeSweepService {

    private static final Logger log = LoggerFactory.getLogger(TradeSweepService.class);

    private final OrderRepository orderRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponUserService couponUserService;

    public TradeSweepService(OrderRepository orderRepository,
                             UserCouponRepository userCouponRepository,
                             CouponUserService couponUserService) {
        this.orderRepository = orderRepository;
        this.userCouponRepository = userCouponRepository;
        this.couponUserService = couponUserService;
    }

    @Transactional
    public void sweep() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        int canceled = cancelExpiredOrders(now);
        int expiredCoupons = expireCoupons(now);
        int completed = completeOldOrders(now);
        if (canceled + expiredCoupons + completed > 0) {
            log.info("Trade sweep: canceledOrders={}, expiredCoupons={}, completedOrders={}",
                    canceled, expiredCoupons, completed);
        }
    }

    private int cancelExpiredOrders(LocalDateTime now) {
        List<Order> orders = orderRepository.findByStatusAndExpireAtBefore("PENDING_PAYMENT", now);
        int count = 0;
        for (Order order : orders) {
            OrderStateMachine.requireTransition(order.getStatus(), "CANCELED");
            order.setStatus("CANCELED");
            order.setCanceledAt(now);
            orderRepository.save(order);
            couponUserService.releaseForOrder(order);
            count++;
        }
        return count;
    }

    private int expireCoupons(LocalDateTime now) {
        List<UserCoupon> coupons = userCouponRepository.findExpiredUnused(now);
        for (UserCoupon coupon : coupons) {
            coupon.setStatus("EXPIRED");
            userCouponRepository.save(coupon);
        }
        return coupons.size();
    }

    private int completeOldOrders(LocalDateTime now) {
        List<Order> orders = orderRepository.findByStatusAndPaidAtBefore("PAID", now.minusDays(7));
        for (Order order : orders) {
            OrderStateMachine.requireTransition(order.getStatus(), "COMPLETED");
            order.setStatus("COMPLETED");
            order.setCompletedAt(now);
            orderRepository.save(order);
        }
        return orders.size();
    }
}
