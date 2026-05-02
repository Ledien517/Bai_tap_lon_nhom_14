package com.auction.common.model;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String brand;
    public Vehicle(String id, String name, String description, double startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime, String brand) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.brand = brand;
    }
    @Override
    public void displayItemDetails() {
        System.out.println("[Vehicle] " + name + " - Hãng: " + brand + " - Giá hiện tại: $" + currentHighestBid);
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