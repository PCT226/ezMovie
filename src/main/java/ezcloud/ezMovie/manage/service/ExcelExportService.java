package ezcloud.ezMovie.manage.service;

import ezcloud.ezMovie.manage.model.dto.DashboardRevenueResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ExcelExportService {

    public byte[] exportDashboardToExcel(DashboardRevenueResponse dashboardData, LocalDate startDate, LocalDate endDate) {
        try (Workbook workbook = new XSSFWorkbook()) {
            
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Summary Sheet
            createSummarySheet(workbook, dashboardData, startDate, endDate, headerStyle, titleStyle, currencyStyle);
            
            // Revenue by Movie Sheet
            createMovieRevenueSheet(workbook, dashboardData, headerStyle, currencyStyle);
            
            // Revenue by Cinema Sheet
            createCinemaRevenueSheet(workbook, dashboardData, headerStyle, currencyStyle);
            
            // Top Movies Sheet
            createTopMoviesSheet(workbook, dashboardData, headerStyle, currencyStyle);
            
            // Top Cinemas Sheet
            createTopCinemasSheet(workbook, dashboardData, headerStyle, currencyStyle);

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error creating Excel file: " + e.getMessage());
        }
    }

    private void createSummarySheet(Workbook workbook, DashboardRevenueResponse data, LocalDate startDate, LocalDate endDate,
                                  CellStyle headerStyle, CellStyle titleStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Summary");
        
        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DASHBOARD STATISTICS REPORT");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
        
        // Date Range
        Row dateRow = sheet.createRow(1);
        Cell dateCell = dateRow.createCell(0);
        dateCell.setCellValue("Period: " + startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                             " to " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 3));
        
        // Total Revenue
        Row totalRow = sheet.createRow(3);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("Total Revenue");
        totalLabelCell.setCellStyle(headerStyle);
        
        Cell totalValueCell = totalRow.createCell(1);
        totalValueCell.setCellValue(data.getTotalRevenue().doubleValue());
        totalValueCell.setCellStyle(currencyStyle);

        // Tổng quan
        Row overviewHeader = sheet.createRow(5);
        overviewHeader.createCell(0).setCellValue("Overview");
        overviewHeader.getCell(0).setCellStyle(headerStyle);
        
        Row overviewRow1 = sheet.createRow(6);
        overviewRow1.createCell(0).setCellValue("Total Movies");
        overviewRow1.createCell(1).setCellValue(data.getTotalMovies());
        Row overviewRow2 = sheet.createRow(7);
        overviewRow2.createCell(0).setCellValue("Total Showtimes");
        overviewRow2.createCell(1).setCellValue(data.getTotalShowtimes());
        Row overviewRow3 = sheet.createRow(8);
        overviewRow3.createCell(0).setCellValue("Total Tickets");
        overviewRow3.createCell(1).setCellValue(data.getTotalTickets());
        Row overviewRow4 = sheet.createRow(9);
        overviewRow4.createCell(0).setCellValue("Total Users");
        overviewRow4.createCell(1).setCellValue(data.getTotalUsers());

        // Recent Tickets
        Row recentHeader = sheet.createRow(11);
        recentHeader.createCell(0).setCellValue("Recent Tickets");
        recentHeader.getCell(0).setCellStyle(headerStyle);
        String[] ticketHeaders = {"Movie Title", "Showtime", "Status"};
        Row ticketHeaderRow = sheet.createRow(12);
        for (int i = 0; i < ticketHeaders.length; i++) {
            Cell cell = ticketHeaderRow.createCell(i);
            cell.setCellValue(ticketHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        int ticketRowNum = 13;
        if (data.getRecentTickets() != null) {
            for (var ticket : data.getRecentTickets()) {
                Row row = sheet.createRow(ticketRowNum++);
                row.createCell(0).setCellValue(ticket.getMovieTitle());
                row.createCell(1).setCellValue(ticket.getShowtime());
                row.createCell(2).setCellValue(ticket.getStatus());
            }
        }

        // Revenue by Movie
        Row movieRevenueHeader = sheet.createRow(ticketRowNum + 2);
        movieRevenueHeader.createCell(0).setCellValue("Revenue by Movie");
        movieRevenueHeader.getCell(0).setCellStyle(headerStyle);
        String[] movieHeaders = {"Movie Title", "Revenue", "Percentage"};
        Row movieHeaderRow = sheet.createRow(ticketRowNum + 3);
        for (int i = 0; i < movieHeaders.length; i++) {
            Cell cell = movieHeaderRow.createCell(i);
            cell.setCellValue(movieHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        int movieRowNum = ticketRowNum + 4;
        BigDecimal totalRevenue = data.getTotalRevenue();
        if (data.getRevenueByMovie() != null) {
            for (Map.Entry<String, BigDecimal> entry : data.getRevenueByMovie().entrySet()) {
                Row row = sheet.createRow(movieRowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                Cell revenueCell = row.createCell(1);
                revenueCell.setCellValue(entry.getValue().doubleValue());
                revenueCell.setCellStyle(currencyStyle);
                double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? 
                    entry.getValue().divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0;
                row.createCell(2).setCellValue(percentage);
            }
        }

        // Revenue by Cinema
        Row cinemaRevenueHeader = sheet.createRow(movieRowNum + 2);
        cinemaRevenueHeader.createCell(0).setCellValue("Revenue by Cinema");
        cinemaRevenueHeader.getCell(0).setCellStyle(headerStyle);
        String[] cinemaHeaders = {"Cinema Name", "Revenue", "Percentage"};
        Row cinemaHeaderRow = sheet.createRow(movieRowNum + 3);
        for (int i = 0; i < cinemaHeaders.length; i++) {
            Cell cell = cinemaHeaderRow.createCell(i);
            cell.setCellValue(cinemaHeaders[i]);
            cell.setCellStyle(headerStyle);
        }
        int cinemaRowNum = movieRowNum + 4;
        if (data.getRevenueByCinema() != null) {
            for (Map.Entry<String, BigDecimal> entry : data.getRevenueByCinema().entrySet()) {
                Row row = sheet.createRow(cinemaRowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                Cell revenueCell = row.createCell(1);
                revenueCell.setCellValue(entry.getValue().doubleValue());
                revenueCell.setCellStyle(currencyStyle);
                double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? 
                    entry.getValue().divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0;
                row.createCell(2).setCellValue(percentage);
            }
        }
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createMovieRevenueSheet(Workbook workbook, DashboardRevenueResponse data, 
                                       CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Revenue by Movie");
        
        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleHeaderCell = titleRow.createCell(0);
        titleHeaderCell.setCellValue("Revenue by Movie");
        titleHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
        
        // Headers
        Row headerRow = sheet.createRow(2);
        String[] headers = {"Movie Title", "Revenue", "Percentage of Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        int rowNum = 3;
        BigDecimal totalRevenue = data.getTotalRevenue();
        for (Map.Entry<String, BigDecimal> entry : data.getRevenueByMovie().entrySet()) {
            Row row = sheet.createRow(rowNum++);
            
            Cell movieTitleCell = row.createCell(0);
            movieTitleCell.setCellValue(entry.getKey());
            
            Cell revenueCell = row.createCell(1);
            revenueCell.setCellValue(entry.getValue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
            
            Cell percentageCell = row.createCell(2);
            double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? 
                entry.getValue().divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0;
            percentageCell.setCellValue(percentage);
        }
        
        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCinemaRevenueSheet(Workbook workbook, DashboardRevenueResponse data, 
                                        CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Revenue by Cinema");
        
        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleHeaderCell = titleRow.createCell(0);
        titleHeaderCell.setCellValue("Revenue by Cinema");
        titleHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 2));
        
        // Headers
        Row headerRow = sheet.createRow(2);
        String[] headers = {"Cinema Name", "Revenue", "Percentage of Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        int rowNum = 3;
        BigDecimal totalRevenue = data.getTotalRevenue();
        for (Map.Entry<String, BigDecimal> entry : data.getRevenueByCinema().entrySet()) {
            Row row = sheet.createRow(rowNum++);
            
            Cell cinemaNameCell = row.createCell(0);
            cinemaNameCell.setCellValue(entry.getKey());
            
            Cell revenueCell = row.createCell(1);
            revenueCell.setCellValue(entry.getValue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
            
            Cell percentageCell = row.createCell(2);
            double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ? 
                entry.getValue().divide(totalRevenue, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")).doubleValue() : 0;
            percentageCell.setCellValue(percentage);
        }
        
        // Auto-size columns
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createTopMoviesSheet(Workbook workbook, DashboardRevenueResponse data, 
                                    CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Top Movies");
        
        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleHeaderCell = titleRow.createCell(0);
        titleHeaderCell.setCellValue("Top Movies by Revenue");
        titleHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
        
        // Headers
        Row headerRow = sheet.createRow(2);
        String[] headers = {"Rank", "Movie Title", "Revenue", "Poster URL"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        int rowNum = 3;
        for (int i = 0; i < data.getTopMovies().size(); i++) {
            var movie = data.getTopMovies().get(i);
            Row row = sheet.createRow(rowNum++);
            
            Cell rankCell = row.createCell(0);
            rankCell.setCellValue(i + 1);
            
            Cell movieTitleCell = row.createCell(1);
            movieTitleCell.setCellValue(movie.getTitle());
            
            Cell revenueCell = row.createCell(2);
            revenueCell.setCellValue(movie.getRevenue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
            
            Cell posterCell = row.createCell(3);
            posterCell.setCellValue(movie.getPoster());
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createTopCinemasSheet(Workbook workbook, DashboardRevenueResponse data, 
                                     CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Top Cinemas");
        
        // Title
        Row titleRow = sheet.createRow(0);
        Cell titleHeaderCell = titleRow.createCell(0);
        titleHeaderCell.setCellValue("Top Cinemas by Revenue");
        titleHeaderCell.setCellStyle(headerStyle);
        sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 3));
        
        // Headers
        Row headerRow = sheet.createRow(2);
        String[] headers = {"Rank", "Cinema Name", "Revenue", "Address"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Data
        int rowNum = 3;
        for (int i = 0; i < data.getTopCinemas().size(); i++) {
            var cinema = data.getTopCinemas().get(i);
            Row row = sheet.createRow(rowNum++);
            
            Cell rankCell = row.createCell(0);
            rankCell.setCellValue(i + 1);
            
            Cell cinemaNameCell = row.createCell(1);
            cinemaNameCell.setCellValue(cinema.getName());
            
            Cell revenueCell = row.createCell(2);
            revenueCell.setCellValue(cinema.getRevenue().doubleValue());
            revenueCell.setCellStyle(currencyStyle);
            
            Cell addressCell = row.createCell(3);
            addressCell.setCellValue(cinema.getAddress());
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("dd/mm/yyyy"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
} 