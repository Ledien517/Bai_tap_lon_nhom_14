package com.auction.dao;

import com.auction.common.entity.Item;
import java.util.ArrayList;
import java.util.List;

public class InMemoryItemDAO implements ItemDAO {

    // Dùng một List ảo trên RAM để làm Database tạm thời
    private final List<Item> database = new ArrayList<>();

    @Override
    public void saveItem(Item item) {
        database.add(item);
        System.out.println("=> [Thành công] Đã lưu sản phẩm vào kho: " + item.getName());
    }

    @Override
    public Item getItemById(String id) {
        for (Item item : database) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        System.out.println("=> [Lỗi] Không tìm thấy sản phẩm có ID: " + id);
        return null;
    }

    @Override
    public List<Item> getAllItems() {
        return database;
    }

    @Override
    public void updateItem(Item item) {
        // Tạm thời để trống
    }

    @Override
    public void deleteItem(String id) {
        // Xóa sản phẩm nếu ID trùng khớp
        database.removeIf(item -> item.getId().equals(id));
        System.out.println("=> Đã xóa sản phẩm có ID: " + id);
    }
}