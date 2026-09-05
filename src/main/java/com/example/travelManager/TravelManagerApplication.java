package com.example.travelManager;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class TravelManagerApplication {

	/**
	 * Toàn bộ nghiệp vụ của hệ thống này diễn ra ở Việt Nam, nên giờ mặc định của
	 * JVM phải là GMT+7 chứ không phải giờ của máy chạy nó.
	 *
	 * Container docker chạy GMT, nên trước khi có dòng này thì mọi
	 * LocalDate.now()/LocalDateTime.now()/ZoneId.systemDefault() trong app đều lệch
	 * 7 tiếng: từ 00:00 tới 07:00 giờ VN, "hôm nay" của app vẫn là ngày hôm trước.
	 * Hệ quả cụ thể là doanh thu khung giờ đó rơi nhầm sang ngày trước trong báo
	 * cáo, hạn coupon và hạn đặt phòng tính sai, còn thanh toán VNPay thì bị từ
	 * chối thẳng bằng mã 15.
	 *
	 * Đặt một chỗ ở đây thay vì sửa 32 lời gọi rải rác: sót một chỗ là lại lệch,
	 * mà code viết sau này cũng tự động đúng. Phải chạy TRƯỚC SpringApplication.run
	 * vì Hibernate và driver JDBC đọc giờ mặc định ngay lúc khởi tạo.
	 *
	 * Lưu ý đi kèm: datasource URL ghim connectionTimeZone=UTC, nên cột thời gian
	 * trong DB vẫn lưu theo UTC y như trước, không bị dịch 7 tiếng vì thay đổi này.
	 */
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
	}

	public static void main(String[] args) {
		SpringApplication.run(TravelManagerApplication.class, args);
	}

}
