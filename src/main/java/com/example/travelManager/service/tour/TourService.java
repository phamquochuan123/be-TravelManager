package com.example.travelManager.service.tour;

import java.util.List;

import javax.sql.rowset.serial.SerialBlob;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.domain.tour.TourImage;
import com.example.travelManager.domain.tour.TourItinerary;
import com.example.travelManager.util.constant.tour.TourStatus;
import com.example.travelManager.domain.request.tour.TourDepartureRequest;
import com.example.travelManager.domain.request.tour.TourItineraryRequest;
import com.example.travelManager.domain.request.tour.TourRequest;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourImageRepository;
import com.example.travelManager.repository.tour.TourItineraryRepository;
import com.example.travelManager.repository.tour.TourRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourService implements ITourService {

    private final TourRepository tourRepository;
    private final TourItineraryRepository itineraryRepository;
    private final TourDepartureRepository departureRepository;
    private final TourImageRepository imageRepository;

    // ── Tour CRUD ────────────────────────────────────────────────

    @Override
    public Tour createTour(TourRequest request, String createdBy) {
        Tour tour = new Tour();
        mapRequestToTour(request, tour);
        tour.setStatus(TourStatus.ACTIVE);
        tour.setCreatedBy(createdBy);
        tour.setUpdatedBy(createdBy);
        return tourRepository.save(tour);
    }

    @Override
    public Tour updateTour(Long tourId, TourRequest request, String updatedBy) {
        Tour tour = getTourById(tourId);
        mapRequestToTour(request, tour);
        tour.setUpdatedBy(updatedBy);
        return tourRepository.save(tour);
    }

    @Override
    public void deleteTour(Long tourId) {
        Tour tour = getTourById(tourId);
        tour.setDeleted(true);
        tour.setStatus(TourStatus.DELETED);
        tourRepository.save(tour);
    }

    @Override
    public Tour getTourById(Long tourId) {
        return tourRepository.findById(tourId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour not found with id: " + tourId));
    }

    @Override
    public List<Tour> getAllActiveTours() {
        return tourRepository.findAllByDeletedFalseAndStatus(TourStatus.ACTIVE);
    }

    @Override
    public List<Tour> getAllToursAdmin() {
        return tourRepository.findAllByDeletedFalse();
    }

    // ── Itinerary ────────────────────────────────────────────────

    @Override
    public TourItinerary addItinerary(Long tourId, TourItineraryRequest request) {
        Tour tour = getTourById(tourId);
        TourItinerary itinerary = new TourItinerary();
        itinerary.setTour(tour);
        mapRequestToItinerary(request, itinerary);
        return itineraryRepository.save(itinerary);
    }

    @Override
    public TourItinerary updateItinerary(Long itineraryId, TourItineraryRequest request) {
        TourItinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new ResourceNotFoundException("Itinerary not found with id: " + itineraryId));
        mapRequestToItinerary(request, itinerary);
        return itineraryRepository.save(itinerary);
    }

    @Override
    public void deleteItinerary(Long itineraryId) {
        if (!itineraryRepository.existsById(itineraryId)) {
            throw new ResourceNotFoundException("Itinerary not found with id: " + itineraryId);
        }
        itineraryRepository.deleteById(itineraryId);
    }

    // ── Departure ────────────────────────────────────────────────

    @Override
    public TourDeparture addDeparture(Long tourId, TourDepartureRequest request) {
        Tour tour = getTourById(tourId);
        TourDeparture departure = new TourDeparture();
        departure.setTour(tour);
        departure.setDepartureDate(request.getDepartureDate());
        departure.setAvailableSlots(request.getAvailableSlots());
        return departureRepository.save(departure);
    }

    @Override
    public void deleteDeparture(Long departureId) {
        if (!departureRepository.existsById(departureId)) {
            throw new ResourceNotFoundException("Departure not found with id: " + departureId);
        }
        departureRepository.deleteById(departureId);
    }

    // ── Image ────────────────────────────────────────────────────

    @Override
    public void addImage(Long tourId, MultipartFile file) throws Exception {
        Tour tour = getTourById(tourId);
        TourImage image = new TourImage();
        image.setTour(tour);
        image.setImageData(new SerialBlob(file.getBytes()));
        int nextOrder = imageRepository.findByTourIdOrderBySortOrderAsc(tourId).size();
        image.setSortOrder(nextOrder);
        imageRepository.save(image);
    }

    @Override
    public void deleteImage(Long imageId) {
        if (!imageRepository.existsById(imageId)) {
            throw new ResourceNotFoundException("Image not found with id: " + imageId);
        }
        imageRepository.deleteById(imageId);
    }

    // ── Private helpers ──────────────────────────────────────────

    private void mapRequestToTour(TourRequest request, Tour tour) {
        tour.setName(request.getName());
        tour.setDescription(request.getDescription());
        tour.setDestination(request.getDestination());
        tour.setDeparture(request.getDeparture());
        tour.setTourType(request.getTourType());
        tour.setPriceAdult(request.getPriceAdult());
        tour.setPriceChild(request.getPriceChild());
        tour.setDurationDays(request.getDurationDays());
        tour.setMaxSlots(request.getMaxSlots());
        tour.setCancellationPolicy(request.getCancellationPolicy());
        tour.setIncludedServices(request.getIncludedServices());
    }

    private void mapRequestToItinerary(TourItineraryRequest request, TourItinerary itinerary) {
        itinerary.setDayNumber(request.getDayNumber());
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setActivities(request.getActivities());
    }
}
