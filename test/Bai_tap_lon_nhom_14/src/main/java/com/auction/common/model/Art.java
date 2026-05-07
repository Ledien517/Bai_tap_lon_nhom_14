package com.auction.common.model;

import java.time.LocalDateTime;

public class Art extends Item {
    private String artist;

    /** Constructor tạo mới — validate strict */
    public Art(Seller seller, String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, double minIncrement, String artist) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, true);
        this.artist = artist;
    }

    /** Constructor load từ file — bỏ qua validate thời gian */
    public Art(Seller seller, String id, String name, String description, double startingPrice,
               LocalDateTime startTime, LocalDateTime endTime, double minIncrement, String artist,
               boolean validate) {
        super(seller, id, name, description, startingPrice, startTime, endTime, minIncrement, validate);
        this.artist = artist;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Art] " + name + " - Họa sĩ: " + artist + " - Giá: " + currentHighestBid);
    }

    public String getArtist()         { return artist; }
    public void setArtist(String a)   { this.artist = a; }

    @Override
    public String getType() { return "Art"; }
}