package com.auction.dao;

import com.auction.common.factory.ItemFactory;
import com.auction.common.model.Item;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryItemDAOTest {

    @Test
    public void testSaveAndRetrieveItem() {
        // 1. Khởi tạo cái "kho"
        ItemDAO dao = new JsonItemDAO();

        // 2. Nhờ Factory tạo ra 1 bức tranh
        Item item = ItemFactory.createItem("art", "A01", "Bức tranh hoa hướng dương", "Tranh sơn dầu",
                500.0, LocalDateTime.now(), LocalDateTime.now().plusDays(7), "Van Gogh");

        // 3. Ném nó vào kho
        dao.saveItem(item);

        // 4. Lấy từ kho ra xem có đúng món đồ đó không
        Item retrievedItem = dao.getItemById("A01");
        assertNotNull(retrievedItem, "Sản phẩm lấy ra không được null");
        assertEquals("Bức tranh hoa hướng dương", retrievedItem.getName(), "Tên sản phẩm phải khớp");

        // 5. Kiểm tra xem kho có đúng 1 món không
        List<Item> allItems = dao.getAllItems();
        assertEquals(1, allItems.size(), "Kho phải có chính xác 1 sản phẩm");
    }
}