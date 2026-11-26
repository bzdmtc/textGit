package com.example.smart_learning.order.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.coupon.entity.UserCoupon;
import com.example.smart_learning.coupon.repository.CouponRepository;
import com.example.smart_learning.coupon.repository.UserCouponRepository;
import com.example.smart_learning.coupon.service.CouponCalculator;
import com.example.smart_learning.coupon.service.CouponUserService;
import com.example.smart_learning.course.entity.Course;
import com.example.smart_learning.course.repository.CourseRepository;
import com.example.smart_learning.enrollment.repository.UserCourseRepository;
import com.example.smart_learning.order.domain.OrderStateMachine;
import com.example.smart_learning.order.dto.CreateOrderRequest;
import com.example.smart_learning.order.dto.OrderDto;
import com.example.smart_learning.order.dto.OrderItemDto;
import com.example.smart_learning.order.dto.OrderPreviewDto;
import com.example.smart_learning.order.dto.PurchaseStatusDto;
import com.example.smart_learning.order.dto.RefundApplyRequest;
import com.example.smart_learning.order.dto.RefundDto;
import com.example.smart_learning.order.entity.Order;
import com.example.smart_learning.order.entity.OrderItem;
import com.example.smart_learning.order.entity.RefundRequest;
import com.example.smart_learning.order.repository.OrderItemRepository;
import com.example.smart_learning.order.repository.OrderRepository;
import com.example.smart_learning.order.repository.RefundRequestRepository;
import com.example.smart_learning.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Master
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int ORDER_EXPIRE_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponUserService couponUserService;
    private final OrderAssembler orderAssembler;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        RefundRequestRepository refundRequestRepository,
                        CourseRepository courseRepository,
                        UserCourseRepository userCourseRepository,
                        UserCouponRepository userCouponRepository,
                        CouponRepository couponRepository,
                        CouponUserService couponUserService,
                        OrderAssembler orderAssembler) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.refundRequestRepository = refundRequestRepository;
        this.courseRepository = courseRepository;
        this.userCourseRepository = userCourseRepository;
        this.userCouponRepository = userCouponRepository;
        this.couponRepository = couponRepository;
        this.couponUserService = couponUserService;
        this.orderAssembler = orderAssembler;
    }

    @Transactional(readOnly = true)
    public OrderPreviewDto preview(CreateOrderRequest request) {
        Long userId = requireUserId();
        List<Course> courses = loadPurchasableCourses(request.courseIds());
        assertNotEnrolled(userId, courses);
        BigDecimal total = total(courses);
        Quote quote = quote(courses, total, request.userCouponId(), userId, false);
        return new OrderPreviewDto(courseItems(courses), total, quote.discount(), quote.payAmount(),
                quote.userCouponId(), quote.couponName(), quote.couponDescription());
    }

    @Transactional
    public OrderDto create(CreateOrderRequest request) {
        Long userId = requireUserId();
        List<Course> courses = loadPurchasableCourses(request.courseIds());
        assertNotEnrolled(userId, courses);
        assertNoPendingOrder(userId, courses);

        BigDecimal total = total(courses);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Quote quote = quote(courses, total, request.userCouponId(), userId, true);

        Order order = new Order();
        order.setOrderNo(nextOrderNo());
        order.setUserId(userId);
        order.setTotalAmount(total);
        order.setDiscountAmount(quote.discount());
        order.setPayAmount(quote.payAmount());
        order.setUserCouponId(quote.userCouponId());
        order.setOrderSource("NORMAL");
        order.setStatus("PENDING_PAYMENT");
        order.setExpireAt(now.plusMinutes(ORDER_EXPIRE_MINUTES));
        order = orderRepository.save(order);

        for (Course course : courses) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setCourseId(course.getId());
            item.setCourseTitleSnapshot(course.getTitle());
            item.setCoverSnapshot(course.getCoverUrl());
            item.setSubjectSnapshot(course.getSubject());
            item.setPriceSnapshot(course.getPrice());
            orderItemRepository.save(item);
        }
        if (quote.userCoupon() != null) {
            UserCoupon userCoupon = quote.userCoupon();
            userCoupon.setStatus("LOCKED");
            userCoupon.setOrderId(order.getId());
            userCouponRepository.save(userCoupon);
        }
        return orderAssembler.toDto(order);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderDto> myOrders(String status, int page, int size) {
        Long userId = requireUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<Order> result = orderRepository.searchByUser(userId, status, pageable);
        return PageResult.of(orderAssembler.toDtos(result.getContent()),
                result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public OrderDto myOrder(String orderNo) {
        Long userId = requireUserId();
        Order order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return orderAssembler.toDto(order);
    }

    @Transactional
    public OrderDto cancel(String orderNo) {
        Long userId = requireUserId();
        Order order = orderRepository.findForUpdateByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        assertOwner(order, userId);
        if ("CANCELED".equals(order.getStatus())) {
            return orderAssembler.toDto(order);
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_INVALID, "只有待支付订单可以取消");
        }
        OrderStateMachine.requireTransition(order.getStatus(), "CANCELED");
        order.setStatus("CANCELED");
        order.setCanceledAt(LocalDateTime.now(ZoneOffset.UTC));
        order = orderRepository.save(order);
        couponUserService.releaseForOrder(order);
        return orderAssembler.toDto(order);
    }

    @Transactional
    public RefundDto applyRefund(String orderNo, RefundApplyRequest request) {
        Long userId = requireUserId();
        Order order = orderRepository.findForUpdateByOrderNo(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        assertOwner(order, userId);
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.REFUND_NOT_ALLOWED, "只有已支付且未完成的订单可以申请退款");
        }
        if (refundRequestRepository.existsByOrderIdAndStatus(order.getId(), "PENDING")) {
            throw new BusinessException(ErrorCode.REFUND_EXISTS);
        }
        OrderStateMachine.requireTransition(order.getStatus(), "REFUNDING");
        order.setStatus("REFUNDING");
        order = orderRepository.save(order);

        RefundRequest refund = new RefundRequest();
        refund.setOrderId(order.getId());
        refund.setOrderNo(order.getOrderNo());
        refund.setUserId(userId);
        refund.setReason(request.reason().trim());
        refund.setAmount(order.getPayAmount());
        refund.setStatus("PENDING");
        refund = refundRequestRepository.save(refund);
        return RefundDto.of(refund, null, null, order.getStatus());
    }

    @Transactional(readOnly = true)
    public PurchaseStatusDto purchaseStatus(Long courseId) {
        Long userId = requireUserId();
        boolean enrolled = userCourseRepository.existsByUserIdAndCourseId(userId, courseId);
        List<String> pending = orderRepository.findPendingOrderNos(userId, courseId);
        return new PurchaseStatusDto(courseId, enrolled,
                pending.isEmpty() ? null : pending.get(0), null);
    }

    private List<Course> loadPurchasableCourses(List<Long> courseIds) {
        List<Long> distinctIds = courseIds == null ? List.of()
                : courseIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择要购买的课程");
        }
        Map<Long, Course> courseMap = courseRepository.findAllById(distinctIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        List<Course> courses = new ArrayList<>();
        for (Long id : distinctIds) {
            Course course = courseMap.get(id);
            if (course == null || !"PUBLISHED".equals(course.getStatus())) {
                throw new BusinessException(ErrorCode.COURSE_NOT_PURCHASABLE);
            }
            courses.add(course);
        }
        return courses;
    }

    private void assertNotEnrolled(Long userId, List<Course> courses) {
        for (Course course : courses) {
            if (userCourseRepository.existsByUserIdAndCourseId(userId, course.getId())) {
                throw new BusinessException(ErrorCode.ALREADY_ENROLLED,
                        "您已拥有课程「" + course.getTitle() + "」，无需重复购买");
            }
        }
    }

    private void assertNoPendingOrder(Long userId, List<Course> courses) {
        for (Course course : courses) {
            List<String> pending = orderRepository.findPendingOrderNos(userId, course.getId());
            if (!pending.isEmpty()) {
                throw new BusinessException(ErrorCode.PENDING_ORDER_EXISTS,
                        "课程「" + course.getTitle() + "」已有待支付订单 " + pending.get(0) + "，请先支付或取消");
            }
        }
    }

    private Quote quote(List<Course> courses, BigDecimal total, Long userCouponId, Long userId, boolean lock) {
        if (userCouponId == null) {
            return new Quote(null, total, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    total, null, null, null);
        }
        UserCoupon userCoupon = (lock
                ? userCouponRepository.findForUpdateById(userCouponId)
                : userCouponRepository.findById(userCouponId))
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        if (!userId.equals(userCoupon.getUserId())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_FOUND);
        }
        if (!"UNUSED".equals(userCoupon.getStatus())) {
            throw new BusinessException(ErrorCode.COUPON_NOT_USABLE, "该优惠券当前不可使用");
        }
        Coupon coupon = couponRepository.findById(userCoupon.getCouponId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!CouponCalculator.isInValidPeriod(coupon, now)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_USABLE, "该优惠券已过期或尚未生效");
        }
        if (!CouponCalculator.matchesSubject(coupon, courses)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_USABLE, "该优惠券不适用于所选课程");
        }
        if (total.compareTo(coupon.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.COUPON_NOT_USABLE,
                    "订单金额未达到优惠券最低消费 ¥" + coupon.getMinAmount());
        }
        BigDecimal discount = CouponCalculator.discount(coupon, total);
        BigDecimal pay = total.subtract(discount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new Quote(userCoupon.getId(), total, discount, pay, userCoupon, coupon.getName(),
                coupon.getDescription());
    }

    private BigDecimal total(List<Course> courses) {
        return courses.stream()
                .map(course -> course.getPrice() == null ? BigDecimal.ZERO : course.getPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<OrderItemDto> courseItems(List<Course> courses) {
        return courses.stream()
                .map(course -> new OrderItemDto(null, course.getId(), course.getTitle(), course.getCoverUrl(),
                        course.getSubject(), course.getPrice()))
                .toList();
    }

    private String nextOrderNo() {
        String base = "SL" + LocalDateTime.now(ZoneOffset.UTC).format(ORDER_NO_TIME);
        for (int i = 0; i < 5; i++) {
            String candidate = base + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
            if (!orderRepository.existsByOrderNo(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR);
    }

    private Long requireUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    private void assertOwner(Order order, Long userId) {
        if (!userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    private record Quote(Long userCouponId, BigDecimal total, BigDecimal discount, BigDecimal payAmount,
                         UserCoupon userCoupon, String couponName, String couponDescription) {
    }
}
