package com.auction.dao;

import com.auction.common.entity.Item;
import java.util.List;

public interface ItemDAO {
    // Lưu một sản phẩm mới vào kho
    void saveItem(Item item);

    // Tìm sản phẩm theo mã ID
    Item getItemById(String id);

    // Lấy ra danh sách toàn bộ sản phẩm
    List<Item> getAllItems();

    // Cập nhật thông tin sản phẩm
    void updateItem(Item item);

    // Xóa sản phẩm khỏi kho
    void deleteItem(String id);
}