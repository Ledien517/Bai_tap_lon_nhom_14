import java.util.ArrayList;
class AuctionSystem {
        private ArrayList<User> userList = new ArrayList<>();

        // Chức năng Đăng ký
        public void register(User user) {
            userList.add(user);
            System.out.println("Đăng ký thành công tài khoản: " + user.getUsername());
        }

        // Chức năng Đăng nhập đơn giản
        public void login(String username, String password) {
            for (User u : userList) {
                if (u.getUsername().equals(username)) {
                    // Trong thực tế cần kiểm tra password, ở đây mình làm đơn giản
                    System.out.print("Chào mừng " + username + "! ");
                    u.showRole();
                    return;
                }
            }
            System.out.println("Lỗi: Tài khoản không tồn tại!");
        }
    }
