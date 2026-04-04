package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.Hotel;
import com.example.travelManager.domain.request.hotel.HotelRequest;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService implements IHotelService {

    private final HotelRepository hotelRepository;

    @Override
    public Hotel createHotel(HotelRequest request, String createdBy) {
        Hotel hotel = new Hotel();
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setStarRating(request.getStarRating());
        hotel.setHotelType(request.getHotelType());
        hotel.setAmenities(request.getAmenities());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        hotel.setCreatedBy(createdBy);
        hotel.setUpdatedBy(createdBy);
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel updateHotel(Long hotelId, HotelRequest request, String updatedBy) {
        Hotel hotel = getHotelById(hotelId);
        hotel.setName(request.getName());
        hotel.setDescription(request.getDescription());
        hotel.setAddress(request.getAddress());
        hotel.setCity(request.getCity());
        hotel.setStarRating(request.getStarRating());
        hotel.setHotelType(request.getHotelType());
        hotel.setAmenities(request.getAmenities());
        hotel.setLatitude(request.getLatitude());
        hotel.setLongitude(request.getLongitude());
        if (request.getIsActive() != null) {
            hotel.setActive(request.getIsActive());
        }
        hotel.setUpdatedBy(updatedBy);
        return hotelRepository.save(hotel);
    }

    @Override
    public void deleteHotel(Long hotelId) {
        Hotel hotel = getHotelById(hotelId);
        hotel.setActive(false);
        hotelRepository.save(hotel);
    }

    @Override
    public Hotel toggleActive(Long hotelId) {
        Hotel hotel = getHotelById(hotelId);
        hotel.setActive(!hotel.isActive());
        return hotelRepository.save(hotel);
    }

    @Override
    public Hotel getHotelById(Long hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + hotelId));
    }

    @Override
    public List<Hotel> getAllHotels() {
        return hotelRepository.findByIsActiveTrue();
    }

    @Override
    public List<Hotel> getAllHotelsAdmin() {
        return hotelRepository.findAll();
    }

    @Override
    public List<Hotel> getHotelsByCity(String city) {
        return hotelRepository.findByCityIgnoreCaseAndIsActiveTrue(city);
    }

    @Override
    public Hotel uploadPhoto(Long hotelId, MultipartFile photo) throws IOException, SQLException {
        Hotel hotel = getHotelById(hotelId);
        if (photo != null && !photo.isEmpty()) {
            hotel.setPhoto(new SerialBlob(photo.getBytes()));
        }
        return hotelRepository.save(hotel);
    }
}
