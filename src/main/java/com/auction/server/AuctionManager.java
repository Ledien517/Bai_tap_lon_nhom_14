package com.auction.server;

import com.auction.common.model.*;
import com.auction.common.protocol.Response;
import com.auction.dao.MySqlItemDAO;
import com.auction.dao.UserDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionManager {
    private static AuctionManager instance;

    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final Map<String, Auction> activeAuctions = new ConcurrentHashMap<>();
    private final MySqlItemDAO itemDAO = new MySqlItemDAO();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private AuctionManager() {
        loadAuctionsFromDAO();
        startAuctionMonitor();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // Nạp toàn bộ sản phẩm từ DAO lên bộ nhớ lúc khởi động Server
    private void loadAuctionsFromDAO() {
        try {
            List<Item> items = itemDAO.getAllItems();
            for (Item item : items) {
                // Tạo một Auction cho từng sản phẩm
                Auction auction = new Auction(item.getSeller(), item);
                activeAuctions.put(item.getId(), auction);
            }
            System.out.println("[AuctionManager] Đã nạp thành công " + activeAuctions.size() + " phiên đấu giá.");
        } catch (Exception e) {
            System.err.println("[AuctionManager Lỗi] Không thể nạp sản phẩm: " + e.getMessage());
        }
    }

    // Trả về danh sách tất cả các phiên đấu giá
    public List<Auction> getAllAuctions() {
        return new ArrayList<>(activeAuctions.values());
    }

    // Lấy thông tin phiên đấu giá theo mã sản phẩm
    public Auction getAuctionByItemId(String itemId) {
        return activeAuctions.get(itemId);
    }

    public List<Auction> searchAuctions(String keyword) {
        List<Auction> result = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllAuctions();
        }
        String lowerKeyword = keyword.toLowerCase();
        for (Auction auction : activeAuctions.values()) {
            Item item = auction.getItem();
            if (item.getName().toLowerCase().contains(lowerKeyword) || 
                item.getDescription().toLowerCase().contains(lowerKeyword)) {
                result.add(auction);
            }
        }
        return result;
    }

    public List<Auction> getAuctionsByBidder(String username) {
        List<Auction> result = new ArrayList<>();
        for (Auction auction : activeAuctions.values()) {
            boolean participated = false;
            for (BidTransaction bid : auction.getBidList()) {
                if (bid.getBidder().getUsername().equals(username)) {
                    participated = true;
                    break;
                }
            }
            if (participated) {
                result.add(auction);
            }
        }
        return result;
    }

    public synchronized void registerAutoBid(String itemId, AutoBid autoBid) {
        Auction auction = activeAuctions.get(itemId);
        if (auction == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá!");
        }
        // Đăng ký auto-bid vào phiên đấu giá, có thể kích hoạt các lượt trả giá tự động
        auction.registerAutoBid(autoBid);
        
        // Cập nhật lại file JSON vì giá có thể đã bị thay đổi bởi AutoBidding
        itemDAO.updateItem(auction.getItem());
        
        // Cập nhật số dư người thắng hiện tại và người bị vượt giá nếu có
        List<BidTransaction> list = auction.getBidList();
        if (!list.isEmpty()) {
            BidTransaction lastBid = list.get(list.size() - 1);
            UserDAO.saveUser(lastBid.getBidder());
            if (list.size() > 1) {
                BidTransaction previousBid = list.get(list.size() - 2);
                UserDAO.saveUser(previousBid.getBidder());
            }
        }
        // Cập nhật số dư người đăng ký AutoBid nếu có thay đổi
        UserDAO.saveUser(autoBid.getBidder());

        // Broadcast thay đổi giá
        broadcast("BID_UPDATE", "Cập nhật giá qua đấu giá tự động cho " + itemId, auction);
    }

    // Đăng ký thêm sản phẩm mới đấu giá (khi Seller gửi yêu cầu)
    public synchronized void registerNewItem(Item item) {
        itemDAO.saveItem(item);
        Auction auction = new Auction(item.getSeller(), item);
        activeAuctions.put(item.getId(), auction);
        
        // Broadcast thông báo có sản phẩm mới tới tất cả Client để cập nhật bảng
        broadcast("NEW_ITEM", "Có sản phẩm mới: " + item.getName(), item);
    }

    // Đặt giá đấu
    public synchronized void placeBid(String itemId, BidTransaction bid) {
        Auction auction = activeAuctions.get(itemId);
        if (auction == null) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá cho sản phẩm này!");
        }

        // Thực hiện đặt giá (synchronized bên trong Auction.java)
        auction.placeBid(bid);

        // Lưu thông tin đấu giá mới vào file JSON thông qua DAO
        Item item = auction.getItem();
        // Không gọi item.getBidList().add(bid) ở đây nữa vì Auction.placeBid(bid) đã thêm trực tiếp vào item rồi.
        itemDAO.updateItem(item);

        // Lưu thông tin số dư khả dụng/đóng băng mới của Bidder vào users.dat
        UserDAO.saveUser(bid.getBidder());
        
        // Nếu có bidder cũ bị trả lại tiền đóng băng, ta cũng lưu lại thông tin bidder cũ đó
        List<BidTransaction> list = auction.getBidList();
        if (list.size() > 1) {
            BidTransaction previousBid = list.get(list.size() - 2);
            UserDAO.saveUser(previousBid.getBidder());
        }

        System.out.println("[AuctionManager] Đặt giá thành công cho SP: " + itemId + " bởi " + bid.getBidderName());

        // Broadcast thông báo giá mới tới tất cả Client đang xem
        broadcast("BID_UPDATE", "Giá mới cho sản phẩm " + itemId, auction);
    }

    // Quản lý kết nối Client
    public void addClient(ClientHandler handler) {
        connectedClients.add(handler);
        System.out.println("[AuctionManager] Đã thêm client handler. Tổng số: " + connectedClients.size());
    }

    public void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
        System.out.println("[AuctionManager] Đã xóa client handler. Tổng số: " + connectedClients.size());
    }

    // Gửi dữ liệu tới tất cả client đang kết nối (Realtime Broadcast)
    public void broadcast(String type, String message, Object data) {
        Response response = new Response("SUCCESS", message, new Object[]{type, data});
        for (ClientHandler client : connectedClients) {
            try {
                client.sendResponse(response);
            } catch (Exception e) {
                // Nếu client ngắt kết nối đột ngột, ta sẽ gỡ bỏ client này
                removeClient(client);
            }
        }
    }

    // Chạy giám sát ngầm để tự động kết thúc phiên đấu giá khi hết giờ
    private void startAuctionMonitor() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Auction auction : activeAuctions.values()) {
                    String oldStatus = auction.getStatus();
                    String newStatus = auction.updateStatus();

                    // Nếu đấu giá đã kết thúc và chưa được xử lý người thắng cuộc
                    // Kiểm tra trạng thái FINISHED và thuộc tính winProcessed của auction
                    if ("FINISHED".equals(newStatus) && !auction.isWinProcessed()) {
                        synchronized (auction) {
                            try {
                                auction.processWinner();
                                
                                // Cập nhật lại trạng thái sản phẩm vào file JSON
                                itemDAO.updateItem(auction.getItem());

                                // Lưu thông tin số dư mới của Seller và Bidder thắng cuộc vào file users.dat
                                List<BidTransaction> bids = auction.getBidList();
                                if (!bids.isEmpty()) {
                                    BidTransaction winBid = bids.get(bids.size() - 1);
                                    UserDAO.saveUser(auction.getItem().getSeller());
                                    UserDAO.saveUser(winBid.getBidder());
                                    System.out.println("[AuctionMonitor] Phiên " + auction.getItem().getId() + " kết thúc. Người thắng: " + winBid.getBidderName());
                                } else {
                                    System.out.println("[AuctionMonitor] Phiên " + auction.getItem().getId() + " kết thúc. Không có ai đấu giá.");
                                }

                                // Broadcast thông tin kết thúc phiên tới toàn bộ client
                                broadcast("AUCTION_FINISHED", "Phiên đấu giá " + auction.getItem().getName() + " đã kết thúc!", auction);
                            } catch (Exception e) {
                                System.err.println("[AuctionMonitor Lỗi] Xử lý kết thúc phiên thất bại: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[AuctionMonitor Lỗi] Lỗi luồng giám sát: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS); // Quét mỗi giây một lần để đảm bảo chính xác thời gian thực
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
