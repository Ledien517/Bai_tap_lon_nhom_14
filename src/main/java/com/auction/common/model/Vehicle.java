package com.auction.common.model;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String brand;
    public Vehicle(Seller seller, String id, String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, double minIncrement, String brand) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement);
        this.brand = brand;
    }
    @Override
    public void displayItemDetails() {
        System.out.println("[Vehicle] " + getName() + " - Hãng: " + brand + " - Giá khởi điểm: $" + getStartingPrice());
    }
    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }
    @Override
    public String getType() {
        return "Vehicle";
    }
}