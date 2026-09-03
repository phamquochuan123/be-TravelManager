package com.example.travelManager.service.hotel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.hotel.BookedRoom;
import com.example.travelManager.domain.hotel.Hotel;
import com.example.travelManager.domain.hotel.Room;
import com.example.travelManager.domain.request.hotel.BookingRequest;
import com.example.travelManager.repository.UserRepository;
import com.example.travelManager.repository.hotel.BookedRoomRepository;
import com.example.travelManager.repository.hotel.RoomRepository;
import com.example.travelManager.util.constant.hotel.RoomStatus;

/**
 * Test cho luồng đặt phòng — nơi từng có 2 lỗi thật:
 * booking không có chủ sở hữu (quyền dựa trên guestEmail do client tự khai),
 * và phòng bị đặt trùng khoảng thời gian.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookedRoomServiceImplTest {

    private static final long HOTEL_ID = 1L;
    private static final long ROOM_ID = 10L;

    @Mock private BookedRoomRepository bookedRoomRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private BookedRoomServiceImpl service;

    private Room room;
    private UserEntity currentUser;

    @BeforeEach
    void setUp() {
        Hotel hotel = new Hotel();
        hotel.setId(HOTEL_ID);

        room = new Room();
        room.setId(ROOM_ID);
        room.setHotel(hotel);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setRoomPrice(BigDecimal.valueOf(500_000));

        currentUser = new UserEntity();
        currentUser.setId(7L);
        currentUser.setEmail("nguoidat@example.com");

        when(roomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(userRepository.findByEmail("nguoidat@example.com")).thenReturn(Optional.of(currentUser));
        when(bookedRoomRepository
                .findByRoom_IdAndStatusNotAndCheckOutDateAfterAndCheckInDateBefore(
                        anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }

    /** Ngày đặt đủ xa để không vướng hạn "phải đặt trước 12 tiếng". */
    private BookingRequest validRequest() {
        BookingRequest request = new BookingRequest();
        request.setCheckInDate(LocalDate.now().plusDays(10));
        request.setCheckOutDate(LocalDate.now().plusDays(12));
        request.setGuestFullName("Nguyen Van A");
        request.setGuestEmail("nguoikhac@example.com"); // cố tình khác email đăng nhập
        request.setNumOfAdults(2);
        request.setNumOfChildren(0);
        return request;
    }

    @Test
    @DisplayName("Booking phải gắn user đang đăng nhập, không phụ thuộc guestEmail client gửi")
    void bookRoom_gansChuSoHuuLaUserDangDangNhap() {
        service.bookRoom(HOTEL_ID, ROOM_ID, validRequest(), "nguoidat@example.com");

        BookedRoom saved = room.getBookings().get(room.getBookings().size() - 1);
        assertThat(saved.getUser()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(currentUser.getId());
        // guestEmail vẫn giữ nguyên giá trị client khai (cho phép đặt hộ người thân),
        // nhưng nó KHÔNG còn là căn cứ xác định quyền sở hữu.
        assertThat(saved.getGuestEmail()).isEqualTo("nguoikhac@example.com");
    }

    @Test
    @DisplayName("Phòng không thuộc khách sạn trong URL thì phải bị từ chối")
    void bookRoom_tuChoiKhiPhongKhongThuocKhachSan() {
        assertThatThrownBy(() -> service.bookRoom(999L, ROOM_ID, validRequest(), "nguoidat@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to hotel");

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Phòng đang bảo trì thì không đặt được")
    void bookRoom_tuChoiKhiPhongDangBaoTri() {
        room.setStatus(RoomStatus.MAINTENANCE);

        assertThatThrownBy(() -> service.bookRoom(HOTEL_ID, ROOM_ID, validRequest(), "nguoidat@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bảo trì");
    }

    @Test
    @DisplayName("Trùng khoảng thời gian với booking khác thì không đặt được (chống double booking)")
    void bookRoom_tuChoiKhiTrungKhoangThoiGian() {
        BookedRoom daCo = new BookedRoom();
        daCo.setCheckInDate(LocalDate.now().plusDays(11));
        daCo.setCheckOutDate(LocalDate.now().plusDays(13));
        when(bookedRoomRepository
                .findByRoom_IdAndStatusNotAndCheckOutDateAfterAndCheckInDateBefore(
                        anyLong(), any(), any(), any()))
                .thenReturn(List.of(daCo));

        assertThatThrownBy(() -> service.bookRoom(HOTEL_ID, ROOM_ID, validRequest(), "nguoidat@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã được đặt");

        verify(roomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đặt phòng luôn dùng khoá bi quan findByIdForUpdate, không dùng findById thường")
    void bookRoom_dungKhoaBiQuan() {
        service.bookRoom(HOTEL_ID, ROOM_ID, validRequest(), "nguoidat@example.com");

        verify(roomRepository).findByIdForUpdate(ROOM_ID);
        verify(roomRepository, never()).findById(ROOM_ID);
    }
}
