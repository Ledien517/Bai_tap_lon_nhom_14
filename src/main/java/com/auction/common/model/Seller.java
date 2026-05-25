package com.auction.common.model;

public class Seller extends User {

    private static final long serialVersionUID = 1L;

    // Constructor chỉ cần 2 tham số, Role được truyền ngầm định cho lớp cha
    public Seller(String username, String password) {
        super(username, password, Role.SELLER);
    }

    private double balance = 0.0;

    // Hàm nhận tiền khi phiên đấu giá kết thúc thành công
    public synchronized void receiveBalance(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhận không hợp lệ!");
        }
        this.balance += amount;
        System.out.println("[Hệ thống] Seller " + getUsername() + " đã nhận thanh toán: " + amount + " $. Số dư hiện tại: " + this.balance + " $");
    }

    public synchronized double getBalance() {
        return this.balance;
    }
}