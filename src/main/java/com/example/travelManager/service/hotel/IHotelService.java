package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.Hotel;
import com.example.travelManager.domain.request.hotel.HotelRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IHotelService {
    Hotel createHotel(HotelRequest request, String createdBy);
    Hotel updateHotel(Long hotelId, HotelRequest request, String updatedBy);
    void deleteHotel(Long hotelId);
    Hotel toggleActive(Long hotelId);
    Hotel getHotelById(Long hotelId);
    List<Hotel> getAllHotels();
    List<Hotel> getAllHotelsAdmin();
    List<Hotel> getHotelsByCity(String city);
    Hotel uploadPhoto(Long hotelId, MultipartFile photo) throws IOException, SQLException;
}
