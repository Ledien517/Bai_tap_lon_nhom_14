package com.auction.common.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String username, String password) {
        super(username, password, Role.ADMIN);
    }
}
