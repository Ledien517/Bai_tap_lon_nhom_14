package com.auction.dao;

import com.auction.common.model.Role;
import com.auction.common.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    // "Database" lưu trữ tạm thời
    private static List<User> userList = new ArrayList<>();

    static {
        // Tạo sẵn một tài khoản Admin để test
        userList.add(new User("admin", "admin123", Role.ADMIN));
    }

    public static boolean isUserExists(String username) {
        for (User user : userList) {
            if (user.getUsername().equals(username)) return true;
        }
        return false;
    }

    public static User authenticate(String username, String password) {
        for (User user : userList) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public static void saveUser(User user) {
        userList.add(user);
    }
}