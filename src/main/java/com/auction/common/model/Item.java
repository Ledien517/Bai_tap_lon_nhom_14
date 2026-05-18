package com.auction.common.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Implement Serializable là BẮT BUỘC để có thể gửi Object này qua mạng (Socket/ObjectOutputStream)
public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    protected Seller seller;
    protected String id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected final double minIncrement;

    // --- BỒI THÊM: Các thuộc tính phục vụ lưu trữ file JSON và cập nhật giao diện của Controller ---
    protected double currentHighestBid;
    protected List<BidTransaction> bidList = new ArrayList<>();

    public Item(Seller seller, String id, String name, String description, double startingPrice,
        LocalDateTime startTime, LocalDateTime endTime, double minIncrement) {
        this.seller = seller;
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = minIncrement;
        this.currentHighestBid = startingPrice; // Mới khởi tạo, giá cao nhất bằng giá sàn
        validDateTime();
    }

    private void validDateTime() {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Không được bỏ trống thời gian!");
        }
        Duration duration = Duration.between(startTime, endTime);
        long minutes = duration.toMinutes();
        long days = duration.toDays();

        // Cho phép startTime trễ tối đa 5 phút để bù trừ thời gian điền Form UI
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Thời gian không được bắt đầu ở quá khứ!");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        }
        if (minutes < 3) {
            throw new IllegalArgumentException("Phiên đấu giá quá ngắn (tối thiểu 3 phút)!");
        }
        if (days > 30) {
            throw new IllegalArgumentException("Phiên đấu giá quá dài (Tối đa 30 ngày)!");
        }
    }

    public String getDetails() {
        return String.format("Mã SP: %s\nTên: %s\nMô tả: %s\nGiá khởi điểm: %.2f $\nKết thúc lúc: %s",
            id, name, description, startingPrice, endTime.toString());
    }

    // Các phương thức trừu tượng BẮT BUỘC các lớp con phải ghi đè (Polymorphism)
    public abstract void displayItemDetails();
    public abstract String getType();

    // ================= GETTERS VÀ SETTERS =================
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getDescription() { return description; }

    public Seller getSeller() { return seller; }

    public double getStartingPrice() { return startingPrice; }

    // SỬA LỖI: Thêm hàm setStartingPrice để Controller cập nhật giá mới từ file JSON
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getMinIncrement() { return minIncrement; }

    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    // SỬA LỖI: Thêm hàm setCurrentHighestBid phục vụ Controller dòng 273
    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    // SỬA LỖI: Thêm hàm getBidList cho Item phục vụ việc đọc dữ liệu từ JsonItemDAO
    public List<BidTransaction> getBidList() {
        if (this.bidList == null) {
            this.bidList = new ArrayList<>();
        }
        return this.bidList;
    }

    public void setBidList(List<BidTransaction> bidList) {
        this.bidList = bidList;
    }

    @Override
    public String toString() {
        return "Mã SP: " + id + " | Tên: " + name + " | Loại: " + getType();
    }
}