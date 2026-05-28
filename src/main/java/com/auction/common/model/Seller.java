package com.auction.common.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    private double balance = 0.0;

    // Constructor chỉ cần 2 tham số, Role được truyền ngầm định cho lớp cha
    public Seller(String username, String password) {
        super(username, password, Role.SELLER);
    }

    // Constructor có thêm tham số balance phục vụ đọc dữ liệu từ DB
    public Seller(String username, String password, double balance) {
        super(username, password, Role.SELLER);
        this.balance = balance;
    }

    public synchronized double getBalance() {
        return balance;
    }

    // Hàm nhận tiền khi phiên đấu giá kết thúc thành công
    public synchronized void receiveBalance(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhận không hợp lệ!");
        }
        this.balance += amount;
        System.out.println("[Hệ thống] Seller " + getUsername() + " đã nhận thanh toán: " + amount + " $. Số dư mới: " + this.balance + " $");
    }

    // Hàm rút tiền của Seller
    public synchronized void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0!");
        }
        if (amount > this.balance) {
            throw new IllegalArgumentException("Số dư không đủ để thực hiện rút tiền!");
        }
        this.balance -= amount;
        System.out.println("[Hệ thống] Seller " + getUsername() + " đã rút tiền: " + amount + " $. Số dư mới: " + this.balance + " $");
    }
}