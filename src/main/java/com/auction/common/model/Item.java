package com.auction.common.model;

import java.time.LocalDateTime;

public abstract class Item {
    protected String id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentHighestBid;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public Item(String id, String name, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice; // Lúc mới tạo, giá cao nhất chính là giá khởi điểm
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Phương thức trừu tượng thể hiện tính Đa hình (Polymorphism)
    public abstract void displayItemDetails();

    // Thêm phương thức này: Các lớp con (Electronics, Art) BẮT BUỘC phải override
    // để trả về loại của nó (VD: return "Electronics";)
    public abstract String getType();

    // ================= GETTERS VÀ SETTERS =================
    // JavaFX cần các getter này để hiển thị dữ liệu lên bảng (Encapsulation)

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }

    // THÊM DÒNG NÀY: Fix lỗi cột giá khởi điểm bị trống
    public double getStartingPrice() { return startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }
}