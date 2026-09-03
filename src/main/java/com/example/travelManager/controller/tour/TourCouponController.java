package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.request.tour.TourCouponRequest;
import com.example.travelManager.domain.response.tour.TourCouponResponse;
import com.example.travelManager.domain.tour.TourCoupon;
import com.example.travelManager.service.tour.TourCouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tour-coupons")
@RequiredArgsConstructor
public class TourCouponController {

    private final TourCouponService couponService;

    @GetMapping
    public ResponseEntity<List<TourCouponResponse>> getAll() {
        return ResponseEntity.ok(
                couponService.getAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TourCouponResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(toResponse(couponService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<TourCouponResponse> create(@Valid @RequestBody TourCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(couponService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TourCouponResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody TourCouponRequest request) {
        return ResponseEntity.ok(toResponse(couponService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Body: { "code": "SUMMER2025", "orderValue": 1500000 }
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        BigDecimal orderValue = new BigDecimal(body.getOrDefault("orderValue", "0").toString());
        return ResponseEntity.ok(couponService.validate(code, orderValue));
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
