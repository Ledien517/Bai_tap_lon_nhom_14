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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {

    // === QUẢN LÝ NGƯỜI DÙNG ===
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> userUsernameCol;
    @FXML private TableColumn<User, String> userRoleCol;
    @FXML private TableColumn<User, Double> userBalanceCol;
    @FXML private TableColumn<User, Double> userFrozenCol;
    @FXML private TableColumn<User, String> userStatusCol;
    @FXML private Label lblUserCount;

    // === QUẢN LÝ SẢN PHẨM ===
    @FXML private TableView<Item> itemTable;
    @FXML private TableColumn<Item, String> itemIdCol;
    @FXML private TableColumn<Item, String> itemNameCol;
    @FXML private TableColumn<Item, String> itemTypeCol;
    @FXML private TableColumn<Item, String> itemSellerCol;
    @FXML private TableColumn<Item, Double> itemPriceCol;
    @FXML private TableColumn<Item, Double> itemMinIncCol;
    @FXML private TableColumn<Item, String> itemStartTimeCol;
    @FXML private TableColumn<Item, String> itemEndTimeCol;
    @FXML private TableColumn<Item, String> itemStatusCol;
    @FXML private Label lblItemCount;
    @FXML private TextField txtSearch;

    // === HEADER ===
    @FXML private Label lblUsername;

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ObservableList<User> userData = FXCollections.observableArrayList();
    private ObservableList<Item> itemData = FXCollections.observableArrayList();
    private NetworkClient.BroadcastListener broadcastListener;
    private Timeline periodicRefresh;

    @FXML
    public void initialize() {
        // 1. Cài đặt các thông tin chung
        User admin = MainApp.getCurrentUser();
        if (admin != null) {
            lblUsername.setText("Chào, " + admin.getUsername());
        }

        // 2. Setup bảng Quản lý Người dùng
        userUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        userRoleCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRole().name()));
        
        userBalanceCol.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            if (u instanceof Bidder b) {
                return new javafx.beans.property.SimpleObjectProperty<>(b.getAvailableBalance());
            } else if (u instanceof Seller s) {
                return new javafx.beans.property.SimpleObjectProperty<>(s.getBalance());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(0.0);
        });

        userFrozenCol.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            if (u instanceof Bidder b) {
                return new javafx.beans.property.SimpleObjectProperty<>(b.getFrozenBalance());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(0.0);
        });

        userStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Định dạng trạng thái người dùng (Ban: Đỏ, Active: Xanh/Bình thường)
        userStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                if ("BANNED".equalsIgnoreCase(status)) {
                    setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else {
                    setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                }
            }
        });

        // Hỗ trợ tô màu đỏ nhẹ cho dòng người dùng bị Ban
        userTable.setRowFactory(tv -> {
            return new TableRow<>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setStyle("");
                    } else if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                        setStyle("-fx-background-color: #fee2e2;"); // light red background
                    } else {
                        setStyle("");
                    }
                }
            };
        });

        userTable.setItems(userData);

        // 3. Setup bảng Quản lý Sản phẩm
        itemIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        itemNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        itemPriceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        itemMinIncCol.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));
        itemTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));

        itemSellerCol.setCellValueFactory(cellData -> {
            Seller s = cellData.getValue().getSeller();
            return new SimpleStringProperty(s != null ? s.getUsername() : "—");
        });

        itemStartTimeCol.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getStartTime();
            return new SimpleStringProperty(t != null ? t.format(DT_FORMATTER) : "—");
        });

        itemEndTimeCol.setCellValueFactory(cellData -> {
            LocalDateTime t = cellData.getValue().getEndTime();
            return new SimpleStringProperty(t != null ? t.format(DT_FORMATTER) : "—");
        });

        itemStatusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            return auction != null
                ? new SimpleStringProperty(translateItemStatus(auction.getStatusDisplay()))
                : new SimpleStringProperty("—");
        });

        // Tô màu trạng thái sản phẩm giống Bidder
        itemStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "Đang diễn ra" -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    case "Sắp diễn ra"  -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    case "Đã kết thúc"  -> setStyle("-fx-text-fill: #94a3b8;");
                    default             -> setStyle("");
                }
            }
        });

        itemTable.setRowFactory(tv -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showAuctionMonitorWindow(row.getItem());
                }
            });
            return row;
        });

        itemTable.setItems(itemData);

        // 4. Load dữ liệu ban đầu
        loadUsersFromServer();
        loadItemsFromServer();

        // 5. Đăng ký nhận Broadcast từ Server
        broadcastListener = (type, payload) ->
            Platform.runLater(() -> handleServerBroadcast(type, payload));
        NetworkClient.getInstance().addBroadcastListener(broadcastListener);

        // 6. Định kỳ làm mới danh sách
        periodicRefresh = new Timeline(new KeyFrame(Duration.seconds(10), e -> {
            userTable.refresh();
            itemTable.refresh();
            updateCounts();
        }));
        periodicRefresh.setCycleCount(Animation.INDEFINITE);
        periodicRefresh.play();
    }

    private String translateItemStatus(String status) {
        return switch (status) {
            case "ACTIVE"   -> "Đang diễn ra";
            case "UPCOMING" -> "Sắp diễn ra";
            case "FINISHED" -> "Đã kết thúc";
            default         -> status;
        };
    }

    private void sortAuctions() {
        FXCollections.sort(itemData, (i1, i2) -> {
            Auction a1 = MainApp.getAuctionForItem(i1);
            Auction a2 = MainApp.getAuctionForItem(i2);
            boolean a1Finished = a1 != null && "FINISHED".equals(a1.getStatusDisplay());
            boolean a2Finished = a2 != null && "FINISHED".equals(a2.getStatusDisplay());
            
            if (a1Finished && !a2Finished) return 1;
            if (!a1Finished && a2Finished) return -1;
            
            if (i1.getStartTime() != null && i2.getStartTime() != null) {
                return i2.getStartTime().compareTo(i1.getStartTime());
            }
            return 0;
        });
    }

    // === NETWORK - TẢI NGƯỜI DÙNG ===
    private void loadUsersFromServer() {
        new Thread(() -> {
            try {
                Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("GET_ALL_USERS", null));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<User> list = (List<User>) res.getData();
                        userData.clear();
                        userData.addAll(list);
                        updateCounts();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi nạp dữ liệu", res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    // === NETWORK - TẢI SẢN PHẨM ===
    @SuppressWarnings("unchecked")
    private void loadItemsFromServer() {
        new Thread(() -> {
            try {
                Response response = NetworkClient.getInstance().sendRequestAndWait(new Request("GET_ITEMS", null));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(response.getStatus())) {
                        List<Auction> auctions = (List<Auction>) response.getData();
                        itemData.clear();
                        for (Auction auction : auctions) {
                            MainApp.registerAuction(auction.getItem().getId(), auction);
                            itemData.add(auction.getItem());
                        }
                        sortAuctions();
                        updateCounts();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi nạp sản phẩm", response.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    // === ĐIỀU KHIỂN: BAN / UNBAN ===
    @FXML
    private void handleBanUser(ActionEvent event) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một người dùng từ danh sách!");
            return;
        }

        if ("superadmin".equalsIgnoreCase(selectedUser.getUsername())) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể ban tài khoản Super Admin!");
            return;
        }

        User currentUser = MainApp.getCurrentUser();
        if (currentUser != null && currentUser.getUsername().equalsIgnoreCase(selectedUser.getUsername())) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Bạn không thể tự khóa (Ban) chính mình!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận Ban");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn khóa tài khoản '" + selectedUser.getUsername() + "'?");
        confirm.showAndWait().ifPresent(btnType -> {
            if (btnType == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("BAN_USER", selectedUser.getUsername()));
                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(res.getStatus())) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã ban tài khoản thành công!");
                                loadUsersFromServer();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", res.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML
    private void handleUnbanUser(ActionEvent event) {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một người dùng từ danh sách!");
            return;
        }

        if (!"BANNED".equalsIgnoreCase(selectedUser.getStatus())) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Tài khoản này hiện không bị khóa.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận mở khóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc chắn muốn mở khóa tài khoản '" + selectedUser.getUsername() + "'?");
        confirm.showAndWait().ifPresent(btnType -> {
            if (btnType == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("UNBAN_USER", selectedUser.getUsername()));
                        Platform.runLater(() -> {
                            if ("SUCCESS".equals(res.getStatus())) {
                                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã mở khóa tài khoản thành công!");
                                loadUsersFromServer();
                            } else {
                                showAlert(Alert.AlertType.ERROR, "Lỗi", res.getMessage());
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
                    }
                }).start();
            }
        });
    }

    @FXML
    private void handleRefreshUsers(ActionEvent event) {
        loadUsersFromServer();
    }

    // === TÌM KIẾM SẢN PHẨM ===
    @FXML
    private void handleSearchItem(ActionEvent event) {
        String keyword = (txtSearch != null) ? txtSearch.getText() : "";
        new Thread(() -> {
            try {
                Response res = NetworkClient.getInstance().sendRequestAndWait(new Request("SEARCH_ITEMS", keyword));
                Platform.runLater(() -> {
                    if ("SUCCESS".equals(res.getStatus())) {
                        @SuppressWarnings("unchecked")
                        List<Auction> results = (List<Auction>) res.getData();
                        itemData.clear();
                        for (Auction auction : results) {
                            MainApp.registerAuction(auction.getItem().getId(), auction);
                            itemData.add(auction.getItem());
                        }
                        sortAuctions();
                        updateCounts();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi tìm kiếm", res.getMessage());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleRefreshAll(ActionEvent event) {
        if (txtSearch != null) txtSearch.clear();
        loadItemsFromServer();
    }

    @FXML
    private void handleViewDetails(ActionEvent event) {
        Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một sản phẩm từ danh sách!");
            return;
        }
        showAuctionMonitorWindow(selectedItem);
    }

    // === BROADCAST LISTENER ===
    private void handleServerBroadcast(String type, Object payload) {
        switch (type) {
            case "NEW_ITEM" -> {
                Item newItem = (Item) payload;
                Auction newAuction = new Auction(newItem.getSeller(), newItem);
                MainApp.registerAuction(newItem.getId(), newAuction);
                boolean exists = itemData.stream().anyMatch(i -> i.getId().equals(newItem.getId()));
                if (!exists) itemData.add(newItem);
                sortAuctions();
                updateCounts();
            }
            case "BID_UPDATE" -> {
                Auction updatedAuction = (Auction) payload;
                MainApp.registerAuction(updatedAuction.getItem().getId(), updatedAuction);
                for (Item current : itemData) {
                    if (current.getId().equals(updatedAuction.getItem().getId())) {
                        current.setStartingPrice(updatedAuction.getCurrentPrice());
                        current.setBidList(updatedAuction.getBidList());
                        break;
                    }
                }
                sortAuctions();
                itemTable.refresh();
            }
            case "AUCTION_FINISHED" -> {
                Auction finishedAuction = (Auction) payload;
                MainApp.registerAuction(finishedAuction.getItem().getId(), finishedAuction);
                sortAuctions();
                itemTable.refresh();
            }
            case "DELETE_AUCTION" -> {
                String deletedItemId = (String) payload;
                itemData.removeIf(item -> item.getId().equals(deletedItemId));
                sortAuctions();
                updateCounts();
            }
        }
    }

    // === HIỂN THỊ CHI TIẾT THEO DÕI CHO ADMIN (CHỈ XEM) ===
    private void showAuctionMonitorWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy thông tin phiên đấu giá!");
            return;
        }

        Stage stage = new Stage();
        stage.setTitle("Màn hình Giám sát Admin - " + item.getName());

        // ===== ROOT =====
        VBox root = new VBox(0);
        root.getStylesheets().add(getClass().getResource("/com/auction/client/css/styles.css").toExternalForm());
        root.setStyle("-fx-background-color: #f8fafc;");

        // ===== HEADER =====
        HBox header = new HBox();
        header.getStyleClass().add("auction-header");
        Label lblTitle = new Label("🏛 GIÁM SÁT ĐẤU GIÁ (ADMIN)");
        lblTitle.getStyleClass().add("auction-title");
        Label lblItemName = new Label(item.getName());
        lblItemName.getStyleClass().add("auction-item-name");
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
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
        txtAuctionDetails.setPrefHeight(200);
        txtAuctionDetails.setText(auction.getInfo());
        infoCard.getChildren().addAll(lblInfoTitle, txtAuctionDetails);

        // Card: Chi tiết Giám sát Admin
        VBox monitorCard = new VBox(12);
        monitorCard.getStyleClass().add("auction-card");

        Label lblMonitorTitle = new Label("📊 Trạng thái Giám sát");
        lblMonitorTitle.getStyleClass().add("auction-section-title");

        HBox priceRow = new HBox(10);
        priceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblPriceStatic = new Label("Giá hiện tại:");
        lblPriceStatic.getStyleClass().add("auction-label");
        Label lblCurrentPrice = new Label(String.format("%.2f $", auction.getCurrentPrice()));
        lblCurrentPrice.getStyleClass().add("auction-price");
        priceRow.getChildren().addAll(lblPriceStatic, lblCurrentPrice);

        // Banner thông báo độc quyền của Admin
        Label lblAdminBanner = new Label("📢 Quyền Admin: Bạn chỉ có thể theo dõi tiến trình đấu giá.");
        lblAdminBanner.setWrapText(true);
        lblAdminBanner.setStyle("-fx-text-fill: #15803d; -fx-background-color: #dcfce7; -fx-padding: 12px; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-border-color: #bbf7d0; -fx-border-radius: 8px; -fx-font-size: 13.5px; -fx-alignment: center;");
        lblAdminBanner.setMaxWidth(Double.MAX_VALUE);

        monitorCard.getChildren().addAll(lblMonitorTitle, priceRow, lblAdminBanner);

        body.getChildren().addAll(infoCard, monitorCard);
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

        Scene scene = new Scene(root, 520, 560);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void updateCounts() {
        if (lblUserCount != null) {
            lblUserCount.setText(userData.size() + " người dùng");
        }
        if (lblItemCount != null) {
            lblItemCount.setText(itemData.size() + " sản phẩm");
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            if (broadcastListener != null) {
                NetworkClient.getInstance().removeBroadcastListener(broadcastListener);
            }
            if (periodicRefresh != null) {
                periodicRefresh.stop();
            }
            MainApp.setCurrentUser(null);
            MainApp.switchScene("/com/auction/client/view/LoginView.fxml");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đăng xuất!");
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