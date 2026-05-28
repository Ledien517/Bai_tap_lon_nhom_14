
package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.NetworkClient;
import com.auction.common.model.*;
import com.auction.common.protocol.Request;
import com.auction.common.protocol.Response;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BidderController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol, typeCol, nameCol, statusCol, sellerCol, startTimeCol, endTimeCol;
    @FXML private TableColumn<Item, Double> priceCol;
    @FXML private TableColumn<Item, Double> minIncCol;
    @FXML private Label lblUsername, lblBalance, lblItemCount;
    @FXML private TextField txtSearch;

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ObservableList<Item> data;
    private Bidder currentBidder;
    private NetworkClient.BroadcastListener broadcastListener;

    @FXML
    public void initialize() {
        // Setup các cột TableView
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        minIncCol.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));
        typeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));

        sellerCol.setCellValueFactory(cellData -> {
            Seller s = cellData.getValue().getSeller();
            return new SimpleStringProperty(s != null ? s.getUsername() : "—");
        });

        startTimeCol.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getStartTime();
            return new SimpleStringProperty(t != null ? t.format(DT_FORMATTER) : "—");
        });

        endTimeCol.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getEndTime();
            return new SimpleStringProperty(t != null ? t.format(DT_FORMATTER) : "—");
        });

        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            return auction != null
                ? new SimpleStringProperty(translateStatus(auction.getStatusDisplay()))
                : new SimpleStringProperty("—");
        });

        // Tô màu trạng thái
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Đang diễn ra" -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    case "Sắp diễn ra"  -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    case "Đã kết thúc"  -> setStyle("-fx-text-fill: #94a3b8;");
                    default             -> setStyle("");
                }
            }
        });

        data = FXCollections.observableArrayList();
        table.setItems(data);

        loadDataFromServer();

        // Lấy Bidder từ session
        User user = MainApp.getCurrentUser();
        if (user instanceof Bidder bidder) setBidder(bidder);

        // Đăng ký nhận Broadcast từ Server
        broadcastListener = (type, payload) ->
            javafx.application.Platform.runLater(() -> handleServerBroadcast(type, payload));
        NetworkClient.getInstance().addBroadcastListener(broadcastListener);

        // Double-click để mở cửa sổ đặt giá
        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showAuctionWindow(row.getItem());
                }
            });
            return row;
        });

        // Timer cập nhật trạng thái mỗi 5 giây
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            table.refresh();
            updateItemCount();
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "ACTIVE"   -> "Đang diễn ra";
            case "UPCOMING" -> "Sắp diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            default         -> status;
        };
    }

    @SuppressWarnings("unchecked")
    private void loadDataFromServer() {
        // Chạy network trên background thread để không làm đóng băng giao diện
        new Thread(() -> {
            try {
                Response response = NetworkClient.getInstance().sendRequestAndWait(new Request("GET_ITEMS", null));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        List<Auction> auctions = (List<Auction>) response.getData();
                        data.clear();
                        for (Auction auction : auctions) {
                            MainApp.registerAuction(auction.getItem().getId(), auction);
                            data.add(auction.getItem());
                        }
                        table.refresh();
                        updateItemCount();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lấy danh sách đấu giá: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Lỗi Hệ Thống", "Đã xảy ra lỗi: " + e.getMessage()));
            }
        }).start();
    }

    public void setBidder(Bidder bidder) {
        this.currentBidder = bidder;
        if (lblUsername != null) lblUsername.setText("Chào, " + bidder.getUsername());
        if (lblBalance  != null) lblBalance.setText(String.format("%.2f $", bidder.getAvailableBalance()));
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            if (broadcastListener != null) NetworkClient.getInstance().removeBroadcastListener(broadcastListener);
            MainApp.setCurrentUser(null);
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay về màn hình đăng nhập!");
        }
    }

    @FXML
    private void handleSearchItem(ActionEvent event) {
        String keyword = (txtSearch != null) ? txtSearch.getText() : "";
        // Chạy network trên background thread
        new Thread(() -> {
            try {
                Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("SEARCH_ITEMS", keyword));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<Auction> results = (List<Auction>) res.getData();
                        data.clear();
                        for (Auction auction : results) {
                            MainApp.registerAuction(auction.getItem().getId(), auction);
                            data.add(auction.getItem());
                        }
                        table.refresh();
                        updateItemCount();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi tìm kiếm", res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRefreshAll(ActionEvent event) {
        if (txtSearch != null) txtSearch.clear();
        loadDataFromServer();
    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        if (currentBidder == null) return;
        // Chạy network trên background thread
        new Thread(() -> {
            try {
                Response res = NetworkClient.getInstance().sendRequestAndWait(
                    new Request("GET_MY_BIDS", currentBidder.getUsername()));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<Auction> results = (List<Auction>) res.getData();
                        data.clear();
                        for (Auction auction : results) {
                            MainApp.registerAuction(auction.getItem().getId(), auction);
                            data.add(auction.getItem());
                        }
                        table.refresh();
                        updateItemCount();
                        if (data.isEmpty()) {
                            showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Bạn chưa tham gia phiên đấu giá nào.\nBấm 'Làm mới' để quay lại danh sách đầy đủ.");
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleTopUp(ActionEvent event) {
        if (currentBidder == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa xác định người dùng!"); return; }
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Nạp tiền vào tài khoản");
        dialog.setHeaderText("Số dư hiện tại: " + String.format("%.2f $", currentBidder.getAvailableBalance()));
        dialog.setContentText("Nhập số tiền muốn nạp ($):");
        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) throw new IllegalArgumentException("Số tiền phải lớn hơn 0!");
                currentBidder.deposit(amount);

                // Chạy network trên background thread
                new Thread(() -> {
                    try {
                        Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("UPDATE_USER", currentBidder));
                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(res.getStatus())) {
                                updateBalanceUI();
                                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                    String.format("Đã nạp thành công %.2f $ vào tài khoản.", amount));
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi Server", res.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", e.getMessage()));
                    }
                }).start();
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một con số hợp lệ!");
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", e.getMessage());
            }
        });
    }

    @FXML
    private void handlePlaceBid() {
        Item selectedItem = table.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một sản phẩm để đặt giá!");
            return;
        }
        showAuctionWindow(selectedItem);
    }

    private void showAuctionWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) return;

        Stage stage = new Stage();
        stage.setTitle("Đấu giá: " + item.getName());

        // ===== ROOT =====
        VBox root = new VBox(0);
        root.getStylesheets().add(getClass().getResource("/com/auction/client/css/styles.css").toExternalForm());
        root.setStyle("-fx-background-color: #f8fafc;");

        // ===== HEADER =====
        HBox header = new HBox();
        header.getStyleClass().add("auction-header");
        Label lblTitle = new Label("🏛 ĐẤU GIÁ TRỰC TUYẾN");
        lblTitle.getStyleClass().add("auction-title");
        Label lblItemName = new Label(item.getName());
        lblItemName.getStyleClass().add("auction-item-name");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(lblTitle, spacer, lblItemName);
        VBox.setMargin(header, new Insets(0));

        // ===== BODY =====
        VBox body = new VBox(14);
        body.setPadding(new Insets(20));
        VBox.setVgrow(body, Priority.ALWAYS);

        // Card: Thông tin phiên
        VBox infoCard = new VBox(8);
        infoCard.getStyleClass().add("auction-card");
        Label lblInfoTitle = new Label("📋 Thông tin phiên đấu giá");
        lblInfoTitle.getStyleClass().add("auction-section-title");
        TextArea txtAuctionDetails = new TextArea();
        txtAuctionDetails.setEditable(false);
        txtAuctionDetails.getStyleClass().add("auction-details");
        txtAuctionDetails.setPrefHeight(170);
        txtAuctionDetails.setText(auction.getInfo());
        infoCard.getChildren().addAll(lblInfoTitle, txtAuctionDetails);

        // Card: Giá hiện tại + đặt giá
        VBox bidCard = new VBox(12);
        bidCard.getStyleClass().add("auction-card");

        Label lblPriceTitle = new Label("💰 Đặt giá");
        lblPriceTitle.getStyleClass().add("auction-section-title");

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblPriceStatic = new Label("Giá hiện tại:");
        lblPriceStatic.getStyleClass().add("auction-label");
        Label lblCurrentPrice = new Label(String.format("%.2f $", auction.getCurrentPrice()));
        lblCurrentPrice.getStyleClass().add("auction-price");
        priceRow.getChildren().addAll(lblPriceStatic, lblCurrentPrice);

        double minPrice = auction.getCurrentPrice() + item.getMinIncrement();
        TextField txtInputBid = new TextField();
        txtInputBid.getStyleClass().add("auction-input");
        txtInputBid.setPromptText(String.format("Nhập giá mới (Min: %.2f $)", minPrice));

        Button btnBid = new Button("✅  XÁC NHẬN ĐẶT GIÁ");
        btnBid.getStyleClass().addAll("auction-btn", "auction-btn-primary");
        btnBid.setMaxWidth(Double.MAX_VALUE);

        bidCard.getChildren().addAll(lblPriceTitle, priceRow, txtInputBid, btnBid);

        // Card: Auto-Bidding
        VBox autoCard = new VBox(10);
        autoCard.getStyleClass().add("auction-card");
        Label lblAutoTitle = new Label("🤖 Đấu giá tự động");
        lblAutoTitle.getStyleClass().add("auction-section-title");

        Label lblAutoDesc = new Label("Hệ thống sẽ tự động đặt giá thay bạn cho đến khi đạt mức giá tối đa.");
        lblAutoDesc.getStyleClass().add("auction-label");
        lblAutoDesc.setWrapText(true);

        HBox autoRow = new HBox(10);
        TextField txtMaxBid = new TextField();
        txtMaxBid.getStyleClass().add("auction-input");
        txtMaxBid.setPromptText("Giá tối đa ($)");
        HBox.setHgrow(txtMaxBid, Priority.ALWAYS);
        TextField txtIncrement = new TextField();
        txtIncrement.getStyleClass().add("auction-input");
        txtIncrement.setPromptText("Bước giá tự động ($)");
        HBox.setHgrow(txtIncrement, Priority.ALWAYS);
        autoRow.getChildren().addAll(txtMaxBid, txtIncrement);

        Button btnAutoBid = new Button("🔁  ĐĂNG KÝ ĐẤU GIÁ TỰ ĐỘNG");
        btnAutoBid.getStyleClass().addAll("auction-btn", "auction-btn-auto");
        btnAutoBid.setMaxWidth(Double.MAX_VALUE);

        autoCard.getChildren().addAll(lblAutoTitle, lblAutoDesc, autoRow, btnAutoBid);

        body.getChildren().addAll(infoCard, bidCard, autoCard);
        root.getChildren().addAll(header, body);

        // ===== TIMER cập nhật realtime =====
        Timeline detailTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Auction latest = MainApp.getAuctionForItem(item);
            if (latest != null) {
                txtAuctionDetails.setText(latest.getInfo());
                lblCurrentPrice.setText(String.format("%.2f $", latest.getCurrentPrice()));
                double nextMin = latest.getCurrentPrice() + item.getMinIncrement();
                txtInputBid.setPromptText(String.format("Nhập giá mới (Min: %.2f $)", nextMin));
            }
        }));
        detailTimer.setCycleCount(Animation.INDEFINITE);
        detailTimer.play();
        stage.setOnCloseRequest(e -> detailTimer.stop());

        // ===== ACTIONS =====
        btnBid.setOnAction(e -> {
            String bidText = txtInputBid.getText();
            txtInputBid.clear();
            btnBid.setDisable(true);
            // Chạy network trên background thread
            new Thread(() -> {
                processBidLogic(item, bidText, btnBid);
            }).start();
        });

        btnAutoBid.setOnAction(e -> {
            try {
                if (txtMaxBid.getText().isEmpty() || txtIncrement.getText().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập đầy đủ giá tối đa và bước giá!");
                    return;
                }
                double maxBid = Double.parseDouble(txtMaxBid.getText());
                double increment = Double.parseDouble(txtIncrement.getText());
                AutoBid autoBid = new AutoBid(currentBidder, item.getId(), maxBid, increment);
                btnAutoBid.setDisable(true);

                // Chạy network trên background thread
                new Thread(() -> {
                    try {
                        Response response = NetworkClient.getInstance().sendRequestAndWait(
                            new Request("REGISTER_AUTO_BID", autoBid));
                        Platform.runLater(() -> {
                            btnAutoBid.setDisable(false);
                            if ("SUCCESS".equals(response.getStatus())) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng ký đấu giá tự động thành công!");
                                txtMaxBid.clear(); txtIncrement.clear();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            btnAutoBid.setDisable(false);
                            showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
                        });
                    }
                }).start();
            } catch (NumberFormatException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập số hợp lệ!");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", ex.getMessage());
            }
        });

        Scene scene = new Scene(root, 520, 720);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void processBidLogic(Item item, String amountStr, Button btnBid) {
        try {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                Platform.runLater(() -> {
                    btnBid.setDisable(false);
                    showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập số tiền!");
                });
                return;
            }
            double amount = Double.parseDouble(amountStr);
            BidTransaction newBid = new BidTransaction(currentBidder, amount);
            Response response = NetworkClient.getInstance().sendRequestAndWait(
                new Request("PLACE_BID", new Object[]{item.getId(), newBid}));

            Platform.runLater(() -> {
                btnBid.setDisable(false);
                if ("SUCCESS".equals(response.getStatus())) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "🎉 Bạn đã đặt giá thành công!");
                    refreshUserBalance();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Thất bại", response.getMessage());
                }
            });
        } catch (NumberFormatException e) {
            Platform.runLater(() -> {
                btnBid.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một con số hợp lệ!");
            });
        } catch (Exception e) {
            Platform.runLater(() -> {
                btnBid.setDisable(false);
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Có lỗi xảy ra: " + e.getMessage());
            });
        }
    }

    private void handleServerBroadcast(String type, Object payload) {
        switch (type) {
            case "NEW_ITEM" -> {
                Item newItem = (Item) payload;
                Auction newAuction = new Auction(newItem.getSeller(), newItem);
                MainApp.registerAuction(newItem.getId(), newAuction);
                boolean exists = data.stream().anyMatch(i -> i.getId().equals(newItem.getId()));
                if (!exists) data.add(newItem);
                table.refresh(); updateItemCount();
            }
            case "BID_UPDATE" -> {
                Auction updatedAuction = (Auction) payload;
                MainApp.registerAuction(updatedAuction.getItem().getId(), updatedAuction);
                for (Item current : data) {
                    if (current.getId().equals(updatedAuction.getItem().getId())) {
                        current.setStartingPrice(updatedAuction.getCurrentPrice());
                        current.setBidList(updatedAuction.getBidList());
                        break;
                    }
                }
                table.refresh(); refreshUserBalance();
            }
            case "AUCTION_FINISHED" -> {
                Auction finishedAuction = (Auction) payload;
                MainApp.registerAuction(finishedAuction.getItem().getId(), finishedAuction);
                table.refresh(); refreshUserBalance();
            }
            case "DELETE_AUCTION" -> {
                String deletedItemId = (String) payload;
                data.removeIf(item -> item.getId().equals(deletedItemId));
                table.refresh(); updateItemCount();
            }
        }
    }

    private void refreshUserBalance() {
        if (currentBidder != null) {
            new Thread(() -> {
                try {
                    Response res = NetworkClient.getInstance().sendRequestAndWait(
                        new Request("GET_USER", currentBidder.getUsername()));
                    if ("SUCCESS".equals(res.getStatus()) && res.getData() instanceof Bidder b) {
                        Platform.runLater(() -> setBidder(b));
                    }
                } catch (Exception e) {
                    System.err.println("[BidderController] Lỗi cập nhật số dư: " + e.getMessage());
                }
            }).start();
        }
    }

    private void updateBalanceUI() {
        if (currentBidder != null && lblBalance != null)
            lblBalance.setText(String.format("%.2f $", currentBidder.getAvailableBalance()));
    }

    private void updateItemCount() {
        if (lblItemCount != null)
            lblItemCount.setText(data.size() + " sản phẩm");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
