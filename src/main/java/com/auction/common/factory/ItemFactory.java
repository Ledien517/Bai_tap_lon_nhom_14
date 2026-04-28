package com.auction.common.factory;

import com.auction.common.entity.Art;
import com.auction.common.entity.Electronics;
import com.auction.common.entity.Item;
import com.auction.common.entity.Vehicle;

import java.time.LocalDateTime;

public class ItemFactory {

    // Phương thức Factory (Factory Method)
    public static Item createItem(String type, String id, String name, String description,
                                  double startingPrice, LocalDateTime startTime, LocalDateTime endTime,
                                  String extraAttribute) {
        // Switch kiểu mới: Gọn hơn và không cần lệnh break
        return switch (type.toLowerCase()) {
            case "art" -> new Art(id, name, description, startingPrice, startTime, endTime, extraAttribute);
            case "electronics" -> new Electronics(id, name, description, startingPrice, startTime, endTime, Integer.parseInt(extraAttribute));
            case "vehicle" -> new Vehicle(id, name, description, startingPrice, startTime, endTime, extraAttribute);
            default -> throw new IllegalArgumentException("Loại sản phẩm không xác định: " + type);
        };
    }
}