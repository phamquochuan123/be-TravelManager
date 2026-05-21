package com.example.travelManager.service.destination;

import com.example.travelManager.domain.destination.Destination;
import com.example.travelManager.domain.request.destination.DestinationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public interface IDestinationService {
    Destination create(DestinationRequest request, String createdBy);
    Destination update(Long id, DestinationRequest request, String updatedBy);
    void delete(Long id);
    Destination toggleActive(Long id);
    Destination getById(Long id);
    List<Destination> getAllActive();
    List<Destination> getAllAdmin();
    List<Destination> getByCity(String city);
    Destination uploadPhoto(Long id, MultipartFile photo) throws IOException, SQLException;
}
