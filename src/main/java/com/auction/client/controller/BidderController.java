
package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.common.model.*;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonItemDAO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class BidderController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol, typeCol, nameCol, statusCol;
    @FXML private TableColumn<Item, Double> priceCol;
    @FXML private TableColumn<Item, Double> minIncCol;
    @FXML private Label lblUsername, lblBalance;


    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;
    private Bidder currentBidder;

    @FXML
    public void initialize() {
        // Setup các cột TableView
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        minIncCol.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));
        typeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));
        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            return auction != null
                ? new SimpleStringProperty(auction.getStatusDisplay())
                : new SimpleStringProperty("NO AUCTION");
        });

        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        for (Item item : data) {
            if (MainApp.getAuctionForItem(item) == null) {
                if (item.getSeller() != null) {
                    Auction auction = new Auction(item.getSeller(), item);
                    MainApp.registerAuction(item.getId(), auction);
                } else {
                    System.out.println("Cảnh báo: Sản phẩm "
                        + item.getId() + " đang thiếu thông tin người bán!");
                }
            }
        }

        loadDataFromServer();

        // ← FIX: Lấy Bidder từ session MainApp thay vì chờ setBidder() bên ngoài
        User user = MainApp.getCurrentUser();
        if (user instanceof Bidder bidder) {
            setBidder(bidder);
        }

        // Bộ đếm cập nhật giá realtime mỗi 2 giây
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(2), e -> {
                List<Item> latestItems = itemDAO.getAllItems();
                if (latestItems == null || latestItems.isEmpty()) return;
                for (Item latest : latestItems) {
                    for (Item current : data) {
                        if (current.getId().equals(latest.getId())
                            && latest.getStartingPrice() > current.getStartingPrice()) {
                            current.setStartingPrice(latest.getStartingPrice());
                            Auction auction = MainApp.getAuctionForItem(current);
                            if (auction != null
                                && latest.getBidList() != null
                                && !latest.getBidList().isEmpty()) {
                                auction.getBidList().clear();
                                auction.getBidList().addAll(latest.getBidList());
                            }
                        }
                    }
                }
                table.refresh();
            })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showAuctionWindow(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadDataFromServer() {
        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);
    }

    public void setBidder(Bidder bidder) {
        this.currentBidder = bidder;
        lblUsername.setText("Chào, " + bidder.getUsername());
        lblBalance.setText(String.format("%.2f $", bidder.getAvailableBalance()));
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            MainApp.setCurrentUser(null); // Xóa session khi logout
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi",
                "Không thể quay về màn hình đăng nhập!");
        }
    }

    @FXML
    private void handleSearchItem(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Tính năng đang phát triển",
            "Mở giao diện Tìm kiếm sản phẩm...");
    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Tính năng đang phát triển",
            "Mở giao diện phiên đang tham gia...");
    }

    @FXML
    private void handleTopUp(ActionEvent event) {
        if (currentBidder == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa xác định người dùng!");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Nạp tiền vào tài khoản");
        dialog.setHeaderText("Số dư hiện tại: "
            + String.format("%.2f $", currentBidder.getAvailableBalance()));
        dialog.setContentText("Vui lòng nhập số tiền muốn nạp:");
        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                currentBidder.deposit(amount);
                updateBalanceUI();
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                    String.format("Đã nạp thành công %.2f $ vào tài khoản.", amount));
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu",
                    "Vui lòng nhập một con số hợp lệ!");
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handlePlaceBid() {
        Item selectedItem = table.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý",
                "Vui lòng chọn một sản phẩm để đặt giá!");
            return;
        }
        showAuctionWindow(selectedItem);
    }

    private void showAuctionWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) return;

        Stage stage = new Stage();
        stage.setTitle("Đấu giá trực tuyến: " + item.getName());

        VBox layout = new VBox(15);
        layout.setPadding(new javafx.geometry.Insets(20));

        Label lblTitle = new Label("CHI TIẾT PHIÊN ĐẤU GIÁ");
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        TextArea txtAuctionDetails = new TextArea();
        txtAuctionDetails.setEditable(false);
        txtAuctionDetails.setPrefHeight(250);
        txtAuctionDetails.setText(auction.getInfo());

        Label lblCurrentPrice = new Label(
            "Giá hiện tại: " + auction.getCurrentPrice() + " $");
        lblCurrentPrice.setStyle(
            "-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");

        TextField txtInputBid = new TextField();
        double minPrice = auction.getCurrentPrice() + item.getMinIncrement();
        txtInputBid.setPromptText(String.format("Nhập giá mới (Min: %.1f)", minPrice));

        Button btnBid = new Button("XÁC NHẬN ĐẶT GIÁ");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        btnBid.setStyle(
            "-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold;");

        Timeline detailTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            txtAuctionDetails.setText(auction.getInfo());
            lblCurrentPrice.setText(
                String.format("Giá hiện tại: %.1f $", auction.getCurrentPrice()));
        }));
        detailTimer.setCycleCount(Animation.INDEFINITE);
        detailTimer.play();
        stage.setOnCloseRequest(e -> detailTimer.stop());

        btnBid.setOnAction(e -> {
            processBidLogic(item, txtInputBid.getText());
            txtInputBid.clear();
        });

        layout.getChildren().addAll(lblTitle, txtAuctionDetails,
            lblCurrentPrice, txtInputBid, btnBid);
        stage.setScene(new Scene(layout, 450, 550));
        stage.show();
    }

    private void processBidLogic(Item item, String amountStr) {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập số tiền!");
                return;
            }
            double amount = Double.parseDouble(amountStr);
            BidTransaction newBid = new BidTransaction(currentBidder, amount);
            Auction auction = MainApp.getAuctionForItem(item);
            if (auction == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi",
                    "Không tìm thấy phiên đấu giá cho sản phẩm này!");
                return;
            }
            auction.placeBid(newBid);
            item.setStartingPrice(auction.getCurrentPrice());
            item.setCurrentHighestBid(auction.getCurrentPrice());
            table.refresh();
            updateBalanceUI();
            showAlert(Alert.AlertType.INFORMATION, "Thành công",
                "Bạn đã đặt giá thành công!");
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu",
                "Vui lòng nhập một con số hợp lệ!");
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.ERROR, "Thông báo đấu giá", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống",
                "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    private void updateBalanceUI() {
        if (currentBidder != null && lblBalance != null) {
            lblBalance.setText(String.format("%.2f $", currentBidder.getAvailableBalance()));
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
