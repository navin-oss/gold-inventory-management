package com.goldinventory.ui.admin;

import com.goldinventory.service.AuthService;
import com.goldinventory.service.ExcelExporter;
import com.goldinventory.database.DBConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminDashboardFrame extends JFrame {
    private AuthService.User currentUser;
    private JTabbedPane tabbedPane;

    private static final Color DARK_NAVY = new Color(0, 32, 96);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color LIGHT_GRAY = new Color(240, 240, 240);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    private JTable goldItemsTable;
    private DefaultTableModel goldItemsModel;
    private JButton addItemBtn, editItemBtn, deleteItemBtn, refreshItemsBtn;
    private JTable salesTable;
    private DefaultTableModel salesModel;
    private JDatePicker datePicker;
    private JButton exportExcelBtn, refreshSalesBtn;
    private JLabel totalSalesLabel;

    public AdminDashboardFrame(AuthService.User user) {
        this.currentUser = user;
        initLookAndFeel();
        initializeUI();
        loadGoldItems();
        loadSalesReport(new Date());
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) {}
    }

    private void initializeUI() {
        setTitle("GoldInventory · Admin Dashboard — " + currentUser.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        getContentPane().setBackground(LIGHT_GRAY);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK_NAVY);
        JLabel titleLabel = new JLabel("Welcome, " + currentUser.getUsername());
        titleLabel.setForeground(GOLD);
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setBorder(new EmptyBorder(10, 20, 10, 0));
        JButton logoutBtn = createStyledButton("Logout", GOLD, DARK_NAVY);
        logoutBtn.addActionListener(e -> {
            dispose();
            new com.goldinventory.ui.LoginFrame().setVisible(true);
        });
        header.add(titleLabel, BorderLayout.WEST);
        header.add(logoutBtn, BorderLayout.EAST);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tabbedPane.setBackground(DARK_NAVY);
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.addTab("Manage Gold Items", createGoldItemsPanel());
        tabbedPane.addTab("Sales Report", createSalesReportPanel());

        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createGoldItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(LIGHT_GRAY);

        // Added "Qty" column
        String[] columns = {"ID", "Name", "Weight (g)", "Purity (K)", "Price/Gram", "Total Price", "Qty", "Status"};
        goldItemsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        goldItemsTable = new JTable(goldItemsModel);
        goldItemsTable.setFont(TABLE_FONT);
        goldItemsTable.setRowHeight(28);
        goldItemsTable.getTableHeader().setFont(BUTTON_FONT);
        goldItemsTable.getTableHeader().setBackground(DARK_NAVY);
        goldItemsTable.getTableHeader().setForeground(GOLD);
        goldItemsTable.setGridColor(DARK_NAVY);
        goldItemsTable.setSelectionBackground(GOLD);
        goldItemsTable.setSelectionForeground(DARK_NAVY);

        // Center-align numeric columns (including Qty)
        int[] numericCols = {0, 2, 3, 4, 5, 6}; // ID, Weight, Purity, Price/Gram, Total, Qty
        for (int colIndex : numericCols) {
            goldItemsTable.getColumnModel().getColumn(colIndex).setCellRenderer(
                new DefaultTableCellRenderer() {{
                    setHorizontalAlignment(SwingConstants.CENTER);
                }}
            );
        }

        JScrollPane scrollPane = new JScrollPane(goldItemsTable);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(LIGHT_GRAY);
        addItemBtn = createStyledButton("Add Item", GOLD, DARK_NAVY);
        editItemBtn = createStyledButton("Edit Item", new Color(255, 165, 0), Color.WHITE);
        deleteItemBtn = createStyledButton("Delete Item", new Color(220, 20, 60), Color.WHITE);
        refreshItemsBtn = createStyledButton("Refresh", new Color(0, 191, 255), Color.WHITE);
        buttonPanel.add(addItemBtn);
        buttonPanel.add(editItemBtn);
        buttonPanel.add(deleteItemBtn);
        buttonPanel.add(refreshItemsBtn);
        addItemBtn.addActionListener(e -> showAddItemDialog());
        editItemBtn.addActionListener(e -> showEditItemDialog());
        deleteItemBtn.addActionListener(e -> deleteSelectedItem());
        refreshItemsBtn.addActionListener(e -> loadGoldItems());

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createSalesReportPanel() {
        // ... (unchanged - sales report doesn't need quantity)
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(LIGHT_GRAY);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controlPanel.setBackground(LIGHT_GRAY);
        controlPanel.add(new JLabel("Select Date:"));
        datePicker = new JDatePicker();
        controlPanel.add(datePicker);
        refreshSalesBtn = createStyledButton("Refresh", new Color(0, 191, 255), Color.WHITE);
        exportExcelBtn = createStyledButton("Export to Excel", GOLD, DARK_NAVY);
        controlPanel.add(refreshSalesBtn);
        controlPanel.add(exportExcelBtn);

        String[] columns = {"Sale ID", "Customer ID", "Item Name", "Weight (g)", "Purity (K)", "Total Amount", "Sale Date"};
        salesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        salesTable = new JTable(salesModel);
        salesTable.setFont(TABLE_FONT);
        salesTable.setRowHeight(28);
        salesTable.getTableHeader().setFont(BUTTON_FONT);
        salesTable.getTableHeader().setBackground(DARK_NAVY);
        salesTable.getTableHeader().setForeground(GOLD);
        salesTable.setGridColor(DARK_NAVY);
        salesTable.setSelectionBackground(GOLD);
        salesTable.setSelectionForeground(DARK_NAVY);

        for (int i = 0; i < columns.length; i++) {
            if (i != 2) {
                salesTable.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer() {{
                    setHorizontalAlignment(SwingConstants.CENTER);
                }});
            }
        }

        JScrollPane scrollPane = new JScrollPane(salesTable);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.setBackground(LIGHT_GRAY);
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
        totalSalesLabel = new JLabel("Total Sales: ₹0.00");
        totalSalesLabel.setFont(HEADER_FONT);
        bottom.add(totalSalesLabel);

        refreshSalesBtn.addActionListener(e -> loadSalesReport(datePicker.getDate()));
        exportExcelBtn.addActionListener(e -> {
            if (ExcelExporter.exportSalesToExcel(datePicker.getDate())) {
                JOptionPane.showMessageDialog(this, "Export completed successfully!");
            }
        });

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFont(BUTTON_FONT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        return button;
    }

    private void loadGoldItems() {
        goldItemsModel.setRowCount(0);
        // Added quantity to SELECT
        String sql = "SELECT item_id, name, weight_grams, purity_karat, price_per_gram, total_price, quantity, status FROM gold_items ORDER BY item_id";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                goldItemsModel.addRow(new Object[]{
                    rs.getInt("item_id"),
                    rs.getString("name"),
                    rs.getDouble("weight_grams"),
                    rs.getInt("purity_karat"),
                    rs.getDouble("price_per_gram"),
                    rs.getDouble("total_price"),
                    rs.getInt("quantity"), // ✅ Quantity added
                    rs.getString("status")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading gold items: " + e.getMessage());
        }
    }

    private void loadSalesReport(Date date) {
        // ... (unchanged)
        salesModel.setRowCount(0);
        double totalSales = 0.0;
        String sql = "SELECT s.sale_id, s.customer_id, g.name, g.weight_grams, "
                + "g.purity_karat, s.total_amount, s.sale_date "
                + "FROM sales s "
                + "JOIN gold_items g ON s.item_id = g.item_id "
                + "WHERE s.sale_date = ? "
                + "ORDER BY s.sale_id";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDate(1, new java.sql.Date(date.getTime()));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("total_amount");
                    totalSales += amount;
                    salesModel.addRow(new Object[]{
                        rs.getInt("sale_id"),
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getDouble("weight_grams"),
                        rs.getInt("purity_karat"),
                        amount,
                        rs.getDate("sale_date")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading sales report: " + e.getMessage());
        }
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        totalSalesLabel.setText("Total Sales: " + fmt.format(totalSales));
    }

    private void showAddItemDialog() {
        JTextField nameField = new JTextField();
        JTextField weightField = new JTextField();
        JComboBox<String> purityCombo = new JComboBox<>(new String[]{"18", "22", "24"});
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField("1"); // ✅ Default quantity = 1

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Weight (grams):"));
        panel.add(weightField);
        panel.add(new JLabel("Purity (karat):"));
        panel.add(purityCombo);
        panel.add(new JLabel("Price per gram:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity:")); // ✅ New field
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Gold Item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");
                double weight = Double.parseDouble(weightField.getText().trim());
                int purity = Integer.parseInt(purityCombo.getSelectedItem().toString());
                double pricePerGram = Double.parseDouble(priceField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim()); // ✅ Parse quantity
                if (weight <= 0 || pricePerGram <= 0 || quantity <= 0) {
                    throw new IllegalArgumentException("Weight, price, and quantity must be positive");
                }
                double totalPrice = weight * pricePerGram;
                // ✅ Insert quantity
                String sql = "INSERT INTO gold_items (name, weight_grams, purity_karat, price_per_gram, total_price, quantity, status) VALUES (?, ?, ?, ?, ?, ?, 'available')";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, name);
                    pstmt.setDouble(2, weight);
                    pstmt.setInt(3, purity);
                    pstmt.setDouble(4, pricePerGram);
                    pstmt.setDouble(5, totalPrice);
                    pstmt.setInt(6, quantity); // ✅ Set quantity
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Gold item added successfully!");
                    loadGoldItems();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error adding item: " + e.getMessage());
            }
        }
    }

    private void showEditItemDialog() {
        int selectedRow = goldItemsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to edit");
            return;
        }
        int itemId = (int) goldItemsModel.getValueAt(selectedRow, 0);
        String status = (String) goldItemsModel.getValueAt(selectedRow, 7); // ✅ Status is now index 7
        if ("sold".equals(status)) {
            JOptionPane.showMessageDialog(this, "Cannot edit sold items");
            return;
        }
        String currentName = (String) goldItemsModel.getValueAt(selectedRow, 1);
        double currentWeight = (Double) goldItemsModel.getValueAt(selectedRow, 2);
        int currentPurity = (Integer) goldItemsModel.getValueAt(selectedRow, 3);
        double currentPricePerGram = (Double) goldItemsModel.getValueAt(selectedRow, 4);
        int currentQuantity = (Integer) goldItemsModel.getValueAt(selectedRow, 6); // ✅ Get quantity

        JTextField nameField = new JTextField(currentName);
        JTextField weightField = new JTextField(String.valueOf(currentWeight));
        JComboBox<String> purityCombo = new JComboBox<>(new String[]{"18", "22", "24"});
        purityCombo.setSelectedItem(String.valueOf(currentPurity));
        JTextField priceField = new JTextField(String.valueOf(currentPricePerGram));
        JTextField quantityField = new JTextField(String.valueOf(currentQuantity)); // ✅ Pre-fill quantity

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("Weight (grams):"));
        panel.add(weightField);
        panel.add(new JLabel("Purity (karat):"));
        panel.add(purityCombo);
        panel.add(new JLabel("Price per gram:"));
        panel.add(priceField);
        panel.add(new JLabel("Quantity:")); // ✅ Edit quantity
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Gold Item",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Name cannot be empty");
                double weight = Double.parseDouble(weightField.getText().trim());
                int purity = Integer.parseInt(purityCombo.getSelectedItem().toString());
                double pricePerGram = Double.parseDouble(priceField.getText().trim());
                int quantity = Integer.parseInt(quantityField.getText().trim()); // ✅ Parse new quantity
                if (weight <= 0 || pricePerGram <= 0 || quantity <= 0) {
                    throw new IllegalArgumentException("Weight, price, and quantity must be positive");
                }
                double totalPrice = weight * pricePerGram;
                // ✅ Update quantity
                String sql = "UPDATE gold_items SET name = ?, weight_grams = ?, purity_karat = ?, price_per_gram = ?, total_price = ?, quantity = ? WHERE item_id = ?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, name);
                    pstmt.setDouble(2, weight);
                    pstmt.setInt(3, purity);
                    pstmt.setDouble(4, pricePerGram);
                    pstmt.setDouble(5, totalPrice);
                    pstmt.setInt(6, quantity); // ✅ Update quantity
                    pstmt.setInt(7, itemId);
                    int rows = pstmt.executeUpdate();
                    if (rows > 0) {
                        JOptionPane.showMessageDialog(this, "Gold item updated successfully!");
                        loadGoldItems();
                    }
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error updating item: " + e.getMessage());
            }
        }
    }

    private void deleteSelectedItem() {
        // ... (unchanged - deletion logic remains same)
        int selectedRow = goldItemsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete");
            return;
        }
        int itemId = (int) goldItemsModel.getValueAt(selectedRow, 0);
        String status = (String) goldItemsModel.getValueAt(selectedRow, 7); // ✅ Status index updated
        if ("sold".equals(status)) {
            JOptionPane.showMessageDialog(this, "Cannot delete sold items");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this item?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM gold_items WHERE item_id = ?";
                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, itemId);
                    pstmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Item deleted successfully!");
                    loadGoldItems();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting item: " + e.getMessage());
            }
        }
    }

    class JDatePicker extends JPanel {
        // ... (unchanged)
        private JComboBox<String> dayCombo, monthCombo, yearCombo;
        public JDatePicker() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
            dayCombo = new JComboBox<>();
            for (int i = 1; i <= 31; i++) dayCombo.addItem(String.format("%02d", i));
            monthCombo = new JComboBox<>();
            for (int i = 1; i <= 12; i++) monthCombo.addItem(String.format("%02d", i));
            yearCombo = new JComboBox<>();
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            for (int i = currentYear - 5; i <= currentYear + 5; i++) yearCombo.addItem(String.valueOf(i));
            add(dayCombo);
            add(new JLabel("/"));
            add(monthCombo);
            add(new JLabel("/"));
            add(yearCombo);
            setDate(new Date());
        }
        public void setDate(Date date) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String[] parts = sdf.format(date).split("/");
            dayCombo.setSelectedItem(parts[0]);
            monthCombo.setSelectedItem(parts[1]);
            yearCombo.setSelectedItem(parts[2]);
        }
        public Date getDate() {
            try {
                String dateStr = yearCombo.getSelectedItem() + "-" + 
                               monthCombo.getSelectedItem() + "-" + 
                               dayCombo.getSelectedItem();
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            } catch (Exception e) {
                return new Date();
            }
        }
    }
}