package com.auction.common.model;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, int warrantyMonths) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Electronics] " + name + " - Bảo hành: " + warrantyMonths + " tháng - Giá hiện tại: $" + currentHighestBid);
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
    public String getType() { return "Electronics"; }
}