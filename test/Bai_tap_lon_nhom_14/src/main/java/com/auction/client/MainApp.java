package com.auction.client;

import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

public class MainApp extends Application {

    // ✅ PHẢI là static để LoginController, RegisterController gọi được
    private static Stage primaryStage;
    private static final Map<String, Auction> activeAuctions = new HashMap<>();

    // 2. Hàm đăng ký một phiên đấu giá mới
    public static void registerAuction(String itemId, Auction auction) {
        activeAuctions.put(itemId, auction);
    }

    // 3. Hàm lấy phiên đấu giá dựa trên Item
    public static Auction getAuctionForItem(Item item) {
        if (item == null) return null;
        return activeAuctions.get(item.getId());
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến");
        // Bắt đầu bằng màn hình đăng nhập
        switchScene("/com/auction/client/view/LoginView.fxml");
        primaryStage.show();
    }

    // ✅ PHẢI là static để các Controller khác gọi được
    public static void switchScene(String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(MainApp.class.getResource(fxmlPath));

        // Màn hình quản lý và Sàn đấu giá thì rộng hơn
        // BỒI THÊM: Thêm kiểm tra "BidderView" để giao diện không bị co xíu
        if (fxmlPath.contains("ItemManagement") || fxmlPath.contains("BidderView")) {
            primaryStage.setScene(new Scene(root, 850, 600)); // Tăng lên 850 để đủ chỗ cho các cột TableView
            primaryStage.setTitle("Hệ thống Đấu giá - Nhóm 14");
        } else {
            primaryStage.setScene(new Scene(root, 350, 300));
        }
    }

    // --- BỒI THÊM: Phương thức để lấy Stage chính (Cần thiết cho LoginController) ---
    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}