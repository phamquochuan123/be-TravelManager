package com.example.travelManager.controller.tour;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourSeasonalPrice;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.repository.tour.TourSeasonalPriceRepository;
import com.example.travelManager.util.constant.tour.SeasonType;
import jakarta.validation.Valid;
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

    private final TourSeasonalPriceRepository seasonalPriceRepository;
    private final TourRepository tourRepository;

    @GetMapping
    public ResponseEntity<List<SeasonalPriceResponse>> getByTour(@PathVariable("tourId") Long tourId) {
        return ResponseEntity.ok(
                seasonalPriceRepository.findByTourId(tourId)
                        .stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<SeasonalPriceResponse> create(
            @PathVariable("tourId") Long tourId,
            @Valid @RequestBody SeasonalPriceRequest request) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));

        TourSeasonalPrice price = new TourSeasonalPrice();
        price.setTour(tour);
        price.setSeasonName(request.getSeasonName());
        price.setStartDate(request.getStartDate());
        price.setEndDate(request.getEndDate());
        price.setSeasonType(request.getSeasonType());
        price.setPriceAdult(request.getPriceAdult());
        price.setPriceChild(request.getPriceChild());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(seasonalPriceRepository.save(price)));
    }

    @PutMapping("/{priceId}")
    public ResponseEntity<SeasonalPriceResponse> update(
            @PathVariable("tourId") Long tourId,
            @PathVariable("priceId") Long priceId,
            @Valid @RequestBody SeasonalPriceRequest request) {
        TourSeasonalPrice price = findInTour(tourId, priceId);
        price.setSeasonName(request.getSeasonName());
        price.setStartDate(request.getStartDate());
        price.setEndDate(request.getEndDate());
        price.setSeasonType(request.getSeasonType());
        price.setPriceAdult(request.getPriceAdult());
        price.setPriceChild(request.getPriceChild());
        return ResponseEntity.ok(toResponse(seasonalPriceRepository.save(price)));
    }

    @DeleteMapping("/{priceId}")
    public ResponseEntity<Void> delete(
            @PathVariable("tourId") Long tourId,
            @PathVariable("priceId") Long priceId) {
        seasonalPriceRepository.delete(findInTour(tourId, priceId));
        return ResponseEntity.noContent().build();
    }

    private TourSeasonalPrice findInTour(Long tourId, Long priceId) {
        TourSeasonalPrice price = seasonalPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("SeasonalPrice not found: " + priceId));
        if (!price.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Seasonal price này không thuộc tour " + tourId);
        }
        return price;
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
        @NotNull
        private SeasonType seasonType;
        @NotNull
        private BigDecimal priceAdult;
        @NotNull
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
    }
}

