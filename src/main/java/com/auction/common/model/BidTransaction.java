package com.auction.common.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction {
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime bidTime;
    private final String formattedTime; // Sửa tên biến thành camelCase

    public BidTransaction(Bidder bidder, double amount) {
        if (bidder == null) {
            throw new IllegalArgumentException("Không được bỏ trống Bidder!");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền đặt không hợp lệ!");
        }
        if (amount > bidder.getAvailableBalance()) {
            throw new IllegalArgumentException("Số dư khả dụng không đủ!");
        }

        this.bidder = bidder;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        this.formattedTime = bidTime.format(formatter);
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public double getAmount() {
        return amount;
    }

    public String getBidderName() {
        return bidder.getUsername();
    }

    public Bidder getBidder() {
        return bidder;
    }

    // Đổi tên cho có ý nghĩa hành động (Clean Code)
    protected void freezeAmount() {
        bidder.freezeMoney(this.amount);
    }

    protected void refundAmount() {
        bidder.releaseMoney(this.amount);
    }
}