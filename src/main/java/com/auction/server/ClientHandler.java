package com.auction.server;

import com.auction.common.model.*;
import com.auction.common.protocol.Request;
import com.auction.common.protocol.Response;
import com.auction.dao.UserDAO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private boolean running = true;
    private User currentUser; // Quản lý session người dùng hiện tại

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Khởi tạo ObjectOutputStream trước ObjectInputStream để tránh deadlock
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Đăng ký với bộ quản lý để có thể nhận realtime broadcast
            AuctionManager.getInstance().addClient(this);

            while (running) {
                try {
                    Request req = (Request) in.readObject();
                    if (req == null) {
                        break;
                    }
                    handleRequest(req);
                } catch (ClassNotFoundException e) {
                    System.err.println("[ClientHandler Lỗi] Định dạng gói tin không hợp lệ: " + e.getMessage());
                } catch (IOException e) {
                    // Client ngắt kết nối
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler Lỗi] Lỗi kết nối: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    // Xử lý gói tin Request từ Client gửi lên
    private void handleRequest(Request req) {
        String type = req.getType();
        Object data = req.getData();

        try {
            switch (type) {
                case "LOGIN":
                    handleLogin(data);
                    break;

                case "REGISTER":
                    handleRegister(data);
                    break;

                case "GET_ITEMS":
                    handleGetItems();
                    break;

                case "PLACE_BID":
                    handlePlaceBid(data);
                    break;

                case "REGISTER_ITEM":
                    handleRegisterItem(data);
                    break;

                case "SEARCH_ITEMS":
                    handleSearchItems(data);
                    break;

                case "GET_MY_BIDS":
                    handleGetMyBids(data);
                    break;

                case "REGISTER_AUTO_BID":
                    handleRegisterAutoBid(data);
                    break;

                case "GET_USER":
                    handleGetUser(data);
                    break;

                case "UPDATE_USER":
                    handleUpdateUser(data);
                    break;

                case "DELETE_ITEM":
                    handleDeleteItem(data);
                    break;

                case "GET_ALL_USERS":
                    handleGetAllUsers();
                    break;

                case "BAN_USER":
                    handleBanUser(data);
                    break;

                case "UNBAN_USER":
                    handleUnbanUser(data);
                    break;

                default:
                    sendResponse(new Response("ERROR", "Yêu cầu không được hỗ trợ: " + type, null));
                    break;
            }
        } catch (Exception e) {
            System.err.println("[ClientHandler Lỗi] Xử lý Request '" + type + "' thất bại: " + e.getMessage());
            try {
                sendResponse(new Response("ERROR", "Lỗi Server: " + e.getMessage(), null));
            } catch (IOException ioException) {
                // Ignore
            }
        }
    }

    private void handleLogin(Object data) throws IOException {
        String[] credentials = (String[]) data;
        String username = credentials[0];
        String password = credentials[1];

        User user = UserDAO.validateUser(username, password);
        if (user != null) {
            if ("BANNED".equalsIgnoreCase(user.getStatus())) {
                sendResponse(new Response("ERROR", "Tài khoản của bạn đã bị khóa (Banned)!", null));
                System.out.println("[ClientHandler] Banned user '" + username + "' cố gắng đăng nhập nhưng đã bị chặn.");
                return;
            }
            this.currentUser = user; // Lưu session
            sendResponse(new Response("SUCCESS", "Đăng nhập thành công!", user));
            System.out.println("[ClientHandler] User '" + username + "' đã đăng nhập.");
        } else {
            sendResponse(new Response("ERROR", "Sai tài khoản hoặc mật khẩu!", null));
        }
    }

    private void handleRegister(Object data) throws IOException {
        User user = (User) data;
        if (UserDAO.isUserExists(user.getUsername())) {
            sendResponse(new Response("ERROR", "Tài khoản đã tồn tại trên hệ thống!", null));
        } else {
            UserDAO.saveUser(user);
            sendResponse(new Response("SUCCESS", "Đăng ký tài khoản thành công!", null));
            System.out.println("[ClientHandler] Đăng ký thành công user: " + user.getUsername());
        }
    }

    private void handleGetItems() throws IOException {
        List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();
        sendResponse(new Response("SUCCESS", "Lấy danh sách đấu giá thành công!", auctions));
    }

    private void handlePlaceBid(Object data) throws IOException {
        Object[] payload = (Object[]) data;
        String itemId = (String) payload[0];
        BidTransaction bid = (BidTransaction) payload[1];

        try {
            // Gọi AuctionManager để xử lý đặt giá đồng bộ và ghi file
            AuctionManager.getInstance().placeBid(itemId, bid);
            sendResponse(new Response("SUCCESS", "Đặt giá đấu thành công!", null));
        } catch (Exception e) {
            sendResponse(new Response("ERROR", e.getMessage(), null));
        }
    }

    private void handleRegisterItem(Object data) throws IOException {
        if (currentUser == null || currentUser.getRole() != Role.SELLER) {
            sendResponse(new Response("ERROR", "Lỗi phân quyền: Chỉ Người bán mới được tạo sản phẩm đấu giá!", null));
            return;
        }
        
        Item item = (Item) data;
        try {
            AuctionManager.getInstance().registerNewItem(item);
            sendResponse(new Response("SUCCESS", "Đăng ký đấu giá sản phẩm thành công!", item));
            System.out.println("[ClientHandler] Sản phẩm mới được tạo: " + item.getName());
        } catch (Exception e) {
            sendResponse(new Response("ERROR", e.getMessage(), null));
        }
    }

    private void handleSearchItems(Object data) throws IOException {
        String keyword = (String) data;
        List<Auction> results = AuctionManager.getInstance().searchAuctions(keyword);
        sendResponse(new Response("SUCCESS", "Đã tìm thấy kết quả.", results));
    }

    private void handleGetMyBids(Object data) throws IOException {
        String username = (String) data;
        List<Auction> results = AuctionManager.getInstance().getAuctionsByBidder(username);
        sendResponse(new Response("SUCCESS", "Thành công.", results));
    }

    private void handleRegisterAutoBid(Object data) throws IOException {
        if (currentUser == null || currentUser.getRole() != Role.BIDDER) {
            sendResponse(new Response("ERROR", "Chỉ Người mua mới được sử dụng tính năng này!", null));
            return;
        }
        AutoBid autoBid = (AutoBid) data;
        try {
            AuctionManager.getInstance().registerAutoBid(autoBid.getItemId(), autoBid);
            sendResponse(new Response("SUCCESS", "Đăng ký đấu giá tự động thành công!", null));
        } catch (Exception e) {
            sendResponse(new Response("ERROR", e.getMessage(), null));
        }
    }

    private void handleGetUser(Object data) throws IOException {
        String username = (String) data;
        User user = UserDAO.getUser(username);
        if (user != null) {
            sendResponse(new Response("SUCCESS", "Lấy thông tin tài khoản thành công!", user));
        } else {
            sendResponse(new Response("ERROR", "Không tìm thấy người dùng!", null));
        }
    }

    private void handleUpdateUser(Object data) throws IOException {
        User user = (User) data;
        UserDAO.saveUser(user);
        sendResponse(new Response("SUCCESS", "Cập nhật tài khoản thành công!", user));
        System.out.println("[ClientHandler] Đã cập nhật thông tin user: " + user.getUsername());
    }

    private void handleDeleteItem(Object data) throws IOException {
        if (currentUser == null || currentUser.getRole() != Role.SELLER) {
            sendResponse(new Response("ERROR", "Lỗi phân quyền: Chỉ Người bán mới được xóa sản phẩm!", null));
            return;
        }
        String itemId = (String) data;
        try {
            AuctionManager.getInstance().deleteItem(itemId, currentUser.getUsername());
            sendResponse(new Response("SUCCESS", "Đã xóa phiên đấu giá thành công!", null));
        } catch (Exception e) {
            sendResponse(new Response("ERROR", e.getMessage(), null));
        }
    }

    private void handleGetAllUsers() throws IOException {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            sendResponse(new Response("ERROR", "Lỗi phân quyền: Chỉ Quản trị viên mới được xem danh sách người dùng!", null));
            return;
        }
        List<User> users = UserDAO.getAllUsers();
        sendResponse(new Response("SUCCESS", "Lấy danh sách người dùng thành công!", users));
    }

    private void handleBanUser(Object data) throws IOException {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            sendResponse(new Response("ERROR", "Lỗi phân quyền: Chỉ Quản trị viên mới được ban người dùng!", null));
            return;
        }
        String usernameToBan = (String) data;
        if ("superadmin".equalsIgnoreCase(usernameToBan.trim())) {
            sendResponse(new Response("ERROR", "Không thể ban tài khoản Super Admin!", null));
            return;
        }
        UserDAO.banUser(usernameToBan);
        sendResponse(new Response("SUCCESS", "Đã khóa tài khoản thành công!", usernameToBan));
        System.out.println("[ClientHandler] Admin '" + currentUser.getUsername() + "' đã khóa người dùng '" + usernameToBan + "'.");
    }

    private void handleUnbanUser(Object data) throws IOException {
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            sendResponse(new Response("ERROR", "Lỗi phân quyền: Chỉ Quản trị viên mới được mở khóa người dùng!", null));
            return;
        }
        String usernameToUnban = (String) data;
        UserDAO.unbanUser(usernameToUnban);
        sendResponse(new Response("SUCCESS", "Đã mở khóa tài khoản thành công!", usernameToUnban));
        System.out.println("[ClientHandler] Admin '" + currentUser.getUsername() + "' đã mở khóa người dùng '" + usernameToUnban + "'.");
    }

    // Gửi response đơn lẻ về cho client này (Thread-safe)
    public synchronized void sendResponse(Response res) throws IOException {
        if (out != null) {
            out.writeObject(res);
            out.flush();
            out.reset(); // Rất quan trọng! Xóa bộ nhớ đệm Object để đảm bảo khi thuộc tính Object thay đổi, Client sẽ nhận được Object mới chứ không phải cache cũ.
        }
    }

    private void closeConnection() {
        running = false;
        AuctionManager.getInstance().removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("[ClientHandler] Đã ngắt kết nối an toàn với Client.");
        } catch (IOException e) {
            System.err.println("[ClientHandler Lỗi] Đóng kết nối thất bại: " + e.getMessage());
        }
    }
}
