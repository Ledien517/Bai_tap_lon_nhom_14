package com.auction.common.factory;

import com.auction.common.model.Art;
import com.auction.common.model.Item;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

// Import các hàm kiểm tra của JUnit
import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

    // Annotation @Test báo cho IntelliJ biết đây là một kịch bản kiểm thử
    @Test
    public void testCreateArt_Success() {
        // 1. Chuẩn bị dữ liệu (Arrange)
        String id = "ART001";
        String name = "Mona Lisa";
        String desc = "Bức tranh nổi tiếng";
        double price = 1000000;
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(7);
        String artist = "Leonardo da Vinci";

        // 2. Thực thi phương thức cần test (Act)
        Item item = ItemFactory.createItem("art", id, name, desc, price, start, end, artist);

        // 3. Kiểm tra kết quả (Assert)
        assertNotNull(item, "Item không được phép null");
        assertTrue(item instanceof Art, "Item phải là một thể hiện của lớp Art");

        // Ép kiểu để kiểm tra thuộc tính riêng của Art
        Art artItem = (Art) item;
        assertEquals("Mona Lisa", artItem.getName(), "Tên sản phẩm không khớp");
        assertEquals("Leonardo da Vinci", artItem.getArtist(), "Tên họa sĩ không khớp");
    }
}