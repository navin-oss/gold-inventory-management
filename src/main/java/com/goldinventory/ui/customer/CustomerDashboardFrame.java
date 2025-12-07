package com.goldinventory.ui.customer;

import com.goldinventory.service.AuthService;
import com.goldinventory.database.DBConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects; // Added for Objects.equals()

public class CustomerDashboardFrame extends JFrame {
    private AuthService.User currentUser;
    private JTabbedPane tabbedPane;

    private static final Color DARK_NAVY = new Color(0, 32, 96);
    private static final Color GOLD = new Color(212, 175, 55);
    private static final Color LIGHT_GRAY = new Color(240, 240, 240);
    private static final Color SUCCESS_GREEN = new Color(34, 139, 34);
    private static final Color WARNING_RED = new Color(220, 53, 69);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    // Enhanced cart with better structure
    private Map<Integer, CartItem> cart = new HashMap<>();
    private JLabel cartTotalLabel;
    private JLabel cartItemCountLabel;

    public CustomerDashboardFrame(AuthService.User user) {
        this.currentUser = user;
        initLookAndFeel();
        initComponents();
        loadAvailableItems();
        loadPurchaseHistory();
    }

    private void initLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException ignored) {}
    }

    private void initComponents() {
        setTitle("GoldInventory · Customer Dashboard — " + currentUser.getUsername());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(LIGHT_GRAY);

        // Enhanced Header
        JPanel header = createEnhancedHeader();

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tabbedPane.setBackground(DARK_NAVY);
        tabbedPane.setForeground(Color.WHITE);
        tabbedPane.addTab("🛍️ Browse Gold Items", createItemsPanel());
        tabbedPane.addTab("📦 My Cart", createCartPanel());
        tabbedPane.addTab("📜 Purchase History", createHistoryPanel());

        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createEnhancedHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DARK_NAVY);
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Welcome message
        JLabel titleLabel = new JLabel("Welcome, " + currentUser.getUsername() + " 👑");
        titleLabel.setForeground(GOLD);
        titleLabel.setFont(HEADER_FONT);

        // Cart summary and logout
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);
        
        // Cart summary
        JPanel cartSummary = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        cartSummary.setOpaque(false);
        
        cartItemCountLabel = new JLabel("🛒 0 items");
        cartItemCountLabel.setForeground(Color.WHITE);
        cartItemCountLabel.setFont(BUTTON_FONT);
        
        cartTotalLabel = new JLabel("₹0.00");
        cartTotalLabel.setForeground(GOLD);
        cartTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        cartSummary.add(cartItemCountLabel);
        cartSummary.add(new JLabel("•"));
        cartSummary.add(cartTotalLabel);

        // Logout button
        JButton logoutBtn = createStyledButton("🚪 Logout", GOLD, DARK_NAVY, new Dimension(120, 40));
        logoutBtn.addActionListener(e -> {
            dispose();
            new com.goldinventory.ui.LoginFrame().setVisible(true);
        });

        rightPanel.add(cartSummary);
        rightPanel.add(logoutBtn);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    // ITEMS PANEL
    private JTable itemsTable;
    private DefaultTableModel itemsModel;

    private JPanel createItemsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(LIGHT_GRAY);

        // Top controls
        JPanel topControls = new JPanel(new BorderLayout());
        topControls.setBackground(LIGHT_GRAY);
        topControls.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(BUTTON_FONT);
        
        JTextField searchField = new JTextField(20);
        searchField.setFont(TABLE_FONT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JButton searchBtn = createStyledButton("🔍 Search", DARK_NAVY, Color.WHITE, new Dimension(100, 40));
        searchBtn.addActionListener(e -> filterItems(searchField.getText().trim()));

        JButton refreshBtn = createStyledButton("🔄 Refresh", GOLD, DARK_NAVY, new Dimension(120, 40));
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadAvailableItems();
        });

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(refreshBtn);

        topControls.add(searchPanel, BorderLayout.WEST);

        // Table
        String[] columns = {"Name", "Weight (g)", "Purity (K)", "Price/Gram", "Total Price", "Stock", "Qty", "Add to Cart", "ID"};
        itemsModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 6 || col == 7; // Qty and Add to Cart are editable
            }
        };
        
        itemsTable = new JTable(itemsModel);
        itemsTable.setFont(TABLE_FONT);
        itemsTable.setRowHeight(35);
        itemsTable.getTableHeader().setFont(BUTTON_FONT);
        itemsTable.getTableHeader().setBackground(DARK_NAVY);
        itemsTable.getTableHeader().setForeground(GOLD);
        itemsTable.setGridColor(new Color(220, 220, 220));
        itemsTable.setSelectionBackground(new Color(212, 175, 55, 100));

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i <= 6; i++) {
            itemsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Hide ID column
        itemsTable.getColumnModel().getColumn(8).setMinWidth(0);
        itemsTable.getColumnModel().getColumn(8).setMaxWidth(0);

        // Set custom editors and renderers
        itemsTable.getColumnModel().getColumn(6).setCellEditor(new SpinnerCellEditor());
        itemsTable.getColumnModel().getColumn(7).setCellRenderer(new AddToCartButtonRenderer());
        itemsTable.getColumnModel().getColumn(7).setCellEditor(new AddToCartButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(itemsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        // Bottom Panel with cart actions
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(LIGHT_GRAY);
        bottom.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Cart info
        JLabel cartInfoLabel = new JLabel("Cart: 0 items • ₹0.00");
        cartInfoLabel.setFont(HEADER_FONT);
        cartInfoLabel.setForeground(DARK_NAVY);

        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        
        JButton viewCartBtn = createStyledButton("📦 View Cart", DARK_NAVY, Color.WHITE, new Dimension(140, 45));
        viewCartBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewCartBtn.addActionListener(e -> tabbedPane.setSelectedIndex(1));
        
        JButton checkoutBtn = createStyledButton("🚀 Checkout", SUCCESS_GREEN, Color.WHITE, new Dimension(140, 45));
        checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        checkoutBtn.addActionListener(e -> doCheckout());

        buttonPanel.add(viewCartBtn);
        buttonPanel.add(checkoutBtn);

        // Update cart info when cart changes
        cartTotalLabel.addPropertyChangeListener("text", e -> {
            int itemCount = cart.values().stream().mapToInt(item -> item.quantity).sum();
            cartInfoLabel.setText("Cart: " + itemCount + " items • " + cartTotalLabel.getText());
        });

        bottom.add(cartInfoLabel, BorderLayout.WEST);
        bottom.add(buttonPanel, BorderLayout.EAST);

        panel.add(topControls, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        
        return panel;
    }

    // CART PANEL
    private JTable cartTable;
    private DefaultTableModel cartModel;

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(LIGHT_GRAY);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(LIGHT_GRAY);
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        JLabel titleLabel = new JLabel("📦 My Shopping Cart");
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(DARK_NAVY);
        
        JLabel itemCountLabel = new JLabel("0 items in cart");
        itemCountLabel.setFont(BUTTON_FONT);
        itemCountLabel.setForeground(Color.DARK_GRAY);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(itemCountLabel, BorderLayout.EAST);

        // Cart Table
        String[] columns = {"Item", "Weight", "Purity", "Unit Price", "Quantity", "Total", "Actions", "ID"};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 4 || col == 6; // Quantity and Actions are editable
            }
        };
        
        cartTable = new JTable(cartModel);
        cartTable.setFont(TABLE_FONT);
        cartTable.setRowHeight(35);
        cartTable.getTableHeader().setFont(BUTTON_FONT);
        cartTable.getTableHeader().setBackground(DARK_NAVY);
        cartTable.getTableHeader().setForeground(GOLD);
        cartTable.setGridColor(new Color(220, 220, 220));

        // Center align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 1; i <= 5; i++) {
            cartTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Hide ID column
        cartTable.getColumnModel().getColumn(7).setMinWidth(0);
        cartTable.getColumnModel().getColumn(7).setMaxWidth(0);

        // Set custom editors
        cartTable.getColumnModel().getColumn(4).setCellEditor(new CartSpinnerCellEditor());
        cartTable.getColumnModel().getColumn(6).setCellRenderer(new CartActionButtonRenderer());
        cartTable.getColumnModel().getColumn(6).setCellEditor(new CartActionButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(cartTable);

        // Bottom Panel with totals and actions
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(LIGHT_GRAY);
        bottomPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        // Left: Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setOpaque(false);
        
        JButton continueShoppingBtn = createStyledButton("🛍️ Continue Shopping", Color.LIGHT_GRAY, DARK_NAVY, new Dimension(180, 45));
        continueShoppingBtn.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        
        JButton clearCartBtn = createStyledButton("🗑️ Clear Cart", WARNING_RED, Color.WHITE, new Dimension(140, 45));
        clearCartBtn.addActionListener(e -> clearCart());
        
        actionPanel.add(continueShoppingBtn);
        actionPanel.add(clearCartBtn);

        // Right: Checkout section
        JPanel checkoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        checkoutPanel.setOpaque(false);
        
        JPanel totalPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        totalPanel.setOpaque(false);
        
        JLabel totalText = new JLabel("Total Amount:");
        totalText.setFont(BUTTON_FONT);
        totalText.setForeground(Color.DARK_GRAY);
        
        JLabel totalAmount = new JLabel("₹0.00");
        totalAmount.setFont(new Font("Segoe UI", Font.BOLD, 20));
        totalAmount.setForeground(DARK_NAVY);
        
        totalPanel.add(totalText);
        totalPanel.add(totalAmount);
        
        JButton checkoutBtn = createStyledButton("🚀 Secure Checkout", SUCCESS_GREEN, Color.WHITE, new Dimension(180, 45));
        checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        checkoutBtn.addActionListener(e -> doCheckout());

        checkoutPanel.add(totalPanel);
        checkoutPanel.add(checkoutBtn);

        // Update total when cart changes
        cartTotalLabel.addPropertyChangeListener("text", e -> {
            totalAmount.setText(cartTotalLabel.getText());
            int itemCount = cart.values().stream().mapToInt(item -> item.quantity).sum();
            itemCountLabel.setText(itemCount + " items in cart");
        });

        bottomPanel.add(actionPanel, BorderLayout.WEST);
        bottomPanel.add(checkoutPanel, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // HISTORY PANEL
    private JTable historyTable;
    private DefaultTableModel historyModel;

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setBackground(LIGHT_GRAY);

        String[] cols = {"Order ID", "Item", "Weight", "Purity", "Amount", "Date"};
        historyModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.setRowHeight(35);
        historyTable.setFont(TABLE_FONT);
        historyTable.getTableHeader().setFont(BUTTON_FONT);
        historyTable.getTableHeader().setBackground(DARK_NAVY);
        historyTable.getTableHeader().setForeground(GOLD);
        historyTable.setGridColor(DARK_NAVY);
        historyTable.setSelectionBackground(GOLD);
        historyTable.setSelectionForeground(DARK_NAVY);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshBtn = createStyledButton("🔄 Refresh History", GOLD, DARK_NAVY, new Dimension(160, 40));
        refreshBtn.addActionListener(e -> loadPurchaseHistory());
        
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setBackground(LIGHT_GRAY);
        bottom.add(refreshBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // Cart Item class
    private static class CartItem {
        int itemId;
        String name;
        double weight;
        int purity;
        double totalPricePerUnit; // Changed to reflect price for one unit
        int quantity;
        int currentStock; // To store the stock at the time of adding to cart/last refresh

        public CartItem(int itemId, String name, double weight, int purity, double totalPricePerUnit, int quantity) {
            this.itemId = itemId;
            this.name = name;
            this.weight = weight;
            this.purity = purity;
            this.totalPricePerUnit = totalPricePerUnit;
            this.quantity = quantity;
        }

        public double getLineTotal() {
            return totalPricePerUnit * quantity;
        }
        
        // Add equals and hashCode for proper map behavior if needed for complex scenarios
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CartItem cartItem = (CartItem) o;
            return itemId == cartItem.itemId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemId);
        }
    }

    // BUSINESS LOGIC METHODS
    private void filterItems(String keyword) {
        if (keyword.isEmpty()) {
            loadAvailableItems();
            return;
        }
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(itemsModel);
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + keyword, 0));
        itemsTable.setRowSorter(sorter);
    }

    private void loadAvailableItems() {
        itemsModel.setRowCount(0);
        String sql = "SELECT item_id, name, weight_grams, purity_karat, price_per_gram, total_price, quantity " +
                     "FROM gold_items WHERE quantity > 0 ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                // Ensure the quantity field for adding to cart is reset to 1
                itemsModel.addRow(new Object[]{
                    rs.getString("name"),
                    rs.getDouble("weight_grams"),
                    rs.getInt("purity_karat"),
                    rs.getDouble("price_per_gram"),
                    rs.getDouble("total_price"), // This is total price for ONE item
                    rs.getInt("quantity"),       // Current stock
                    1,                            // Default quantity to add
                    "🛒 Add to Cart",
                    rs.getInt("item_id")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading items: " + e.getMessage(),
                                          "DB Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // Print stack trace for debugging
        }
        updateCartTotal();
    }

    private void updateCartTotal() {
        double total = 0.0;
        int itemCount = 0;
        
        for (CartItem item : cart.values()) {
            total += item.getLineTotal();
            itemCount += item.quantity;
        }
        
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        cartTotalLabel.setText(fmt.format(total));
        cartItemCountLabel.setText("🛒 " + itemCount + " items");
        
        // Refresh cart table if it exists
        if (cartModel != null) {
            refreshCartTable();
        }
    }

    private void refreshCartTable() {
        cartModel.setRowCount(0);
        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        
        for (CartItem item : cart.values()) {
            cartModel.addRow(new Object[]{
                item.name,
                item.weight + "g",
                item.purity + "K",
                fmt.format(item.totalPricePerUnit), // Display unit price
                item.quantity,
                fmt.format(item.getLineTotal()),
                "❌ Remove",
                item.itemId
            });
        }
    }

    private void clearCart() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is already empty", 
                                        "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear your cart? This will remove all items.",
            "Clear Cart", JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            cart.clear();
            updateCartTotal();
            JOptionPane.showMessageDialog(this, "Cart cleared successfully!",
                                        "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void removeFromCart(int itemId) {
        CartItem removedItem = cart.remove(itemId);
        if (removedItem != null) {
            updateCartTotal();
            JOptionPane.showMessageDialog(this, 
                "Removed " + removedItem.name + " from cart",
                "Item Removed", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void updateCartItemQuantity(int itemId, int newQuantity) {
        CartItem item = cart.get(itemId);
        if (item != null) {
            if (newQuantity <= 0) {
                removeFromCart(itemId);
            } else {
                // IMPORTANT: When updating quantity in cart, we need to consider the stock
                // available *after* accounting for what's already in the cart.
                int stockExcludingCurrentItem = getAvailableStock(itemId); 
                
                // If the item was just added to cart, its stock needs to be re-evaluated
                // If it's already in the cart, the stock for validation should be current_db_stock + item.quantity
                
                int effectiveAvailableStock = getAvailableStock(itemId); // Get current DB stock
                
                if (newQuantity > effectiveAvailableStock) {
                    JOptionPane.showMessageDialog(this,
                        "Only " + effectiveAvailableStock + " items available in stock for " + item.name,
                        "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                    // Revert spinner value if possible, or prevent update
                    return; 
                }
                item.quantity = newQuantity;
                updateCartTotal();
            }
        }
    }

    private int getAvailableStock(int itemId) {
        String sql = "SELECT quantity FROM gold_items WHERE item_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("quantity");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle error, maybe log or show an error message
            JOptionPane.showMessageDialog(this, "Error checking stock: " + e.getMessage(), 
                                          "DB Error", JOptionPane.ERROR_MESSAGE);
        }
        return 0; // Return 0 if unable to get stock, preventing over-purchase
    }

    private void doCheckout() {
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Your cart is empty. Add some items first!",
                                        "Empty Cart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Re-validate stock right before checkout to catch any concurrent changes
        for (CartItem item : cart.values()) {
            int availableStock = getAvailableStock(item.itemId);
            if (item.quantity > availableStock) {
                JOptionPane.showMessageDialog(this,
                    "Not enough stock for " + item.name + 
                    "\nAvailable: " + availableStock + ", Requested: " + item.quantity +
                    "\nPlease adjust your cart.",
                    "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                return; // Stop checkout if any item is out of stock
            }
        }

        // Calculate total
        double totalAmount = cart.values().stream()
            .mapToDouble(CartItem::getLineTotal)
            .sum();

        NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        int confirm = JOptionPane.showConfirmDialog(this,
            "Confirm purchase of " + cart.size() + " unique items (" + 
            cart.values().stream().mapToInt(item -> item.quantity).sum() + 
            " total units) for " + fmt.format(totalAmount) + "?",
            "Confirm Checkout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        Connection conn = null;
        boolean success = false;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            String updateSql = "UPDATE gold_items SET quantity = quantity - ? WHERE item_id = ? AND quantity >= ?"; // Added quantity check
            PreparedStatement updateStmt = conn.prepareStatement(updateSql);
            
            String insertSaleSql = "INSERT INTO sales (customer_id, item_id, sale_date, total_amount, quantity) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement saleStmt = conn.prepareStatement(insertSaleSql);

            for (CartItem item : cart.values()) {
                // Update inventory
                updateStmt.setInt(1, item.quantity); // Quantity to deduct
                updateStmt.setInt(2, item.itemId);
                updateStmt.setInt(3, item.quantity); // Ensure stock is at least 'quantity' before deducting
                
                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected == 0) {
                    // This means either item_id didn't exist, or stock became insufficient between validation and update
                    throw new SQLException("Failed to update stock for item: " + item.name + ". Insufficient stock or item not found.");
                }

                // Record sale
                saleStmt.setInt(1, currentUser.getUserId());
                saleStmt.setInt(2, item.itemId);
                saleStmt.setDate(3, Date.valueOf(LocalDate.now()));
                saleStmt.setDouble(4, item.getLineTotal()); // Total amount for this specific line item
                saleStmt.setInt(5, item.quantity); // Quantity for this specific line item
                saleStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            success = true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try { 
                    conn.rollback(); // Rollback on error
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace(); // Log rollback error
                }
            }
            JOptionPane.showMessageDialog(this, "Purchase failed: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); // Print full stack trace for debugging
        } finally {
            if (conn != null) {
                try { 
                    conn.setAutoCommit(true); // Reset auto-commit mode
                    conn.close(); 
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace(); // Log close error
                }
            }
        }
        
        if (success) {
            JOptionPane.showMessageDialog(this, 
                "✅ Purchase Successful!\nThank you for your order!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
            cart.clear();
            updateCartTotal();
            loadAvailableItems();
            loadPurchaseHistory();
            tabbedPane.setSelectedIndex(0); // Return to items tab
        }
    }

    private void loadPurchaseHistory() {
        if (historyModel == null) return;
        historyModel.setRowCount(0);

        String sql = "SELECT s.sale_id, g.name, g.weight_grams, g.purity_karat, s.total_amount, s.sale_date " +
                     "FROM sales s JOIN gold_items g ON s.item_id = g.item_id " +
                     "WHERE s.customer_id = ? ORDER BY s.sale_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUser.getUserId());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    historyModel.addRow(new Object[]{
                        rs.getInt("sale_id"),
                        rs.getString("name"),
                        rs.getDouble("weight_grams"),
                        rs.getInt("purity_karat") + "K",
                        "₹" + rs.getDouble("total_amount"), // Already formatted from DB
                        rs.getDate("sale_date")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "History Load Failed: " + e.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // HELPER METHOD FOR STYLED BUTTONS
    private JButton createStyledButton(String text, Color bgColor, Color fgColor, Dimension size) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(BUTTON_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setPreferredSize(size);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    // INNER CLASSES

    // Spinner for quantity in items table
    private class SpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;

        public SpinnerCellEditor() {
            spinner = new JSpinner();
            spinner.setFont(TABLE_FONT);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            int maxQty = 1;
            Object availObj = table.getValueAt(row, 5); // Stock column
            if (availObj instanceof Number) {
                maxQty = ((Number) availObj).intValue();
            }

            // If the item is already in the cart, default to that quantity
            int itemId = (Integer) itemsModel.getValueAt(row, 8);
            CartItem cartItem = cart.get(itemId);
            int initialValue = (cartItem != null) ? cartItem.quantity : 1;

            // Ensure initialValue doesn't exceed available stock
            initialValue = Math.min(initialValue, maxQty);


            spinner.setModel(new SpinnerNumberModel(initialValue, 1, maxQty, 1));
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }
    }

    // Spinner for quantity in cart table
    private class CartSpinnerCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final JSpinner spinner;
        private int currentItemId; // Store the item ID for context

        public CartSpinnerCellEditor() {
            spinner = new JSpinner();
            spinner.setFont(TABLE_FONT);
            spinner.addChangeListener(e -> {
                // This listener ensures that if a user rapidly changes the spinner,
                // the `stopCellEditing` is called to update the cart immediately.
                // However, be cautious with too many updates.
                if (cartTable.isEditing()) { // Only trigger if actively editing a cell
                    // Ideally, you'd want to debounce this or only update on focus lost/enter
                    // For now, let's keep the existing `stopCellEditing` mechanism triggered by `fireEditingStopped`
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentItemId = (Integer) table.getValueAt(row, 7); // ID column
            CartItem item = cart.get(currentItemId);
            
            int initialValue = 1;
            if (item != null) {
                initialValue = item.quantity;
            }

            // Get current stock from DB for validation
            int effectiveAvailableStock = getAvailableStock(currentItemId); 
            
            // The maximum quantity for the spinner should be the current available stock
            spinner.setModel(new SpinnerNumberModel(initialValue, 1, Math.max(1, effectiveAvailableStock), 1)); 
            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }

        @Override
        public boolean stopCellEditing() {
            // Retrieve values using currentItemId, which was set in getTableCellEditorComponent
            int newQuantity = (Integer) getCellEditorValue();
            updateCartItemQuantity(currentItemId, newQuantity);
            return super.stopCellEditing();
        }
    }

    // Add to Cart Button
    private class AddToCartButtonRenderer extends JButton implements TableCellRenderer {
        public AddToCartButtonRenderer() {
            setOpaque(true);
            setFont(BUTTON_FONT);
            setBackground(SUCCESS_GREEN);
            setForeground(Color.WHITE);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int col) {
            setText("🛒 Add to Cart");
            return this;
        }
    }

    private class AddToCartButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int editingRow;

        public AddToCartButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("🛒 Add to Cart");
            button.setOpaque(true);
            button.setFont(BUTTON_FONT);
            button.setBackground(SUCCESS_GREEN);
            button.setForeground(Color.WHITE);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            try {
                int itemId = (Integer) itemsModel.getValueAt(editingRow, 8);
                String name = (String) itemsModel.getValueAt(editingRow, 0);
                double weight = (Double) itemsModel.getValueAt(editingRow, 1);
                int purity = (Integer) itemsModel.getValueAt(editingRow, 2);
                // This is total price for ONE item from the 'gold_items' table
                double totalPricePerUnit = (Double) itemsModel.getValueAt(editingRow, 4); 
                int quantityToAdd = (Integer) itemsModel.getValueAt(editingRow, 6);
                
                // Get current available stock from the database at this moment
                int availableStock = getAvailableStock(itemId);
                
                // Check if item is already in cart
                CartItem existingCartItem = cart.get(itemId);
                int currentCartQuantity = (existingCartItem != null) ? existingCartItem.quantity : 0;

                int potentialNewQuantity = currentCartQuantity + quantityToAdd;

                if (potentialNewQuantity > availableStock) {
                    JOptionPane.showMessageDialog(CustomerDashboardFrame.this,
                        "Only " + availableStock + " items available in stock for " + name + 
                        ". You have " + currentCartQuantity + " in cart. Cannot add " + quantityToAdd + ".",
                        "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                    return "Add to Cart"; // Don't proceed with adding
                }
                
                // If item exists in cart, update its quantity
                if (existingCartItem != null) {
                    existingCartItem.quantity = potentialNewQuantity;
                    cart.put(itemId, existingCartItem); // Put back to update
                } else {
                    // Otherwise, create a new cart item
                    CartItem newItem = new CartItem(itemId, name, weight, purity, totalPricePerUnit, quantityToAdd);
                    cart.put(itemId, newItem);
                }
                
                updateCartTotal(); // Refresh cart display
                
                JOptionPane.showMessageDialog(CustomerDashboardFrame.this,
                    "✅ Added/Updated " + quantityToAdd + " × " + name + " in cart! Total in cart: " + potentialNewQuantity,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                JOptionPane.showMessageDialog(CustomerDashboardFrame.this,
                    "Error adding to cart: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace(); // For debugging
            }
            return "Added!";
        }
    }

    // Cart Action Buttons
    private class CartActionButtonRenderer extends JButton implements TableCellRenderer {
        public CartActionButtonRenderer() {
            setOpaque(true);
            setFont(BUTTON_FONT);
            setBackground(WARNING_RED);
            setForeground(Color.WHITE);
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int col) {
            setText("❌ Remove");
            return this;
        }
    }

    private class CartActionButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int editingRow;

        public CartActionButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton("❌ Remove");
            button.setOpaque(true);
            button.setFont(BUTTON_FONT);
            button.setBackground(WARNING_RED);
            button.setForeground(Color.WHITE);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            editingRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            int itemId = (Integer) cartTable.getValueAt(editingRow, 7);
            removeFromCart(itemId);
            return "Removed!";
        }
    }
}