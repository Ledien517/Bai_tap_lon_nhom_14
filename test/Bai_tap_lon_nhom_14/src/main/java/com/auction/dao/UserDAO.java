package com.auction.dao;

import com.auction.common.model.Role;
import com.auction.common.model.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    private static final String DATA_FILE = "users.dat";
    private static List<User> userList = new ArrayList<>();

    // Khối static này chạy 1 lần khi class được nạp
    static {
        loadUsersFromFile();
    }

    // Đọc dữ liệu từ file
    @SuppressWarnings("unchecked")
    private static void loadUsersFromFile() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                userList = (List<User>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Lỗi khi đọc file dữ liệu: " + e.getMessage());
            }
        } else {
            // Nếu file chưa tồn tại (chạy lần đầu), tạo tài khoản Admin mặc định và lưu lại
            userList.add(new User("admin", "admin123", Role.ADMIN));
            saveUsersToFile();
        }
    }

    // Ghi dữ liệu ra file
    private static void saveUsersToFile() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(userList);
        } catch (IOException e) {
            System.out.println("Lỗi khi ghi file dữ liệu: " + e.getMessage());
        }
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
        // Lưu lại vào file ngay sau khi thêm user mới
        saveUsersToFile();
    }
}
