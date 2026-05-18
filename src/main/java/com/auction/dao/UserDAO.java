package com.auction.dao;

import com.auction.common.model.User;
import java.io.*;
import java.util.HashMap;

public class UserDAO {
    private static final String FILE_PATH = "users.dat";

    // Đọc danh sách tài khoản từ file (Dùng cơ chế Serialization)
    @SuppressWarnings("unchecked")
    private static synchronized HashMap<String, User> loadUsers() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new HashMap<>(); // Nếu chưa có file, trả về một danh sách rỗng mới
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (HashMap<String, User>) ois.readObject();
        } catch (Exception e) {
            // Nếu file bị lỗi cấu trúc hoặc trống, tự động reset về map rỗng để tránh sập app
            return new HashMap<>();
        }
    }

    // Ghi danh sách tài khoản ngược lại vào file
    private static synchronized void saveUsers(HashMap<String, User> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("[Lỗi DAO] Không thể ghi dữ liệu người dùng: " + e.getMessage());
        }
    }

    // Kiểm tra tên đăng nhập đã tồn tại trong hệ thống chưa
    public static synchronized boolean isUserExists(String username) {
        if (username == null) return false;
        HashMap<String, User> users = loadUsers();
        return users.containsKey(username.toLowerCase());
    }

    // Lưu người dùng mới vào hệ thống (Chấp nhận cả đối tượng con là Bidder hoặc Seller nhờ Đa hình)
    public static synchronized void saveUser(User user) {
        if (user == null || user.getUsername() == null) return;
        HashMap<String, User> users = loadUsers();
        users.put(user.getUsername().toLowerCase(), user);
        saveUsers(users);
    }

    // Kiểm tra thông tin đăng nhập: Đúng mật khẩu thì trả về Object User, sai trả về null
    public static synchronized User validateUser(String username, String password) {
        if (username == null || password == null) return null;

        HashMap<String, User> users = loadUsers();
        User user = users.get(username.toLowerCase());

        if (user != null && user.getPassword().equals(password)) {
            return user; // Đăng nhập đúng, trả về toàn bộ thông tin đối tượng (gồm cả vai trò Role)
        }
        return null; // Sai mật khẩu hoặc không tồn tại tài khoản
    }
}