package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.model.*;
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

        try {
            // 1. Áp dụng Đa hình (Polymorphism) để khởi tạo đúng loại User
            // Sử dụng Switch Expression của Java 14+
            User newUser = switch (selectedRole) {
                case BIDDER -> new Bidder(username, password);
                case SELLER -> new Seller(username, password);
                // Nếu dự án em có thêm Admin thì uncomment dòng dưới:
                // case ADMIN -> new Admin(username, password);
                default -> throw new IllegalArgumentException("Vai trò không hợp lệ!");
            };

            // ---------------------------------------------------------
            // TODO (TUẦN SAU - LẬP TRÌNH MẠNG):
            // Thay vì gọi trực tiếp UserDAO ở đây, em sẽ chuyển thành:
            // 1. Chuyển đối tượng newUser thành chuỗi JSON (dùng thư viện Gson/Jackson)
            // 2. Gửi chuỗi JSON đó qua Socket (hoặc API) lên Server
            // 3. Server nhận được, Server mới gọi UserDAO để lưu và trả kết quả về cho Client.
            // ---------------------------------------------------------

            // CODE TẠM THỜI ĐỂ TEST GIAO DIỆN (Sẽ xóa khi làm Server)
            if (UserDAO.isUserExists(username)) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên đăng nhập đã tồn tại!");
            } else {
                UserDAO.saveUser(newUser); // newUser giờ đây là 1 Bidder hoặc Seller
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đăng ký thành công!\nVui lòng đăng nhập.");
                handleGoToLogin(); // Tự động chuyển trang sau khi đăng ký thành công
            }

        } catch (Exception e) {
            // Bắt lỗi tổng quát và hiện lên màn hình thay vì in ngầm ra console
            showAlert(Alert.AlertType.ERROR, "Lỗi Hệ Thống", "Đã xảy ra lỗi: " + e.getMessage());
        }
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