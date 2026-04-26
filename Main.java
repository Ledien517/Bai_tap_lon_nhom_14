public class Main {
    public static void main(String[] args) {
        AuctionSystem system = new AuctionSystem();

        // 1. Khởi tạo các loại người dùng (Polymorphism - Đa hình)
        User user1 = new Bidder("nguyen_van_a", "123");
        User user2 = new Seller("shop_thoi_trang", "456");
        User user3 = new Admin("admin_root", "789");

        // 2. Đăng ký vào hệ thống
        system.register(user1);
        system.register(user2);
        system.register(user3);

        System.out.println("--- Kiểm tra Đăng nhập ---");

        // 3. Thử đăng nhập các vai trò khác nhau
        system.login("nguyen_van_a", "123");
        system.login("shop_thoi_trang", "456");
        system.login("admin_root", "789");
    }
}
