package com.auction.common.model;

import java.time.*;

public abstract class Item {
    protected Seller seller;
    protected String id;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentHighestBid;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected final double minIncrement;

    /**
     * Constructor dùng khi Seller TẠO MỚI sản phẩm — validate strict.
     */
    public Item(Seller seller, String id, String name, String description, double startingPrice,
                LocalDateTime startTime, LocalDateTime endTime, double minIncrement) {
        this(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, true);
    }

    /**
     * Constructor dùng khi LOAD TỪ FILE — bỏ qua validate thời gian.
     * Gson dùng reflection nên constructor này phải tồn tại qua subclass.
     */
    protected Item(Seller seller, String id, String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, double minIncrement, boolean validate) {
        this.seller = seller;
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minIncrement = minIncrement;
        if (validate) {
            validDateTime();
        }
    }

    public String getDetails() {
        return String.format(
                "Mã SP: %s\nTên: %s\nMô tả: %s\nGiá khởi điểm: %,.0f $\nBước giá tối thiểu: %,.0f $\nKết thúc lúc: %s",
                id, name, description, startingPrice, minIncrement, endTime.toString()
        );
    }

    public String getStatusDisplay() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) return "UPCOMING";
        if (now.isAfter(endTime))   return "FINISHED";
        return "ACTIVE";
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public abstract void displayItemDetails();
    public abstract String getType();

    // ===== Getters =====
    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }
    public String getName()             { return name; }
    public double getStartingPrice()    { return startingPrice; }
    public double getCurrentHighestBid(){ return currentHighestBid; }
    public void setCurrentHighestBid(double v) { this.currentHighestBid = v; }
    public double getMinIncrement()     { return minIncrement; }
    public Seller getSeller()           { return seller; }
    public void setSeller(Seller s)     { this.seller = s; }
    public String getDescription()      { return description; }
    public LocalDateTime getEndTime()   { return endTime; }
    public LocalDateTime getStartTime() { return startTime; }

    private void validDateTime() {
        if (startTime == null || endTime == null)
            throw new IllegalArgumentException("Không được bỏ trống thời gian!");
        Duration dur = Duration.between(startTime, endTime);
        if (startTime.isBefore(LocalDateTime.now().minusMinutes(5)))
            throw new IllegalArgumentException("Thời gian bắt đầu không được ở quá khứ!");
        if (endTime.isBefore(startTime))
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu!");
        if (dur.toMinutes() < 3)
            throw new IllegalArgumentException("Phiên đấu giá quá ngắn (tối thiểu 3 phút)!");
        if (dur.toDays() > 30)
            throw new IllegalArgumentException("Phiên đấu giá quá dài (tối đa 30 ngày)!");
    }

    @Override
    public String toString() {
        return "Mã SP: " + id + "\nTên: " + name + "\nMô tả: " + description;
    }
}