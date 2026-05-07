package com.auction.common.model;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private int warrantyMonths;

    /** Constructor tạo mới — validate strict */
    public Electronics(Seller seller, String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, double minIncrement, int warrantyMonths) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, true);
        this.warrantyMonths = warrantyMonths;
    }

    /** Constructor load từ file — bỏ qua validate thời gian */
    public Electronics(Seller seller, String id, String name, String description, double startingPrice,
                       LocalDateTime startTime, LocalDateTime endTime, double minIncrement, int warrantyMonths,
                       boolean validate) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, validate);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Electronics] " + name + " - Bảo hành: " + warrantyMonths + " tháng");
    }

    public int getWarrantyMonths()           { return warrantyMonths; }
    public void setWarrantyMonths(int w)     { this.warrantyMonths = w; }

    @Override
    public String getType() { return "Electronics"; }
}