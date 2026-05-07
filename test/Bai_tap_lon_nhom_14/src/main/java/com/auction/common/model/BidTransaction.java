package com.auction.common.model;
import java.time.*;
import java.time.format.DateTimeFormatter;
public class BidTransaction {
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime bidTime;
    private String Time;
    public BidTransaction(Bidder bidder, double amount){
        if (bidder == null){
            throw new IllegalArgumentException("Không được bỏ trống Bidder!");
        }
        if (amount > bidder.getAvailableBalance()){
            throw new IllegalArgumentException("Số dư khả dụng không đủ!");
        }
        else if (amount <= 0){
            throw new IllegalArgumentException("Số tiền đặt không hợp lệ!");
        }
        this.bidder = bidder;
        this.amount = amount;
        this.bidTime = LocalDateTime.now();
        DateTimeFormatter perfectTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        this.Time = bidTime.format(perfectTime);
    }
    public String getTime(){
        return Time;
    }
    public double getAmount(){
        return amount;
    }
    public String getBidderName(){
        return bidder.getUsername();
    }
    public Bidder getBidder(){
        return bidder;
    }
    void frozen(){
        bidder.freezeMoney(this.amount);
    }
    void refund(){
        bidder.releaseMoney(this.amount);
    }
}

