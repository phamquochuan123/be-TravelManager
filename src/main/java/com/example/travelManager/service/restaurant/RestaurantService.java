package com.example.travelManager.service.restaurant;

import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.request.restaurant.RestaurantRequest;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService implements IRestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public Restaurant createRestaurant(RestaurantRequest request, String createdBy) {
        Restaurant r = new Restaurant();
        mapRequest(r, request);
        r.setCreatedBy(createdBy);
        r.setUpdatedBy(createdBy);
        return restaurantRepository.save(r);
    }

    @Override
    public Restaurant updateRestaurant(Long id, RestaurantRequest request, String updatedBy) {
        Restaurant r = getById(id);
        mapRequest(r, request);
        if (request.getIsActive() != null) r.setActive(request.getIsActive());
        r.setUpdatedBy(updatedBy);
        return restaurantRepository.save(r);
    }

    @Override
    public void deleteRestaurant(Long id) {
        Restaurant r = getById(id);
        r.setActive(false);
        restaurantRepository.save(r);
    }

    @Override
    public Restaurant toggleActive(Long id) {
        Restaurant r = getById(id);
        r.setActive(!r.isActive());
        return restaurantRepository.save(r);
    }

    @Override
    public Restaurant getById(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
    }

    @Override
    public List<Restaurant> getAllActive() {
        return restaurantRepository.findByIsActiveTrue();
    }

    @Override
    public List<Restaurant> getAllAdmin() {
        return restaurantRepository.findAll();
    }

    @Override
    public List<Restaurant> getByCity(String city) {
        return restaurantRepository.findByCityIgnoreCaseAndIsActiveTrue(city);
    }

    @Override
    public Restaurant uploadPhoto(Long id, MultipartFile photo) throws IOException, SQLException {
        Restaurant r = getById(id);
        if (photo != null && !photo.isEmpty()) {
            r.setPhoto(new SerialBlob(photo.getBytes()));
        }
        return restaurantRepository.save(r);
    }

    private void mapRequest(Restaurant r, RestaurantRequest req) {
        r.setName(req.getName());
        r.setDescription(req.getDescription());
        r.setAddress(req.getAddress());
        r.setCity(req.getCity());
        r.setCuisineType(req.getCuisineType());
        r.setPriceRange(req.getPriceRange());
        r.setCapacity(req.getCapacity());
        r.setOpeningHours(req.getOpeningHours());
        r.setAmenities(req.getAmenities());
        if (req.getPricePerPerson() != null) r.setPricePerPerson(req.getPricePerPerson());
    }
}
