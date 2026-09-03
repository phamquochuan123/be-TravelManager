package com.example.travelManager.service.tour;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourSeasonalPrice;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.repository.tour.TourSeasonalPriceRepository;
import com.example.travelManager.util.constant.tour.SeasonType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourSeasonalPriceService {

    private final TourSeasonalPriceRepository seasonalPriceRepository;
    private final TourRepository tourRepository;

    public List<TourSeasonalPrice> getByTour(Long tourId) {
        return seasonalPriceRepository.findByTourId(tourId);
    }

    public TourSeasonalPrice create(Long tourId, String seasonName, LocalDate startDate, LocalDate endDate,
                                     SeasonType seasonType, BigDecimal priceAdult, BigDecimal priceChild) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found: " + tourId));
        TourSeasonalPrice price = new TourSeasonalPrice();
        price.setTour(tour);
        applyFields(price, seasonName, startDate, endDate, seasonType, priceAdult, priceChild);
        return seasonalPriceRepository.save(price);
    }

    public TourSeasonalPrice update(Long tourId, Long priceId, String seasonName, LocalDate startDate,
                                     LocalDate endDate, SeasonType seasonType, BigDecimal priceAdult,
                                     BigDecimal priceChild) {
        TourSeasonalPrice price = findInTour(tourId, priceId);
        applyFields(price, seasonName, startDate, endDate, seasonType, priceAdult, priceChild);
        return seasonalPriceRepository.save(price);
    }

    public void delete(Long tourId, Long priceId) {
        seasonalPriceRepository.delete(findInTour(tourId, priceId));
    }

    private void applyFields(TourSeasonalPrice price, String seasonName, LocalDate startDate, LocalDate endDate,
                              SeasonType seasonType, BigDecimal priceAdult, BigDecimal priceChild) {
        price.setSeasonName(seasonName);
        price.setStartDate(startDate);
        price.setEndDate(endDate);
        price.setSeasonType(seasonType);
        price.setPriceAdult(priceAdult);
        price.setPriceChild(priceChild);
    }

    private TourSeasonalPrice findInTour(Long tourId, Long priceId) {
        TourSeasonalPrice price = seasonalPriceRepository.findById(priceId)
                .orElseThrow(() -> new ResourceNotFoundException("SeasonalPrice not found: " + priceId));
        if (!price.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Seasonal price này không thuộc tour " + tourId);
        }
        return price;
    }
}
