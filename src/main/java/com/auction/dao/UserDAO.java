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
                // Đọc dữ liệu thành công
                userList = (List<User>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Lỗi khi đọc file (file hỏng): " + e.getMessage());
                // Nếu lỗi, khởi tạo danh sách rỗng để app không bị crash
                userList = new ArrayList<>();
            }
        } else {
            // TRƯỜNG HỢP RESET (FILE KHÔNG TỒN TẠI)
            System.out.println("Không tìm thấy file, đang khởi tạo dữ liệu mặc định...");

            // Đảm bảo danh sách sạch trước khi thêm Admin
            userList = new ArrayList<>();
            userList.add(new User("admin", "admin123", Role.ADMIN));

            // Lưu lại ngay để tạo file vật lý mới
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
