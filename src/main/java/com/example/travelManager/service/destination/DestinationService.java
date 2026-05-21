package com.example.travelManager.service.destination;

import com.example.travelManager.domain.destination.Destination;
import com.example.travelManager.domain.request.destination.DestinationRequest;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.destination.DestinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DestinationService implements IDestinationService {

    private final DestinationRepository destinationRepository;

    @Override
    public Destination create(DestinationRequest request, String createdBy) {
        Destination d = new Destination();
        mapRequest(d, request);
        d.setCreatedBy(createdBy);
        d.setUpdatedBy(createdBy);
        return destinationRepository.save(d);
    }

    @Override
    public Destination update(Long id, DestinationRequest request, String updatedBy) {
        Destination d = getById(id);
        mapRequest(d, request);
        if (request.getIsActive() != null) d.setActive(request.getIsActive());
        d.setUpdatedBy(updatedBy);
        return destinationRepository.save(d);
    }

    @Override
    public void delete(Long id) {
        Destination d = getById(id);
        d.setActive(false);
        destinationRepository.save(d);
    }

    @Override
    public Destination toggleActive(Long id) {
        Destination d = getById(id);
        d.setActive(!d.isActive());
        return destinationRepository.save(d);
    }

    @Override
    public Destination getById(Long id) {
        return destinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Destination not found: " + id));
    }

    @Override
    public List<Destination> getAllActive() {
        return destinationRepository.findByIsActiveTrue();
    }

    @Override
    public List<Destination> getAllAdmin() {
        return destinationRepository.findAll();
    }

    @Override
    public List<Destination> getByCity(String city) {
        return destinationRepository.findByCityIgnoreCaseAndIsActiveTrue(city);
    }

    @Override
    public Destination uploadPhoto(Long id, MultipartFile photo) throws IOException, SQLException {
        Destination d = getById(id);
        if (photo != null && !photo.isEmpty()) {
            d.setPhoto(new SerialBlob(photo.getBytes()));
        }
        return destinationRepository.save(d);
    }

    private void mapRequest(Destination d, DestinationRequest req) {
        d.setName(req.getName());
        d.setDescription(req.getDescription());
        d.setDestinationType(req.getDestinationType());
        d.setProvince(req.getProvince());
        d.setCity(req.getCity());
    }
}
