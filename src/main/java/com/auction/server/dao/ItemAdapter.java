package com.auction.dao;

import com.auction.common.entity.Art;
import com.auction.common.entity.Electronics;
import com.auction.common.entity.Item;
import com.auction.common.entity.Vehicle;
import com.google.gson.*;

import java.lang.reflect.Type;

public class ItemAdapter implements JsonDeserializer<Item> {
    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Gson sẽ "ngửi" xem trong chuỗi JSON có thuộc tính đặc trưng nào không để ép về đúng class con
        // Ghi chú: Nếu các class Art, Electronics... của bạn dùng tên biến khác, bạn có thể tự sửa lại chữ ở trong ngoặc kép nhé.
        if (jsonObject.has("artist") || jsonObject.has("painter") || jsonObject.has("material")) {
            return context.deserialize(jsonObject, Art.class);
        } else if (jsonObject.has("brand") || jsonObject.has("warrantyMonths")) {
            return context.deserialize(jsonObject, Electronics.class);
        } else if (jsonObject.has("engine") || jsonObject.has("mileage")) {
            return context.deserialize(jsonObject, Vehicle.class);
        }

        // Mặc định tạm thời ép về Art nếu không nhận diện được
        return context.deserialize(jsonObject, Art.class);
    }
}