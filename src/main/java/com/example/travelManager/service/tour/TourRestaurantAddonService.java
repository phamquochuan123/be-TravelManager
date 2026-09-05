package com.example.travelManager.service.tour;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.travelManager.domain.UserEntity;
import com.example.travelManager.domain.request.tour.RestaurantAddonRequest;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.util.constant.restaurant.MealSlot;
import com.example.travelManager.util.constant.restaurant.RestaurantBookingStatus;

import lombok.RequiredArgsConstructor;

/**
 * Dựng các bữa ăn kèm theo một đơn tour.
 *
 * Tồn tại để luật đặt bữa chỉ nằm ĐÚNG MỘT CHỖ. Trước đây logic đặt nhà hàng bị chép
 * làm hai bản gần giống nhau — TourBookingController.book (đường đang chạy thật) và
 * TourService.bookTour — nên mọi thay đổi luật đều phải nhớ sửa cả hai, và quên một
 * bên thì hai đường tạo đơn cho ra kết quả khác nhau mà không có gì báo.
 */
@Service
@RequiredArgsConstructor
public class TourRestaurantAddonService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;

    /** Các bữa đã dựng kèm tổng tiền, để nơi gọi cộng vào giá đơn. */
    public record KetQua(List<RestaurantBooking> buaAn, BigDecimal tongTien) {}

    /**
     * Kiểm tra và dựng danh sách bữa ăn. Chưa lưu — nơi gọi gắn vào TourBooking rồi
     * lưu một lượt để cascade lo phần ghi xuống DB.
     *
     * @param khoaRowNhaHang khoá bi quan row nhà hàng trước khi đếm chỗ. Bật ở đường
     *                       đặt tour thật để hai khách đặt cùng lúc không cùng lọt qua
     *                       phép kiểm sức chứa; tắt ở nơi đã nằm trong giao dịch khác.
     */
    public KetQua dungCacBua(List<RestaurantAddonRequest> yeuCau, Tour tour, LocalDate ngayKhoiHanh,
                             TourBooking donTour, UserEntity user, int tongKhach,
                             String contactName, String contactPhone, String contactEmail,
                             RestaurantBookingStatus trangThai, boolean khoaRowNhaHang) {

        List<RestaurantBooking> ketQua = new ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;
        if (yeuCau == null || yeuCau.isEmpty()) return new KetQua(ketQua, tongTien);

        // Ngày cuối của tour. durationDays đếm cả ngày khởi hành nên tour 3 ngày đi từ
        // 21 sẽ kết thúc ngày 23, phải trừ 1. Tour thiếu durationDays (dữ liệu cũ) thì
        // coi như đi trong ngày.
        int soNgay = tour != null && tour.getDurationDays() > 0 ? tour.getDurationDays() : 1;
        LocalDate ngayCuoi = ngayKhoiHanh.plusDays(soNgay - 1L);

        // Khung bữa đã bị chiếm, theo từng ngày. Đây là ràng buộc về lịch của KHÁCH,
        // khác hẳn phép kiểm sức chứa nhà hàng bên dưới: một người không thể ăn trưa ở
        // hai nơi cùng lúc, kể cả khi cả hai nhà hàng đều còn chỗ.
        Map<LocalDate, Map<MealSlot, String>> daChiem = new HashMap<>();

        for (RestaurantAddonRequest r : yeuCau) {
            Restaurant nhaHang = restaurantRepository.findById(r.getRestaurantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Restaurant not found: " + r.getRestaurantId()));

            if (r.getBookingDate() == null || r.getBookingTime() == null) {
                throw new IllegalArgumentException(
                        "Thiếu ngày hoặc giờ đặt bàn cho nhà hàng \"" + nhaHang.getName() + "\"");
            }

            LocalDate ngay = r.getBookingDate();
            if (ngay.isBefore(ngayKhoiHanh) || ngay.isAfter(ngayCuoi)) {
                throw new IllegalArgumentException(String.format(
                        "Ngày đặt bàn ở \"%s\" (%s) nằm ngoài thời gian tour (%s đến %s)",
                        nhaHang.getName(), ngay, ngayKhoiHanh, ngayCuoi));
            }

            MealSlot khung = MealSlot.cuaGio(r.getBookingTime());
            if (khung == null) {
                throw new IllegalArgumentException(String.format(
                        "Giờ đặt bàn ở \"%s\" (%s) không thuộc bữa nào. Nhận đặt từ %s đến %s.",
                        nhaHang.getName(), r.getBookingTime(),
                        MealSlot.SANG.batDau(), MealSlot.TOI.ketThuc()));
            }

            Map<MealSlot, String> khungTrongNgay = daChiem.computeIfAbsent(ngay, k -> new HashMap<>());
            String daDat = khungTrongNgay.get(khung);
            if (daDat != null) {
                throw new IllegalArgumentException(String.format(
                        "Ngày %s bữa %s đã đặt ở \"%s\" rồi. Mỗi bữa chỉ đặt được một nhà hàng — "
                                + "hãy chọn bữa khác hoặc ngày khác cho \"%s\".",
                        ngay, khung.nhan(), daDat, nhaHang.getName()));
            }
            khungTrongNgay.put(khung, nhaHang.getName());

            if (nhaHang.getCapacity() != null) {
                if (khoaRowNhaHang) {
                    // Khoá row nhà hàng — cùng lý do như RestaurantController.book
                    restaurantRepository.findByIdForUpdate(nhaHang.getId());
                }
                int daCo = restaurantBookingRepository.sumGuestCountByRestaurantAndDateTime(
                        nhaHang.getId(), ngay, r.getBookingTime(), RestaurantBookingStatus.CANCELLED);
                if (daCo + tongKhach > nhaHang.getCapacity()) {
                    throw new IllegalStateException(
                            "Nhà hàng \"" + nhaHang.getName() + "\" không đủ chỗ trống vào khung giờ này");
                }
            }

            if (nhaHang.getPricePerPerson() != null) {
                tongTien = tongTien.add(
                        nhaHang.getPricePerPerson().multiply(BigDecimal.valueOf(tongKhach)));
            }

            RestaurantBooking bua = new RestaurantBooking();
            bua.setRestaurant(nhaHang);
            bua.setUser(user);
            bua.setTourBooking(donTour);
            bua.setBookingDate(ngay);
            bua.setBookingTime(r.getBookingTime());
            bua.setGuestCount(tongKhach);
            bua.setContactName(contactName);
            bua.setContactPhone(contactPhone);
            bua.setContactEmail(contactEmail);
            bua.setStatus(trangThai);
            bua.setConfirmationCode(org.apache.commons.lang3.RandomStringUtils.randomNumeric(10));
            ketQua.add(bua);
        }

        return new KetQua(ketQua, tongTien);
    }
}
