package com.auction.common.factory;

import com.auction.common.model.*;

import java.time.LocalDateTime;

public class ItemFactory {

    // Phương thức Factory (Factory Method)
    public static Item createItem(Seller seller, String type, String id, String name, String description,
                                  double startingPrice, LocalDateTime startTime, LocalDateTime endTime,
                                  double minIncrement, String extraAttribute) {
        // Switch kiểu mới: Gọn hơn và không cần lệnh break
        return switch (type.toLowerCase()) {
            case "art" -> new Art(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, extraAttribute);
            case "electronics" -> new Electronics(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, Integer.parseInt(extraAttribute));
            case "vehicle" -> new Vehicle(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, extraAttribute);
            default -> throw new IllegalArgumentException("Loại sản phẩm không xác định: " + type);
        };
    }
}