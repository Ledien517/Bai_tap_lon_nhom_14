package com.auction.common.model;

public class Seller extends User {

    // Constructor chỉ cần 2 tham số, Role được truyền ngầm định cho lớp cha
    public Seller(String username, String password) {
        super(username, password, Role.SELLER);
    }

    // Hàm nhận tiền khi phiên đấu giá kết thúc thành công
    public synchronized void receiveBalance(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nhận không hợp lệ!");
        }
        // Ở đây em có thể tự định nghĩa thêm thuộc tính balance cho Seller nếu muốn,
        // Tạm thời chúng ta in ra log để theo dõi.
        System.out.println("[Hệ thống] Seller " + getUsername() + " đã nhận thanh toán: " + amount + " $");
    }
}