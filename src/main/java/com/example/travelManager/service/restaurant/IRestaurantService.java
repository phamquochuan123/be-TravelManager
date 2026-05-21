package com.example.travelManager.service.restaurant;

import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.request.restaurant.RestaurantRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IRestaurantService {
    Restaurant createRestaurant(RestaurantRequest request, String createdBy);
    Restaurant updateRestaurant(Long id, RestaurantRequest request, String updatedBy);
    void deleteRestaurant(Long id);
    Restaurant toggleActive(Long id);
    Restaurant getById(Long id);
    List<Restaurant> getAllActive();
    List<Restaurant> getAllAdmin();
    List<Restaurant> getByCity(String city);
    Restaurant uploadPhoto(Long id, MultipartFile photo) throws IOException, SQLException;
}
