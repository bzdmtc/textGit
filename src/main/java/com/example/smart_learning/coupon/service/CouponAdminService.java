package com.example.smart_learning.coupon.service;

import com.example.smart_learning.common.api.PageResult;
import com.example.smart_learning.common.datasource.Master;
import com.example.smart_learning.common.error.BusinessException;
import com.example.smart_learning.common.error.ErrorCode;
import com.example.smart_learning.coupon.dto.CouponDto;
import com.example.smart_learning.coupon.dto.CouponRequest;
import com.example.smart_learning.coupon.entity.Coupon;
import com.example.smart_learning.coupon.repository.CouponRepository;
import com.example.smart_learning.permission.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Master
public class CouponAdminService {

    private final CouponRepository couponRepository;
    private final AuditService auditService;

    public CouponAdminService(CouponRepository couponRepository, AuditService auditService) {
        this.couponRepository = couponRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResult<CouponDto> page(String keyword, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), 100));
        Page<Coupon> result = couponRepository.search(keyword, status, pageable);
        return PageResult.of(result.getContent().stream().map(CouponDto::of).toList(),
                result.getTotalElements(), page, size);
    }

    @Transactional
    public CouponDto create(CouponRequest request) {
        Coupon coupon = new Coupon();
        coupon.setRemainingQuantity(request.totalQuantity());
        apply(coupon, request);
        coupon = couponRepository.save(coupon);
        auditService.record("COUPON_CREATE", "COUPON", coupon.getId(), null, CouponDto.of(coupon));
        return CouponDto.of(coupon);
    }

    @Transactional
    public CouponDto update(Long id, CouponRequest request) {
        Coupon coupon = requireCoupon(id);
        validate(request);
        int claimed = coupon.getTotalQuantity() - coupon.getRemainingQuantity();
        if (request.totalQuantity() < claimed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "已领取 " + claimed + " 张，发放数量不能小于已领取数量");
        }
        CouponDto before = CouponDto.of(coupon);
        apply(coupon, request);
        coupon.setRemainingQuantity(request.totalQuantity() - claimed);
        coupon = couponRepository.save(coupon);
        auditService.record("COUPON_UPDATE", "COUPON", id, before, CouponDto.of(coupon));
        return CouponDto.of(coupon);
    }

    @Transactional
    public CouponDto changeStatus(Long id, String status) {
        if (!"ON".equals(status) && !"OFF".equals(status)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 ON/OFF");
        }
        Coupon coupon = requireCoupon(id);
        CouponDto before = CouponDto.of(coupon);
        coupon.setStatus(status);
        coupon = couponRepository.save(coupon);
        auditService.record("COUPON_STATUS", "COUPON", id, before, CouponDto.of(coupon));
        return CouponDto.of(coupon);
    }

    private void apply(Coupon coupon, CouponRequest request) {
        validate(request);
        coupon.setName(request.name().trim());
        coupon.setType(request.type());
        coupon.setValueAmount(request.value());
        coupon.setMinAmount(request.minAmount() == null ? BigDecimal.ZERO : request.minAmount());
        coupon.setTotalQuantity(request.totalQuantity());
        coupon.setPerUserLimit(request.perUserLimit() == null ? 1 : request.perUserLimit());
        coupon.setStartAt(request.startAt());
        coupon.setEndAt(request.endAt());
        coupon.setSubject(request.subject() == null || request.subject().isBlank() ? null : request.subject().trim());
        coupon.setDescription(request.description());
        coupon.setStatus(request.status() == null || request.status().isBlank() ? "ON" : request.status());
    }

    private void validate(CouponRequest request) {
        if (!"FIXED".equals(request.type()) && !"PERCENT".equals(request.type())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "优惠券类型只能是 FIXED/PERCENT");
        }
        if ("PERCENT".equals(request.type())
                && request.value().compareTo(BigDecimal.valueOf(100)) >= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "折扣百分比必须小于 100（90 表示 9 折）");
        }
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "开始时间必须早于结束时间");
        }
        if (request.status() != null && !request.status().isBlank()
                && !"ON".equals(request.status()) && !"OFF".equals(request.status())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "状态只能是 ON/OFF");
        }
    }

    private Coupon requireCoupon(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }
}
