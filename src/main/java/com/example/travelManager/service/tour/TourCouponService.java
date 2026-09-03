package com.example.travelManager.service.tour;

import com.example.travelManager.domain.request.tour.TourCouponRequest;
import com.example.travelManager.domain.tour.TourCoupon;
import com.example.travelManager.exception.DuplicateResourceException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourCouponRepository;
import com.example.travelManager.util.constant.tour.CouponType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TourCouponService {

    private final TourCouponRepository couponRepository;

    public List<TourCoupon> getAll() {
        return couponRepository.findAll();
    }

    public TourCoupon getById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
    }

    public TourCoupon create(TourCouponRequest request) {
        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Mã coupon '" + request.getCode() + "' đã tồn tại");
        }
        return couponRepository.save(fromRequest(new TourCoupon(), request));
    }

    public TourCoupon update(Long id, TourCouponRequest request) {
        TourCoupon coupon = getById(id);
        // Cho phép đổi code miễn là không trùng với coupon khác
        couponRepository.findByCode(request.getCode()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new DuplicateResourceException("Mã coupon '" + request.getCode() + "' đã được dùng");
            }
        });
        return couponRepository.save(fromRequest(coupon, request));
    }

    public void delete(Long id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found: " + id);
        }
        couponRepository.deleteById(id);
    }

    /**
     * Kiểm tra coupon trước khi đặt tour — trả về thông tin giảm giá để FE hiển thị.
     */
    public Map<String, Object> validate(String code, BigDecimal orderValue) {
        TourCoupon coupon = couponRepository.findValidCoupon(code, LocalDate.now()).orElse(null);

        if (coupon == null) {
            return Map.of("valid", false, "message", "Mã coupon không hợp lệ hoặc đã hết hạn");
        }
        if (coupon.getMinOrderValue() != null && orderValue.compareTo(coupon.getMinOrderValue()) < 0) {
            return Map.of(
                    "valid", false,
                    "message", "Đơn hàng tối thiểu " + coupon.getMinOrderValue().toPlainString() + " ₫ để dùng mã này");
        }

        BigDecimal discount;
        if (coupon.getCouponType() == CouponType.PERCENT) {
            discount = orderValue.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100));
        } else {
            discount = coupon.getDiscountValue();
        }

        return Map.of(
                "valid", true,
                "code", coupon.getCode(),
                "couponType", coupon.getCouponType().name(),
                "discountValue", coupon.getDiscountValue(),
                "discountAmount", discount,
                "finalPrice", orderValue.subtract(discount)
        );
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
}
