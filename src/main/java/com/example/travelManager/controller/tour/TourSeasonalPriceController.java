package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.tour.TourSeasonalPrice;
import com.example.travelManager.service.tour.TourSeasonalPriceService;
import com.example.travelManager.util.constant.tour.SeasonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/tours/{tourId}/seasonal-prices")
@RequiredArgsConstructor
public class TourSeasonalPriceController {

    private final TourSeasonalPriceService seasonalPriceService;

    @GetMapping
    public ResponseEntity<List<SeasonalPriceResponse>> getByTour(@PathVariable("tourId") Long tourId) {
        return ResponseEntity.ok(
                seasonalPriceService.getByTour(tourId).stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<SeasonalPriceResponse> create(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody SeasonalPriceRequest request) {
        TourSeasonalPrice price = seasonalPriceService.create(tourId, request.getSeasonName(),
                request.getStartDate(), request.getEndDate(), request.getSeasonType(),
                request.getPriceAdult(), request.getPriceChild());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(price));
    }

    @PutMapping("/{priceId}")
    public ResponseEntity<SeasonalPriceResponse> update(
            @PathVariable("tourId") Long tourId,
            @PathVariable("priceId") Long priceId,
            @Valid @RequestBody SeasonalPriceRequest request) {
        TourSeasonalPrice price = seasonalPriceService.update(tourId, priceId, request.getSeasonName(),
                request.getStartDate(), request.getEndDate(), request.getSeasonType(),
                request.getPriceAdult(), request.getPriceChild());
        return ResponseEntity.ok(toResponse(price));
    }

    @DeleteMapping("/{priceId}")
    public ResponseEntity<Void> delete(
            @PathVariable("tourId") Long tourId,
            @PathVariable("priceId") Long priceId) {
        seasonalPriceService.delete(tourId, priceId);
        return ResponseEntity.noContent().build();
    }

    private SeasonalPriceResponse toResponse(TourSeasonalPrice p) {
        SeasonalPriceResponse res = new SeasonalPriceResponse();
        res.setId(p.getId());
        res.setTourId(p.getTour().getId());
        res.setSeasonName(p.getSeasonName());
        res.setStartDate(p.getStartDate());
        res.setEndDate(p.getEndDate());
        res.setSeasonType(p.getSeasonType());
        res.setPriceAdult(p.getPriceAdult());
        res.setPriceChild(p.getPriceChild());
        LocalDate today = LocalDate.now();
        res.setIsActive(p.getStartDate() != null && p.getEndDate() != null
                && !today.isBefore(p.getStartDate()) && !today.isAfter(p.getEndDate()));
        return res;
    }

    @Data
    public static class SeasonalPriceRequest {
        @NotBlank
        private String seasonName;
        @NotNull
        private LocalDate startDate;
        @NotNull
        private LocalDate endDate;
        private SeasonType seasonType;
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal priceAdult;
        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        private BigDecimal priceChild;
    }

    @Data
    public static class SeasonalPriceResponse {
        private Long id;
        private Long tourId;
        private String seasonName;
        private LocalDate startDate;
        private LocalDate endDate;
        private SeasonType seasonType;
        private BigDecimal priceAdult;
        private BigDecimal priceChild;
        private Boolean isActive;
    }
}
