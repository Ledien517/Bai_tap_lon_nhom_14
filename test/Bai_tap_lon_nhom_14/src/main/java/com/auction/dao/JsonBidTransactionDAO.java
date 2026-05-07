package com.auction.dao;

import com.auction.common.model.BidTransaction;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Lưu lịch sử đặt giá vào file JSON trong thư mục bid_history/.
 * Mỗi sản phẩm một file: bid_history/auction_<itemId>_bids.json
 *
 * Vì BidTransaction có circular reference (Bidder → User → Serializable),
 * chúng ta lưu dưới dạng String đã format sẵn thay vì serialize nguyên đối tượng.
 */
public class JsonBidTransactionDAO implements BidTransactionDAO {

    private static final String DIR = "bid_history";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public JsonBidTransactionDAO() {
        // Tạo thư mục nếu chưa có
        File dir = new File(DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String getFilePath(String itemId) {
        return DIR + "/auction_" + itemId + "_bids.json";
    }

    @Override
    public void saveBid(BidTransaction bid, String itemId) {
        List<String> history = loadRaw(itemId);
        String entry = String.format("💰 %s đặt: %,.0f $ lúc [%s]",
                bid.getBidderName(), bid.getAmount(), bid.getTime());
        history.add(entry);
        writeToFile(itemId, history);
    }

    @Override
    public List<String> getBidHistoryDisplay(String itemId) {
        return loadRaw(itemId);
    }

    private List<String> loadRaw(String itemId) {
        File file = new File(getFilePath(itemId));
        if (!file.exists()) return new ArrayList<>();
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<List<String>>() {}.getType();
            List<String> result = gson.fromJson(reader, listType);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            System.out.println("[BidDAO] Lỗi đọc file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void writeToFile(String itemId, List<String> history) {
        try (Writer writer = new FileWriter(getFilePath(itemId))) {
            gson.toJson(history, writer);
        } catch (IOException e) {
            System.out.println("[BidDAO] Lỗi ghi file: " + e.getMessage());
        }
    }
}
