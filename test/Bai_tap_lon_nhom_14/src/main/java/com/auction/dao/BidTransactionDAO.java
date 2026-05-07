package com.auction.dao;

import com.auction.common.model.BidTransaction;

import java.util.List;

/**
 * DAO Pattern Interface cho lịch sử đặt giá.
 * Tách biệt logic lưu trữ khỏi business logic.
 */
public interface BidTransactionDAO {
    /**
     * Lưu một giao dịch đặt giá vào persistence.
     * @param bid giao dịch cần lưu
     * @param itemId mã sản phẩm (dùng làm key file)
     */
    void saveBid(BidTransaction bid, String itemId);

    /**
     * Lấy toàn bộ lịch sử đặt giá của một sản phẩm.
     * @param itemId mã sản phẩm
     */
    List<String> getBidHistoryDisplay(String itemId);
}
