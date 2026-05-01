package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    // ✅ PHẢI là static để LoginController, RegisterController gọi được
    private static Stage primaryStage;

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

        // Màn hình quản lý thì rộng hơn
        if (fxmlPath.contains("ItemManagement")) {
            primaryStage.setScene(new Scene(root, 700, 600));
            primaryStage.setTitle("Quản lý Đấu giá - Nhóm 14");
        } else {
            primaryStage.setScene(new Scene(root, 350, 300));
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}