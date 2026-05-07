package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.factory.ItemFactory;
import com.auction.common.model.Auction;
import com.auction.common.model.Item;
import com.auction.common.model.Seller;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonItemDAO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.LocalDateTime;

public class ItemManagementController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol, typeCol, nameCol, statusCol;
    @FXML private TableColumn<Item, Double> priceCol, minIncCol;

    @FXML private TextField txtId, txtName, txtPrice, txtExtraParam, txtMinIncrement;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblExtraParam;

    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;
    private Seller currentSeller;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        statusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatusDisplay()));

        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        // Tạo Auction cho các item từ file (seller có thể null — xử lý gracefully)
        for (Item item : data) {
            if (MainApp.getAuctionForItem(item) == null && item.getSeller() != null) {
                Auction auction = new Auction(item.getSeller(), item);
                MainApp.registerAuction(item.getId(), auction);
            }
        }

        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> updateExtraField(nv));
        cbType.setValue("Art");

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> table.refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleAddItem() {
        try {
            String type = cbType.getValue();
            String id   = txtId.getText().trim();
            String name = txtName.getText().trim();
            String extra = txtExtraParam.getText().trim();

            if (id.isEmpty() || name.isEmpty() || extra.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng điền đầy đủ các trường!");
                return;
            }

            double price    = Double.parseDouble(txtPrice.getText().trim());
            double minInc   = Double.parseDouble(txtMinIncrement.getText().trim());

            Item newItem = ItemFactory.createItem(
                    currentSeller, type, id, name, "Mô tả sản phẩm", price,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(7),
                    minInc, extra
            );

            // Tạo Auction VÀ đăng ký vào MainApp (fix bug cũ: chỉ tạo chứ không đăng ký)
            Auction newAuction = new Auction(currentSeller, newItem);
            MainApp.registerAuction(newItem.getId(), newAuction);

            itemDAO.saveItem(newItem);
            data.add(newItem);
            clearForm();

            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    "Đã thêm sản phẩm '" + name + "' và khởi tạo phiên đấu giá!");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá và bước giá phải là số hợp lệ!");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay về màn hình đăng nhập!");
        }
    }

    public void setSeller(Seller seller) {
        this.currentSeller = seller;
    }

    private void updateExtraField(String type) {
        if (type == null) return;
        switch (type) {
            case "Art"         -> { lblExtraParam.setText("Tác giả:");        txtExtraParam.setPromptText("Nhập tên tác giả"); }
            case "Electronics" -> { lblExtraParam.setText("Bảo hành (tháng):"); txtExtraParam.setPromptText("Số tháng bảo hành"); }
            case "Vehicle"     -> { lblExtraParam.setText("Hãng xe:");         txtExtraParam.setPromptText("Nhập hãng xe"); }
        }
    }

    private void clearForm() {
        txtId.clear(); txtName.clear(); txtPrice.clear(); txtExtraParam.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}