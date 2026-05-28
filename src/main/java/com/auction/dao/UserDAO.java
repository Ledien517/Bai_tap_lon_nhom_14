package com.auction.dao;

import com.auction.common.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private static void ensureSuperAdminExists() {
        String sql = "INSERT INTO users (username, password, role, available_balance, frozen_balance, status) " +
                     "VALUES ('superadmin', 'admin123', 'ADMIN', 0, 0, 'ACTIVE') " +
                     "ON DUPLICATE KEY UPDATE password = 'admin123', role = 'ADMIN', status = 'ACTIVE'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Không thể đảm bảo tài khoản SuperAdmin tồn tại: " + e.getMessage(), e);
        }
    }

    public static synchronized boolean isUserExists(String username) {
        if (username == null) return false;
        if ("superadmin".equalsIgnoreCase(username.trim())) {
            ensureSuperAdminExists();
        }
        String sql = "SELECT username FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi kiểm tra sự tồn tại của tài khoản '" + username + "': " + e.getMessage(), e);
        }
    }

    public static synchronized User getUser(String username) {
        if (username == null) return null;
        if ("superadmin".equalsIgnoreCase(username.trim())) {
            ensureSuperAdminExists();
        }
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String pass = rs.getString("password");
                    if (pass != null) pass = pass.trim();
                    String roleStr = rs.getString("role");
                    String status = rs.getString("status");
                    Role role = Role.valueOf(roleStr);
                    User user = null;
                    if (role == Role.BIDDER) {
                        double avail = rs.getDouble("available_balance");
                        double frozen = rs.getDouble("frozen_balance");
                        user = new Bidder(rs.getString("username"), pass, avail, frozen);
                    } else if (role == Role.SELLER) {
                        double balance = rs.getDouble("available_balance");
                        user = new Seller(rs.getString("username"), pass, balance);
                    } else if (role == Role.ADMIN) {
                        user = new Admin(rs.getString("username"), pass);
                    }
                    if (user != null) {
                        user.setStatus(status != null ? status : "ACTIVE");
                    }
                    return user;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn tài khoản '" + username + "': " + e.getMessage(), e);
        }
        return null;
    }

    public static synchronized void saveUser(User user) {
        if (user == null || user.getUsername() == null) return;
        String sql = "INSERT INTO users (username, password, role, available_balance, frozen_balance, status) VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE password = ?, role = ?, available_balance = ?, frozen_balance = ?, status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername().trim().toLowerCase());
            pstmt.setString(2, user.getPassword() != null ? user.getPassword().trim() : "");
            pstmt.setString(3, user.getRole().name());
            
            double avail = 0;
            double frozen = 0;
            if (user instanceof Bidder) {
                Bidder b = (Bidder) user;
                avail = b.getAvailableBalance();
                frozen = b.getFrozenBalance();
            } else if (user instanceof Seller) {
                Seller s = (Seller) user;
                avail = s.getBalance();
            }
            pstmt.setDouble(4, avail);
            pstmt.setDouble(5, frozen);
            pstmt.setString(6, user.getStatus());
            
            pstmt.setString(7, user.getPassword() != null ? user.getPassword().trim() : "");
            pstmt.setString(8, user.getRole().name());
            pstmt.setDouble(9, avail);
            pstmt.setDouble(10, frozen);
            pstmt.setString(11, user.getStatus());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu tài khoản '" + user.getUsername() + "': " + e.getMessage(), e);
        }
    }

    public static synchronized User validateUser(String username, String password) {
        User user = getUser(username);
        if (user != null) {
            String dbPass = user.getPassword() != null ? user.getPassword().trim() : "";
            String inputPass = password != null ? password.trim() : "";
            if (dbPass.equals(inputPass)) {
                return user;
            }
        }
        return null;
    }

    public static synchronized List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String username = rs.getString("username");
                String pass = rs.getString("password");
                if (pass != null) pass = pass.trim();
                String roleStr = rs.getString("role");
                String status = rs.getString("status");
                
                User user = null;
                Role role = Role.valueOf(roleStr);
                if (role == Role.BIDDER) {
                    double avail = rs.getDouble("available_balance");
                    double frozen = rs.getDouble("frozen_balance");
                    user = new Bidder(username, pass, avail, frozen);
                } else if (role == Role.SELLER) {
                    double balance = rs.getDouble("available_balance");
                    user = new Seller(username, pass, balance);
                } else if (role == Role.ADMIN) {
                    user = new Admin(username, pass);
                }
                
                if (user != null) {
                    user.setStatus(status != null ? status : "ACTIVE");
                    list.add(user);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách tài khoản: " + e.getMessage(), e);
        }
        return list;
    }

    public static synchronized void banUser(String username) {
        if (username == null || "superadmin".equalsIgnoreCase(username.trim())) return;
        String sql = "UPDATE users SET status = 'BANNED' WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim().toLowerCase());
            pstmt.executeUpdate();
            System.out.println("[UserDAO] Đã BAN người dùng: " + username);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi khóa tài khoản '" + username + "': " + e.getMessage(), e);
        }
    }

    public static synchronized void unbanUser(String username) {
        if (username == null) return;
        String sql = "UPDATE users SET status = 'ACTIVE' WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim().toLowerCase());
            pstmt.executeUpdate();
            System.out.println("[UserDAO] Đã UNBAN người dùng: " + username);
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi mở khóa tài khoản '" + username + "': " + e.getMessage(), e);
        }
    }
}