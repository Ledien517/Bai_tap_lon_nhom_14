package com.auction.client;

import com.auction.common.factory.ItemFactory;
import com.auction.common.entity.Item;
import com.auction.dao.ItemDAO;
import com.auction.dao.JsonItemDAO;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class MainApp extends Application {

    private final ItemDAO itemDAO = new JsonItemDAO();
    private TableView<Item> table = new TableView<>();
    private ObservableList<Item> data;

    @Override
    public void start(Stage primaryStage) {
        // 1. Cấu hình các cột cho bảng
        TableColumn<Item, String> idCol = new TableColumn<>("Mã ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Item, String> nameCol = new TableColumn<>("Tên sản phẩm");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Item, Double> priceCol = new TableColumn<>("Giá khởi điểm");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        table.getColumns().addAll(idCol, nameCol, priceCol);

        // Đổ dữ liệu từ DAO vào bảng
        data = FXCollections.observableArrayList(itemDAO.getAllItems());
        table.setItems(data);

        // 2. Tạo Form nhập liệu (Phía dưới bảng)
        GridPane form = new GridPane();
        form.setPadding(new Insets(10));
        form.setHgap(10);
        form.setVgap(10);

        TextField txtId = new TextField(); txtId.setPromptText("Nhập ID (VD: A02)");
        TextField txtName = new TextField(); txtName.setPromptText("Tên sản phẩm");
        TextField txtPrice = new TextField(); txtPrice.setPromptText("Giá");

        // ComboBox để chọn loại sản phẩm (Art, Electronics, Vehicle)
        ComboBox<String> cbType = new ComboBox<>();
        cbType.getItems().addAll("Art", "Electronics", "Vehicle");
        cbType.setValue("Art"); // Mặc định là Art

        Button btnAdd = new Button("Thêm sản phẩm");
        btnAdd.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");

        // Thêm các thành phần vào Form (Cột, Hàng)
        form.add(new Label("Loại:"), 0, 0); form.add(cbType, 1, 0);
        form.add(new Label("Mã ID:"), 0, 1); form.add(txtId, 1, 1);
        form.add(new Label("Tên:"), 0, 2); form.add(txtName, 1, 2);
        form.add(new Label("Giá:"), 0, 3); form.add(txtPrice, 1, 3);
        form.add(btnAdd, 1, 4);

        // 3. Xử lý sự kiện khi nhấn nút "Thêm"
        btnAdd.setOnAction(e -> {
            try {
                String type = cbType.getValue();
                String id = txtId.getText();
                String name = txtName.getText();
                double price = Double.parseDouble(txtPrice.getText());

                // Sử dụng Factory để tạo đối tượng (giả sử Factory của bạn có phương thức này)
                // Nếu chưa có các tham số phụ (như artist/brand), tôi tạm để mặc định để test khung trước
                Item newItem = ItemFactory.createItem(type, id, name, "Mô tả sản phẩm", price, LocalDateTime.now(), LocalDateTime.now().plusDays(7), "Default Value");                // Lưu vào file JSON thông qua DAO
                itemDAO.saveItem(newItem);

                // Cập nhật hiển thị trên bảng ngay lập tức
                data.add(newItem);

                // Xóa trống form sau khi thêm thành công
                txtId.clear(); txtName.clear(); txtPrice.clear();

            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Vui lòng kiểm tra lại thông tin nhập vào!");
                alert.show();
            }
        });

        // 4. Sắp xếp bố cục tổng thể
        VBox root = new VBox(10, table, form);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 700, 600);
        primaryStage.setTitle("Quản lý Đấu giá - Nhóm 14");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}