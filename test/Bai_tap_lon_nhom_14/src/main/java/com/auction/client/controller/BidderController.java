package com.auction.client.controller;

import com.auction.client.MainApp;
import com.auction.client.observer.BidderAuctionObserver;
import com.auction.common.model.*;
import com.auction.dao.BidTransactionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonBidTransactionDAO;
import com.auction.dao.JsonItemDAO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Locale;

public class BidderController {

    @FXML private TableView<Item> table;
    @FXML private TableColumn<Item, String> idCol, typeCol, nameCol, statusCol;
    @FXML private TableColumn<Item, String> priceCol;   // String để format số đẹp
    @FXML private Label lblUsername, lblBalance;

    private final ItemDAO itemDAO = new JsonItemDAO();
    private final BidTransactionDAO bidDAO = new JsonBidTransactionDAO();
    private ObservableList<Item> data;
    private Bidder currentBidder;

    private static final NumberFormat FMT = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        typeCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getClass().getSimpleName()));

        // Format giá đẹp: 10,000,000 thay vì 1.0E7
        priceCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(FMT.format(cellData.getValue().getCurrentHighestBid()) + " $"));

        statusCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStatusDisplay()));

        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        // Tạo Auction cho tất cả item (kể cả item không có seller → dùng dummy seller)
        for (Item item : data) {
            if (MainApp.getAuctionForItem(item) == null) {
                Seller seller = item.getSeller();
                if (seller == null) {
                    // Item load từ JSON cũ chưa có sellerUsername → tạo dummy
                    seller = new Seller("unknown_seller", "", null);
                    item.setSeller(seller);
                }
                Auction auction = new Auction(seller, item);
                MainApp.registerAuction(item.getId(), auction);
            }
        }

        // Auto-refresh bảng mỗi giây
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> table.refresh()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        // Double-click → mở cửa sổ đấu giá đầy đủ
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

    public void setBidder(Bidder bidder) {
        this.currentBidder = bidder;
        lblUsername.setText("Chào, " + bidder.getUsername());
        updateBalanceUI();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try { MainApp.switchScene("/com/auction/client/view/LoginView.fxml"); }
        catch (Exception e) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đăng xuất!"); }
    }

    @FXML
    private void handleSearchItem(ActionEvent event) {
        // Tính năng tìm kiếm: lọc theo tên
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Tìm kiếm sản phẩm");
        dialog.setHeaderText("Tìm kiếm sản phẩm theo tên:");
        dialog.setContentText("Nhập từ khóa:");

        dialog.showAndWait().ifPresent(keyword -> {
            String kw = keyword.trim().toLowerCase();
            if (kw.isEmpty()) {
                table.setItems(data);
                return;
            }
            ObservableList<Item> filtered = FXCollections.observableArrayList();
            for (Item item : data) {
                if (item.getName().toLowerCase().contains(kw)
                        || item.getId().toLowerCase().contains(kw)
                        || item.getType().toLowerCase().contains(kw)) {
                    filtered.add(item);
                }
            }
            table.setItems(filtered);
            if (filtered.isEmpty()) {
                showAlert(Alert.AlertType.INFORMATION, "Kết quả tìm kiếm",
                        "Không tìm thấy sản phẩm nào với từ khóa: \"" + keyword + "\"\n"
                                + "Bấm Tìm kiếm lần nữa với ô trống để xem tất cả.");
            }
        });
    }

    @FXML
    private void handleMyAuctions(ActionEvent event) {
        handleTopUp(event);
    }

    @FXML
    private void handleTopUp(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Số dư hiện tại: " + FMT.format(currentBidder.getAvailableBalance()) + " $");
        dialog.setContentText("Nhập số tiền muốn nạp ($):");
        dialog.showAndWait().ifPresent(s -> {
            try {
                double amount = Double.parseDouble(s.trim());
                if (amount <= 0) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải lớn hơn 0!"); return; }
                currentBidder.deposit(amount);
                updateBalanceUI();
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Nạp thành công " + FMT.format(amount) + " $\nSố dư mới: " + FMT.format(currentBidder.getAvailableBalance()) + " $");
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một số hợp lệ!");
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            }
        });
    }

    @FXML
    private void handleWithdraw(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rút tiền");
        dialog.setHeaderText(
                "Số dư khả dụng: " + FMT.format(currentBidder.getAvailableBalance()) + " $\n"
                        + "Đang đóng băng: " + FMT.format(currentBidder.getFrozenBalance()) + " $"
        );
        dialog.setContentText("Nhập số tiền muốn rút ($):");
        dialog.showAndWait().ifPresent(s -> {
            try {
                double amount = Double.parseDouble(s.trim());
                if (amount <= 0) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải lớn hơn 0!"); return; }
                currentBidder.withdraw(amount);
                updateBalanceUI();
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Rút thành công " + FMT.format(amount) + " $\nSố dư còn lại: " + FMT.format(currentBidder.getAvailableBalance()) + " $");
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một số hợp lệ!");
            } catch (IllegalArgumentException e) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
            }
        });
    }

    /**
     * Nút "Đặt giá nhanh" — dialog 1 bước, KHÁC với double-click (mở cửa sổ đầy đủ).
     * Yêu cầu phải chọn sản phẩm trên bảng trước.
     */
    @FXML
    private void handlePlaceBid() {
        Item selectedItem = table.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chọn một sản phẩm từ bảng trước!");
            return;
        }
        Auction auction = MainApp.getAuctionForItem(selectedItem);
        if (auction == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy phiên đấu giá!");
            return;
        }

        // Quick-bid dialog
        String status = auction.getStatusDisplay();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Đặt giá nhanh — " + selectedItem.getName());
        dialog.setHeaderText(
                "Trạng thái: " + status + "\n"
                        + "Giá hiện tại: " + FMT.format(auction.getCurrentPrice()) + " $\n"
                        + "Bước giá tối thiểu: " + FMT.format(selectedItem.getMinIncrement()) + " $\n"
                        + "Số dư của bạn: " + FMT.format(currentBidder.getAvailableBalance()) + " $"
        );
        dialog.setContentText("Nhập giá đặt:");

        dialog.showAndWait().ifPresent(s -> {
            processBidLogic(selectedItem, auction, s, null);
        });
    }

    // ===== Cửa sổ đấu giá đầy đủ (mở bằng double-click) =====
    private void showAuctionWindow(Item item) {
        Auction auction = MainApp.getAuctionForItem(item);
        if (auction == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống",
                    "Không tìm thấy phiên đấu giá!\nHãy đăng nhập lại để tải dữ liệu mới.");
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("Đấu giá: " + item.getName());

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(18));

        // --- Chi tiết sản phẩm ---
        Label lblTitle = new Label("📦 " + item.getName().toUpperCase());
        lblTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");

        TextArea txtInfo = new TextArea(auction.getInfo());
        txtInfo.setEditable(false);
        txtInfo.setPrefHeight(130);
        txtInfo.setWrapText(true);
        txtInfo.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // --- Lịch sử đặt giá ---
        Label lblHistory = new Label("📋 Lịch sử đặt giá:");
        lblHistory.setStyle("-fx-font-weight: bold;");

        ListView<String> bidListView = new ListView<>();
        bidListView.setPrefHeight(140);

        // Load từ in-memory trước, fallback sang file JSON
        for (BidTransaction bid : auction.getBidList()) {
            bidListView.getItems().add(formatBid(bid));
        }
        if (bidListView.getItems().isEmpty()) {
            bidListView.getItems().addAll(bidDAO.getBidHistoryDisplay(item.getId()));
        }
        if (bidListView.getItems().isEmpty()) {
            bidListView.getItems().add("(Chưa có ai đặt giá)");
        }

        // --- Giá hiện tại ---
        Label lblCurrentPrice = new Label(
                "💰 Giá hiện tại: " + FMT.format(auction.getCurrentPrice()) + " $"
        );
        lblCurrentPrice.setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Đăng ký Observer realtime
        BidderAuctionObserver observer = new BidderAuctionObserver(bidListView, lblCurrentPrice);
        auction.addObserver(observer);

        // --- Số dư ---
        Label lblBal = new Label();
        lblBal.setStyle("-fx-text-fill: #444;");
        Runnable refreshBal = () -> lblBal.setText(
                "Số dư khả dụng: " + FMT.format(currentBidder.getAvailableBalance()) + " $   "
                        + "| Đóng băng: " + FMT.format(currentBidder.getFrozenBalance()) + " $"
        );
        refreshBal.run();

        Timeline balTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            refreshBal.run();
            // Cập nhật info text
            txtInfo.setText(auction.getInfo());
        }));
        balTimer.setCycleCount(Animation.INDEFINITE);
        balTimer.play();

        // --- Input ---
        TextField txtInputBid = new TextField();
        txtInputBid.setPromptText("Nhập giá của bạn (tối thiểu "
                + FMT.format(auction.getCurrentPrice() + item.getMinIncrement()) + " $)...");

        Button btnBid = new Button("✔  XÁC NHẬN ĐẶT GIÁ");
        btnBid.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        btnBid.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnBid, Priority.ALWAYS);

        btnBid.setOnAction(e -> {
            processBidLogic(item, auction, txtInputBid.getText(), lblCurrentPrice);
            txtInputBid.clear();
            txtInfo.setText(auction.getInfo());
            // Update prompt
            txtInputBid.setPromptText("Tối thiểu " + FMT.format(auction.getCurrentPrice() + item.getMinIncrement()) + " $...");
        });
        txtInputBid.setOnAction(e -> btnBid.fire());

        HBox inputRow = new HBox(8, txtInputBid, btnBid);
        HBox.setHgrow(txtInputBid, Priority.ALWAYS);

        stage.setOnCloseRequest(e -> {
            balTimer.stop();
            auction.removeObserver(observer);
        });

        layout.getChildren().addAll(
                lblTitle, txtInfo,
                lblHistory, bidListView,
                lblCurrentPrice, lblBal,
                inputRow
        );

        stage.setScene(new Scene(layout, 460, 570));
        stage.show();
    }

    // ===== Logic đặt giá dùng chung =====
    private void processBidLogic(Item item, Auction auction, String amountStr, Label lblCurrentPrice) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập số tiền!");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập một số hợp lệ!");
            return;
        }

        if (amount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt phải lớn hơn 0!"); return;
        }
        if (amount > 10_000_000_000.0) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt vượt mức cho phép!"); return;
        }
        if (amount > currentBidder.getAvailableBalance()) {
            showAlert(Alert.AlertType.ERROR, "Số dư không đủ",
                    "Bạn chỉ có " + FMT.format(currentBidder.getAvailableBalance()) + " $ khả dụng!"); return;
        }

        try {
            BidTransaction newBid = new BidTransaction(currentBidder, amount);
            auction.placeBid(newBid);
            bidDAO.saveBid(newBid, item.getId());

            updateBalanceUI();
            table.refresh();

            // Nếu đặt từ cửa sổ đầy đủ, Observer tự cập nhật label
            // Nếu đặt từ quick-bid dialog, cập nhật thủ công
            if (lblCurrentPrice != null) {
                lblCurrentPrice.setText("💰 Giá hiện tại: " + FMT.format(auction.getCurrentPrice()) + " $");
            }

            showAlert(Alert.AlertType.INFORMATION, "Đặt giá thành công!",
                    "✅ Bạn đã đặt " + FMT.format(amount) + " $ cho sản phẩm '" + item.getName() + "'\n"
                            + "Số dư khả dụng còn: " + FMT.format(currentBidder.getAvailableBalance()) + " $");

        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Không thể đặt giá", e.getMessage());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", e.getMessage());
        }
    }

    private String formatBid(BidTransaction bid) {
        return String.format("💰 %s: %s $ — [%s]",
                bid.getBidderName(), FMT.format(bid.getAmount()), bid.getTime());
    }

    private void updateBalanceUI() {
        if (currentBidder != null)
            lblBalance.setText(FMT.format(currentBidder.getAvailableBalance()) + " $");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}