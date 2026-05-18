package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.factory.ItemFactory;
import com.auction.common.model.Item;
import com.auction.common.model.Seller;
import com.auction.common.model.Auction;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonItemDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.animation.Animation;

import java.time.LocalDateTime;

public class ItemManagementController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol;
    @FXML private TableColumn<Item, String> typeCol;
    @FXML private TableColumn<Item, String> nameCol;
    @FXML private TableColumn<Item, Double> priceCol;
    @FXML private TableColumn<Item, String> statusCol;
    @FXML private TableColumn<Item, Double> minIncCol;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblExtraParam;
    @FXML private TextField txtExtraParam;
    @FXML private TextField txtMinIncrement;

    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;
    private Seller currentSeller;

    @FXML
    public void initialize() {
        // --- 1. Liên kết các cột dữ liệu ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Hiển thị loại sản phẩm dựa trên tên lớp (Art, Vehicle...)
        typeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        // SỬA LỖI TẠI ĐÂY: Lấy trạng thái hiển thị từ Auction qua MainApp thay vì gọi trực tiếp từ Item
        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            if (auction != null) {
                return new SimpleStringProperty(auction.getStatusDisplay());
            } else {
                return new SimpleStringProperty("NO AUCTION");
            }
        });

        minIncCol.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));

        // --- 2. Nạp dữ liệu vào bảng ---
        var items = itemDAO.getAllItems();
        if (items == null) {
            data = FXCollections.observableArrayList();
            System.err.println("Cảnh báo: Không tải được dữ liệu từ file JSON!");
        } else {
            data = FXCollections.observableArrayList(items);
        }
        table.setItems(data);

        // --- 3. Cấu hình ComboBox và Extra Field ---
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateExtraField(newValue);
        });
        cbType.setValue("Art");

        // --- 4. Bộ cập nhật thời gian thực (Làm mới bảng mỗi giây) ---
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> table.refresh())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleAddItem() {
        try {
            String type = cbType.getValue();
            String id = txtId.getText();
            String name = txtName.getText();
            double price = Double.parseDouble(txtPrice.getText());
            double minIncrement = Double.parseDouble(txtMinIncrement.getText());
            String extraParam = txtExtraParam.getText();

            if (extraParam == null || extraParam.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng nhập: " + lblExtraParam.getText()).show();
                return;
            }

            // Tạo Item từ Factory
            Item newItem = ItemFactory.createItem(
                currentSeller, type, id, name, "Mô tả sản phẩm", price,
                LocalDateTime.now(), LocalDateTime.now().plusDays(7), minIncrement,
                extraParam
            );

            // Khởi tạo Auction để kích hoạt logic nghiệp vụ và đăng ký vào MainApp hệ thống công khai
            Auction newAuction = new Auction(currentSeller, newItem);
            MainApp.registerAuction(newItem.getId(), newAuction);

            // Lưu và cập nhật UI
            itemDAO.saveItem(newItem);
            data.add(newItem);

            new Alert(Alert.AlertType.INFORMATION, "Đã thêm sản phẩm và khởi tạo phiên đấu giá!").show();
            clearForm();

        } catch (NumberFormatException nfe) {
            new Alert(Alert.AlertType.ERROR, "Lỗi: Giá và bước giá phải là con số!").show();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Lỗi hệ thống: " + ex.getMessage()).show();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Không thể quay về màn hình đăng nhập!").show();
        }
    }

    public void setSeller(Seller seller) {
        this.currentSeller = seller;
        System.out.println("Sẵn sàng quản lý cho Seller: " + seller.getUsername());
    }

    private void updateExtraField(String type) {
        if (type == null) return;
        switch (type) {
            case "Art":
                lblExtraParam.setText("Tác giả:");
                txtExtraParam.setPromptText("Nhập tên tác giả");
                break;
            case "Vehicle":
                lblExtraParam.setText("Hãng xe:");
                txtExtraParam.setPromptText("Nhập hãng xe");
                break;
            case "Electronics":
                lblExtraParam.setText("Bảo hành (tháng):");
                txtExtraParam.setPromptText("Nhập số tháng");
                break;
        }
    }

    private void clearForm() {
        txtId.clear();
        txtName.clear();
        txtPrice.clear();
        txtMinIncrement.clear();
        txtExtraParam.clear();
    }
}