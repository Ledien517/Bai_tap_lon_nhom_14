package com.auction.common.model;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String brand;

    /** Constructor tạo mới — validate strict */
    public Vehicle(Seller seller, String id, String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, double minIncrement, String brand) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, true);
        this.brand = brand;
    }

    /** Constructor load từ file — bỏ qua validate thời gian */
    public Vehicle(Seller seller, String id, String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, double minIncrement, String brand,
                   boolean validate) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, validate);
        this.brand = brand;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Vehicle] " + name + " - Hãng: " + brand + " - Giá: " + currentHighestBid);
    }

    public String getBrand()        { return brand; }
    public void setBrand(String b)  { this.brand = b; }

    @Override
    public String getType() { return "Vehicle"; }
}