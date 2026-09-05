package com.example.travelManager.service.hotel;

import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.util.constant.hotel.HotelBookingStatus;
import com.example.travelManager.util.constant.hotel.RoomStatus;
import com.example.travelManager.domain.request.hotel.BookingRequest;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookedRoomServiceImpl implements IBookedRoomService {

    private final BookedRoomRepository bookedRoomRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public String bookRoom(Long hotelId, Long roomId, BookingRequest request, String currentUserEmail) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (room.getHotel() == null || !room.getHotel().getId().equals(hotelId)) {
            throw new IllegalArgumentException("Room " + roomId + " does not belong to hotel " + hotelId);
        }

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new IllegalStateException("Phòng đang bảo trì, không thể đặt");
        }

        // Sức chứa phòng: nhà hàng và tour đều có kiểm tra tương ứng (capacity / decrementSlot),
        // riêng khách sạn thì trước đây không kiểm — đặt phòng 2 người cho 10 khách vẫn qua.
        int soKhach = request.getNumOfAdults() + request.getNumOfChildren();
        if (room.getMaxGuests() > 0 && soKhach > room.getMaxGuests()) {
            throw new IllegalArgumentException(
                    "Phòng này chỉ ở tối đa " + room.getMaxGuests() + " khách, bạn đang đặt cho "
                            + soKhach + " khách. Vui lòng chọn phòng khác hoặc đặt thêm phòng.");
        }

        // Check-in lúc 14:00 — phải đặt trước ít nhất 12 tiếng (trước 02:00 cùng ngày)
        LocalDateTime checkInAt14 = request.getCheckInDate().atTime(14, 0);
        LocalDateTime bookingDeadline = checkInAt14.minusHours(12); // = 02:00 cùng ngày check-in
        if (LocalDateTime.now().isAfter(bookingDeadline)) {
            throw new IllegalStateException(
                "Chỉ được đặt phòng trước ít nhất 12 tiếng so với giờ nhận phòng (14:00). " +
                "Hạn đặt phòng cho ngày " + request.getCheckInDate() + " đã kết thúc lúc 02:00 cùng ngày."
            );
        }

        List<BookedRoom> conflicts = bookedRoomRepository
                .findByRoom_IdAndStatusNotAndCheckOutDateAfterAndCheckInDateBefore(
                        roomId, HotelBookingStatus.CANCELLED,
                        request.getCheckInDate(), request.getCheckOutDate());
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Phòng đã được đặt cho khoảng thời gian này. Vui lòng chọn ngày khác.");
        }

        BookedRoom booking = new BookedRoom();
        // Chủ sở hữu thật = người đang đăng nhập. guestEmail bên dưới do client tự khai
        // (có thể là email người thân khi đặt hộ) nên KHÔNG dùng để xét quyền.
        if (currentUserEmail != null && !currentUserEmail.isBlank()) {
            userRepository.findByEmail(currentUserEmail).ifPresent(booking::setUser);
        }
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setGuestFullName(request.getGuestFullName());
        booking.setGuestEmail(request.getGuestEmail());
        booking.setNumOfAdults(request.getNumOfAdults());
        booking.setNumOfChildren(request.getNumOfChildren());
        booking.setTotalPrice(calculateTotalPrice(room, request.getCheckInDate(), request.getCheckOutDate()));

        room.addBooking(booking);
        roomRepository.save(room);

        return booking.getBookingConfirmationCode();
    }

    /**
     * Chốt giá tại thời điểm đặt: giá phòng × số đêm (tối thiểu 1 đêm).
     * Dùng chung với nhánh fallback ở PaymentController cho booking cũ chưa có total_price.
     */
    public static java.math.BigDecimal calculateTotalPrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        if (room == null || room.getRoomPrice() == null || checkIn == null || checkOut == null) {
            return null;
        }
        long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut));
        return room.getRoomPrice().multiply(java.math.BigDecimal.valueOf(nights));
    }

    @Override
    public BookedRoom findByConfirmationCode(String confirmationCode) {
        return bookedRoomRepository.findByBookingConfirmationCode(confirmationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with code: " + confirmationCode));
    }

    @Override
    public List<BookedRoom> getAllBookings() {
        return bookedRoomRepository.findAll();
    }

    @Override
    public List<BookedRoom> getAllBookingsByRoomId(Long roomId) {
        return bookedRoomRepository.findByRoom_Id(roomId);
    }

    @Override
    public List<BookedRoom> getAllBookingsByRoomIds(List<Long> roomIds) {
        return bookedRoomRepository.findByRoom_IdIn(roomIds);
    }

    @Override
    public List<BookedRoom> getAllBookingsByHotelId(Long hotelId) {
        return bookedRoomRepository.findByRoom_Hotel_Id(hotelId);
    }

    @Override
    public List<BookedRoom> getBookingsByGuestEmail(String email) {
        return bookedRoomRepository.findByGuestEmail(email);
    }

    @Override
    @Transactional
    public void cancelBooking(Long bookingId) {
        BookedRoom booking = bookedRoomRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == HotelBookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking đã được hủy trước đó");
        }

        // Huỷ MỀM, không xoá bản ghi. Cách cũ gọi deleteById() nên mất sạch lịch sử đặt phòng,
        // và nếu booking đã thanh toán thì bản ghi payments trỏ tới booking_id này thành mồ côi,
        // đối soát tiền không ra. Đây cũng là cách module tour đang làm (TourBookingController.cancel).
        // Việc kiểm tra trùng lịch đã loại CANCELLED sẵn nên phòng vẫn được giải phóng đúng.
        booking.setStatus(HotelBookingStatus.CANCELLED);
        bookedRoomRepository.save(booking);

        Room room = booking.getRoom();
        boolean conBookingKhac = bookedRoomRepository
                .existsByRoom_IdAndStatusNot(room.getId(), HotelBookingStatus.CANCELLED);
        if (!conBookingKhac) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(room);
        }
    }
}
