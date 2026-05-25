package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.model.*;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonItemDAO;
import com.auction.dao.UserDAO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableRow;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.animation.Animation;
import javafx.util.Duration;

import java.util.List;

public class AdminController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, String> statusCol;

    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> itemIdCol;
    @FXML private TableColumn<Item, String> itemNameCol;
    @FXML private TableColumn<Item, String> itemSellerCol;
    @FXML private TableColumn<Item, String> itemPriceCol;
    @FXML private TableColumn<Item, String> itemStatusCol;
    @FXML private TableColumn<Item, String> itemBanCol;

    private ObservableList<User> userData;
    private ObservableList<Item> itemData;
    private ItemDAO itemDAO = new JsonItemDAO();
    private Timeline timeline;

    @FXML
    public void initialize() {
        // --- 1. Thiết lập cột cho User Table ---
        usernameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getUsername()));
        roleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().toString()));
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().isBanned() ? "Đã khóa (Banned)" : "Hoạt động"));

        // --- 2. Tải dữ liệu User ---
        userData = FXCollections.observableArrayList(UserDAO.getAllUsersList());
        userTable.setItems(userData);

        // --- 3. Thiết lập cột cho Item Table ---
        itemIdCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId()));
        itemNameCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        itemSellerCol.setCellValueFactory(cellData -> {
            Seller seller = cellData.getValue().getSeller();
            return new SimpleStringProperty(seller != null ? seller.getUsername() : "Không rõ");
        });
        itemPriceCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.format("%.2f $", cellData.getValue().getCurrentHighestBid())));
        itemStatusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            if (auction != null) {
                return new SimpleStringProperty(auction.getStatusDisplay());
            } else {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                if (item.getStartTime() != null && now.isBefore(item.getStartTime())) {
                    return new SimpleStringProperty("UPCOMING");
                }
                if (item.getEndTime() != null && now.isAfter(item.getEndTime())) {
                    return new SimpleStringProperty("FINISHED");
                }
                return new SimpleStringProperty("ACTIVE");
            }
        });
        itemBanCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().isDeletedByAdmin() ? "Bị xóa (Vi phạm)" : "Bình thường"));

        // --- 4. Tải dữ liệu Item ---
        List<Item> items = itemDAO.getAllItems();
        itemData = FXCollections.observableArrayList(items != null ? items : new java.util.ArrayList<>());
        itemTable.setItems(itemData);

        // --- 5. Tự động cập nhật trạng thái đấu giá ---
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            itemTable.refresh();
            // Đồng bộ dữ liệu
            List<Item> latestItems = itemDAO.getAllItems();
            if (latestItems != null) {
                for (Item latest : latestItems) {
                    for (Item current : itemData) {
                        if (current.getId().equals(latest.getId())) {
                            current.setCurrentHighestBid(latest.getCurrentHighestBid());
                            current.setDeletedByAdmin(latest.isDeletedByAdmin());
                            break;
                        }
                    }
                }
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Thêm sự kiện Double Click để mở cửa sổ chi tiết đấu giá
        itemTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showAuctionWindow(row.getItem());
                }
            });
            return row;
        });
    }

    @FXML
    private void handleToggleBanUser(ActionEvent event) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn người dùng cần khóa/mở khóa!");
            return;
        }
        // Không cho phép Admin tự khóa chính mình (Role = ADMIN) nếu có
        if (selectedUser.getRole() == Role.ADMIN) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể khóa tài khoản Quản trị viên!");
            return;
        }

        selectedUser.setBanned(!selectedUser.isBanned());
        UserDAO.saveUser(selectedUser); // Lưu vào file
        userTable.refresh();
        String msg = selectedUser.isBanned() ? "Đã khóa" : "Đã mở khóa";
        showAlert(Alert.AlertType.INFORMATION, "Thành công", msg + " tài khoản: " + selectedUser.getUsername());
    }

    @FXML
    private void handleDeleteViolationItem(ActionEvent event) {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng chọn sản phẩm cần xóa!");
            return;
        }
        if (selectedItem.isDeletedByAdmin()) {
            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Sản phẩm này đã bị xóa do vi phạm từ trước rồi!");
            return;
        }

        Auction auction = MainApp.getAuctionForItem(selectedItem);
        if (auction != null && "FINISHED".equals(auction.getStatusDisplay())) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể xóa sản phẩm đã kết thúc phiên đấu giá!");
            return;
        } else if (auction == null && java.time.LocalDateTime.now().isAfter(selectedItem.getEndTime())) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Không thể xóa sản phẩm đã kết thúc phiên đấu giá!");
            return;
        }

        selectedItem.setDeletedByAdmin(true);
        itemDAO.updateItem(selectedItem); // Lưu cờ isDeletedByAdmin = true vào file json

        if (auction != null) {
            auction.cancelAndRefund();
        }

        itemTable.refresh();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sản phẩm: " + selectedItem.getName() + " vì vi phạm chính sách và hoàn tiền cho người đặt giá (nếu có).");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (timeline != null) timeline.stop();
        try {
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAuctionWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Sản phẩm chưa có phiên đấu giá trong bộ nhớ hệ thống!");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Chi tiết đấu giá: " + item.getName());

        VBox layout = new VBox(15);
        layout.setPadding(new javafx.geometry.Insets(20));

        Label lblTitle = new Label("CHI TIẾT PHIÊN ĐẤU GIÁ");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextArea txtAuctionDetails = new TextArea();
        txtAuctionDetails.setEditable(false);
        txtAuctionDetails.setPrefHeight(250);
        txtAuctionDetails.setText(auction.getInfo());

        Label lblCurrentPrice = new Label("Giá hiện tại: " + auction.getCurrentPrice() + " $");
        lblCurrentPrice.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");

        Timeline detailTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            txtAuctionDetails.setText(auction.getInfo());
            lblCurrentPrice.setText(String.format("Giá hiện tại: %.1f $", auction.getCurrentPrice()));
        }));
        detailTimer.setCycleCount(Animation.INDEFINITE);
        detailTimer.play();
        stage.setOnCloseRequest(e -> detailTimer.stop());

        layout.getChildren().addAll(lblTitle, txtAuctionDetails, lblCurrentPrice);
        stage.setScene(new Scene(layout, 450, 400));
        stage.show();
    }
}