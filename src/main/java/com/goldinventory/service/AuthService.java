package com.goldinventory.service;

import com.goldinventory.database.DBConnection;
import javax.swing.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;

public class AuthService {

    // LOGIN
    public static User authenticate(String username, String password) {
        String sql = "SELECT user_id, username, password, role FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                String inputHash = hashPassword(password);
                if (storedHash.equals(inputHash)) {
                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Database error during authentication: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    // REGISTER
    public static boolean register(String username, String password, String role) {
        try (Connection conn = DBConnection.getConnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT * FROM users WHERE username=?");
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) return false; // username already exists

            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users(username, password, role) VALUES (?, ?, ?)"
            );
            stmt.setString(1, username);
            stmt.setString(2, hashPassword(password));
            stmt.setString(3, role);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // HASHING
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    // USER CLASS
    public static class User {
        private final int userId;
        private final String username;
        private final String role;

        public User(int userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}
