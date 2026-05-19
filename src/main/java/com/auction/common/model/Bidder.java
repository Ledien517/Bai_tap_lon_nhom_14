package com.auction.common.model;

public class Bidder extends User {
    private double availableBalance = 0;
    private double frozenBalance = 0;

    // Loại bỏ tham số Role dư thừa
    public Bidder(String username, String password) {
        super(username, password, Role.BIDDER);
    }

    public synchronized double getAvailableBalance() {
        return availableBalance;
    }

    public synchronized double getFrozenBalance() {
        return frozenBalance;
    }

    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp vào không hợp lệ!");
        }
        // Thao tác trực tiếp với biến vì hàm đã được synchronized
        this.availableBalance += amount;
    }

    public synchronized void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút không hợp lệ!");
        }
        if (amount > availableBalance) {
            throw new IllegalArgumentException("Số tiền rút vượt quá số dư khả dụng!");
        }
        this.availableBalance -= amount;
    }

    // Đóng băng tiền khi đặt giá (Bid)
    public synchronized void freezeMoney(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền đóng băng không hợp lệ!");
        }
        if (amount > availableBalance) {
            throw new IllegalStateException("Không đủ số dư khả dụng để đóng băng!");
        }
        this.availableBalance -= amount;
        this.frozenBalance += amount;
    }

    // Hoàn tiền 1 có người trả giá cao hơn
    public synchronized void releaseMoney(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền hoàn trả không hợp lệ!");
        }
        if (amount > frozenBalance) {
            throw new IllegalStateException("Lỗi hệ thống: Số tiền hoàn lớn hơn số tiền đang đóng băng!");
        }
        this.frozenBalance -= amount;
        this.availableBalance += amount;
    }

    // Thanh toán khi thắng đấu giá
    public synchronized void paid(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ!");
        }
        if (amount > frozenBalance) {
            throw new IllegalStateException("Lỗi hệ thống: Số tiền thanh toán lớn hơn tiền đóng băng!");
        }
        // Chỉ cần trừ phần tiền đóng băng (tiền này coi như chuyển cho Seller)
        this.frozenBalance -= amount;
    }
}