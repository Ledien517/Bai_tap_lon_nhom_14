package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.*;
import java.time.LocalDateTime;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class AuctionController {
    @FXML private Label lblItemID;
    @FXML private Label lblItemName;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStatus;
    @FXML private TextField txtBidAmount;
    @FXML private TextArea txtLog;

    private Auction auction;
    private Bidder currentBidder;

    @FXML
    public void initialize() {
        // Khởi tạo dữ liệu mẫu dựa trên các class của bạn
        Seller seller = new Seller("admin_seller", "Minh Cường", "123", "seller@auction.com");
        Item laptop = new Item("LAP-2026", "MacBook Pro M3", "Máy mới nguyên seal");
        
        // Giả sử bạn là người đi đấu giá
        currentBidder = new Bidder("user_01", "Người Mua A", "pass", "buyer@email.com");
        currentBidder.setAvailableBalance(1000000000);

        // Tạo phiên đấu giá (Khởi điểm 1000, bước giá 100)
        auction = new Auction(seller, laptop, 1000.0, 100.0, 
                             LocalDateTime.now(), LocalDateTime.now().plusMinutes(4));

        // Hiển thị thông tin lên giao diện
        lblItemID.setText(laptop.getItemID());
        lblItemName.setText(laptop.getItemName());
        refreshUI();
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            refreshUI(); // Cứ mỗi 1 giây tự động làm mới giao diện một lần
        }));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void refreshUI() {
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        lblStatus.setText(auction.getStatusDisplay());
        txtLog.setText(auction.getInfo());
        txtLog.setScrollTop(Double.MAX_VALUE); // Luôn cuộn xuống dòng mới nhất
    }

    @FXML
    private void handlePlaceBid() {
        try {
            double amount = Double.parseDouble(txtBidAmount.getText());
            
            // Thực hiện đặt giá
            BidTransaction bid = new BidTransaction(currentBidder, amount);
            auction.placeBid(bid);
            
            refreshUI();
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số tiền hợp lệ!");
        } catch (Exception e) {
            showAlert("Thông báo", e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}