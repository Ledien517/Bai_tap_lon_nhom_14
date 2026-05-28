package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.NetworkClient;
import com.auction.common.model.*;
import com.auction.common.protocol.Request;
import com.auction.common.protocol.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<Role> cbRole;
    @FXML private Button btnRegister;

    @FXML
    public void initialize() {
        // Thay vì addAll(Role.values()), ta chỉ add cứng 2 quyền được phép đăng ký
        cbRole.getItems().addAll(Role.BIDDER, Role.SELLER);
        cbRole.setValue(Role.BIDDER); // Mặc định là người mua
    }

    @FXML
    private void handleRegister() {
        String username = txtUsername.getText().trim(); // Dùng trim() để loại bỏ dấu cách thừa
        String password = txtPassword.getText();
        Role selectedRole = cbRole.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Vô hiệu hóa nút để tránh nhấn nhiều lần
        if (btnRegister != null) btnRegister.setDisable(true);

        // Chạy network trên background thread
        new Thread(() -> {
            try {
                // 1. Áp dụng Đa hình (Polymorphism) để khởi tạo đúng loại User
                // Sử dụng Switch Expression của Java 14+
                User newUser = switch (selectedRole) {
                    case BIDDER -> new Bidder(username, password);
                    case SELLER -> new Seller(username, password);
                    default -> throw new IllegalArgumentException("Vai trò không hợp lệ!");
                };

                Response response = NetworkClient.getInstance().sendRequestAndWait(
                    new Request("REGISTER", newUser)
                );

                Platform.runLater(() -> {
                    if (btnRegister != null) btnRegister.setDisable(false);
                    if ("SUCCESS".equals(response.getStatus())) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công!\nVui lòng đăng nhập.");
                        handleGoToLogin(); // Tự động chuyển trang sau khi đăng ký thành công
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Thất bại", response.getMessage());
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (btnRegister != null) btnRegister.setDisable(false);
                    // Bắt lỗi tổng quát và hiện lên màn hình thay vì in ngầm ra console
                    showAlert(Alert.AlertType.ERROR, "Lỗi Hệ Thống", "Đã xảy ra lỗi: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleGoToLogin() {
        try {
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Giao Diện", "Không thể tải trang đăng nhập!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}