package com.goldinventory.service;

import com.goldinventory.database.DBConnection;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.swing.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExcelExporter {
    public static boolean exportSalesToExcel(Date saleDate) {
        JFileChooser fileChooser = new JFileChooser();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String defaultFileName = "sales_" + dateFormat.format(saleDate) + ".xlsx";
        fileChooser.setSelectedFile(new java.io.File(defaultFileName));
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        java.io.File fileToSave = fileChooser.getSelectedFile();
        String sql = "SELECT s.sale_id, s.customer_id, g.name as item_name, "
                + "g.weight_grams, g.purity_karat, s.total_amount, s.sale_date "
                + "FROM sales s "
                + "JOIN gold_items g ON s.item_id = g.item_id "
                + "WHERE s.sale_date = ? "
                + "ORDER BY s.sale_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             Workbook workbook = new XSSFWorkbook()) {
            pstmt.setDate(1, new java.sql.Date(saleDate.getTime()));
            ResultSet rs = pstmt.executeQuery();
            Sheet sheet = workbook.createSheet("Sales Report");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Sale ID", "Customer ID", "Item Name", "Weight (g)", 
                              "Purity (K)", "Total Amount", "Sale Date"};
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GOLD.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            int rowNum = 1;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rs.getInt("sale_id"));
                row.createCell(1).setCellValue(rs.getInt("customer_id"));
                row.createCell(2).setCellValue(rs.getString("item_name"));
                row.createCell(3).setCellValue(rs.getDouble("weight_grams"));
                row.createCell(4).setCellValue(rs.getInt("purity_karat"));
                row.createCell(5).setCellValue(rs.getDouble("total_amount"));
                row.createCell(6).setCellValue(rs.getDate("sale_date").toString());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (FileOutputStream fileOut = new FileOutputStream(fileToSave)) {
                workbook.write(fileOut);
            }
            JOptionPane.showMessageDialog(null,
                "Sales data exported successfully to:\n" + fileToSave.getAbsolutePath(),
                "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error exporting to Excel: " + e.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}