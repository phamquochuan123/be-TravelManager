package com.example.travelManager.service.tour;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.sql.rowset.serial.SerialBlob;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.domain.tour.TourCoupon;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.domain.tour.TourImage;
import com.example.travelManager.domain.tour.TourItinerary;
import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.domain.request.tour.TourBookingRequest;
import com.example.travelManager.domain.request.tour.TourDepartureRequest;
import com.example.travelManager.domain.request.tour.TourItineraryRequest;
import com.example.travelManager.domain.request.tour.TourRequest;
import com.example.travelManager.domain.response.tour.TourBookingResponse;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.example.travelManager.repository.tour.TourCouponRepository;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourImageRepository;
import com.example.travelManager.repository.tour.TourItineraryRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;
import com.example.travelManager.util.constant.tour.BookingStatus;
import com.example.travelManager.util.constant.tour.CouponType;
import com.example.travelManager.util.constant.tour.TourStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TourService implements ITourService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TourRepository tourRepository;
    private final TourItineraryRepository itineraryRepository;
    private final TourDepartureRepository departureRepository;
    private final TourImageRepository imageRepository;
    private final TourBookingRepository bookingRepository;
    private final TourCouponRepository couponRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookedRoomRepository bookedRoomRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;
    private final UserRepository userRepository;

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
        boolean hasActiveBookings = bookingRepository.existsByTourIdAndStatusIn(
                tourId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (hasActiveBookings) {
            throw new IllegalStateException("Không thể xóa tour đang có booking hoạt động");
        }
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

    @Override
    public Tour toggleActive(Long tourId) {
        Tour tour = getTourById(tourId);
        if (tour.getStatus() == TourStatus.ACTIVE) {
            tour.setStatus(TourStatus.INACTIVE);
        } else if (tour.getStatus() == TourStatus.INACTIVE) {
            tour.setStatus(TourStatus.ACTIVE);
        }
        return tourRepository.save(tour);
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

    // ── Booking ──────────────────────────────────────────────────

    @Override
    @Transactional
    public TourBookingResponse bookTour(Long tourId, TourBookingRequest request, String userEmail) {
        Tour tour = getTourById(tourId);
        TourDeparture departure = departureRepository.findById(request.getDepartureId())
                .orElseThrow(() -> new ResourceNotFoundException("Departure not found: " + request.getDepartureId()));
        if (!departure.getTour().getId().equals(tourId)) {
            throw new IllegalArgumentException("Departure không thuộc tour này");
        }

        int totalGuests = request.getNumAdults() + request.getNumChildren();
        if (departure.getAvailableSlots() < totalGuests) {
            throw new IllegalStateException("Không đủ chỗ. Còn " + departure.getAvailableSlots() + " chỗ");
        }

        if (tour.getDurationNights() > 0 && request.getRoomId() == null) {
            throw new IllegalArgumentException("Tour " + tour.getDurationNights() + " đêm yêu cầu chọn khách sạn");
        }

        UserEntity user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        // Tính giá
        BigDecimal tourPrice = tour.getPriceAdult().multiply(BigDecimal.valueOf(request.getNumAdults()))
                .add(tour.getPriceChild() != null
                        ? tour.getPriceChild().multiply(BigDecimal.valueOf(request.getNumChildren()))
                        : BigDecimal.ZERO);

        BigDecimal hotelPrice = BigDecimal.ZERO;
        Room room = null;
        if (request.getRoomId() != null) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + request.getRoomId()));
            hotelPrice = room.getRoomPrice().multiply(BigDecimal.valueOf(tour.getDurationNights()));
        }

        BigDecimal restaurantPrice = BigDecimal.ZERO;
        Restaurant restaurant = null;
        if (request.getRestaurantId() != null) {
            restaurant = restaurantRepository.findById(request.getRestaurantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + request.getRestaurantId()));
            if (restaurant.getPricePerPerson() != null) {
                restaurantPrice = restaurant.getPricePerPerson().multiply(BigDecimal.valueOf(totalGuests));
            }
        }

        BigDecimal rawTotal = tourPrice.add(hotelPrice).add(restaurantPrice);

        BigDecimal packageDiscountAmt = BigDecimal.ZERO;
        if (tour.getPackageDiscountPercent() != null && tour.getPackageDiscountPercent() > 0) {
            packageDiscountAmt = rawTotal
                    .multiply(BigDecimal.valueOf(tour.getPackageDiscountPercent() / 100.0))
                    .setScale(0, RoundingMode.HALF_UP);
        }

        BigDecimal afterPackage = rawTotal.subtract(packageDiscountAmt);

        BigDecimal couponDiscountAmt = BigDecimal.ZERO;
        TourCoupon coupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponRepository.findValidCoupon(request.getCouponCode(), LocalDate.now())
                    .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không hợp lệ hoặc đã hết hạn"));
            if (coupon.getMinOrderValue() == null || afterPackage.compareTo(coupon.getMinOrderValue()) >= 0) {
                if (coupon.getCouponType() == CouponType.PERCENT) {
                    couponDiscountAmt = afterPackage
                            .multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100)))
                            .setScale(0, RoundingMode.HALF_UP);
                } else {
                    couponDiscountAmt = coupon.getDiscountValue();
                }
                coupon.setUsedCount(coupon.getUsedCount() + 1);
            }
        }

        BigDecimal totalDiscount = packageDiscountAmt.add(couponDiscountAmt);
        BigDecimal finalPrice = rawTotal.subtract(totalDiscount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) finalPrice = BigDecimal.ZERO;

        // Tạo BookedRoom
        BookedRoom bookedRoom = null;
        if (room != null) {
            bookedRoom = new BookedRoom();
            bookedRoom.setRoom(room);
            bookedRoom.setCheckInDate(departure.getDepartureDate());
            bookedRoom.setCheckOutDate(departure.getDepartureDate().plusDays(tour.getDurationNights()));
            bookedRoom.setGuestFullName(request.getContactName());
            bookedRoom.setGuestEmail(request.getContactEmail());
            bookedRoom.setNumOfAdults(request.getNumAdults());
            bookedRoom.setNumOfChildren(request.getNumChildren());
            bookedRoom.calculateTotalNumOfGuests();
            bookedRoom.setStatus(HotelBookingStatus.PENDING);
            bookedRoom.setBookingConfirmationCode(
                    org.apache.commons.lang3.RandomStringUtils.randomNumeric(10));
            bookedRoom = bookedRoomRepository.save(bookedRoom);
        }

        // Tạo RestaurantBooking
        RestaurantBooking restaurantBooking = null;
        if (restaurant != null) {
            restaurantBooking = new RestaurantBooking();
            restaurantBooking.setRestaurant(restaurant);
            restaurantBooking.setUser(user);
            restaurantBooking.setBookingDate(departure.getDepartureDate());
            restaurantBooking.setBookingTime(
                    request.getRestaurantBookingTime() != null
                            ? request.getRestaurantBookingTime()
                            : LocalTime.of(12, 0));
            restaurantBooking.setGuestCount(totalGuests);
            restaurantBooking.setContactName(request.getContactName());
            restaurantBooking.setContactPhone(request.getContactPhone());
            restaurantBooking.setContactEmail(request.getContactEmail());
            restaurantBooking.setStatus(RestaurantBookingStatus.CONFIRMED);
            restaurantBooking.setConfirmationCode(
                    org.apache.commons.lang3.RandomStringUtils.randomNumeric(10));
            restaurantBooking = restaurantBookingRepository.save(restaurantBooking);
        }

        // Tạo TourBooking
        TourBooking booking = new TourBooking();
        booking.setTour(tour);
        booking.setDeparture(departure);
        booking.setUser(user);
        booking.setCoupon(coupon);
        booking.setBookedRoom(bookedRoom);
        booking.setRestaurantBooking(restaurantBooking);
        booking.setContactName(request.getContactName());
        booking.setContactPhone(request.getContactPhone());
        booking.setContactEmail(request.getContactEmail());
        booking.setNumAdults(request.getNumAdults());
        booking.setNumChildren(request.getNumChildren());
        booking.setOriginalPrice(rawTotal);
        booking.setPackageHotelPrice(hotelPrice);
        booking.setPackageRestaurantPrice(restaurantPrice);
        booking.setDiscountAmount(totalDiscount);
        booking.setFinalPrice(finalPrice);
        booking.setNote(request.getNote());
        booking.setStatus(BookingStatus.PENDING);
        booking = bookingRepository.save(booking);

        // Giảm slot
        departure.setAvailableSlots(departure.getAvailableSlots() - totalGuests);
        departureRepository.save(departure);

        return toBookingResponse(booking);
    }

    @Override
    public List<TourBookingResponse> getMyBookings(String userEmail) {
        return bookingRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream().map(this::toBookingResponse).toList();
    }

    @Override
    public List<TourBookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toBookingResponse).toList();
    }

    private TourBookingResponse toBookingResponse(TourBooking b) {
        TourBookingResponse res = new TourBookingResponse();
        res.setId(b.getId());
        res.setTourId(b.getTour().getId());
        res.setTourName(b.getTour().getName());
        res.setTourDestination(b.getTour().getDestination());
        res.setDepartureId(b.getDeparture().getId());
        res.setDepartureDate(b.getDeparture().getDepartureDate());
        res.setNumAdults(b.getNumAdults());
        res.setNumChildren(b.getNumChildren());
        res.setContactName(b.getContactName());
        res.setContactPhone(b.getContactPhone());
        res.setContactEmail(b.getContactEmail());
        res.setOriginalPrice(b.getOriginalPrice());
        res.setPackageHotelPrice(b.getPackageHotelPrice());
        res.setPackageRestaurantPrice(b.getPackageRestaurantPrice());
        res.setDiscountAmount(b.getDiscountAmount());
        res.setFinalPrice(b.getFinalPrice());
        res.setPackageDiscountPercent(b.getTour().getPackageDiscountPercent());
        res.setStatus(b.getStatus());
        res.setNote(b.getNote());
        res.setCreatedAt(b.getCreatedAt());
        if (b.getBookedRoom() != null) {
            BookedRoom br = b.getBookedRoom();
            res.setBookedRoomId(br.getBookingId());
            res.setRoomType(br.getRoom().getRoomType());
            res.setHotelId(br.getRoom().getHotel().getId());
            res.setHotelName(br.getRoom().getHotel().getName());
            res.setCheckInDate(br.getCheckInDate());
            res.setCheckOutDate(br.getCheckOutDate());
        }
        if (b.getRestaurantBooking() != null) {
            RestaurantBooking rb = b.getRestaurantBooking();
            res.setRestaurantBookingId(rb.getId());
            res.setRestaurantId(rb.getRestaurant().getId());
            res.setRestaurantName(rb.getRestaurant().getName());
        }
        return res;
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
        tour.setDurationNights(request.getDurationNights());
        tour.setMaxSlots(request.getMaxSlots());
        tour.setPackageDiscountPercent(request.getPackageDiscountPercent());
        tour.setCancellationPolicy(request.getCancellationPolicy());
        tour.setIncludedServices(request.getIncludedServices());
        tour.setLinkedHotels(buildLinkedJson(autoLinkHotels(request.getDestination())));
        tour.setLinkedRestaurants(buildLinkedJson(autoLinkRestaurants(request.getDestination())));
    }

    private String extractPrimaryCity(String destination) {
        if (destination == null || destination.isBlank()) return "";
        return destination.split("[–\\-/,]")[0].trim().toLowerCase();
    }

    private List<Integer> autoLinkHotels(String destination) {
        String city = extractPrimaryCity(destination);
        if (city.isEmpty()) return List.of();
        return hotelRepository.findAll().stream()
                .filter(h -> h.isActive()
                        && h.getCity() != null
                        && !h.getCity().isBlank()
                        && (h.getCity().toLowerCase().contains(city) || city.contains(h.getCity().toLowerCase())))
                .map(h -> h.getId().intValue())
                .collect(Collectors.toList());
    }

    private List<Integer> autoLinkRestaurants(String destination) {
        String city = extractPrimaryCity(destination);
        if (city.isEmpty()) return List.of();
        return restaurantRepository.findAll().stream()
                .filter(r -> r.isActive()
                        && r.getCity() != null
                        && !r.getCity().isBlank()
                        && (r.getCity().toLowerCase().contains(city) || city.contains(r.getCity().toLowerCase())))
                .map(r -> r.getId().intValue())
                .collect(Collectors.toList());
    }

    private String buildLinkedJson(List<Integer> ids) {
        try { return MAPPER.writeValueAsString(ids); }
        catch (Exception e) { return "[]"; }
    }

    private void mapRequestToItinerary(TourItineraryRequest request, TourItinerary itinerary) {
        itinerary.setDayNumber(request.getDayNumber());
        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setActivities(request.getActivities());
    }
}
