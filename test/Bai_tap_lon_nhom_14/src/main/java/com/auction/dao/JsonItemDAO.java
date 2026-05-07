package com.auction.dao;

import com.auction.common.model.Item;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FIX:
 * 1. Thêm custom serializer để ghi "sellerUsername" và "minIncrement" vào JSON
 *    (trước đây Gson bỏ qua field protected của Item)
 * 2. Dùng ItemAdapter khi đọc (giữ nguyên)
 */
public class JsonItemDAO implements ItemDAO {

    private static final String FILE_PATH = "items.json";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(Item.class, new ItemAdapter())
            .registerTypeHierarchyAdapter(Item.class, new ItemSerializer())
            .setPrettyPrinting()
            .create();

    private final List<Item> database = new ArrayList<>();

    public JsonItemDAO() {
        loadDataFromFile();
    }

    private void loadDataFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Item>>() {}.getType();
            List<Item> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                database.addAll(loaded);
                System.out.println("=> Đã tải " + database.size() + " sản phẩm từ file.");
            }
        } catch (Exception e) {
            System.out.println("=> [Lỗi] Không thể đọc dữ liệu: " + e.getMessage());
        }
    }

    @Override
    public void saveItem(Item item) {
        database.add(item);
        persist();
        System.out.println("=> [Thành công] Đã lưu sản phẩm: " + item.getId());
    }

    @Override
    public Item getItemById(String id) {
        return database.stream().filter(i -> i.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<Item> getAllItems() {
        return database;
    }

    @Override
    public void updateItem(Item item) {
        deleteItem(item.getId());
        saveItem(item);
    }

    @Override
    public void deleteItem(String id) {
        database.removeIf(i -> i.getId().equals(id));
        persist();
    }

    private void persist() {
        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(database, writer);
        } catch (IOException e) {
            System.out.println("=> [Lỗi] Không thể ghi file: " + e.getMessage());
        }
    }

    // ===== Custom Serializer: thêm sellerUsername + minIncrement vào JSON =====
    private static class ItemSerializer implements JsonSerializer<Item> {
        @Override
        public JsonElement serialize(Item src, Type typeOfSrc, JsonSerializationContext context) {
            // Dùng Gson mặc định để serialize toàn bộ fields của subclass
            Gson baseGson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            JsonObject obj = baseGson.toJsonTree(src).getAsJsonObject();

            // Thêm sellerUsername để ItemAdapter có thể đọc lại
            if (src.getSeller() != null) {
                obj.addProperty("sellerUsername", src.getSeller().getUsername());
            }
            // Thêm minIncrement (field final không tự serialize được trong mọi trường hợp)
            obj.addProperty("minIncrement", src.getMinIncrement());
            return obj;
        }
    }
}