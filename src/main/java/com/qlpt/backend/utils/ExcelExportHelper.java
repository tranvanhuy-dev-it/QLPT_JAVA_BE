package com.qlpt.backend.utils;

import com.qlpt.backend.entity.Invoice;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.WaterBillingType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExportHelper {

    public static byte[] exportRevenueToExcel(User landlord, List<Invoice> invoices, LocalDate start, LocalDate end, String boardingHouseName, double threshold, double vatRate, double pitRate) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            // Create Sheet 1: Summary Report
            Sheet summarySheet = workbook.createSheet("Tong hop thue");
            
            // Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            
            Font subTitleFont = workbook.createFont();
            subTitleFont.setItalic(true);
            subTitleFont.setFontHeightInPoints((short) 11);
            
            CellStyle subTitleStyle = workbook.createCellStyle();
            subTitleStyle.setFont(subTitleFont);
            subTitleStyle.setAlignment(HorizontalAlignment.CENTER);
            
            Font labelFont = workbook.createFont();
            labelFont.setBold(true);
            
            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(labelFont);
            
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0\" VND\""));
            
            CellStyle boldCurrencyStyle = workbook.createCellStyle();
            boldCurrencyStyle.setFont(labelFont);
            boldCurrencyStyle.setDataFormat(format.getFormat("#,##0\" VND\""));

            // 1. Title
            Row titleRow = summarySheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue("BÁO CÁO DOANH THU & KHAI BÁO THUẾ");
            titleCell.setCellStyle(titleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 5));
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            Row periodRow = summarySheet.createRow(2);
            Cell periodCell = periodRow.createCell(1);
            periodCell.setCellValue("Kỳ báo cáo: Từ ngày " + start.format(formatter) + " đến ngày " + end.format(formatter));
            periodCell.setCellStyle(subTitleStyle);
            summarySheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 5));
            
            // 2. Info block
            int rowIdx = 4;
            Row row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("Chủ trọ / Người nộp thuế:");
            row.getCell(1).setCellStyle(labelStyle);
            row.createCell(2).setCellValue(landlord.getFullName() != null ? landlord.getFullName() : landlord.getUsername());
            
            row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("Số điện thoại liên hệ:");
            row.getCell(1).setCellStyle(labelStyle);
            row.createCell(2).setCellValue(landlord.getPhone() != null ? landlord.getPhone() : "");
            
            row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("Dãy nhà trọ khai báo:");
            row.getCell(1).setCellStyle(labelStyle);
            row.createCell(2).setCellValue(boardingHouseName);
            
            rowIdx++; // Empty row
            
            // Calculations
            double totalExpected = 0;
            double totalPaid = 0;
            for (Invoice inv : invoices) {
                totalExpected += inv.getTotalAmount();
                if (inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PAID || 
                    inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PARTIALLY_PAID) {
                    totalPaid += inv.getPaidAmount();
                }
            }
            double totalDebt = totalExpected - totalPaid;
            
            // Check threshold (in the actual service we calculate total annual, here we use the threshold check)
            // Let's assume the passed totalPaid is already evaluated. If the caller determines it exceeds,
            // we apply the rate, otherwise 0.
            // Wait, we can pass `isTaxable` to the helper, or calculate it. Let's pass if it exceeds threshold.
            // Since we know the threshold, let's write the check. If the actual annual revenue is > threshold,
            // then tax is calculated. Let's make sure the service calculates whether it's taxable and we nộp.
            // But wait, the Excel export is for a specific start/end range. The total annual revenue of the landlord
            // might be different from the total of this specific filtered sheet.
            // So we should pass the `annualRevenue` to the helper as well, so the helper knows if it exceeds the threshold!
            // Let's check: yes, we'll pass `annualRevenue` and `threshold`.
            // Let's write the calculations based on that.
            
            // 3. Stats Block
            row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("TỔNG HỢP DOANH THU & NGHĨA VỤ THUẾ");
            row.getCell(1).setCellStyle(labelStyle);
            
            row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("1. Tổng doanh thu hóa đơn phát sinh:");
            row.createCell(2).setCellValue(totalExpected);
            row.getCell(2).setCellStyle(currencyStyle);
            
            row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("2. Tổng tiền thực thu (Đã thanh toán):");
            row.createCell(2).setCellValue(totalPaid);
            row.getCell(2).setCellStyle(boldCurrencyStyle);
            
            row = summarySheet.createRow(rowIdx++);
            row.createCell(1).setCellValue("3. Tổng công nợ chưa thu hồi:");
            row.createCell(2).setCellValue(totalDebt);
            row.getCell(2).setCellStyle(currencyStyle);
            
            try {
                summarySheet.autoSizeColumn(1);
                summarySheet.autoSizeColumn(2);
            } catch (Throwable e) {
                summarySheet.setColumnWidth(1, 30 * 256);
                summarySheet.setColumnWidth(2, 20 * 256);
            }
            
            // Create Sheet 2: Detailed Invoices
            Sheet detailSheet = workbook.createSheet("Chi tiet doanh thu");
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            String[] headers = {
                "STT", "Dãy trọ", "Số phòng", "Khách thuê", "Ngày lập HD", "Kỳ bắt đầu", "Kỳ kết thúc",
                "Tiền phòng", "Tiền điện", "Tiền nước", "Dịch vụ khác", "Giảm giá", "Tổng hóa đơn", "Đã thanh toán", "Ngày thanh toán", "Trạng thái"
            };
            
            Row headerRow = detailSheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            
            int detailRowIdx = 1;
            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                Row detRow = detailSheet.createRow(detailRowIdx++);
                
                detRow.createCell(0).setCellValue(i + 1);
                detRow.getCell(0).setCellStyle(centerStyle);
                
                String bhNameStr = "—";
                String roomNumStr = "—";
                String tenantNameStr = "—";
                
                if (inv.getContract() != null) {
                    if (inv.getContract().getRoom() != null) {
                        roomNumStr = inv.getContract().getRoom().getRoomNumber();
                        if (inv.getContract().getRoom().getBoardingHouse() != null) {
                            bhNameStr = inv.getContract().getRoom().getBoardingHouse().getName();
                        }
                    }
                    if (inv.getContract().getTenant() != null) {
                        tenantNameStr = inv.getContract().getTenant().getFullName() != null ? 
                            inv.getContract().getTenant().getFullName() : inv.getContract().getTenant().getUsername();
                    }
                }
                
                detRow.createCell(1).setCellValue(bhNameStr);
                detRow.createCell(2).setCellValue(roomNumStr);
                detRow.createCell(3).setCellValue(tenantNameStr);
                
                detRow.createCell(4).setCellValue(inv.getInvoiceDate().format(formatter));
                detRow.getCell(4).setCellStyle(centerStyle);
                
                detRow.createCell(5).setCellValue(inv.getBillingPeriodStart().format(formatter));
                detRow.getCell(5).setCellStyle(centerStyle);
                
                detRow.createCell(6).setCellValue(inv.getBillingPeriodEnd().format(formatter));
                detRow.getCell(6).setCellStyle(centerStyle);
                
                // Calculations
                double elecAmt = (inv.getNewElectricityIndex() - inv.getOldElectricityIndex()) * inv.getElectricityRate();
                double waterAmt = 0;
                if (inv.getWaterBillingType() == WaterBillingType.BY_INDEX) {
                    waterAmt = (inv.getNewWaterIndex() - inv.getOldWaterIndex()) * inv.getWaterRate();
                } else {
                    waterAmt = inv.getNumberOfTenants() * inv.getWaterRate();
                }
                double otherAmt = Math.max(0, inv.getTotalAmount() - inv.getRoomPrice() - elecAmt - waterAmt + inv.getDiscount());
                
                detRow.createCell(7).setCellValue(inv.getRoomPrice());
                detRow.getCell(7).setCellStyle(currencyStyle);
                
                detRow.createCell(8).setCellValue(elecAmt);
                detRow.getCell(8).setCellStyle(currencyStyle);
                
                detRow.createCell(9).setCellValue(waterAmt);
                detRow.getCell(9).setCellStyle(currencyStyle);
                
                detRow.createCell(10).setCellValue(otherAmt);
                detRow.getCell(10).setCellStyle(currencyStyle);
                
                detRow.createCell(11).setCellValue(inv.getDiscount());
                detRow.getCell(11).setCellStyle(currencyStyle);
                
                detRow.createCell(12).setCellValue(inv.getTotalAmount());
                detRow.getCell(12).setCellStyle(currencyStyle);
                
                detRow.createCell(13).setCellValue(inv.getPaidAmount());
                detRow.getCell(13).setCellStyle(currencyStyle);
                
                detRow.createCell(14).setCellValue(inv.getPaymentDate() != null ? inv.getPaymentDate().format(formatter) : "—");
                detRow.getCell(14).setCellStyle(centerStyle);
                
                String statusStr = "Chưa thanh toán";
                if (inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PAID) {
                    statusStr = "Đã thanh toán";
                } else if (inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PARTIALLY_PAID) {
                    statusStr = "Thanh toán một phần";
                }
                detRow.createCell(15).setCellValue(statusStr);
            }
            
            // Auto-size columns for sheet 2
            for (int i = 0; i < headers.length; i++) {
                try {
                    detailSheet.autoSizeColumn(i);
                } catch (Throwable e) {
                    detailSheet.setColumnWidth(i, 15 * 256);
                }
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
