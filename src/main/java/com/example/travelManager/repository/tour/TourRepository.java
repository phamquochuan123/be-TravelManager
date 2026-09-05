package com.example.travelManager.repository.tour;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.util.constant.tour.TourRecurrenceType;
import com.example.travelManager.util.constant.tour.TourStatus;

public interface TourRepository extends JpaRepository<Tour, Long>, JpaSpecificationExecutor<Tour> {

    List<Tour> findAllByDeletedFalseAndStatus(TourStatus status);

    List<Tour> findAllByDeletedFalse();

    /**
     * Các tour đang bán có khai báo mẫu lặp — đầu vào của TourDepartureScheduler.
     * Dùng IN thay vì "khác NONE" để tour có recurrence_type NULL (dữ liệu cũ)
     * cũng bị loại, vì NULL không bao giờ khớp IN.
     */
    List<Tour> findAllByDeletedFalseAndStatusAndRecurrenceTypeIn(
            TourStatus status, Collection<TourRecurrenceType> types);
}
