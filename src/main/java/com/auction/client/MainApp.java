    
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
            try {
                String host = "zephyr.proxy.rlwy.net";
                int port = 51807;
                NetworkClient.getInstance().connect(host, port);
                System.out.println("[AutoConfig] Đã kết nối tới Server trên Railway: " + host + ":" + port);
            } catch (Exception e) {
                System.err.println("[MainApp Lỗi] Không thể kết nối tới Server: " + e.getMessage());
            }
            primaryStage = stage;
            primaryStage.setTitle("Hệ thống Đấu giá Trực tuyến");
            switchScene("/com/auction/client/view/LoginView.fxml");
            primaryStage.show();
        }

        @Override
        public void stop() throws Exception {
            NetworkClient.getInstance().disconnect();
            super.stop();
        }
    
        public static void switchScene(String fxmlPath) throws Exception {
            Parent root = FXMLLoader.load(MainApp.class.getResource(fxmlPath));
            Scene scene;
            if (fxmlPath.contains("ItemManagement") || fxmlPath.contains("BidderView")) {
                scene = new Scene(root, 900, 650);
                primaryStage.setTitle("Hệ thống Đấu giá - Nhóm 14");
            } else {
                scene = new Scene(root, 450, 400);
            }
            
            // Tải file CSS dùng chung
            String css = MainApp.class.getResource("/com/auction/client/css/styles.css").toExternalForm();
            scene.getStylesheets().add(css);
            
            primaryStage.setScene(scene);
        }
    
        public static Stage getPrimaryStage() {
            return primaryStage;
        }
    
        public static void main(String[] args) {
            launch(args);
        }
    }
