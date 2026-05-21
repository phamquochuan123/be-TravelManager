package com.example.travelManager.controller.destination;

import com.example.travelManager.domain.destination.Destination;
import com.example.travelManager.domain.request.destination.DestinationRequest;
import com.example.travelManager.domain.response.destination.DestinationResponse;
import com.example.travelManager.service.destination.IDestinationService;
import com.example.travelManager.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/destinations")
@RequiredArgsConstructor
public class DestinationController {

    private final IDestinationService destinationService;

    @PostMapping
    public ResponseEntity<DestinationResponse> create(@Valid @RequestBody DestinationRequest request) {
        String user = SecurityUtil.getCurrentUserLogin().orElse("system");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(destinationService.create(request, user)));
    }

    @GetMapping
    public ResponseEntity<List<DestinationResponse>> getAll(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "admin", defaultValue = "false") boolean admin) {
        List<Destination> list;
        if (admin) list = destinationService.getAllAdmin();
        else if (city != null) list = destinationService.getByCity(city);
        else list = destinationService.getAllActive();
        return ResponseEntity.ok(list.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DestinationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(destinationService.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DestinationResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DestinationRequest request) {
        String user = SecurityUtil.getCurrentUserLogin().orElse("system");
        return ResponseEntity.ok(toResponse(destinationService.update(id, request, user)));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<DestinationResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(destinationService.toggleActive(id)));
    }

    @PatchMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DestinationResponse> uploadPhoto(
            @PathVariable Long id,
            @RequestParam("photo") MultipartFile photo) throws IOException, SQLException {
        return ResponseEntity.ok(toResponse(destinationService.uploadPhoto(id, photo)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        destinationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private DestinationResponse toResponse(Destination d) {
        byte[] photoBytes = null;
        if (d.getPhoto() != null) {
            try { photoBytes = d.getPhoto().getBytes(1, (int) d.getPhoto().length()); }
            catch (SQLException ignored) {}
        }
        DestinationResponse res = new DestinationResponse();
        res.setId(d.getId());
        res.setName(d.getName());
        res.setDescription(d.getDescription());
        res.setDestinationType(d.getDestinationType());
        res.setProvince(d.getProvince());
        res.setCity(d.getCity());
        res.setActive(d.isActive());
        res.setPhoto(photoBytes);
        return res;
    }
}
