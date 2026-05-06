package com.auction.client.controller;

import javafx.stage.Stage;
import javafx.scene.Scene;
import com.auction.client.MainApp;
import com.auction.common.model.Bidder; // Bồi thêm: Model Bidder
import com.auction.common.model.Item;   // Bồi thêm: Model Item
import com.auction.dao.ItemDAO;         // Bồi thêm: DAO lấy hàng
import com.auction.dao.JsonItemDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.util.Duration;
import javafx.scene.layout.VBox;

public class BidderController {

    // --- BỒI THÊM: Các UI Components cho TableView và thông tin Bidder ---
    @FXML
    private TableView<Item> table;
    @FXML
    private TableColumn<Item, String> idCol, typeCol, nameCol, statusCol;
    @FXML
    private TableColumn<Item, Double> priceCol;
    @FXML
    private Label lblUsername, lblBalance;

    // --- BỒI THÊM: Logic Fields ---
    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;
    private Bidder currentBidder;

    // --- BỒI THÊM: Hàm initialize để thiết lập bảng và đồng bộ thời gian ---
    @FXML
    public void initialize() {
        // Cấu hình các cột
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        typeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));

        statusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatusDisplay()));

        // Nạp dữ liệu
        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        // Bồi thêm: Bộ cập nhật thời gian thực để Bidder thấy giây nhảy lùi
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> table.refresh())
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
        // BỒI THÊM: Click đúp vào sản phẩm để mở cửa sổ đấu giá riêng
        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Item selectedItem = row.getItem();
                    showAuctionWindow(selectedItem); // Hàm này sẽ viết ở Bước 2
                }
            });
            return row;
        });
    }

    // --- BỒI THÊM: Nhận dữ liệu Bidder từ LoginController ---
    public void setBidder(Bidder bidder) {
        this.currentBidder = bidder;
        lblUsername.setText("Chào, " + bidder.getUsername());
        lblBalance.setText(String.format("%.2f $", bidder.getAvailableBalance()));
    }

    // 1. Xử lý nút Đăng xuất (GIỮ NGUYÊN 100% CODE CŨ)
    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Trở về màn hình đăng nhập
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay về màn hình đăng nhập!");
        }
    }

    // 2. Xử lý nút "Tìm kiếm sản phẩm" (GIỮ NGUYÊN 100% CODE CŨ)
    @FXML
    private void handleSearchItem(ActionEvent event) {
        // Tạm thời hiển thị thông báo, sau này bạn sẽ viết code mở giao diện tìm kiếm ở đây
        showAlert(Alert.AlertType.INFORMATION, "Tính năng đang phát triển", "Mở giao diện Tìm kiếm sản phẩm...");
    }

    // 3. Xử lý nút "Phiên đang tham gia" (GIỮ NGUYÊN 100% CODE CŨ)
    @FXML
    private void handleMyAuctions(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Tính năng đang phát triển", "Mở danh sách các phiên đấu giá bạn đang theo dõi...");
    }

    // 4. Xử lý nút "Nạp tiền / Số dư" (GIỮ NGUYÊN 100% CODE CŨ)
    @FXML
    private void handleTopUp(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Tính năng đang phát triển", "Mở giao diện Nạp tiền vào tài khoản...");
    }

    // --- BỒI THÊM: Xử lý Đặt giá (Mới) ---
    @FXML
    private void handlePlaceBid() {
        Item selectedItem = table.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một sản phẩm để đặt giá!");
            return;
        }

        // Ở đây bạn sẽ bồi thêm logic gọi auction.placeBid()
        // và trừ tiền/đóng băng tiền của currentBidder
        showAlert(Alert.AlertType.CONFIRMATION, "Đặt giá", "Bạn đang đặt giá cho: " + selectedItem.getName());
    }

    // Hàm phụ trợ để hiển thị hộp thoại thông báo (GIỮ NGUYÊN 100% CODE CŨ)
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAuctionWindow(Item item) {
        Stage stage = new Stage();
        stage.setTitle("Đấu giá trực tuyến: " + item.getName());

        // Layout chính cho cửa sổ popup
        VBox layout = new VBox(15);
        layout.setPadding(new javafx.geometry.Insets(20));

        // 1. Hiển thị getInfo() từ Model
        Label lblTitle = new Label("CHI TIẾT SẢN PHẨM");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextArea txtInfo = new TextArea(item.getDetails()); // Hoặc gọi auction.getInfo()
        txtInfo.setEditable(false);
        txtInfo.setPrefHeight(100);

        // 2. Hiển thị BidList (Lịch sử đặt giá)
        Label lblHistory = new Label("Lịch sử đặt giá (BidList):");
        ListView<String> bidListView = new ListView<>();
        bidListView.setPrefHeight(120);
        // bidListView.getItems().addAll(auction.getBidListAsString()); // Bồi dữ liệu thật ở đây

        // 3. Khu vực thao tác đặt giá và hiển thị số tiền hiện tại
        Label lblCurrentPrice = new Label("Giá hiện tại: " + item.getCurrentHighestBid() + " $");
        lblCurrentPrice.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        TextField txtInputBid = new TextField();
        txtInputBid.setPromptText("Nhập số tiền đấu giá mới...");

        Button btnBid = new Button("XÁC NHẬN ĐẶT GIÁ");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold;");

        // Xử lý khi nhấn nút Đặt giá ngay trên cửa sổ mới này
        btnBid.setOnAction(e -> {
            processBidLogic(item, txtInputBid.getText());
            lblCurrentPrice.setText("Giá hiện tại: " + item.getStartingPrice() + " $"); // Cập nhật lại giá hiển thị
        });

        layout.getChildren().addAll(lblTitle, txtInfo, lblHistory, bidListView, lblCurrentPrice, txtInputBid, btnBid);

        stage.setScene(new Scene(layout, 400, 500));
        stage.show();
    }

    private void processBidLogic(Item item, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);

            // Lỗi 1: Kiểm tra bước giá (Phải cao hơn giá hiện tại)
            if (amount <= item.getCurrentHighestBid()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đặt giá", "Giá mới phải cao hơn giá hiện tại!");
                return;
            }

            // Lỗi 2: Kiểm tra số dư khả dụng thực tế
            if (currentBidder.getAvailableBalance() < amount) {
                showAlert(Alert.AlertType.ERROR, "Lỗi số dư", "Số dư khả dụng không đủ để đặt giá này!");
                return;
            }

            // Lỗi 3: Kiểm tra thời gian phiên đấu giá
            if (!item.getStatusDisplay().equals("ACTIVE")) {
                showAlert(Alert.AlertType.ERROR, "Lỗi thời gian", "Phiên đấu giá này đã kết thúc hoặc chưa bắt đầu!");
                return;
            }

            // --- XỬ LÝ THÀNH CÔNG ---

            // 1. Cập nhật Model Item
            item.setCurrentHighestBid(amount);
            item.setStartingPrice(amount); // Để TableView cập nhật cột giá hiển thị
            itemDAO.saveItem(item);        // Lưu vào file JSON

            // 2. Xử lý dòng tiền của Bidder (Đúng theo class Bidder bạn gửi)
            currentBidder.freezeMoney(amount);

            // 3. Cập nhật giao diện (UI)
            table.refresh(); // Làm mới bảng chính
            lblBalance.setText(String.format("%.2f $", currentBidder.getAvailableBalance())); // Cập nhật Header

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã đặt giá và đóng băng tiền cọc thành công!");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một con số hợp lệ!");
        } catch (IllegalArgumentException e) {
            // Bắt các lỗi ném ra từ class Bidder (ví dụ số tiền âm)
            showAlert(Alert.AlertType.ERROR, "Lỗi logic", e.getMessage());
        }
    }
}