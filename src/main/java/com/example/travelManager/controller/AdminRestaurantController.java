package com.example.travelManager.controller;

import com.example.travelManager.domain.restaurant.MenuItem;
import com.example.travelManager.domain.restaurant.Restaurant;
import com.example.travelManager.exception.ResourceNotFoundException;
import com.example.travelManager.repository.restaurant.MenuItemRepository;
import com.example.travelManager.repository.restaurant.RestaurantRepository;
import com.example.travelManager.util.InputValidator;
import com.example.travelManager.util.constant.restaurant.MenuCategory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.math.BigDecimal;
import java.sql.Blob;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/restaurants")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final com.example.travelManager.service.NominatimGeocodingService geocodingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "status", required = false) String status) {

        List<RestaurantItem> items = restaurantRepository.findAll().stream()
                .filter(r -> search == null || search.isBlank()
                        || r.getName().toLowerCase().contains(search.toLowerCase())
                        || (r.getAddress() != null && r.getAddress().toLowerCase().contains(search.toLowerCase())))
                .filter(r -> status == null || "all".equalsIgnoreCase(status)
                        || ("ACTIVE".equalsIgnoreCase(status) && r.isActive())
                        || ("INACTIVE".equalsIgnoreCase(status) && !r.isActive()))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .map(this::toItem)
                .collect(Collectors.toList());

        int total = items.size();
        int fromIdx = Math.min(page * size, total);
        int toIdx = Math.min(fromIdx + size, total);

        Map<String, Object> result = new HashMap<>();
        result.put("content", items.subList(fromIdx, toIdx));
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("totalElements", total);
        return ResponseEntity.ok(result);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<RestaurantItem> create(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address", required = false, defaultValue = "") String address,
            @RequestParam(name = "city", required = false, defaultValue = "") String city,
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude,
            @RequestParam(name = "cuisineType", required = false, defaultValue = "") String cuisineType,
            @RequestParam(name = "openTime", required = false, defaultValue = "08:00") String openTime,
            @RequestParam(name = "closeTime", required = false, defaultValue = "22:00") String closeTime,
            @RequestParam(name = "maxTables", defaultValue = "20") int maxTables,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "menuCategories", required = false) String menuCategories,
            @RequestParam(name = "images", required = false) List<MultipartFile> images,
            org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest) throws Exception {

        Restaurant restaurant = new Restaurant();
        apDungThongTin(restaurant, name, address, city, openTime, closeTime, maxTables, description);
        restaurant.setLatitude(latitude);
        restaurant.setLongitude(longitude);
        restaurant.setActive(true);
        if (cuisineType != null && !cuisineType.isBlank()) {
            try {
                restaurant.setCuisineType(
                    com.example.travelManager.util.constant.restaurant.CuisineType.valueOf(cuisineType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            restaurant.setPhoto(new SerialBlob(images.get(0).getBytes()));
        }
        boSungToaDoNeuThieu(restaurant);

        Restaurant daLuu = restaurantRepository.save(restaurant);
        luuThucDon(daLuu, menuCategories, multipartRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toItem(daLuu));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<RestaurantItem> update(
            @PathVariable("id") Long id,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "address", required = false, defaultValue = "") String address,
            @RequestParam(name = "city", required = false, defaultValue = "") String city,
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude,
            @RequestParam(name = "cuisineType", required = false, defaultValue = "") String cuisineType,
            @RequestParam(name = "openTime", required = false, defaultValue = "08:00") String openTime,
            @RequestParam(name = "closeTime", required = false, defaultValue = "22:00") String closeTime,
            @RequestParam(name = "maxTables", defaultValue = "20") int maxTables,
            @RequestParam(name = "description", required = false, defaultValue = "") String description,
            @RequestParam(name = "menuCategories", required = false) String menuCategories,
            @RequestParam(name = "images", required = false) List<MultipartFile> images,
            org.springframework.web.multipart.MultipartHttpServletRequest multipartRequest) throws Exception {

        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
        apDungThongTin(restaurant, name, address, city, openTime, closeTime, maxTables, description);
        if (latitude != null) restaurant.setLatitude(latitude);
        if (longitude != null) restaurant.setLongitude(longitude);
        if (cuisineType != null && !cuisineType.isBlank()) {
            try {
                restaurant.setCuisineType(
                    com.example.travelManager.util.constant.restaurant.CuisineType.valueOf(cuisineType.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (images != null && !images.isEmpty() && !images.get(0).isEmpty()) {
            restaurant.setPhoto(new SerialBlob(images.get(0).getBytes()));
        }

        boSungToaDoNeuThieu(restaurant);

        Restaurant daLuu = restaurantRepository.save(restaurant);
        luuThucDon(daLuu, menuCategories, multipartRequest);
        return ResponseEntity.ok(toItem(daLuu));
    }

    /** Gán thông tin cơ bản sau khi kiểm tra — dùng chung cho tạo mới và sửa. */
    private void apDungThongTin(Restaurant restaurant, String name, String address, String city,
                                String openTime, String closeTime, int maxTables, String description) {
        restaurant.setName(InputValidator.ten(name, "Tên nhà hàng"));
        restaurant.setAddress(InputValidator.tuyChon(address, "Địa chỉ"));
        restaurant.setCity(InputValidator.batBuoc(city, "Thành phố"));
        restaurant.setCapacity(InputValidator.trongKhoang(maxTables, 1, 100_000, "Sức chứa"));
        restaurant.setDescription(InputValidator.tuyChon(description, "Mô tả", InputValidator.DAI_TOI_DA_MO_TA));
        restaurant.setOpeningHours(gioMoCua(openTime, closeTime));
    }

    /**
     * Ghép giờ mở cửa và chặn khoảng giờ vô nghĩa.
     *
     * Không kiểm thì "23:00-06:00" lưu được, mà RestaurantController lại dùng chuỗi
     * này để xét khách đặt bàn có nằm trong giờ hay không — khoảng giờ ngược làm
     * mọi khung giờ đều bị coi là ngoài giờ mở cửa.
     */
    private String gioMoCua(String openTime, String closeTime) {
        java.time.LocalTime mo, dong;
        try {
            // Chấp nhận khoảng trắng thừa: dữ liệu cũ lưu "10:00 - 22:00".
            mo = java.time.LocalTime.parse(openTime.trim());
            dong = java.time.LocalTime.parse(closeTime.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Giờ mở/đóng cửa phải theo định dạng HH:mm");
        }
        if (!dong.isAfter(mo)) {
            throw new IllegalArgumentException(
                    "Giờ đóng cửa (" + closeTime + ") phải sau giờ mở cửa (" + openTime + ")");
        }
        // Ghi lại dạng chuẩn "HH:mm-HH:mm" để lần đọc sau không phải đoán khoảng trắng nữa.
        return mo + "-" + dong;
    }

    /** Cùng lý do như AdminHotelController.boSungToaDoNeuThieu — picker Google đang 403. */
    private void boSungToaDoNeuThieu(Restaurant restaurant) {
        if (restaurant.getLatitude() != null && restaurant.getLongitude() != null) return;
        geocodingService.tim(restaurant.getName(), restaurant.getAddress(), restaurant.getCity())
                .ifPresent(toaDo -> {
                    restaurant.setLatitude(toaDo.lat());
                    restaurant.setLongitude(toaDo.lon());
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found: " + id));
        restaurant.setActive(false);
        restaurantRepository.save(restaurant);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lưu thực đơn admin gửi lên.
     *
     * AdminRestaurantsPage.tsx đã gửi trường `menuCategories` (JSON) kèm ảnh từng món
     * từ trước, nhưng controller không hề khai báo tham số đó — Spring bỏ qua phần
     * multipart không khớp tham số nào, nên admin nhập món, bấm lưu, dữ liệu biến mất
     * mà không có lỗi nào. Phương thức này là chỗ tiếp nhận còn thiếu.
     *
     * FE gửi TOÀN BỘ trạng thái mong muốn của thực đơn, nên đây là thao tác đồng bộ:
     * món có id thì cập nhật, món không còn trong payload thì xoá, món mới thì thêm.
     * Không xoá sạch rồi chèn lại — làm thế thì id món đổi sau mỗi lần lưu.
     */
    private void luuThucDon(Restaurant restaurant, String json,
                            org.springframework.web.multipart.MultipartHttpServletRequest request)
            throws Exception {
        // null nghĩa là client không đụng tới thực đơn (vd. gọi từ chỗ khác) — giữ nguyên.
        // Chuỗi rỗng hay mảng rỗng mới là "xoá hết món".
        if (json == null) return;

        List<MenuCategoryPayload> nhomMon;
        try {
            nhomMon = MAPPER.readValue(json, new TypeReference<List<MenuCategoryPayload>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Dữ liệu thực đơn không đọc được");
        }
        if (nhomMon == null) return;

        List<MenuItem> hienCo = menuItemRepository.findByRestaurantIdOrderBySortOrderAscIdAsc(restaurant.getId());
        Map<Long, MenuItem> theoId = hienCo.stream()
                .collect(Collectors.toMap(MenuItem::getId, m -> m));

        Set<Long> conGiuLai = new HashSet<>();
        List<MenuItem> canLuu = new ArrayList<>();

        for (MenuCategoryPayload nhom : nhomMon) {
            MenuCategory category = MenuCategory.tuNhan(nhom.getName());
            if (category == null) {
                throw new IllegalArgumentException("Nhóm món không hợp lệ: " + nhom.getName());
            }
            List<MenuItemPayload> mon = nhom.getItems() != null ? nhom.getItems() : List.of();

            for (int i = 0; i < mon.size(); i++) {
                MenuItemPayload p = mon.get(i);

                // Nhóm Admin*Controller không có @Valid nên phải tự kiểm ở đây.
                String tenMon = InputValidator.ten(p.getName(), "Tên món");
                if (p.getPrice() == null || p.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Giá của món \"" + tenMon + "\" phải lớn hơn 0");
                }

                MenuItem m = (p.getId() != null && theoId.containsKey(p.getId()))
                        ? theoId.get(p.getId())
                        : new MenuItem();
                m.setRestaurant(restaurant);
                m.setName(tenMon);
                m.setDescription(InputValidator.tuyChon(p.getDescription(), "Mô tả món",
                        InputValidator.DAI_TOI_DA_MO_TA));
                m.setPrice(p.getPrice());
                m.setCategory(category);
                m.setBestSeller(Boolean.TRUE.equals(p.getIsBestSeller()));
                m.setNewItem(Boolean.TRUE.equals(p.getIsNew()));
                m.setAvailable(p.getAvailable() == null || p.getAvailable());
                m.setSortOrder(i);

                // Ảnh món gửi kèm dạng part tên "menuImage_<tên nhóm>_<vị trí>".
                if (request != null) {
                    MultipartFile anh = request.getFile("menuImage_" + nhom.getName() + "_" + i);
                    if (anh != null && !anh.isEmpty()) {
                        m.setPhoto(new SerialBlob(anh.getBytes()));
                    }
                }

                if (m.getId() != null) conGiuLai.add(m.getId());
                canLuu.add(m);
            }
        }

        List<MenuItem> canXoa = hienCo.stream()
                .filter(m -> !conGiuLai.contains(m.getId()))
                .toList();
        if (!canXoa.isEmpty()) menuItemRepository.deleteAll(canXoa);
        if (!canLuu.isEmpty()) menuItemRepository.saveAll(canLuu);
    }

    /** Thực đơn trả về cho form quản trị, nhóm theo nhãn tiếng Việt FE đang dùng. */
    private List<MenuCategoryPayload> docThucDon(Long restaurantId) {
        List<MenuItem> mon = menuItemRepository.findByRestaurantIdOrderBySortOrderAscIdAsc(restaurantId);
        List<MenuCategoryPayload> ketQua = new ArrayList<>();
        for (MenuCategory c : MenuCategory.values()) {
            MenuCategoryPayload nhom = new MenuCategoryPayload();
            nhom.setName(c.nhanTiengViet());
            nhom.setItems(mon.stream()
                    .filter(m -> m.getCategory() == c)
                    .map(m -> {
                        MenuItemPayload p = new MenuItemPayload();
                        p.setId(m.getId());
                        p.setName(m.getName());
                        p.setDescription(m.getDescription());
                        p.setPrice(m.getPrice());
                        p.setIsBestSeller(m.isBestSeller());
                        p.setIsNew(m.isNewItem());
                        p.setAvailable(m.isAvailable());
                        p.setImageUrl(blobToDataUrl(m.getPhoto()));
                        return p;
                    })
                    .toList());
            ketQua.add(nhom);
        }
        return ketQua;
    }

    private RestaurantItem toItem(Restaurant r) {
        RestaurantItem item = new RestaurantItem();
        item.setId(r.getId());
        item.setName(r.getName());
        item.setAddress(r.getAddress() != null ? r.getAddress() : "");
        item.setCity(r.getCity() != null ? r.getCity() : "");
        item.setLatitude(r.getLatitude());
        item.setLongitude(r.getLongitude());
        item.setCuisineType(r.getCuisineType() != null ? r.getCuisineType().name() : "");
        item.setMaxTables(r.getCapacity() != null ? r.getCapacity() : 0);
        item.setDescription(r.getDescription());
        item.setStatus(r.isActive() ? "ACTIVE" : "INACTIVE");
        item.setRating(r.getAverageRating());

        // Parse openTime / closeTime from openingHours "HH:mm-HH:mm"
        // Dữ liệu sẵn có lưu dạng "10:00 - 22:00", tách xong còn dính khoảng trắng;
        // gửi nguyên chuỗi đó lên lại thì LocalTime.parse ném lỗi và lưu chết 400.
        String oh = r.getOpeningHours();
        if (oh != null && oh.contains("-")) {
            String[] parts = oh.split("-", 2);
            item.setOpenTime(parts[0].trim());
            item.setCloseTime(parts[1].trim());
        } else {
            item.setOpenTime("08:00");
            item.setCloseTime("22:00");
        }

        String photoUrl = blobToDataUrl(r.getPhoto());
        item.setImageUrls(photoUrl != null ? List.of(photoUrl) : List.of());

        // Không có phần này thì form sửa nhà hàng luôn mở ra với thực đơn rỗng,
        // và lần lưu kế tiếp sẽ xoá sạch món đang có.
        item.setMenuCategories(docThucDon(r.getId()));

        return item;
    }

    private String blobToDataUrl(Blob blob) {
        if (blob == null) return null;
        try {
            byte[] bytes = blob.getBytes(1, (int) blob.length());
            return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    public static class RestaurantItem {
        private long id;
        private String name;
        private String address;
        // Form sửa gửi lại `city`, mà apDungThongTin bắt buộc trường này. Thiếu nó ở
        // đây là form nạp thành phố rỗng rồi lưu lại chết 400 "Thành phố không được
        // để trống" — admin không sửa nổi bất kỳ nhà hàng nào.
        private String city;
        private Double latitude;
        private Double longitude;
        private String cuisineType;
        private String openTime;
        private String closeTime;
        private int maxTables;
        private String description;
        private List<String> imageUrls;
        private String status;
        private Double rating;
        private List<MenuCategoryPayload> menuCategories;
    }

    /** Khớp interface MenuCategory của AdminRestaurantsPage.tsx. */
    @Data
    public static class MenuCategoryPayload {
        private String name;                 // nhãn tiếng Việt: "Khai vị", "Món chính"...
        private List<MenuItemPayload> items;
    }

    /**
     * Khớp interface MenuItem của AdminRestaurantsPage.tsx.
     *
     * Tên hai cờ để nguyên isBestSeller/isNew vì FE gửi lên và đọc về đúng tên đó.
     * Dùng Boolean (không phải boolean) để phân biệt "FE không gửi" với "FE gửi false".
     */
    @Data
    public static class MenuItemPayload {
        private Long id;
        private String name;
        private String description;
        private BigDecimal price;
        private String imageUrl;
        private Boolean isBestSeller;
        private Boolean isNew;
        private Boolean available;
    }
}

