package com.example.travelManager.service.tour;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.domain.tour.TourDeparture;
import com.example.travelManager.repository.tour.TourDepartureRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.tour.TourDepartureStatus;
import com.example.travelManager.util.constant.tour.TourRecurrenceType;
import com.example.travelManager.util.constant.tour.TourStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Giữ cho lịch khởi hành luôn "sống": trạng thái chạy theo ngày hôm nay, và
 * chuyến mới được sinh trước nhiều tháng theo mẫu lặp của từng tour.
 *
 * Không có 2 job này thì lịch chỉ đúng tại thời điểm nhập: chuyến cũ đi qua vẫn
 * nằm nguyên trạng thái SCHEDULED và vẫn đặt được, còn khi hết chuyến cuối thì
 * trang chi tiết hiện "Chưa có lịch khởi hành" cho tới khi admin nhập tay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TourDepartureScheduler {

    /** Còn bấy nhiêu ngày nữa là đi thì chuyến chuyển sang UPCOMING. */
    private static final int NGAY_BAO_SAP_KHOI_HANH = 7;

    /** Dùng khi tour không khai báo monthsAhead. */
    private static final int SO_THANG_GIU_LICH_MAC_DINH = 3;

    /** Chặn trên cho mỗi tour mỗi lần chạy — cấu hình sai không làm ngập bảng. */
    private static final int TOI_DA_CHUYEN_MOI_MOI_LAN = 60;

    /** Trạng thái đã là điểm dừng, job không đụng tới nữa. */
    private static final Collection<TourDepartureStatus> TRANG_THAI_KET_THUC =
            EnumSet.of(TourDepartureStatus.COMPLETED, TourDepartureStatus.CANCELLED);

    private final TourRepository tourRepository;
    private final TourDepartureRepository departureRepository;

    // ── Job 1: trạng thái chạy theo ngày ─────────────────────────

    /**
     * Mỗi giờ soát lại trạng thái các chuyến chưa kết thúc.
     *
     * Chạy theo giờ chứ không theo ngày để sau khi khởi động lại app hoặc đổi
     * dữ liệu bằng SQL thì trạng thái tự khớp lại trong vòng một tiếng, không
     * phải chờ tới đợt chạy đêm.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    @Transactional
    public int capNhatTrangThaiChuyen() {
        LocalDate homNay = LocalDate.now();
        List<TourDeparture> canSoat = departureRepository.findChuyenChuaKetThuc(TRANG_THAI_KET_THUC);

        List<TourDeparture> daDoi = new ArrayList<>();
        for (TourDeparture chuyen : canSoat) {
            TourDepartureStatus moi = suyRaTrangThai(chuyen, homNay);
            if (moi != null && moi != chuyen.getStatus()) {
                chuyen.setStatus(moi);
                daDoi.add(chuyen);
            }
        }

        if (daDoi.isEmpty()) return 0;
        departureRepository.saveAll(daDoi);
        log.info("Cập nhật trạng thái {} chuyến khởi hành theo ngày {}", daDoi.size(), homNay);
        return daDoi.size();
    }

    /**
     * Trạng thái đúng ra phải có của một chuyến tại ngày {@code homNay},
     * hoặc null nếu không nên đụng vào trạng thái hiện tại.
     */
    private TourDepartureStatus suyRaTrangThai(TourDeparture chuyen, LocalDate homNay) {
        LocalDate ngayDi = chuyen.getDepartureDate();
        if (ngayDi == null) return null;

        Tour tour = chuyen.getTour();
        int soNgay = (tour != null && tour.getDurationDays() > 0) ? tour.getDurationDays() : 1;
        LocalDate ngayVe = ngayDi.plusDays(soNgay - 1L);

        TourDepartureStatus hienTai = chuyen.getStatus();

        if (homNay.isAfter(ngayVe))  return TourDepartureStatus.COMPLETED;

        if (!homNay.isBefore(ngayDi)) {
            // Đang trong hành trình. IN_PROGRESS là cách gọi khác của ONGOING —
            // admin đã đặt thì giữ nguyên, đổi qua lại chỉ gây nhiễu lịch sử.
            return hienTai == TourDepartureStatus.IN_PROGRESS ? null : TourDepartureStatus.ONGOING;
        }

        // Chưa tới ngày đi. CONFIRMED là quyết định của admin (chốt chuyến chắc
        // chắn chạy) nên không được hạ xuống UPCOMING/SCHEDULED.
        if (hienTai == TourDepartureStatus.CONFIRMED) return null;

        return trangThaiChuaKhoiHanh(ngayDi, homNay);
    }

    /**
     * SCHEDULED hay UPCOMING cho một chuyến chưa tới ngày đi.
     *
     * Dùng chung cho cả lúc sinh chuyến mới lẫn lúc soát trạng thái — viết mốc
     * "còn 7 ngày" ở hai nơi thì chỉ cần lệch một dấu bằng là chuyến vừa tạo đã
     * sai trạng thái, rồi bị job ghi lại ngay lượt sau.
     */
    private TourDepartureStatus trangThaiChuaKhoiHanh(LocalDate ngayDi, LocalDate homNay) {
        return homNay.isBefore(ngayDi.minusDays(NGAY_BAO_SAP_KHOI_HANH))
                ? TourDepartureStatus.SCHEDULED
                : TourDepartureStatus.UPCOMING;
    }

    // ── Job 2: tự sinh chuyến mới theo mẫu lặp ───────────────────

    /**
     * Mỗi ngày một lần, bù đủ lịch cho mọi tour có mẫu lặp: thiếu ngày nào trong
     * cửa sổ tương lai thì tạo ngày đó, nên lịch không bao giờ cạn.
     *
     * Dùng fixedDelay chứ không cron để Spring chạy luôn một lượt lúc khởi động:
     * job chỉ tạo ngày còn thiếu nên chạy thừa là vô hại, đổi lại hệ thống vừa
     * deploy hay vừa restore DB là có lịch ngay, không phải chờ qua đêm.
     */
    @Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
    @Transactional
    public int sinhLichKhoiHanhTheoMau() {
        LocalDate homNay = LocalDate.now();
        List<Tour> tours = tourRepository.findAllByDeletedFalseAndStatusAndRecurrenceTypeIn(
                TourStatus.ACTIVE, EnumSet.of(TourRecurrenceType.WEEKLY, TourRecurrenceType.MONTHLY));

        int tongTao = 0;
        for (Tour tour : tours) {
            int soThang = (tour.getMonthsAhead() != null && tour.getMonthsAhead() > 0)
                    ? tour.getMonthsAhead()
                    : SO_THANG_GIU_LICH_MAC_DINH;

            // Bắt đầu từ ngày mai: chuyến khởi hành ngay hôm nay thì khách
            // không kịp đặt, mà tạo ra lại bị job trạng thái đẩy sang ONGOING.
            LocalDate tuNgay = homNay.plusDays(1);
            LocalDate denNgay = homNay.plusMonths(soThang);

            List<LocalDate> ungVien = tinhNgayTheoMau(tour, tuNgay, denNgay);
            if (ungVien.isEmpty()) continue;

            Set<LocalDate> daCo = new HashSet<>(
                    departureRepository.findNgayKhoiHanhTuNgay(tour.getId(), tuNgay));

            int sucChua = tour.getMaxSlots() > 0 ? tour.getMaxSlots() : 20;
            List<TourDeparture> canTao = new ArrayList<>();
            for (LocalDate ngay : ungVien) {
                if (daCo.contains(ngay)) continue;
                if (canTao.size() >= TOI_DA_CHUYEN_MOI_MOI_LAN) break;

                TourDeparture chuyen = new TourDeparture();
                chuyen.setTour(tour);
                chuyen.setDepartureDate(ngay);
                chuyen.setMaxSlots(sucChua);
                chuyen.setAvailableSlots(sucChua);
                chuyen.setStatus(trangThaiChuaKhoiHanh(ngay, homNay));
                canTao.add(chuyen);
            }

            if (canTao.isEmpty()) continue;
            departureRepository.saveAll(canTao);
            tongTao += canTao.size();
            log.info("Tour {} ({}): sinh thêm {} chuyến tới {}",
                    tour.getId(), tour.getName(), canTao.size(), denNgay);
        }

        if (tongTao > 0) {
            log.info("Đã sinh tổng cộng {} chuyến khởi hành cho {} tour có lịch lặp",
                    tongTao, tours.size());
        }
        return tongTao;
    }

    /** Mọi ngày khớp mẫu lặp của tour trong khoảng [tuNgay, denNgay]. */
    private List<LocalDate> tinhNgayTheoMau(Tour tour, LocalDate tuNgay, LocalDate denNgay) {
        String cauHinh = tour.getRecurrenceDays();
        if (cauHinh == null || cauHinh.isBlank()) {
            log.warn("Tour {} bật lịch lặp {} nhưng recurrenceDays rỗng — bỏ qua",
                    tour.getId(), tour.getRecurrenceType());
            return List.of();
        }

        return switch (tour.getRecurrenceType()) {
            case WEEKLY  -> ngayTheoTuan(tour, cauHinh, tuNgay, denNgay);
            case MONTHLY -> ngayTheoThang(tour, cauHinh, tuNgay, denNgay);
            case NONE    -> List.of();
        };
    }

    private List<LocalDate> ngayTheoTuan(Tour tour, String cauHinh,
                                         LocalDate tuNgay, LocalDate denNgay) {
        Set<DayOfWeek> thu = EnumSet.noneOf(DayOfWeek.class);
        for (String phan : cauHinh.split(",")) {
            DayOfWeek d = docThu(phan.trim());
            if (d != null) thu.add(d);
            else log.warn("Tour {}: thứ '{}' không hợp lệ trong recurrenceDays", tour.getId(), phan);
        }
        if (thu.isEmpty()) return List.of();

        List<LocalDate> ketQua = new ArrayList<>();
        for (LocalDate ngay = tuNgay; !ngay.isAfter(denNgay); ngay = ngay.plusDays(1)) {
            if (thu.contains(ngay.getDayOfWeek())) ketQua.add(ngay);
        }
        return ketQua;
    }

    private List<LocalDate> ngayTheoThang(Tour tour, String cauHinh,
                                          LocalDate tuNgay, LocalDate denNgay) {
        Set<Integer> ngayTrongThang = new HashSet<>();
        for (String phan : cauHinh.split(",")) {
            try {
                int n = Integer.parseInt(phan.trim());
                if (n >= 1 && n <= 31) ngayTrongThang.add(n);
                else log.warn("Tour {}: ngày {} ngoài khoảng 1-31", tour.getId(), n);
            } catch (NumberFormatException e) {
                log.warn("Tour {}: '{}' không phải số ngày hợp lệ", tour.getId(), phan);
            }
        }
        if (ngayTrongThang.isEmpty()) return List.of();

        List<LocalDate> ketQua = new ArrayList<>();
        YearMonth thang = YearMonth.from(tuNgay);
        YearMonth thangCuoi = YearMonth.from(denNgay);
        while (!thang.isAfter(thangCuoi)) {
            for (int n : ngayTrongThang) {
                // Tháng ngắn không có ngày này thì bỏ qua, không dồn về ngày cuối
                // tháng — dồn sẽ đẻ ra chuyến 28/02 mà admin không hề khai báo.
                if (n > thang.lengthOfMonth()) continue;
                LocalDate ngay = thang.atDay(n);
                if (!ngay.isBefore(tuNgay) && !ngay.isAfter(denNgay)) ketQua.add(ngay);
            }
            thang = thang.plusMonths(1);
        }
        ketQua.sort(null);
        return ketQua;
    }

    private DayOfWeek docThu(String s) {
        return switch (s.toUpperCase()) {
            case "MON", "MONDAY", "T2"    -> DayOfWeek.MONDAY;
            case "TUE", "TUESDAY", "T3"   -> DayOfWeek.TUESDAY;
            case "WED", "WEDNESDAY", "T4" -> DayOfWeek.WEDNESDAY;
            case "THU", "THURSDAY", "T5"  -> DayOfWeek.THURSDAY;
            case "FRI", "FRIDAY", "T6"    -> DayOfWeek.FRIDAY;
            case "SAT", "SATURDAY", "T7"  -> DayOfWeek.SATURDAY;
            case "SUN", "SUNDAY", "CN"    -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }
}
