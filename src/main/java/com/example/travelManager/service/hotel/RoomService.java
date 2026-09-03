package com.example.travelManager.service.hotel;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.domain.request.hotel.RoomCreateRequest;
import com.example.travelManager.exception.InternalServerException;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.HotelRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService implements IRoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookedRoomRepository bookedRoomRepository;

    @Override
    public Room addNewRoom(MultipartFile file, String roomType, BigDecimal roomPrice)
            throws IOException, SQLException {
        Room room = new Room();
        room.setRoomPrice(roomPrice);
        room.setRoomType(roomType);
        if (!file.isEmpty()) {
            room.setPhoto(file.getBytes());
        }
        return roomRepository.save(room);
    }

    @Override
    public List<String> getAllRoomTypes() {
        return roomRepository.findDistinctRoomTypes();
    }

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public byte[] getRoomPhotoByRoomId(Long roomId) throws SQLException {
        Optional<Room> theRoom = roomRepository.findById(roomId);
        if (theRoom.isEmpty()) {
            throw new ResourceNotFoundException("Sorry, Room not found!");
        }
        return theRoom.get().getPhoto();
    }

    @Override
    public void deleteRoom(Long roomId) {
        Optional<Room> theRoom = roomRepository.findById(roomId);
        if (theRoom.isEmpty()) {
            throw new ResourceNotFoundException("Sorry, Room not found!");
        }
        if (bookedRoomRepository.existsByRoom_IdAndStatusNot(roomId, HotelBookingStatus.CANCELLED)) {
            throw new IllegalStateException("Không thể xóa phòng đang có booking hoạt động");
        }
        roomRepository.deleteById(roomId);
    }

    @Override
    public Room updateRoom(Long roomId, String roomType, BigDecimal roomPrice, byte[] photoBytes) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        if (roomType != null)
            room.setRoomType(roomType);
        if (roomPrice != null)
            room.setRoomPrice(roomPrice);
        if (photoBytes != null && photoBytes.length > 0) {
            room.setPhoto(photoBytes);
        }

        return roomRepository.save(room);

    }

    @Override
    public Optional<Room> getRoomById(Long roomId) {
        return roomRepository.findById(roomId);
    }

    @Override
    public List<Room> getRoomsByHotelId(Long hotelId) {
        return roomRepository.findByHotel_Id(hotelId);
    }

    @Override
    public Room addRoomToHotel(Long hotelId, RoomCreateRequest request, MultipartFile photo)
            throws IOException, SQLException {
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + hotelId));
        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType(request.getRoomType());
        room.setRoomPrice(request.getRoomPrice());
        room.setRoomNumber(request.getRoomNumber());
        room.setMaxGuests(request.getMaxGuests());
        room.setNumBeds(request.getNumBeds());
        room.setArea(request.getArea());
        room.setDescription(request.getDescription());
        if (photo != null && !photo.isEmpty()) {
            room.setPhoto(photo.getBytes());
        }
        return roomRepository.save(room);
    }

    @Override
    public Room updateRoomInHotel(Long hotelId, Long roomId, RoomCreateRequest request, MultipartFile photo)
            throws IOException, SQLException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        if (room.getHotel() == null || !room.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Room does not belong to hotel " + hotelId);
        }
        if (request.getRoomType() != null) room.setRoomType(request.getRoomType());
        if (request.getRoomPrice() != null) room.setRoomPrice(request.getRoomPrice());
        if (request.getRoomNumber() != null) room.setRoomNumber(request.getRoomNumber());
        if (request.getMaxGuests() > 0) room.setMaxGuests(request.getMaxGuests());
        if (request.getNumBeds() > 0) room.setNumBeds(request.getNumBeds());
        if (request.getArea() != null) room.setArea(request.getArea());
        if (request.getDescription() != null) room.setDescription(request.getDescription());
        if (photo != null && !photo.isEmpty()) {
            room.setPhoto(photo.getBytes());
        }
        return roomRepository.save(room);
    }

    @Override
    public long countRoomsByHotelId(Long hotelId) {
        return roomRepository.countByHotel_Id(hotelId);
    }

    @Override
    public Map<Long, Long> countRoomsByHotelIds(List<Long> hotelIds) {
        if (hotelIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : roomRepository.countGroupedByHotelIds(hotelIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    public void deleteRoomFromHotel(Long hotelId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        if (room.getHotel() == null || !room.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Room does not belong to hotel " + hotelId);
        }
        if (bookedRoomRepository.existsByRoom_IdAndStatusNot(roomId, HotelBookingStatus.CANCELLED)) {
            throw new IllegalStateException("Không thể xóa phòng đang có booking hoạt động");
        }
        roomRepository.deleteById(roomId);
    }
}
