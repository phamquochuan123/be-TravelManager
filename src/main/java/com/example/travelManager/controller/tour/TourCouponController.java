package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.request.tour.TourCouponRequest;
import com.example.travelManager.domain.response.tour.TourCouponResponse;
import com.example.travelManager.domain.tour.TourCoupon;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourCouponRepository;
import com.example.travelManager.util.constant.tour.CouponType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tour-coupons")
@RequiredArgsConstructor
public class TourCouponController {

    private final TourCouponRepository couponRepository;

    @GetMapping
    public ResponseEntity<List<TourCouponResponse>> getAll() {
        return ResponseEntity.ok(
                couponRepository.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TourCouponResponse> getById(@PathVariable("id") Long id) {
        TourCoupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
        return ResponseEntity.ok(toResponse(coupon));
    }

    @PostMapping
    public ResponseEntity<TourCouponResponse> create(@Valid @RequestBody TourCouponRequest request) {
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Mã coupon '" + request.getCode() + "' đã tồn tại");
        }
        TourCoupon coupon = fromRequest(new TourCoupon(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(couponRepository.save(coupon)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TourCouponResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TourCouponRequest request) {
        TourCoupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
        // Cho phép đổi code miễn là không trùng với coupon khác
        couponRepository.findByCode(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Mã coupon '" + request.getCode() + "' đã được dùng");
            }
        });
        return ResponseEntity.ok(toResponse(couponRepository.save(fromRequest(coupon, request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found: " + id);
        }
        couponRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Kiểm tra coupon trước khi đặt tour — trả về thông tin giảm giá để FE hiển thị.
     * Body: { "code": "SUMMER2025", "orderValue": 1500000 }
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        BigDecimal orderValue = new BigDecimal(body.getOrDefault("orderValue", "0").toString());

        TourCoupon coupon = couponRepository.findValidCoupon(code, LocalDate.now())
                .orElse(null);

        if (coupon == null) {
            return ResponseEntity.ok(Map.of("valid", false, "message", "Mã coupon không hợp lệ hoặc đã hết hạn"));
        }
        if (coupon.getMinOrderValue() != null && orderValue.compareTo(coupon.getMinOrderValue()) < 0) {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "message", "Đơn hàng tối thiểu " + coupon.getMinOrderValue().toPlainString() + " ₫ để dùng mã này"));
        }

        BigDecimal discount;
        if (coupon.getCouponType() == CouponType.PERCENT) {
            discount = orderValue.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
        } else {
            discount = coupon.getDiscountValue();
        }

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "code", coupon.getCode(),
                "couponType", coupon.getCouponType().name(),
                "discountValue", coupon.getDiscountValue(),
                "discountAmount", discount,
                "finalPrice", orderValue.subtract(discount)
        ));
    }

    private TourCoupon fromRequest(TourCoupon coupon, TourCouponRequest req) {
        coupon.setCode(req.getCode().toUpperCase().trim());
        coupon.setCouponType(req.getCouponType());
        coupon.setDiscountValue(req.getDiscountValue());
        coupon.setMinOrderValue(req.getMinOrderValue());
        coupon.setUsageLimit(req.getUsageLimit() > 0 ? req.getUsageLimit() : 100);
        coupon.setStartDate(req.getStartDate());
        coupon.setEndDate(req.getEndDate());
        coupon.setActive(req.isActive());
        return coupon;
    }

    private TourCouponResponse toResponse(TourCoupon c) {
        TourCouponResponse res = new TourCouponResponse();
        res.setId(c.getId());
        res.setCode(c.getCode());
        res.setCouponType(c.getCouponType());
        res.setDiscountValue(c.getDiscountValue());
        res.setMinOrderValue(c.getMinOrderValue());
        res.setUsageLimit(c.getUsageLimit());
        res.setUsedCount(c.getUsedCount());
        res.setStartDate(c.getStartDate());
        res.setEndDate(c.getEndDate());
        res.setActive(c.isActive());
        return res;
    }
}

