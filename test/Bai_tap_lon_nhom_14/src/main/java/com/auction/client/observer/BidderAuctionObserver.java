package com.auction.client.observer;

import com.auction.common.model.BidTransaction;
import com.auction.common.observer.AuctionObserver;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.time.format.DateTimeFormatter;

/**
 * Implementation của Observer Pattern phía Bidder.
 * Khi Auction gọi notify, class này tự động cập nhật UI (ListView + Label giá).
 * Dùng Platform.runLater() để đảm bảo thread-safe với JavaFX.
 */
public class BidderAuctionObserver implements AuctionObserver {

    private final ListView<String> bidListView;
    private final Label lblCurrentPrice;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public BidderAuctionObserver(ListView<String> bidListView, Label lblCurrentPrice) {
        this.bidListView = bidListView;
        this.lblCurrentPrice = lblCurrentPrice;
    }

    @Override
    public void onBidPlaced(BidTransaction bid) {
        String entry = String.format("💰 %s đặt: %,.0f $ lúc [%s]",
                bid.getBidderName(),
                bid.getAmount(),
                bid.getTime());
        // Bắt buộc update UI từ JavaFX Application Thread
        Platform.runLater(() -> {
            bidListView.getItems().add(entry);
            // Tự scroll xuống cuối để thấy bid mới nhất
            bidListView.scrollTo(bidListView.getItems().size() - 1);
        });
    }

    @Override
    public void onPriceUpdated(double newPrice) {
        Platform.runLater(() ->
            lblCurrentPrice.setText(String.format("Giá hiện tại: %,.0f $", newPrice))
        );
    }

    @Override
    public void onAuctionEnded(String winnerName, double finalPrice) {
        Platform.runLater(() -> {
            bidListView.getItems().add("🏆 Phiên kết thúc! Người thắng: "
                    + winnerName + " — Giá: " + String.format("%,.0f $", finalPrice));
            lblCurrentPrice.setText("PHIÊN ĐÃ KẾT THÚC");
            lblCurrentPrice.setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
        });
    }
}
