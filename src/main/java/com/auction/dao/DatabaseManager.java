package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    // Đọc thông tin kết nối từ biến môi trường Railway
    // Nếu không có (chạy local) thì dùng giá trị mặc định
    private static final String HOST = System.getenv().getOrDefault("MYSQL_HOST", "kodama.proxy.rlwy.net");
    private static final String PORT = System.getenv().getOrDefault("MYSQL_PORT", "16400");
    private static final String DB_NAME = System.getenv().getOrDefault("MYSQL_DATABASE", "railway");
    private static final String USER = System.getenv().getOrDefault("MYSQL_USER", "root");
    private static final String PASS = System.getenv().getOrDefault("MYSQL_PASSWORD", "NwlOmLVZlDDneACUTvApzOoYTlJsfJYh");

    // Chuỗi kết nối JDBC
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    static {
        // Tự động khởi tạo bảng ngay khi ứng dụng (hoặc DAO) chạy lần đầu
        initializeDatabase();
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    private static void initializeDatabase() {
        System.out.println("[DatabaseManager] Đang kiểm tra cấu trúc cơ sở dữ liệu trên Railway...");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            // 1. Tạo bảng Users
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(255) PRIMARY KEY, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "role VARCHAR(50) NOT NULL, " +
                    "available_balance DOUBLE DEFAULT 0, " +
                    "frozen_balance DOUBLE DEFAULT 0" +
                    ")";
            stmt.executeUpdate(createUsersTable);
            
            // Cập nhật bảng users (cho các DB cũ chưa có cột này)
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN available_balance DOUBLE DEFAULT 0");
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN frozen_balance DOUBLE DEFAULT 0");
            } catch (SQLException ignored) {
                // Ignore if columns already exist
            }


            // 2. Tạo bảng Items
            String createItemsTable = "CREATE TABLE IF NOT EXISTS items (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "description TEXT, " +
                    "starting_price DOUBLE NOT NULL, " +
                    "start_time DATETIME NOT NULL, " +
                    "end_time DATETIME NOT NULL, " +
                    "min_increment DOUBLE NOT NULL, " +
                    "seller_username VARCHAR(255) NOT NULL, " +
                    "item_type VARCHAR(50) NOT NULL, " +
                    "FOREIGN KEY (seller_username) REFERENCES users(username) ON DELETE CASCADE" +
                    ")";
            stmt.executeUpdate(createItemsTable);

            // 3. Tạo bảng Bids (Lịch sử đặt giá)
            String createBidsTable = "CREATE TABLE IF NOT EXISTS bids (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "item_id VARCHAR(255) NOT NULL, " +
                    "bidder_username VARCHAR(255) NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "bid_time DATETIME NOT NULL, " +
                    "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (bidder_username) REFERENCES users(username) ON DELETE CASCADE" +
                    ")";
            stmt.executeUpdate(createBidsTable);

            // 4. Tạo bảng AutoBids (Đấu giá tự động)
            String createAutoBidsTable = "CREATE TABLE IF NOT EXISTS auto_bids (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "item_id VARCHAR(255) NOT NULL, " +
                    "bidder_username VARCHAR(255) NOT NULL, " +
                    "max_bid DOUBLE NOT NULL, " +
                    "increment DOUBLE NOT NULL, " +
                    "registered_time DATETIME NOT NULL, " +
                    "FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (bidder_username) REFERENCES users(username) ON DELETE CASCADE" +
                    ")";
            stmt.executeUpdate(createAutoBidsTable);

            System.out.println("[DatabaseManager] Khởi tạo cơ sở dữ liệu MySQL thành công!");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager Lỗi] Không thể khởi tạo database: " + e.getMessage());
        }
    }
}
