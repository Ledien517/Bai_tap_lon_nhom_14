
package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.NetworkClient;
import com.auction.common.model.User;
import com.auction.common.protocol.Request;
import com.auction.common.protocol.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;

    @FXML
    public void initialize() {}

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo",
                "Vui lòng nhập đầy đủ tài khoản và mật khẩu!");
            return;
        }

        // Vô hiệu hóa nút để tránh nhấn nhiều lần
        if (btnLogin != null) btnLogin.setDisable(true);

        // Chạy network trên background thread để không làm đóng băng giao diện
        new Thread(() -> {
            try {
                Response response = NetworkClient.getInstance().sendRequestAndWait(
                    new Request("LOGIN", new String[]{username, password})
                );

                Platform.runLater(() -> {
                    if (btnLogin != null) btnLogin.setDisable(false);
                    try {
                        if ("SUCCESS".equals(response.getStatus())) {
                            User user = (User) response.getData();
                            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                "Đăng nhập thành công!\nChào mừng "
                                    + user.getUsername() + " (" + user.getRole() + ")");

                            // Lưu user vào session trước khi chuyển màn hình
                            MainApp.setCurrentUser(user);

                            switch (user.getRole()) {
                                case BIDDER -> MainApp.switchScene(
                                    "/com/auction/client/view/BidderView.fxml");
                                case SELLER -> MainApp.switchScene(
                                    "/com/auction/client/view/ItemManagementView.fxml");
                                default     -> MainApp.switchScene(
                                    "/com/auction/client/view/LoginView.fxml");
                            }
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Thất bại", response.getMessage());
                        }
                    } catch (Exception e) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi Hệ Thống",
                            "Không thể xử lý đăng nhập: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (btnLogin != null) btnLogin.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Lỗi Hệ Thống",
                        "Không thể xử lý đăng nhập: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleGoToRegister() {
        try {
            MainApp.switchScene("/com/auction/client/view/RegisterView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi Giao Diện",
                "Không thể tải trang đăng ký!");
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
