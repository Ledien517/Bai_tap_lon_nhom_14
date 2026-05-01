package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.model.Role;
import com.auction.common.model.User;
import com.auction.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Role> cbRole;

    @FXML
    public void initialize() {
        cbRole.getItems().addAll(Role.values());
        cbRole.setValue(Role.BIDDER); // Mặc định
    }

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        Role selectedRole = cbRole.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (UserDAO.isUserExists(username)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên đăng nhập đã tồn tại!");
        } else {
            UserDAO.saveUser(new User(username, password, selectedRole));
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công!\nVui lòng đăng nhập.");
            try {
                handleGoToLogin();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleGoToLogin() throws Exception {
        MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}