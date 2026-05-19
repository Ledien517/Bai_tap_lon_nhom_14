
package com.auction.client;

import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import com.auction.common.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class MainApp extends Application {

    private static Stage primaryStage;
    private static final Map<String, Auction> activeAuctions = new HashMap<>();
    private static User currentUser; // ← THÊM MỚI: lưu user đang đăng nhập

    // Lưu user vào session
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    // Lấy user từ session
    public static User getCurrentUser() {
        return currentUser;
    }

    public static void registerAuction(String itemId, Auction auction) {
        activeAuctions.put(itemId, auction);
    }

    public static Auction getAuctionForItem(Item item) {
        if (item == null) return null;
        return activeAuctions.get(item.getId());
    }

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến");
        switchScene("/com/auction/client/view/LoginView.fxml");
        primaryStage.show();
    }

    public static void switchScene(String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(MainApp.class.getResource(fxmlPath));

        if (fxmlPath.contains("ItemManagement") || fxmlPath.contains("BidderView")) {
            primaryStage.setScene(new Scene(root, 850, 600));
            primaryStage.setTitle("Hệ thống Đấu giá - Nhóm 14");
        } else {
            primaryStage.setScene(new Scene(root, 350, 300));
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
