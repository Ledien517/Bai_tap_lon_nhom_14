package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.factory.ItemFactory;
import com.auction.common.model.Item;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonItemDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;

public class ItemManagementController {

    // --- CÁC THÀNH PHẦN GIAO DIỆN QUẢN LÝ SẢN PHẨM ---
    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol;
    @FXML private TableColumn<Item, String> typeCol;
    @FXML private TableColumn<Item, String> nameCol;
    @FXML private TableColumn<Item, Double> priceCol;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblExtraParam;
    @FXML private TextField txtExtraParam;

    // TODO: Tạm thời dùng DAO để test UI. Sau này phần này phải đổi thành Socket gọi lên Server!
    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;

    // --- HÀM KHỞI TẠO (Tự động chạy khi mở màn hình) ---
    @FXML
    public void initialize() {
        // 1. Cấu hình cột
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type")); // Đã sửa ở bài trước
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        // 2. Load dữ liệu
        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        // 3. Cấu hình ComboBox VÀ THÊM LISTENER (SỰ KIỆN)
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");

        // Đoạn code này giúp đổi tên Label mỗi khi em chọn loại sản phẩm khác
        cbType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateExtraField(newValue);
        });

        // Thiết lập giá trị mặc định ban đầu
        cbType.setValue("Art");
    }

    // --- HÀM XỬ LÝ NÚT BẤM ---

    // Logic khi bấm nút "Thêm sản phẩm"
    @FXML
    private void handleAddItem() {
        try {
            String type = cbType.getValue();
            String id = txtId.getText();
            String name = txtName.getText();
            double price = Double.parseDouble(txtPrice.getText());

<<<<<<< HEAD
            // Áp dụng Factory Method Pattern
            Item newItem = ItemFactory.createItem(type, id, name, "Mô tả", price, LocalDateTime.now(), LocalDateTime.now().plusDays(7), "Default");
=======
            // 1. LẤY DỮ LIỆU TỪ Ô TEXTFIELD MỚI TẠO
            String extraParam = txtExtraParam.getText();
>>>>>>> ad1dae19891bd60796444b8b669d5e7227119819

            // 2. KIỂM TRA RỖNG
            if (extraParam == null || extraParam.trim().isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng nhập thông tin cho ô: " + lblExtraParam.getText());
                alert.show();
                return; // Dừng lại không chạy tiếp
            }

            // 3. GỌI FACTORY ĐỂ TẠO ITEM
            Item newItem = ItemFactory.createItem(
                    type,
                    id,
                    name,
                    "Mô tả",
                    price,
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(7),
                    extraParam // Truyền biến này vào thay vì chữ "Default"
            );

            // 4. LƯU VÀ CẬP NHẬT BẢNG
            itemDAO.saveItem(newItem);
            data.add(newItem);

<<<<<<< HEAD
            // Xóa form sau khi thêm thành công
            txtId.clear(); txtName.clear(); txtPrice.clear();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Vui lòng kiểm tra lại thông tin nhập vào (Giá phải là số)!");
            alert.show();
        }
    }

    // Logic khi bấm nút "Đăng xuất" ở thanh Header
    @FXML
    private void handleLogout() {
        try {
            // Quay trở về màn hình đăng nhập
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Không thể quay về màn hình đăng nhập!");
=======
            // 5. XOÁ TRẮNG FORM SAU KHI THÊM THÀNH CÔNG
            txtId.clear();
            txtName.clear();
            txtPrice.clear();
            txtExtraParam.clear();

        } catch (NumberFormatException nfe) {
            // Lỗi do nhập chữ vào chỗ cần nhập số
            nfe.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi định dạng: Ô Giá (hoặc ô Bảo hành) phải là CON SỐ!");
            alert.show();
        } catch (Exception ex) {
            // Lỗi khác
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi hệ thống: " + ex.getMessage());
>>>>>>> ad1dae19891bd60796444b8b669d5e7227119819
            alert.show();
        }
    }
    // THÊM TOÀN BỘ HÀM NÀY VÀO NGAY DƯỚI HÀM initialize()
    private void updateExtraField(String type) {
        if (type == null) return;

        switch (type) {
            case "Art":
                lblExtraParam.setText("Tác giả:");
                txtExtraParam.setPromptText("Nhập tên tác giả (VD: Picasso)");
                break;
            case "Vehicle":
                lblExtraParam.setText("Hãng xe:");
                txtExtraParam.setPromptText("Nhập hãng xe (VD: Toyota)");
                break;
            case "Electronics":
                lblExtraParam.setText("Bảo hành (tháng):");
                txtExtraParam.setPromptText("Nhập SỐ tháng (VD: 12)");
                break;
        }
    }
}