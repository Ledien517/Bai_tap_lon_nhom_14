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

    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateExtraField(newValue);
        });

        cbType.setValue("Art");
    }

    @FXML
    private void handleAddItem() {
        try {
            String type = cbType.getValue();
            String id = txtId.getText();
            String name = txtName.getText();

            // Chuyển đổi giá (có thể gây lỗi NumberFormatException)
            double price = Double.parseDouble(txtPrice.getText());

            // Lấy dữ liệu extra
            String extraParam = txtExtraParam.getText();

            if (extraParam == null || extraParam.trim().isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng nhập: " + lblExtraParam.getText()).show();
                return;
            }

            // CHỈ KHAI BÁO newItem MỘT LẦN DUY NHẤT
            Item newItem = ItemFactory.createItem(
                    type, id, name, "Mô tả", price,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                    extraParam
            );

            itemDAO.saveItem(newItem);
            data.add(newItem);

            // Xóa form sau khi thêm thành công
            clearForm();

        } catch (NumberFormatException nfe) {
            new Alert(Alert.AlertType.ERROR, "Lỗi: Giá phải là con số!").show();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Lỗi hệ thống: " + ex.getMessage()).show();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Không thể quay về màn hình đăng nhập!").show();
        }
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
        txtExtraParam.clear();
    }
}