package com.auction.dao;

import com.auction.common.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    public static synchronized boolean isUserExists(String username) {
        if (username == null) return false;
        String sql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO Lỗi] isUserExists: " + e.getMessage());
        }
        return false;
    }

    public static synchronized User getUser(String username) {
        if (username == null) return null;
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String pass = rs.getString("password");
                    String roleStr = rs.getString("role");
                    Role role = Role.valueOf(roleStr);
                    if (role == Role.BIDDER) {
                        return new Bidder(rs.getString("username"), pass);
                    } else if (role == Role.SELLER) {
                        return new Seller(rs.getString("username"), pass);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDAO Lỗi] getUser: " + e.getMessage());
        }
        return null;
    }

    public static synchronized void saveUser(User user) {
        if (user == null || user.getUsername() == null) return;
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE password = ?, role = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername().toLowerCase());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getPassword());
            pstmt.setString(5, user.getRole().name());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDAO Lỗi] saveUser: " + e.getMessage());
        }
    }

    public static synchronized User validateUser(String username, String password) {
        User user = getUser(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}