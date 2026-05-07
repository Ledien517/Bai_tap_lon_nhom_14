package com.auction.common.observer;

import com.auction.common.model.BidTransaction;

/**
 * Observer Pattern Interface
 * Được gọi bởi Auction khi có sự kiện thay đổi (bid mới, kết thúc, v.v.)
 */
public interface AuctionObserver {
    void onBidPlaced(BidTransaction bid);
    void onPriceUpdated(double newPrice);
    void onAuctionEnded(String winnerName, double finalPrice);
}
