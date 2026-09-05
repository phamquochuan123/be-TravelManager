package com.example.travelManager.util.constant.restaurant;

/**
 * Nhóm món trong thực đơn.
 *
 * Bốn giá trị này phải khớp đúng MENU_TABS ở RestaurantDetailPage.tsx — trang chi
 * tiết lọc món bằng `m.category === tab.key`, thêm giá trị lạ là món đó không rơi
 * vào tab nào và biến mất khỏi giao diện mà không báo lỗi.
 */
public enum MenuCategory {
    APPETIZER("Khai vị"),
    MAIN("Món chính"),
    DESSERT("Tráng miệng"),
    DRINK("Đồ uống");

    /**
     * Nhãn tiếng Việt mà TRANG QUẢN TRỊ dùng làm tên nhóm.
     *
     * Hai giao diện gọi nhóm món bằng hai thứ tiếng khác nhau: AdminRestaurantsPage.tsx
     * gửi lên MENU_CATS = ['Khai vị','Món chính','Tráng miệng','Đồ uống'], còn
     * RestaurantDetailPage.tsx lọc theo mã enum. Ánh xạ phải nằm đúng một chỗ ở đây —
     * dịch sai một nhãn là món lưu vào nhóm không tồn tại rồi biến mất khỏi trang khách
     * mà không có lỗi nào.
     */
    private final String nhanTiengViet;

    MenuCategory(String nhanTiengViet) {
        this.nhanTiengViet = nhanTiengViet;
    }

    public String nhanTiengViet() {
        return nhanTiengViet;
    }

    /** Đọc nhóm từ nhãn tiếng Việt của trang quản trị, hoặc từ chính tên enum. */
    public static MenuCategory tuNhan(String nhan) {
        if (nhan == null || nhan.isBlank()) return null;
        String s = nhan.trim();
        for (MenuCategory c : values()) {
            if (c.nhanTiengViet.equalsIgnoreCase(s) || c.name().equalsIgnoreCase(s)) return c;
        }
        return null;
    }
}
