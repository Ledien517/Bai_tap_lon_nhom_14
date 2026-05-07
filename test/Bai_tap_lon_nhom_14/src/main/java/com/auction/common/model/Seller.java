package com.auction.common.model;

public class Seller extends User {
    private static final long serialVersionUID = 3L;

    private double balance = 0.0;

    public Seller(String username, String password, Role role) {
        super(username, password, Role.SELLER);
    }

    public double getBalance() { return balance; }

    public synchronized void receiveBalance(double amount) {
        if (amount > 0) this.balance += amount;
    }
}
