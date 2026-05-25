package com.auction.dao;

import com.auction.common.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class JsonItemDAO implements ItemDAO {

    // Tên file sẽ được sinh ra ngay trong thư mục gốc của project
    private static final String FILE_PATH = "items.json";

    // Khởi tạo công cụ Gson với tùy chọn in ra file cho đẹp, dễ đọc (PrettyPrinting)
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(java.time.LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(Item.class, new ItemAdapter())
            .setPrettyPrinting()
            .create();

    // Dùng 1 List trung gian để gom các sản phẩm lại trước khi ghi ra file
    private final List<Item> database = new ArrayList<>();
    // Constructor: Hàm này tự động chạy ngay khi JsonItemDAO được tạo ra
    public JsonItemDAO() {
        loadDataFromFile();
    }

    // Hàm thực hiện việc mở file và đọc dữ liệu
    private void loadDataFromFile() {
        java.io.File file = new java.io.File(FILE_PATH);

        // Nếu file có tồn tại thì mới đọc
        if (file.exists()) {
            try (java.io.Reader reader = new java.io.FileReader(file)) {
                // Hướng dẫn Gson cách dịch chuỗi JSON thành một Danh sách (List) các Item
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.ArrayList<Item>>(){}.getType();
                java.util.List<Item> loadedItems = gson.fromJson(reader, listType);

                // Nếu đọc thành công và có dữ liệu, thêm tất cả vào biến database
                if (loadedItems != null) {
                    database.addAll(loadedItems);
                    System.out.println("=> [Hệ thống] Đã tải thành công dữ liệu từ ổ cứng lên giao diện!");
                }
            } catch (Exception e) {
                System.out.println("=> [Lỗi] Không thể đọc dữ liệu: " + e.getMessage());
            }
        }
    }
    @Override
    public synchronized void saveItem(Item item) {
        // Thêm sản phẩm vào danh sách
        database.add(item);

        // Mở file ra và bắt đầu ghi
        try (Writer writer = new FileWriter(FILE_PATH)) {
            // Gson sẽ tự động dịch toàn bộ cái danh sách (database) thành chữ JSON và lưu vào file
            gson.toJson(database, writer);
            System.out.println("=> [Thành công] Đã lưu sản phẩm vào file: " + FILE_PATH);
        } catch (IOException e) {
            System.out.println("=> [Lỗi] Không thể ghi file: " + e.getMessage());
        }
    }

    // Các hàm này tạm thời để trống, chúng ta sẽ viết code cho nó sau
    @Override
    public Item getItemById(String id) {
        for (Item item : database) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    @Override
    public List<Item> getAllItems() {
        return database;
    }

    @Override
    public void updateItem(Item item) {
        // Logic cập nhật: Xóa cái cũ, thêm cái mới rồi ghi lại ra file
        deleteItem(item.getId());
        saveItem(item);
    }

    @Override
    public synchronized void deleteItem(String id) {
        // Xóa sản phẩm khỏi danh sách
        database.removeIf(item -> item.getId().equals(id));

        // Mở file ra và ghi đè danh sách mới (đã xóa) lên
        try (java.io.Writer writer = new java.io.FileWriter(FILE_PATH)) {
            gson.toJson(database, writer);
        } catch (java.io.IOException e) {
            System.out.println("=> [Lỗi] Không thể cập nhật file: " + e.getMessage());
        }
    }
}