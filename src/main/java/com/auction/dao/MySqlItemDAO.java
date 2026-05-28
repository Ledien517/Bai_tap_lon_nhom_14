package com.auction.dao;

import com.auction.common.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MySqlItemDAO implements ItemDAO {
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(Item.class, new ItemAdapter())
            .create();

    public MySqlItemDAO() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS auction_items (" +
                     "id VARCHAR(255) PRIMARY KEY, " +
                     "json_data LONGTEXT NOT NULL)";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Không thể tạo bảng auction_items: " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized void saveItem(Item item) {
        String sql = "INSERT INTO auction_items (id, json_data) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE json_data = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String json = gson.toJson(item, Item.class);
            pstmt.setString(1, item.getId());
            pstmt.setString(2, json);
            pstmt.setString(3, json);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lưu sản phẩm '" + item.getName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public synchronized Item getItemById(String id) {
        String sql = "SELECT json_data FROM auction_items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String json = rs.getString("json_data");
                    return gson.fromJson(json, Item.class);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi truy vấn sản phẩm ID '" + id + "': " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public synchronized List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT json_data FROM auction_items";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String json = rs.getString("json_data");
                Item item = gson.fromJson(json, Item.class);
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi lấy danh sách sản phẩm đấu giá: " + e.getMessage(), e);
        }
        return items;
    }

    @Override
    public synchronized void updateItem(Item item) {
        saveItem(item); // MySQL có "ON DUPLICATE KEY UPDATE" nên update giống hệt save
    }

    @Override
    public synchronized void deleteItem(String id) {
        String sql = "DELETE FROM auction_items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            throw new RuntimeException("Lỗi xóa sản phẩm ID '" + id + "': " + e.getMessage(), e);
        }
    }
}
