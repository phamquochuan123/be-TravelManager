package com.example.travelManager.controller;

import com.example.travelManager.domain.restaurant.RestaurantBooking;
import com.example.travelManager.domain.tour.TourBooking;
import com.example.travelManager.repository.restaurant.RestaurantBookingRepository;
import com.example.travelManager.repository.tour.TourBookingRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class ExportController {

    private final TourBookingRepository tourBookingRepository;
    private final RestaurantBookingRepository restaurantBookingRepository;

    // ── Excel ─────────────────────────────────────────────────────

    @GetMapping("/excel/tour-bookings")
    public ResponseEntity<byte[]> exportTourBookingsExcel() throws Exception {
        List<TourBooking> bookings = tourBookingRepository.findAllByOrderByCreatedAtDesc();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tour Bookings");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"ID", "Tour", "Điểm đến", "Tên liên hệ", "Email", "SĐT",
                    "Người lớn", "Trẻ em", "Giá gốc", "Giảm giá", "Thành tiền", "Trạng thái", "Ngày đặt"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (TourBooking b : bookings) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(b.getId());
                row.createCell(1).setCellValue(b.getTour() != null ? b.getTour().getName() : "");
                row.createCell(2).setCellValue(b.getTour() != null ? b.getTour().getDestination() : "");
                row.createCell(3).setCellValue(b.getContactName());
                row.createCell(4).setCellValue(b.getContactEmail());
                row.createCell(5).setCellValue(b.getContactPhone());
                row.createCell(6).setCellValue(b.getNumAdults());
                row.createCell(7).setCellValue(b.getNumChildren());
                row.createCell(8).setCellValue(b.getOriginalPrice() != null ? b.getOriginalPrice().doubleValue() : 0);
                row.createCell(9).setCellValue(b.getDiscountAmount() != null ? b.getDiscountAmount().doubleValue() : 0);
                row.createCell(10).setCellValue(b.getFinalPrice() != null ? b.getFinalPrice().doubleValue() : 0);
                row.createCell(11).setCellValue(b.getStatus() != null ? b.getStatus().name() : "");
                row.createCell(12).setCellValue(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tour-bookings.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    @GetMapping("/excel/restaurant-bookings")
    public ResponseEntity<byte[]> exportRestaurantBookingsExcel() throws Exception {
        List<RestaurantBooking> bookings = restaurantBookingRepository.findAllByOrderByCreatedAtDesc();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Restaurant Bookings");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {"ID", "Nhà hàng", "Tên liên hệ", "Email", "SĐT",
                    "Số khách", "Ngày đặt", "Giờ đặt", "Trạng thái", "Mã xác nhận"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (RestaurantBooking b : bookings) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(b.getId());
                row.createCell(1).setCellValue(b.getRestaurant() != null ? b.getRestaurant().getName() : "");
                row.createCell(2).setCellValue(b.getContactName());
                row.createCell(3).setCellValue(b.getContactEmail());
                row.createCell(4).setCellValue(b.getContactPhone());
                row.createCell(5).setCellValue(b.getGuestCount());
                row.createCell(6).setCellValue(b.getBookingDate() != null ? b.getBookingDate().toString() : "");
                row.createCell(7).setCellValue(b.getBookingTime() != null ? b.getBookingTime().toString() : "");
                row.createCell(8).setCellValue(b.getStatus() != null ? b.getStatus().name() : "");
                row.createCell(9).setCellValue(b.getConfirmationCode());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=restaurant-bookings.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    // ── PDF ───────────────────────────────────────────────────────

    @GetMapping("/pdf/tour-bookings")
    public ResponseEntity<byte[]> exportTourBookingsPdf() throws Exception {
        List<TourBooking> bookings = tourBookingRepository.findAllByOrderByCreatedAtDesc();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph("BÁO CÁO ĐẶT TOUR")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16));
        document.add(new Paragraph("Tổng: " + bookings.size() + " đơn hàng")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(11));
        document.add(new Paragraph(" "));

        float[] colWidths = {30f, 120f, 100f, 80f, 50f, 50f, 80f, 80f};
        Table table = new Table(colWidths);
        table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        String[] headers = {"ID", "Tour", "Tên liên hệ", "Email", "NL", "TE", "Thành tiền", "Trạng thái"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (TourBooking b : bookings) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(b.getId()))));
            table.addCell(new Cell().add(new Paragraph(b.getTour() != null ? b.getTour().getName() : "")));
            table.addCell(new Cell().add(new Paragraph(b.getContactName() != null ? b.getContactName() : "")));
            table.addCell(new Cell().add(new Paragraph(b.getContactEmail() != null ? b.getContactEmail() : "")));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(b.getNumAdults()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(b.getNumChildren()))));
            String price = b.getFinalPrice() != null ? b.getFinalPrice().toPlainString() : "0";
            table.addCell(new Cell().add(new Paragraph(price)));
            table.addCell(new Cell().add(new Paragraph(b.getStatus() != null ? b.getStatus().name() : "")));
            if (b.getFinalPrice() != null) totalRevenue = totalRevenue.add(b.getFinalPrice());
        }

        document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Tổng doanh thu: " + totalRevenue.toPlainString() + " VNĐ")
                .setBold().setTextAlignment(TextAlignment.RIGHT));
        document.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tour-bookings.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    @GetMapping("/pdf/restaurant-bookings")
    public ResponseEntity<byte[]> exportRestaurantBookingsPdf() throws Exception {
        List<RestaurantBooking> bookings = restaurantBookingRepository.findAllByOrderByCreatedAtDesc();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph("BÁO CÁO ĐẶT BÀN NHÀ HÀNG")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16));
        document.add(new Paragraph("Tổng: " + bookings.size() + " đơn đặt bàn")
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(11));
        document.add(new Paragraph(" "));

        float[] colWidths = {30f, 120f, 100f, 70f, 60f, 60f, 80f};
        Table table = new Table(colWidths);
        table.setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100));

        String[] headers = {"ID", "Nhà hàng", "Tên liên hệ", "Số khách", "Ngày đặt", "Giờ", "Trạng thái"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (RestaurantBooking b : bookings) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(b.getId()))));
            table.addCell(new Cell().add(new Paragraph(b.getRestaurant() != null ? b.getRestaurant().getName() : "")));
            table.addCell(new Cell().add(new Paragraph(b.getContactName() != null ? b.getContactName() : "")));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(b.getGuestCount()))));
            table.addCell(new Cell().add(new Paragraph(b.getBookingDate() != null ? b.getBookingDate().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(b.getBookingTime() != null ? b.getBookingTime().toString() : "")));
            table.addCell(new Cell().add(new Paragraph(b.getStatus() != null ? b.getStatus().name() : "")));
        }

        document.add(table);
        document.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=restaurant-bookings.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
}
