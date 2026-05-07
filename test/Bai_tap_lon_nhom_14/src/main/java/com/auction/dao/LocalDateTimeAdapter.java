package com.auction.dao;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    // Định dạng ngày tháng, ví dụ: "2026-04-26T10:49:00"
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext context) {
        // Biến LocalDateTime thành chuỗi chữ
        return new JsonPrimitive(localDateTime.format(FORMATTER));
    }

    @Override
    public LocalDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        // Biến chuỗi chữ ngược lại thành LocalDateTime
        return LocalDateTime.parse(jsonElement.getAsString(), FORMATTER);
    }
}