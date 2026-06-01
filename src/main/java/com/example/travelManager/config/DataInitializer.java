package com.example.travelManager.config;

import com.example.travelManager.domain.Role;
import com.example.travelManager.domain.destination.Destination;
import com.example.travelManager.domain.tour.Tour;
import com.example.travelManager.repository.RoleRepository;
import com.example.travelManager.repository.destination.DestinationRepository;
import com.example.travelManager.repository.tour.TourRepository;
import com.example.travelManager.util.constant.destination.DestinationType;
import com.example.travelManager.util.constant.tour.TourStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final DestinationRepository destinationRepository;
    private final TourRepository tourRepository;

    public DataInitializer(RoleRepository roleRepository,
                           DestinationRepository destinationRepository,
                           TourRepository tourRepository) {
        this.roleRepository = roleRepository;
        this.destinationRepository = destinationRepository;
        this.tourRepository = tourRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedRoles();
        seedDestinations();
        seedTours();
    }

    // ─── Roles ───────────────────────────────────────────────────────────────────

    private void seedRoles() {
        createRoleIfNotExists("ADMIN", "Quản trị viên");
        createRoleIfNotExists("STAFF", "Nhân viên");
        createRoleIfNotExists("USER", "Người dùng thông thường");
    }

    private void createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setActive(true);
            roleRepository.save(role);
        }
    }

    // ─── Destinations ─────────────────────────────────────────────────────────────

    private void seedDestinations() {
        if (destinationRepository.count() > 0) return;

        List<Object[]> data = List.of(
            new Object[]{"Vịnh Hạ Long",         "Hạ Long",          "Quảng Ninh",           DestinationType.BEACH,    "Kỳ quan thiên nhiên thế giới với hàng nghìn đảo đá vôi"},
            new Object[]{"Hội An",                "Hội An",           "Quảng Nam",            DestinationType.CULTURE,  "Phố cổ di sản UNESCO với đèn lồng và kiến trúc cổ kính"},
            new Object[]{"Sapa",                  "Sapa",             "Lào Cai",              DestinationType.MOUNTAIN, "Thị trấn núi mù sương với ruộng bậc thang và bản làng dân tộc"},
            new Object[]{"Đà Nẵng",               "Đà Nẵng",          "Đà Nẵng",              DestinationType.BEACH,    "Thành phố biển năng động với cầu Rồng và bãi biển Mỹ Khê"},
            new Object[]{"Nha Trang",             "Nha Trang",        "Khánh Hòa",            DestinationType.BEACH,    "Thành phố biển nổi tiếng với vịnh biển xanh và resort cao cấp"},
            new Object[]{"Phú Quốc",              "Phú Quốc",         "Kiên Giang",           DestinationType.BEACH,    "Đảo ngọc lớn nhất Việt Nam với bãi biển hoang sơ tuyệt đẹp"},
            new Object[]{"Đà Lạt",                "Đà Lạt",           "Lâm Đồng",             DestinationType.MOUNTAIN, "Thành phố ngàn hoa với khí hậu mát mẻ quanh năm"},
            new Object[]{"Hà Nội",                "Hà Nội",           "Hà Nội",               DestinationType.CITY,     "Thủ đô nghìn năm văn hiến với Hồ Gươm và phố cổ 36 phường"},
            new Object[]{"TP. Hồ Chí Minh",      "Hồ Chí Minh",      "Hồ Chí Minh",          DestinationType.CITY,     "Trung tâm kinh tế sôi động với văn hóa ẩm thực phong phú"},
            new Object[]{"Huế",                   "Huế",              "Thừa Thiên Huế",        DestinationType.CULTURE,  "Cố đô với Hoàng thành, lăng tẩm và ẩm thực cung đình"},
            new Object[]{"Mũi Né",                "Phan Thiết",       "Bình Thuận",            DestinationType.BEACH,    "Làng chài với đồi cát đỏ, đồi cát trắng và resort ven biển"},
            new Object[]{"Cát Bà",                "Cát Bà",           "Hải Phòng",             DestinationType.NATURE,   "Đảo lớn nhất vịnh Hạ Long với rừng quốc gia và hang động"},
            new Object[]{"Phong Nha - Kẻ Bàng",  "Đồng Hới",         "Quảng Bình",            DestinationType.NATURE,   "Hang động Sơn Đoòng lớn nhất thế giới, di sản UNESCO"},
            new Object[]{"Mộc Châu",              "Mộc Châu",         "Sơn La",               DestinationType.NATURE,   "Cao nguyên với đồi chè xanh mướt, vườn đào và thác Dải Yếm"},
            new Object[]{"Hà Giang",              "Hà Giang",         "Hà Giang",             DestinationType.MOUNTAIN, "Cực Bắc Tổ quốc với cao nguyên đá Đồng Văn hùng vĩ"},
            new Object[]{"Côn Đảo",               "Côn Đảo",          "Bà Rịa-Vũng Tàu",     DestinationType.BEACH,    "Quần đảo hoang sơ với rùa biển, san hô và di tích lịch sử"},
            new Object[]{"Ninh Bình",             "Ninh Bình",        "Ninh Bình",            DestinationType.NATURE,   "Hạ Long trên cạn với Tràng An, Tam Cốc và cố đô Hoa Lư"},
            new Object[]{"Quy Nhơn",              "Quy Nhơn",         "Bình Định",            DestinationType.BEACH,    "Thành phố biển bình yên với Eo Gió, Kỳ Co và tháp Chăm"},
            new Object[]{"Vũng Tàu",              "Vũng Tàu",         "Bà Rịa-Vũng Tàu",     DestinationType.BEACH,    "Thành phố biển gần TP.HCM với Bãi Trước và Bãi Sau"},
            new Object[]{"Đảo Lý Sơn",            "Lý Sơn",           "Quảng Ngãi",           DestinationType.BEACH,    "Đảo tỏi nổi tiếng với địa hình núi lửa và biển xanh"},
            new Object[]{"Thác Bản Giốc",         "Trùng Khánh",      "Cao Bằng",             DestinationType.NATURE,   "Thác nước hùng vĩ nhất Đông Nam Á trên biên giới Việt-Trung"},
            new Object[]{"Cần Thơ",               "Cần Thơ",          "Cần Thơ",              DestinationType.CITY,     "Thủ phủ miền Tây với chợ nổi Cái Răng và vườn trái cây"},
            new Object[]{"Phan Rang - Tháp Chàm", "Phan Rang",        "Ninh Thuận",           DestinationType.CULTURE,  "Tháp Chăm cổ kính, vườn nho, cừu và bãi biển Ninh Chữ"},
            new Object[]{"Buôn Ma Thuột",         "Buôn Ma Thuột",    "Đắk Lắk",             DestinationType.CULTURE,  "Thủ phủ Tây Nguyên với hồ Lắk, thác và văn hóa cồng chiêng"},
            new Object[]{"Mù Cang Chải",          "Mù Cang Chải",     "Yên Bái",             DestinationType.MOUNTAIN, "Ruộng bậc thang đẹp nhất Việt Nam vào mùa vàng lúa chín"},
            new Object[]{"Tam Đảo",               "Tam Đảo",          "Vĩnh Phúc",           DestinationType.MOUNTAIN, "Thị trấn trên mây với khí hậu mát mẻ và cảnh quan núi rừng"},
            new Object[]{"Phú Yên",               "Tuy Hòa",          "Phú Yên",             DestinationType.BEACH,    "Bình yên với gành Đá Đĩa, đầm Ô Loan và bãi Môn hoang sơ"},
            new Object[]{"Cô Tô",                 "Cô Tô",            "Quảng Ninh",          DestinationType.BEACH,    "Đảo tiên xanh biếc với bãi biển trong vắt và hải sản tươi ngon"},
            new Object[]{"Tây Ninh",              "Tây Ninh",         "Tây Ninh",            DestinationType.CULTURE,  "Núi Bà Đen và Tòa Thánh Cao Đài linh thiêng nổi tiếng"},
            new Object[]{"Bangkok - Pattaya",     "Bangkok",          "Thái Lan",            DestinationType.CITY,     "Tour quốc tế Thái Lan nổi tiếng với chùa vàng và biển xanh"},
            new Object[]{"Singapore",             "Singapore",        "Singapore",           DestinationType.CITY,     "Đảo quốc sư tử với Gardens by the Bay và Marina Bay Sands"},
            new Object[]{"Bali",                  "Bali",             "Indonesia",           DestinationType.BEACH,    "Thiên đường nhiệt đới với đền Hindu, ruộng lúa và biển xanh"},
            new Object[]{"Hàn Quốc - Seoul",      "Seoul",            "Hàn Quốc",            DestinationType.CITY,     "Thủ đô xứ kim chi với Gyeongbokgung và phố Myeongdong sầm uất"},
            new Object[]{"Nhật Bản - Tokyo",      "Tokyo",            "Nhật Bản",            DestinationType.CITY,     "Thành phố nơi truyền thống gặp hiện đại, hoa anh đào tuyệt đẹp"}
        );

        for (Object[] row : data) {
            Destination d = new Destination();
            d.setName((String) row[0]);
            d.setCity((String) row[1]);
            d.setProvince((String) row[2]);
            d.setDestinationType((DestinationType) row[3]);
            d.setDescription((String) row[4]);
            d.setActive(true);
            destinationRepository.save(d);
        }
        log.info("Seeded {} destinations", data.size());
    }

    // ─── Tours ────────────────────────────────────────────────────────────────────

    private void seedTours() {
        if (tourRepository.count() > 0) return;

        // {name, destination, days, nights, priceAdult, priceChild, description}
        List<Object[]> data = List.of(
            new Object[]{"Tour Vịnh Hạ Long 3N2Đ - Khám phá hang động",   "Vịnh Hạ Long",        3, 2, 3_800_000L, 2_500_000L, "Khám phá kỳ quan thiên nhiên thế giới với du thuyền hạng sang, tham quan hang Sửng Sốt, hang Thiên Cung và chèo kayak qua các đảo đá vôi hùng vĩ."},
            new Object[]{"Tour Hạ Long - Cát Bà 4N3Đ Luxury",             "Vịnh Hạ Long",        4, 3, 5_200_000L, 3_500_000L, "Combo du thuyền Hạ Long kết hợp khám phá đảo Cát Bà, vườn quốc gia và các bãi biển hoang sơ."},
            new Object[]{"Tour Hội An - Đà Nẵng 4N3Đ",                    "Hội An",              4, 3, 4_500_000L, 3_000_000L, "Khám phá phố cổ Hội An về đêm với đèn lồng lung linh, tham quan bảo tàng, làng gốm Thanh Hà và bãi biển An Bàng."},
            new Object[]{"Tour Hội An Phố Cổ 2N1Đ - Trải nghiệm văn hóa", "Hội An",              2, 1, 2_200_000L, 1_500_000L, "Dạo bộ phố cổ, thả đèn hoa đăng trên sông Hoài, học nấu ăn truyền thống Hội An cùng người dân địa phương."},
            new Object[]{"Tour Sapa Fansipan 3N2Đ - Chinh phục nóc nhà",  "Sapa",                3, 2, 3_500_000L, 2_300_000L, "Chinh phục đỉnh Fansipan 3.143m bằng cáp treo và đi bộ, tham quan bản Cát Cát, ruộng bậc thang Mù Cang Chải."},
            new Object[]{"Tour Sapa Bản Làng 2N1Đ - Homestay Dân tộc",    "Sapa",                2, 1, 1_800_000L, 1_200_000L, "Homestay tại bản người H'Mông, trekking qua ruộng bậc thang, giao lưu văn hóa và thưởng thức ẩm thực núi rừng."},
            new Object[]{"Tour Đà Nẵng - Bà Nà Hills 3N2Đ",               "Đà Nẵng",             3, 2, 3_900_000L, 2_600_000L, "Tham quan Bà Nà Hills, Cầu Vàng nổi tiếng, bán đảo Sơn Trà, Ngũ Hành Sơn và bãi biển Mỹ Khê."},
            new Object[]{"Tour Đà Nẵng City Break 2N1Đ",                  "Đà Nẵng",             2, 1, 2_500_000L, 1_700_000L, "Khám phá thành phố biển đáng sống nhất Việt Nam với Cầu Rồng, Cầu Tình Yêu, Bảo tàng điêu khắc Chăm."},
            new Object[]{"Tour Nha Trang biển xanh 4N3Đ",                 "Nha Trang",           4, 3, 4_800_000L, 3_200_000L, "Tắm biển Nha Trang, lặn ngắm san hô, tham quan đảo Hòn Mun, Vinpearl Land và tháp Bà Ponagar cổ kính."},
            new Object[]{"Tour Nha Trang - Đà Lạt 5N4Đ",                  "Nha Trang",           5, 4, 6_500_000L, 4_300_000L, "Combo biển Nha Trang và cao nguyên Đà Lạt: tắm biển, lặn san hô, đèo Ngoạn Mục, thác Pongour."},
            new Object[]{"Tour Phú Quốc 4N3Đ - Đảo Ngọc Thiên Đường",    "Phú Quốc",            4, 3, 5_500_000L, 3_700_000L, "Khám phá Phú Quốc với Vinpearl Safari, lặn ngắm san hô 4 đảo, chợ đêm Phú Quốc và hoàng hôn Mũi Gành Dầu."},
            new Object[]{"Tour Phú Quốc Nghỉ Dưỡng 3N2Đ Resort 5 sao",   "Phú Quốc",            3, 2, 7_800_000L, 5_000_000L, "Nghỉ dưỡng tại resort 5 sao bãi Trường, spa thư giãn, tour câu cá và ngắm hoàng hôn trên biển."},
            new Object[]{"Tour Đà Lạt 3N2Đ - Thành phố ngàn hoa",        "Đà Lạt",              3, 2, 2_800_000L, 1_900_000L, "Tham quan Dinh Bảo Đại, Hồ Xuân Hương, vườn hoa Đà Lạt, thác Datanla, thác Prenn và làng hoa."},
            new Object[]{"Tour Đà Lạt Khám phá 4N3Đ",                    "Đà Lạt",              4, 3, 3_600_000L, 2_400_000L, "Vườn dâu, vườn cà phê, cung đường đèo Ngoạn Mục, làng cà phê Cầu Đất Farm và rừng thông Madagui."},
            new Object[]{"Tour Hà Nội - Ninh Bình 2N1Đ",                  "Hà Nội",              2, 1, 1_600_000L, 1_100_000L, "Khám phá Hà Nội - Phố cổ, Hồ Gươm, Lăng Bác, Văn Miếu kết hợp Tam Cốc - Bích Động Ninh Bình."},
            new Object[]{"Tour Huế - Hội An - Đà Nẵng 5N4Đ",             "Huế",                 5, 4, 5_800_000L, 3_900_000L, "Hành trình di sản miền Trung: Đại Nội Huế, lăng tẩm vua chúa, phố cổ Hội An, Mỹ Sơn và bãi biển Đà Nẵng."},
            new Object[]{"Tour Huế Cố Đô 3N2Đ - Hoàng Thành",            "Huế",                 3, 2, 3_200_000L, 2_100_000L, "Tham quan Đại Nội, lăng Tự Đức, lăng Khải Định, chùa Thiên Mụ và thưởng thức ẩm thực cung đình Huế."},
            new Object[]{"Tour Ninh Bình Tràng An 2N1Đ",                  "Ninh Bình",           2, 1, 1_900_000L, 1_300_000L, "Chèo thuyền Tràng An - Di sản thế giới, leo núi Hang Múa, tham quan cố đô Hoa Lư và chùa Bái Đính."},
            new Object[]{"Tour Cần Thơ Miền Tây 3N2Đ",                   "Cần Thơ",             3, 2, 2_600_000L, 1_800_000L, "Chợ nổi Cái Răng, vườn trái cây Cồn Sơn, miệt vườn Long Hồ và làng nghề bánh tráng Mỹ Lồng."},
            new Object[]{"Tour Vũng Tàu Biển Gần 2N1Đ",                  "Vũng Tàu",            2, 1, 1_500_000L, 1_000_000L, "Tắm biển Bãi Sau, tham quan Tượng Chúa Kitô, Hải Đăng Vũng Tàu và thưởng thức hải sản tươi ngon."},
            new Object[]{"Tour Hà Giang Loop 4N3Đ - Cực Bắc",            "Hà Giang",            4, 3, 4_200_000L, 2_800_000L, "Khám phá cao nguyên đá Đồng Văn, đèo Mã Pí Lèng hùng tráng, cột cờ Lũng Cú và chợ phiên Đồng Văn."},
            new Object[]{"Tour Phong Nha 3N2Đ - Hang Động Kỳ Bí",        "Phong Nha - Kẻ Bàng", 3, 2, 3_400_000L, 2_200_000L, "Khám phá hang Phong Nha bằng thuyền, hang Tối, hang Én và thiên đường Phong Nha Kẻ Bàng."},
            new Object[]{"Tour Mù Cang Chải Mùa Vàng 3N2Đ",              "Mù Cang Chải",        3, 2, 3_000_000L, 2_000_000L, "Chiêm ngưỡng ruộng bậc thang mùa lúa chín vàng óng, trekking bản Mù và khám phá văn hóa Mông."},
            new Object[]{"Tour Bangkok - Pattaya 5N4Đ",                   "Bangkok - Pattaya",   5, 4, 9_900_000L, 7_500_000L, "Khám phá Thái Lan với chùa Wat Arun, chợ nổi, Pattaya Walking Street, Sanctuary of Truth và show Alcazar."},
            new Object[]{"Tour Singapore 4N3Đ - Đảo Quốc Sư Tử",         "Singapore",           4, 3,12_500_000L, 9_000_000L, "Universal Studios, Gardens by the Bay, Marina Bay Sands, Jurong Bird Park và trải nghiệm ẩm thực đa văn hóa."},
            new Object[]{"Tour Bali 5N4Đ - Thiên Đường Nhiệt Đới",       "Bali",                5, 4,10_800_000L, 8_000_000L, "Đền Tanah Lot, ruộng bậc thang Tegalalang, Ubud Sacred Monkey Forest, bãi biển Seminyak và Kuta."},
            new Object[]{"Tour Hàn Quốc - Seoul 5N4Đ",                   "Hàn Quốc - Seoul",    5, 4,15_900_000L,12_000_000L, "Gyeongbokgung, Bukchon Hanok Village, Đảo Nami, núi Seoraksan, phố Myeongdong và trải nghiệm K-culture."},
            new Object[]{"Tour Nhật Bản - Tokyo - Osaka 7N6Đ",           "Nhật Bản - Tokyo",    7, 6,28_500_000L,22_000_000L, "Núi Phú Sĩ, Tokyo Disneyland, Kyoto chùa Kinkakuji, Osaka Dotonbori và trải nghiệm văn hóa Nhật Bản."},
            new Object[]{"Tour Quy Nhơn - Phú Yên 4N3Đ",                 "Quy Nhơn",            4, 3, 4_100_000L, 2_700_000L, "Gành Đá Đĩa, Hòn Yến, bãi biển Quy Nhơn, tháp Đôi, eo gió và đầm Ô Loan trong xanh."},
            new Object[]{"Tour Côn Đảo 4N3Đ - Đảo Hoang Sơ",            "Côn Đảo",             4, 3, 7_200_000L, 5_000_000L, "Lặn ngắm san hô, xem rùa biển đẻ trứng, tham quan nhà tù Côn Đảo và bãi Đầm Trầu hoang sơ."}
        );

        for (Object[] row : data) {
            Tour t = new Tour();
            t.setName((String) row[0]);
            t.setDestination((String) row[1]);
            t.setDurationDays((int) row[2]);
            t.setDurationNights((int) row[3]);
            t.setPriceAdult(BigDecimal.valueOf((long) row[4]));
            t.setPriceChild(BigDecimal.valueOf((long) row[5]));
            t.setDescription((String) row[6]);
            t.setStatus(TourStatus.ACTIVE);
            t.setDeleted(false);
            tourRepository.save(t);
        }
        log.info("Seeded {} tours", data.size());
    }
}
