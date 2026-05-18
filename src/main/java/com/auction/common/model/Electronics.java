package com.auction.common.model;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths;

    public Electronics(Seller seller, String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, double minIncrement, int warrantyMonths) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Electronics] " + name + " - Bảo hành: " + warrantyMonths + " tháng - Giá hiện tại: $" + getStartingPrice());
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
    public String getType() { return "Electronics"; }
}