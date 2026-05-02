package com.auction.common.model;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artist;

    public Art(String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, String artist) {
        super(id, name, description, startingPrice, startTime, endTime);
        this.artist = artist;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Art] " + name + " - Họa sĩ: " + artist + " - Giá hiện tại: $" + currentHighestBid);
    }
    // Thêm 2 hàm này vào cuối class Art
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
    public String getType() { return "Art"; }
}