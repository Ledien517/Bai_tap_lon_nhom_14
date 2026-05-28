package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.NetworkClient;
import com.auction.common.factory.ItemFactory;
import com.auction.common.model.Item;
import com.auction.common.model.Seller;
import com.auction.common.model.Auction;
import com.auction.common.model.User;
import com.auction.common.protocol.Request;
import com.auction.common.protocol.Response;
import javafx.application.Platform;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class ItemManagementController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol;
    @FXML private TableColumn<Item, String> typeCol;
    @FXML private TableColumn<Item, String> nameCol;
    @FXML private TableColumn<Item, Double> priceCol;
    @FXML private TableColumn<Item, String> statusCol;
    @FXML private TableColumn<Item, Double> minIncCol;
    @FXML private TableColumn<Item, String> startTimeCol;
    @FXML private TableColumn<Item, String> endTimeCol;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblExtraParam;
    @FXML private TextField txtExtraParam;
    @FXML private TextField txtMinIncrement;
    @FXML private TextField txtStartTime;
    @FXML private TextField txtEndTime;
    @FXML private TextField txtDescription;
    @FXML private Label lblSellerInfo;
    @FXML private Label lblBalance;

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ObservableList<Item> data;
    private Seller currentSeller;
    private NetworkClient.BroadcastListener broadcastListener;

    @FXML
    public void initialize() {
        if (MainApp.getCurrentUser() instanceof Seller) {
            this.currentSeller = (Seller) MainApp.getCurrentUser();
            if (lblSellerInfo != null) {
                lblSellerInfo.setText("Chào, " + currentSeller.getUsername());
            }
            updateBalanceUI();
            refreshUserBalance();
        }

        // --- 1. Liên kết các cột dữ liệu ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        typeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            if (auction != null) {
                String status = auction.getStatusDisplay();
                return new SimpleStringProperty(translateStatus(status));
            }
            return new SimpleStringProperty("—");
        });

        minIncCol.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));

        startTimeCol.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getStartTime();
            return new SimpleStringProperty(t != null ? t.format(DT_FORMATTER) : "—");
        });

        endTimeCol.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getEndTime();
            return new SimpleStringProperty(t != null ? t.format(DT_FORMATTER) : "—");
        });

        // --- 2. Tô màu trạng thái ---
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Đang diễn ra" -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        case "Sắp diễn ra"  -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                        case "Đã kết thúc"  -> setStyle("-fx-text-fill: #94a3b8;");
                        default              -> setStyle("");
                    }
                }
            }
        });

        // --- 3. Nạp dữ liệu ---
        data = FXCollections.observableArrayList();
        table.setItems(data);
        loadDataFromServer();

        // --- Double-click để mở cửa sổ theo dõi cuộc đấu giá ---
        table.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showAuctionWindow(row.getItem());
                }
            });
            return row;
        });

        // --- 4. Đăng ký nhận Broadcast ---
        broadcastListener = (type, payload) -> Platform.runLater(() -> handleServerBroadcast(type, payload));
        NetworkClient.getInstance().addBroadcastListener(broadcastListener);

        // --- 5. ComboBox ---
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> updateExtraField(newV));
        cbType.setValue("Art");

        // --- 6. Bộ làm mới bảng mỗi giây ---
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> table.refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
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
                            // Chỉ hiển thị sản phẩm của Seller đang đăng nhập
                            if (currentSeller != null &&
                                auction.getItem().getSeller().getUsername().equals(currentSeller.getUsername())) {
                                data.add(auction.getItem());
                            }
                        }
                        table.refresh();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lấy danh sách: " + response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() ->
                    showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    private void handleServerBroadcast(String type, Object payload) {
        switch (type) {
            case "NEW_ITEM" -> {
                Item newItem = (Item) payload;
                Auction newAuction = new Auction(newItem.getSeller(), newItem);
                MainApp.registerAuction(newItem.getId(), newAuction);
                // Chỉ thêm vào bảng nếu là sản phẩm của Seller hiện tại
                if (currentSeller != null && newItem.getSeller().getUsername().equals(currentSeller.getUsername())) {
                    boolean exists = data.stream().anyMatch(i -> i.getId().equals(newItem.getId()));
                    if (!exists) data.add(newItem);
                }
                table.refresh();
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
                table.refresh();
            }
            case "AUCTION_FINISHED" -> {
                Auction finishedAuction = (Auction) payload;
                MainApp.registerAuction(finishedAuction.getItem().getId(), finishedAuction);
                table.refresh();
                refreshUserBalance();
            }
            case "DELETE_AUCTION" -> {
                String deletedItemId = (String) payload;
                data.removeIf(item -> item.getId().equals(deletedItemId));
                table.refresh();
            }
        }
    }

    @FXML
    private void handleAddItem() {
        try {
            String type = cbType.getValue();
            String id = txtId.getText().trim();
            String name = txtName.getText().trim();
            String extraParam = txtExtraParam.getText().trim();
            String description = (txtDescription != null && !txtDescription.getText().trim().isEmpty())
                    ? txtDescription.getText().trim() : "Không có mô tả";

            if (id.isEmpty() || name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ Mã ID và Tên sản phẩm!");
                return;
            }
            double price = Double.parseDouble(txtPrice.getText());
            double minIncrement = Double.parseDouble(txtMinIncrement.getText());

            if (extraParam.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập: " + lblExtraParam.getText());
                return;
            }

            // Parse thời gian — dùng mặc định nếu để trống
            LocalDateTime startTime = parseDateTime(txtStartTime.getText(), LocalDateTime.now());
            LocalDateTime endTime = parseDateTime(txtEndTime.getText(), LocalDateTime.now().plusDays(7));

            if (!endTime.isAfter(startTime)) {
                showAlert(Alert.AlertType.WARNING, "Sai thời gian", "Thời gian kết thúc phải sau thời gian bắt đầu!");
                return;
            }

            Item newItem = ItemFactory.createItem(
                currentSeller, type, id, name, description,
                price, startTime, endTime, minIncrement, extraParam
            );

            // Chạy network trên background thread
            new Thread(() -> {
                try {
                    Response response = NetworkClient.getInstance().sendRequestAndWait(new Request("REGISTER_ITEM", newItem));
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đăng ký sản phẩm đấu giá thành công!");
                            clearForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", e.getMessage()));
                }
            }).start();

        } catch (NumberFormatException nfe) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Giá và Bước giá phải là con số hợp lệ!");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteItem() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một sản phẩm để xóa!");
            return;
        }

        // Hộp thoại xác nhận
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Bạn có chắc muốn xóa sản phẩm này không?");
        confirm.setContentText("Sản phẩm: \"" + selected.getName() + "\" (Mã: " + selected.getId() + ")\n"
            + "⚠ Lưu ý: Chỉ có thể xóa khi chưa có người đặt giá.");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Chạy network trên background thread
            new Thread(() -> {
                try {
                    Response response = NetworkClient.getInstance()
                        .sendRequestAndWait(new Request("DELETE_ITEM", selected.getId()));
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(response.getStatus())) {
                            // Bảng sẽ tự cập nhật qua broadcast DELETE_AUCTION
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã xóa sản phẩm khỏi hệ thống!");
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Lỗi", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() ->
                        showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                }
            }).start();
        }
    }

    @FXML
    private void handleRefresh() {
        loadDataFromServer();
    }

    @FXML
    private void handleLogout() {
        try {
            if (broadcastListener != null) {
                NetworkClient.getInstance().removeBroadcastListener(broadcastListener);
            }
            MainApp.setCurrentUser(null);
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay về màn hình đăng nhập!");
        }
    }

    public void setSeller(Seller seller) {
        this.currentSeller = seller;
        if (lblSellerInfo != null) {
            lblSellerInfo.setText("Chào, " + seller.getUsername());
        }
        updateBalanceUI();
    }

    private void updateBalanceUI() {
        if (currentSeller != null) {
            Platform.runLater(() -> {
                if (lblBalance != null) {
                    lblBalance.setText(String.format("Số dư: %.2f $", currentSeller.getBalance()));
                }
            });
        }
    }

    private void refreshUserBalance() {
        if (currentSeller != null) {
            new Thread(() -> {
                try {
                    Response res = NetworkClient.getInstance().sendRequestAndWait(
                        new Request("GET_USER", currentSeller.getUsername()));
                    if ("SUCCESS".equals(res.getStatus()) && res.getData() instanceof Seller s) {
                        this.currentSeller = s;
                        updateBalanceUI();
                    }
                } catch (Exception e) {
                    System.err.println("[ItemManagementController] Lỗi cập nhật số dư: " + e.getMessage());
                }
            }).start();
        }
    }

    @FXML
    private void handleWithdraw() {
        if (currentSeller == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa xác định người dùng!");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("0");
        dialog.setTitle("Rút tiền khỏi tài khoản");
        dialog.setHeaderText("Số dư hiện tại: " + String.format("%.2f $", currentSeller.getBalance()));
        dialog.setContentText("Nhập số tiền muốn rút ($):");
        dialog.showAndWait().ifPresent(amountStr -> {
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0!");
                currentSeller.withdraw(amount);

                // Chạy network trên background thread
                new Thread(() -> {
                    try {
                        Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("UPDATE_USER", currentSeller));
                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(res.getStatus())) {
                                updateBalanceUI();
                                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                    String.format("Đã rút thành công %.2f $ từ tài khoản.", amount));
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

    private void showAuctionWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) return;

        Stage stage = new Stage();
        stage.setTitle("Theo dõi đấu giá: " + item.getName());

        // ===== ROOT =====
        VBox root = new VBox(0);
        root.getStylesheets().add(getClass().getResource("/com/auction/client/css/styles.css").toExternalForm());
        root.setStyle("-fx-background-color: #f8fafc;");

        // ===== HEADER =====
        HBox header = new HBox();
        header.getStyleClass().add("auction-header");
        Label lblTitle = new Label("🏛 THEO DÕI ĐẤU GIÁ (SELLER)");
        lblTitle.getStyleClass().add("auction-title");
        Label lblItemName = new Label(item.getName());
        lblItemName.getStyleClass().add("auction-item-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(lblTitle, spacer, lblItemName);

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
        txtAuctionDetails.setPrefHeight(220);
        txtAuctionDetails.setText(auction.getInfo());
        infoCard.getChildren().addAll(lblInfoTitle, txtAuctionDetails);

        // Card: Giá hiện tại + Thông báo Người Bán
        VBox bidCard = new VBox(12);
        bidCard.getStyleClass().add("auction-card");

        Label lblPriceTitle = new Label("💰 Thông tin giá");
        lblPriceTitle.getStyleClass().add("auction-section-title");

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblPriceStatic = new Label("Giá hiện tại:");
        lblPriceStatic.getStyleClass().add("auction-label");
        Label lblCurrentPrice = new Label(String.format("%.2f $", auction.getCurrentPrice()));
        lblCurrentPrice.getStyleClass().add("auction-price");
        priceRow.getChildren().addAll(lblPriceStatic, lblCurrentPrice);

        Label lblWarning = new Label("⚠ Bạn là Người bán sản phẩm này. Bạn chỉ được phép theo dõi cuộc đấu giá và không thể tham gia đặt giá hoặc cấu hình đấu giá tự động.");
        lblWarning.getStyleClass().add("auction-label");
        lblWarning.setStyle("-fx-text-fill: #e11d48; -fx-font-weight: bold;");
        lblWarning.setWrapText(true);

        bidCard.getChildren().addAll(lblPriceTitle, priceRow, lblWarning);

        body.getChildren().addAll(infoCard, bidCard);
        root.getChildren().addAll(header, body);

        // ===== TIMER cập nhật realtime =====
        Timeline detailTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            Auction latest = MainApp.getAuctionForItem(item);
            if (latest != null) {
                txtAuctionDetails.setText(latest.getInfo());
                lblCurrentPrice.setText(String.format("%.2f $", latest.getCurrentPrice()));
            }
        }));
        detailTimer.setCycleCount(Animation.INDEFINITE);
        detailTimer.play();
        stage.setOnCloseRequest(e -> detailTimer.stop());

        Scene scene = new Scene(root, 520, 520);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private LocalDateTime parseDateTime(String text, LocalDateTime defaultValue) {
        if (text == null || text.trim().isEmpty()) return defaultValue;
        try {
            return LocalDateTime.parse(text.trim(), DT_FORMATTER);
        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.WARNING, "Định dạng thời gian không hợp lệ",
                "Vui lòng nhập theo định dạng: dd/MM/yyyy HH:mm\n(VD: 28/05/2026 10:00)\nHệ thống sẽ dùng thời gian mặc định.");
            return defaultValue;
        }
    }

    private void updateExtraField(String type) {
        if (type == null) return;
        switch (type) {
            case "Art"         -> { lblExtraParam.setText("Tác giả:"); txtExtraParam.setPromptText("Nhập tên tác giả"); }
            case "Vehicle"     -> { lblExtraParam.setText("Hãng xe:"); txtExtraParam.setPromptText("Nhập hãng xe"); }
            case "Electronics" -> { lblExtraParam.setText("Bảo hành (tháng):"); txtExtraParam.setPromptText("Nhập số tháng"); }
        }
    }

    private void clearForm() {
        txtId.clear(); txtName.clear(); txtPrice.clear();
        txtMinIncrement.clear(); txtExtraParam.clear();
        txtStartTime.clear(); txtEndTime.clear();
        if (txtDescription != null) txtDescription.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}