package com.auction.common.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.auction.dao.UserDAO;

public class Auction {
    private final Seller seller;
    private final Item item;
    private final double startingPrice;
    private final double minIncrement;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private double currentPrice;
    // volatile giúp mọi luồng (Thread) đều thấy ngay trạng thái mới nhất
    private volatile String status;
    private boolean isFinished = false;
    private boolean winProcessed = false;

    // Khai báo bằng Interface List, khởi tạo bằng ArrayList
    private final List<BidTransaction> bidList = new ArrayList<>();

    public Auction(Seller seller, Item item) {
        if (seller == null || item == null) {
            throw new IllegalArgumentException("Seller và Item không được bỏ trống!");
        }
        this.seller = seller;
        this.item = item;
        this.startingPrice = item.getStartingPrice(); // Bắt buộc dùng getter của Item
        this.minIncrement = item.getMinIncrement();
        this.currentPrice = item.getStartingPrice();
        this.startTime = item.getStartTime();
        this.endTime = item.getEndTime();

        this.status = updateStatus();

        if (item.getBidList() != null && !item.getBidList().isEmpty()) {
            this.bidList.addAll(item.getBidList());
            this.currentPrice = this.bidList.get(this.bidList.size() - 1).getAmount();
        }
    }

    // Hàm cập nhật trạng thái dựa trên thời gian hiện tại
    public String updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) return "UPCOMING";
        if (now.isAfter(endTime)) return "FINISHED";
        return "ACTIVE";
    }

    /**
     * SỬA LỖI: Thêm phương thức này vì BidderController và ItemManagementController gọi auction.getStatusDisplay()
     * Nó sẽ gọi lại hàm updateStatus để lấy trạng thái thời gian thực tế mới nhất.
     */
    public String getStatusDisplay() {
        this.status = updateStatus();
        return this.status;
    }

    /**
     * Hàm đặt giá - Trái tim của Concurrency
     * Cần đồng bộ hóa (synchronized) để không bị race condition
     */
    public synchronized void placeBid(BidTransaction newBid) {
        this.status = updateStatus();

        // Chấp nhận trạng thái "ACTIVE" hoặc "RUNNING" tuỳ thuộc vào quy ước giao diện
        if (!"ACTIVE".equals(status) && !"RUNNING".equals(status)) {
            throw new IllegalStateException("Phiên đấu giá đang ở trạng thái: " + status);
        }

        if (bidList.isEmpty()) {
            if (newBid.getAmount() < startingPrice) {
                throw new IllegalArgumentException("Giá khởi điểm là: " + startingPrice);
            }
            processNewBid(newBid, null);
        } else {
            BidTransaction lastBid = bidList.get(bidList.size() - 1);
            if (lastBid.getBidder().equals(newBid.getBidder())) {
                throw new IllegalArgumentException("Bạn đang là người giữ giá cao nhất!");
            }
            if (newBid.getAmount() < currentPrice + minIncrement) {
                throw new IllegalArgumentException("Bạn phải đặt giá cao hơn giá hiện tại ít nhất: " + minIncrement);
            }
            processNewBid(newBid, lastBid);
        }
    }

    // Tách logic xử lý tiền vào hàm private cho dễ đọc (Clean Code)
    private void processNewBid(BidTransaction newBid, BidTransaction lastBid) {
        if (lastBid != null) {
            lastBid.refundAmount();
            UserDAO.saveUser(lastBid.getBidder());
        }
        newBid.freezeAmount();
        UserDAO.saveUser(newBid.getBidder());
        bidList.add(newBid);
        currentPrice = newBid.getAmount();

        // Đồng bộ ngược lại giá mới nhất vào đối tượng Item để phục vụ lưu file JSON
        item.setStartingPrice(currentPrice);
        item.setCurrentHighestBid(currentPrice);
        item.getBidList().add(newBid);
    }

    /**
     * Xác định người thắng và thanh toán.
     * Sẽ được gọi bởi AuctionManager (Server) khi hết giờ.
     */
    public synchronized void processWinner() {
        this.status = updateStatus();
        if ("UPCOMING".equals(status) || "ACTIVE".equals(status) || "RUNNING".equals(status)) {
            throw new IllegalStateException("Phiên đấu giá chưa kết thúc!");
        }

        if (!isFinished) {
            isFinished = true;
            if (bidList.isEmpty()) {
                return;
            }

            if (!winProcessed) {
                BidTransaction winBid = bidList.get(bidList.size() - 1);
                seller.receiveBalance(currentPrice);
                winBid.getBidder().paid(currentPrice);
                UserDAO.saveUser(seller);
                UserDAO.saveUser(winBid.getBidder());
                winProcessed = true;
            }
        }
    }

    /**
     * Hủy phiên đấu giá và hoàn tiền cho người trả giá cao nhất.
     */
    public synchronized void cancelAndRefund() {
        if (!isFinished) {
            isFinished = true;
            if (!bidList.isEmpty()) {
                BidTransaction lastBid = bidList.get(bidList.size() - 1);
                lastBid.refundAmount();
                UserDAO.saveUser(lastBid.getBidder());
            }
        }
    }

    /**
     * Trả về danh sách an toàn. Khi 1 thread gọi hàm này,
     * nó tạo ra 1 bản sao, không lo bị Exception nếu thread khác đang đặt giá.
     */
    public synchronized List<BidTransaction> getBidList() {
        return new ArrayList<>(bidList);
    }

    public synchronized void syncBidList(List<BidTransaction> newList) {
        this.bidList.clear();
        if (newList != null) {
            this.bidList.addAll(newList);
            if (!this.bidList.isEmpty()) {
                this.currentPrice = this.bidList.get(this.bidList.size() - 1).getAmount();
            }
        }
    }

    /**
     * SỬA LỖI: Định dạng lại chuỗi thông tin theo đúng yêu cầu hiển thị TextArea của BidderController
     */
    public synchronized String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== THÔNG TIN PHIÊN ĐẤU GIÁ ===\n");
        sb.append("Mã sản phẩm: ").append(item.getId()).append("\n");
        sb.append("Tên sản phẩm: ").append(item.getName()).append("\n");
        sb.append("Bước giá tối thiểu: ").append(minIncrement).append(" $\n");
        sb.append("Trạng thái: ").append(getStatusDisplay()).append("\n\n");
        sb.append("--- LỊCH SỬ ĐẶT GIÁ LUỒNG THỜI GIAN ---\n");

        if (bidList.isEmpty()) {
            sb.append("- Chưa có người đặt giá cho sản phẩm này.\n");
        } else {
            if ("FINISHED".equals(getStatusDisplay())) {
                BidTransaction winBid = bidList.get(bidList.size() - 1);
                sb.append(String.format("🌟 NGƯỜI CHIẾN THẮNG: %s (%.2f $) 🌟\n\n", 
                    winBid.getBidderName(), winBid.getAmount()));
            }
            // In ngược từ lượt mới nhất xuống để người xem dễ nhìn thấy trên UI
            for (int i = bidList.size() - 1; i >= 0; i--) {
                BidTransaction bid = bidList.get(i);
                sb.append(String.format(" [%d] Người dùng: %s ---> Đặt giá: %.2f $\n",
                    (i + 1), bid.getBidderName(), bid.getAmount()));
            }
        }
        return sb.toString();
    }

    // --- CÁC HÀM GETTER ---
    public double getCurrentPrice() { return currentPrice; }
    public String getStatus() { return status; }
    public Item getItem() { return item; }
}