package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.BookedRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final IBookedRoomService bookedRoomService;

    public List<BookedRoom> getAllBookingsByRoomId(Long roomId) {
        return bookedRoomService.getAllBookingsByRoomId(roomId);
    }
}
