package com.example.travelManager.domain.restaurant;

import java.math.BigDecimal;
import java.sql.Blob;

import com.example.travelManager.util.constant.restaurant.MenuCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một món trong thực đơn của nhà hàng.
 *
 * Trang chi tiết nhà hàng đã dựng sẵn tab "Thực đơn" và MenuItemCard từ trước,
 * nhưng phía backend chưa hề có bảng nào chứa món — nên tab đó luôn rỗng.
 * Entity này lấp đúng chỗ trống đó; các trường bám theo interface MenuItem mà
 * RestaurantDetailPage.tsx đang mong đợi.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MenuCategory category;

    @Lob
    private Blob photo;

    /** Món bán chạy — FE hiện nhãn đỏ "Bán chạy". */
    private boolean bestSeller = false;

    /** Món mới — FE hiện nhãn xanh "Mới". */
    private boolean newItem = false;

    /** Tạm hết món thì ẩn khỏi thực đơn mà không phải xoá dữ liệu. */
    private boolean available = true;

    /** Thứ tự hiển thị trong cùng một nhóm món. */
    private int sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
}
