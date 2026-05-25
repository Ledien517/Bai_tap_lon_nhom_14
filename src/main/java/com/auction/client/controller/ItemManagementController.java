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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ItemManagementController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol;
    @FXML private TableColumn<Item, String> typeCol;
    @FXML private TableColumn<Item, String> nameCol;
    @FXML private TableColumn<Item, Double> priceCol;
    @FXML private TableColumn<Item, String> statusCol;
    @FXML private TableColumn<Item, Double> minIncCol;
    @FXML private TableColumn<Item, String> startDateCol;
    @FXML private TableColumn<Item, String> endDateCol;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cbType;
    @FXML private Label lblExtraParam;
    @FXML private TextField txtExtraParam;
    @FXML private TextField txtMinIncrement;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Label lblSellerBalance;

    private final ItemDAO itemDAO = new JsonItemDAO();
    private ObservableList<Item> data;
    private Seller currentSeller;

    @FXML
    public void initialize() {
        // Lấy Seller hiện tại từ phiên đăng nhập
        if (MainApp.getCurrentUser() instanceof Seller) {
            this.currentSeller = (Seller) MainApp.getCurrentUser();
        }
        
        if (this.currentSeller != null && lblSellerBalance != null) {
            lblSellerBalance.setText(String.format("%.2f $", this.currentSeller.getBalance()));
        }

        // --- 1. Liên kết các cột dữ liệu ---
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        // Hiển thị loại sản phẩm dựa trên tên lớp (Art, Vehicle...)
        typeCol.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        // Lấy trạng thái hiển thị từ Auction qua MainApp, nếu chưa có (load từ file) thì tính qua Item
        statusCol.setCellValueFactory(cellData -> {
            Item item = cellData.getValue();
            Auction auction = MainApp.getAuctionForItem(item);
            if (auction != null) {
                return new SimpleStringProperty(auction.getStatusDisplay());
            } else {
                LocalDateTime now = LocalDateTime.now();
                if (item.getStartTime() != null && now.isBefore(item.getStartTime())) {
                    return new SimpleStringProperty("UPCOMING");
                }
                if (item.getEndTime() != null && now.isAfter(item.getEndTime())) {
                    return new SimpleStringProperty("FINISHED");
                }
                return new SimpleStringProperty("ACTIVE");
            }
        });

        minIncCol.setCellValueFactory(new PropertyValueFactory<>("minIncrement"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        startDateCol.setCellValueFactory(cellData -> {
            LocalDateTime start = cellData.getValue().getStartTime();
            return new SimpleStringProperty(start != null ? start.format(formatter) : "");
        });

        endDateCol.setCellValueFactory(cellData -> {
            LocalDateTime end = cellData.getValue().getEndTime();
            return new SimpleStringProperty(end != null ? end.format(formatter) : "");
        });

        // --- 2. Nạp dữ liệu vào bảng ---
        var items = itemDAO.getAllItems();
        if (items == null) {
            data = FXCollections.observableArrayList();
            System.err.println("Cảnh báo: Không tải được dữ liệu từ file JSON!");
        } else {
            if (this.currentSeller != null) {
                java.util.List<Item> myItems = new java.util.ArrayList<>();
                // Dùng bản sao của danh sách để tránh lỗi ConcurrentModificationException khi vừa duyệt vừa xóa
                java.util.List<Item> itemsCopy = new java.util.ArrayList<>(items);
                
                for (Item item : itemsCopy) {
                    if (item.getSeller() != null && item.getSeller().getUsername().equals(this.currentSeller.getUsername())) {
                        if (item.isDeletedByAdmin()) {
                            // Cảnh báo ngay lúc vừa mở form nếu bị admin xóa trong lúc offline
                            javafx.application.Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.ERROR);
                                alert.setTitle("Cảnh báo Vi phạm");
                                alert.setHeaderText(null);
                                alert.setContentText("Sản phẩm " + item.getName() + " của bạn đã bị Admin xóa do vi phạm chính sách!");
                                alert.showAndWait();
                            });
                            // Xóa hẳn khỏi cơ sở dữ liệu sau khi đã thông báo cho người dùng
                            itemDAO.deleteItem(item.getId());
                        } else {
                            myItems.add(item);
                        }
                    }
                }
                data = FXCollections.observableArrayList(myItems);
            } else {
                data = FXCollections.observableArrayList(items);
            }
        }
        table.setItems(data);

        // --- 3. Cấu hình ComboBox và Extra Field ---
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            updateExtraField(newValue);
        });
        cbType.setValue("Art");

        // --- 4. Bộ cập nhật thời gian thực (Làm mới bảng mỗi giây và đồng bộ giá) ---
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                if (this.currentSeller != null && lblSellerBalance != null) {
                    lblSellerBalance.setText(String.format("%.2f $", this.currentSeller.getBalance()));
                }

                // 1. Đồng bộ trực tiếp từ bộ nhớ (MainApp) để UI phản hồi tức thời
                for (Item current : data) {
                    Auction auction = MainApp.getAuctionForItem(current);
                    if (auction != null) {
                        if (auction.getCurrentPrice() > current.getStartingPrice()) {
                            current.setStartingPrice(auction.getCurrentPrice());
                            current.setCurrentHighestBid(auction.getCurrentPrice());
                        }
                        // Tự động kiểm tra và trao tiền nếu phiên đã kết thúc
                        if ("FINISHED".equals(auction.getStatusDisplay())) {
                            try { auction.processWinner(); } catch (Exception ignored) {}
                        }
                    }
                }

                // 2. Đồng bộ từ Database (nếu có bidder ghi vào file JSON)
                List<Item> latestItems = itemDAO.getAllItems();
                if (latestItems != null && !latestItems.isEmpty()) {
                    java.util.List<Item> toRemove = new java.util.ArrayList<>();
                    for (Item latest : latestItems) {
                        for (Item current : data) {
                            if (current.getId().equals(latest.getId())) {
                                if (latest.isDeletedByAdmin()) {
                                    toRemove.add(current);
                                    javafx.application.Platform.runLater(() -> {
                                        Alert alert = new Alert(Alert.AlertType.ERROR);
                                        alert.setTitle("Cảnh báo Vi phạm");
                                        alert.setHeaderText(null);
                                        alert.setContentText("Sản phẩm " + latest.getName() + " của bạn đã bị Admin xóa do vi phạm chính sách!");
                                        alert.show(); // Dùng show() thay vì showAndWait() để không block Timeline
                                    });
                                    // Xóa hẳn khỏi cơ sở dữ liệu
                                    itemDAO.deleteItem(latest.getId());
                                } else if (latest.getStartingPrice() > current.getStartingPrice()) {
                                    current.setStartingPrice(latest.getStartingPrice());
                                    current.setCurrentHighestBid(latest.getCurrentHighestBid());
                                    Auction auction = MainApp.getAuctionForItem(current);
                                    if (auction != null && latest.getBidList() != null && !latest.getBidList().isEmpty()) {
                                        auction.syncBidList(latest.getBidList());
                                    }
                                }
                                break;
                            }
                        }
                    }
                    if (!toRemove.isEmpty()) {
                        data.removeAll(toRemove);
                    }
                }
                table.refresh();
            })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Thêm sự kiện Double Click để mở cửa sổ chi tiết đấu giá
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

            LocalDate startDateVal = dpStartDate.getValue();
            LocalDate endDateVal = dpEndDate.getValue();

            if (startDateVal == null || endDateVal == null) {
                new Alert(Alert.AlertType.WARNING, "Vui lòng chọn Ngày bắt đầu và Ngày kết thúc!").show();
                return;
            }
            if (endDateVal.isBefore(startDateVal)) {
                new Alert(Alert.AlertType.WARNING, "Ngày kết thúc không được trước ngày bắt đầu!").show();
                return;
            }

            LocalDateTime startDateTime;
            if (startDateVal.equals(LocalDate.now())) {
                startDateTime = LocalDateTime.now(); // Nếu chọn hôm nay, lấy thời gian hiện tại
            } else {
                startDateTime = startDateVal.atStartOfDay();
            }
            
            LocalDateTime endDateTime = endDateVal.atTime(23, 59, 59);

            // Tạo Item từ Factory
            Item newItem = ItemFactory.createItem(
                currentSeller, type, id, name, "Mô tả sản phẩm", price,
                startDateTime, endDateTime, minIncrement,
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
        } catch (IllegalArgumentException iae) {
            new Alert(Alert.AlertType.WARNING, iae.getMessage()).show();
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
        dpStartDate.setValue(null);
        dpEndDate.setValue(null);
    }

    private void showAuctionWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) {
            new Alert(Alert.AlertType.WARNING, "Sản phẩm chưa có phiên đấu giá trong bộ nhớ hệ thống!").show();
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