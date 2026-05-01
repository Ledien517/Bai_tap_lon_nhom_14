package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.model.User;
import com.auction.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        User user = UserDAO.authenticate(username, password);

        if (user != null) {
            try {
                // ✅ Chuyển thẳng sang màn hình quản lý
                MainApp.switchScene("/com/auction/client/view/ItemManagementView.fxml");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình chính!");
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Sai tên đăng nhập hoặc mật khẩu!");
        }
    }

    @FXML
    private void handleGoToRegister() throws Exception {
        MainApp.switchScene("/com/auction/client/view/RegisterView.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}