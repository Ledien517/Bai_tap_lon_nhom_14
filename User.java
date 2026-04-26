public class User {
    // Lớp cha User
    private String username;
    private String password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    // Phương thức hiển thị chức năng (sẽ được ghi đè ở lớp con)
    public void showRole() {
        System.out.println("User chung");
    }
}
class Bidder extends User {
    public Bidder(String username, String password) {
        super(username, password);
    }

    @Override
    public void showRole() {
        System.out.println("Vai trò: Bidder - Chức năng: Tham gia đấu giá sản phẩm.");
    }
}
class Seller extends User {
    public Seller(String username, String password) {
        super(username, password);
    }

    @Override
    public void showRole() {
        System.out.println("Vai trò: Seller - Chức năng: Đăng sản phẩm đấu giá.");
    }
}

class Admin extends User {
    public Admin(String username, String password) {
        super(username, password);
    }

    @Override
    public void showRole() {
        System.out.println("Vai trò: Admin - Chức năng: Quản lý toàn bộ hệ thống.");
    }
}
