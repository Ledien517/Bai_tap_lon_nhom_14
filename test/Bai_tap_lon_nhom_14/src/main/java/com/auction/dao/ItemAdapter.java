package com.auction.dao;

import com.auction.common.model.*;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

/**
 * Custom Gson deserializer cho Item.
 *
 * FIX:
 * 1. Đọc field "sellerUsername" từ JSON để khôi phục Seller tối thiểu (chỉ có username)
 * 2. Dùng constructor validate=false để items cũ (startTime ở quá khứ) không bị reject
 * 3. Đọc "minIncrement" từ JSON (trước đây bị mất)
 */
public class ItemAdapter implements JsonDeserializer<Item> {

    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        // --- Đọc các field chung ---
        String id          = getStr(obj, "id", "");
        String name        = getStr(obj, "name", "");
        String description = getStr(obj, "description", "Mô tả");
        double startPrice  = getDbl(obj, "startingPrice", 0);
        double curHighest  = getDbl(obj, "currentHighestBid", startPrice);
        double minInc      = getDbl(obj, "minIncrement", 1000);
        LocalDateTime startTime = context.deserialize(obj.get("startTime"), LocalDateTime.class);
        LocalDateTime endTime   = context.deserialize(obj.get("endTime"),   LocalDateTime.class);

        // Khôi phục Seller tối thiểu từ "sellerUsername" (nếu có)
        Seller seller = null;
        if (obj.has("sellerUsername") && !obj.get("sellerUsername").isJsonNull()) {
            String username = obj.get("sellerUsername").getAsString();
            seller = new Seller(username, "", null);
        }

        // Nhận diện loại Item và tạo bằng constructor validate=false
        Item item;
        if (obj.has("artist") || obj.has("painter") || obj.has("material")) {
            String artist = getStr(obj, "artist", "Unknown");
            item = new Art(seller, id, name, description, startPrice,
                    startTime, endTime, minInc, artist, false);
        } else if (obj.has("brand") || obj.has("engine") || obj.has("mileage")) {
            String brand = getStr(obj, "brand", "Unknown");
            item = new Vehicle(seller, id, name, description, startPrice,
                    startTime, endTime, minInc, brand, false);
        } else {
            // Electronics hoặc không nhận diện được
            int warranty = obj.has("warrantyMonths") ? obj.get("warrantyMonths").getAsInt() : 0;
            item = new Electronics(seller, id, name, description, startPrice,
                    startTime, endTime, minInc, warranty, false);
        }

        // Khôi phục giá hiện tại (có thể đã được đặt giá trước đây)
        item.setCurrentHighestBid(curHighest);
        return item;
    }

    private String getStr(JsonObject obj, String key, String def) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : def;
    }

    private double getDbl(JsonObject obj, String key, double def) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsDouble() : def;
    }
}