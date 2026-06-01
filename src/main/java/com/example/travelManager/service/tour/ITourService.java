package com.example.travelManager.service.tour;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.domain.tour.TourItinerary;
import com.example.travelManager.domain.request.tour.TourDepartureRequest;
import com.example.travelManager.domain.request.tour.TourItineraryRequest;
import com.example.travelManager.domain.request.tour.TourRequest;

public interface ITourService {

    // --- Tour CRUD ---
    Tour createTour(TourRequest request, String createdBy);

    Tour updateTour(Long tourId, TourRequest request, String updatedBy);

    void deleteTour(Long tourId); // soft delete

    Tour getTourById(Long tourId);

    List<Tour> getAllActiveTours();

    List<Tour> getAllToursAdmin(); // bao gom ca inactive

    Tour toggleActive(Long tourId);

    // --- Itinerary ---
    TourItinerary addItinerary(Long tourId, TourItineraryRequest request);

    TourItinerary updateItinerary(Long itineraryId, TourItineraryRequest request);

    void deleteItinerary(Long itineraryId);

    // --- Departure ---
    TourDeparture addDeparture(Long tourId, TourDepartureRequest request);

    void deleteDeparture(Long departureId);

    // --- Image ---
    void addImage(Long tourId, MultipartFile file) throws Exception;

    void deleteImage(Long imageId);
}
