package com.auction.common.model;

/**
 * Bidder kế thừa User (đã Serializable).
 * Quản lý số dư khả dụng và số dư đóng băng.
 *
 * FIX SO VỚI BẢN CŨ:
 * - Thêm serialVersionUID (cần thiết vì User là Serializable)
 * - Thêm getFrozenBalance() public (BidderController cần để hiển thị)
 */
public class Bidder extends User {
    private static final long serialVersionUID = 2L;

    private double availableBalance = 0;
    private double frozenBalance = 0;

    public Bidder(String username, String password, Role role) {
        super(username, password, Role.BIDDER);
    }

    public double getAvailableBalance() { return availableBalance; }
    public double getFrozenBalance()    { return frozenBalance; }

    public synchronized void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0!");
        this.availableBalance += amount;
    }

    public synchronized void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0!");
        if (amount > availableBalance) throw new IllegalArgumentException(
            String.format("Số tiền rút vượt quá số dư khả dụng (%,.2f $)!", availableBalance));
        this.availableBalance -= amount;
    }

    public synchronized void freezeMoney(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền đóng băng không hợp lệ!");
        if (amount > availableBalance) throw new IllegalArgumentException("Số dư không đủ để đóng băng!");
        this.frozenBalance += amount;
        this.availableBalance -= amount;
    }

    public synchronized void releaseMoney(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Số tiền hoàn trả không hợp lệ!");
        this.frozenBalance -= amount;
        this.availableBalance += amount;
    }

    public synchronized void paid(double amount) {
        if (amount > 0 && amount <= frozenBalance) {
            this.frozenBalance -= amount;
        }
    }
}
