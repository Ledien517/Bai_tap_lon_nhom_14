package com.auction.client.controller;

import com.auction.client.MainApp;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class AdminController {

    // Hàm này được gọi khi bấm nút "Đăng xuất" trên giao diện Admin
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Quay trở về màn hình đăng nhập
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Lỗi khi quay về màn hình đăng nhập!");
        }
    }
}