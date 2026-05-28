package com.auction.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

// ZoneId dùng thống nhất toàn bộ so sánh thời gian theo giờ Việt Nam
// vì Server Railway chạy UTC còn người dùng nhập giờ VN (UTC+7)

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Seller seller;
    private final Item item;
    private final double startingPrice;
    private final double minIncrement;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    private double currentPrice;
    // volatile giúp mọi luồng (Thread) đều thấy ngay trạng thái mới nhất
    private volatile String status;
    private boolean isFinished = false;
    private boolean winProcessed = false;

    // Khai báo bằng Interface List, khởi tạo bằng ArrayList
    private final List<BidTransaction> bidList = new ArrayList<>();
    
    // Lưu cấu hình Auto-bids cho phiên này
    private final List<AutoBid> autoBids = new ArrayList<>();

    public Auction(Seller seller, Item item) {
        if (seller == null || item == null) {
            throw new IllegalArgumentException("Seller và Item không được bỏ trống!");
        }
        this.seller = seller;
        this.item = item;
        this.startingPrice = item.getStartingPrice(); // Bắt buộc dùng getter của Item
        this.minIncrement = item.getMinIncrement();
        this.startTime = item.getStartTime();
        this.endTime = item.getEndTime();

        // Nạp lại các lượt đặt giá cũ nếu có trong Item
        if (item.getBidList() != null && !item.getBidList().isEmpty()) {
            this.bidList.addAll(item.getBidList());
            this.currentPrice = item.getCurrentHighestBid();
        } else {
            this.currentPrice = item.getStartingPrice();
        }

        this.status = updateStatus();
    }

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    // Hàm cập nhật trạng thái dựa trên thời gian hiện tại (giờ Việt Nam)
    public String updateStatus() {
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
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

        // --- THUẬT TOÁN ANTI-SNIPING ---
        // Nếu có lượt đặt giá hợp lệ trong 30 giây cuối cùng, tự động gia hạn thêm 60 giây
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        if (now.isAfter(endTime.minusSeconds(30)) && now.isBefore(endTime)) {
            this.endTime = this.endTime.plusSeconds(60);
            this.item.setEndTime(this.endTime);
            System.out.println("[Anti-Sniping] Phiên " + item.getId() + " được gia hạn thêm 60 giây!");
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

        // Kích hoạt vòng lặp xử lý Đấu giá tự động khi có bất kì ai đặt giá mới
        processAutoBids();
    }

    // Tách logic xử lý tiền vào hàm private cho dễ đọc (Clean Code)
    private void processNewBid(BidTransaction newBid, BidTransaction lastBid) {
        if (lastBid != null) {
            lastBid.refundAmount();
        }
        newBid.freezeAmount();
        bidList.add(newBid);
        currentPrice = newBid.getAmount();

        // Đồng bộ ngược lại giá mới nhất vào đối tượng Item để phục vụ lưu file JSON
        item.setStartingPrice(currentPrice);
        item.setCurrentHighestBid(currentPrice);
    }

    /**
     * Thuật toán Đấu giá tự động (Auto-Bidding)
     */
    public synchronized void registerAutoBid(AutoBid autoBid) {
        this.status = updateStatus();
        if (!"ACTIVE".equals(status) && !"RUNNING".equals(status)) {
            throw new IllegalStateException("Phiên đấu giá đang ở trạng thái: " + status);
        }
        
        // Hủy đăng ký cũ nếu có
        autoBids.removeIf(ab -> ab.getBidder().equals(autoBid.getBidder()));
        autoBids.add(autoBid);
        // Ưu tiên theo thời gian đăng ký sớm nhất
        autoBids.sort((a, b) -> a.getRegisteredTime().compareTo(b.getRegisteredTime()));
        
        System.out.println("[Auto-Bidding] User " + autoBid.getBidder().getUsername() + " đã đăng ký đấu giá tự động cho " + item.getId());
        processAutoBids();
    }

    private void processAutoBids() {
        boolean changed = true;
        // Chạy vòng lặp cho đến khi không có Auto-bid nào đủ điều kiện đặt giá thêm
        while (changed) {
            changed = false;
            if (autoBids.isEmpty()) break;

            BidTransaction lastBid = bidList.isEmpty() ? null : bidList.get(bidList.size() - 1);
            Bidder currentWinner = (lastBid != null) ? lastBid.getBidder() : null;

            for (AutoBid autoBid : autoBids) {
                // Không tự đấu giá đè lên chính mình
                if (autoBid.getBidder().equals(currentWinner)) {
                    continue;
                }

                double requiredPrice = currentPrice + minIncrement;
                if (bidList.isEmpty()) {
                    requiredPrice = startingPrice;
                }
                
                double actualIncrement = Math.max(minIncrement, autoBid.getIncrement());
                double desiredPrice = currentPrice + actualIncrement;
                if (bidList.isEmpty()) {
                    desiredPrice = Math.max(startingPrice, actualIncrement);
                }

                // Nếu giá yêu cầu đặt thêm vẫn nằm trong mức maxBid của người chơi
                if (requiredPrice <= autoBid.getMaxBid()) {
                    double finalPrice = Math.min(desiredPrice, autoBid.getMaxBid());
                    
                    try {
                        BidTransaction newBid = new BidTransaction(autoBid.getBidder(), finalPrice);
                        processNewBid(newBid, lastBid);
                        System.out.println("[Auto-Bidding] Hệ thống tự đặt " + finalPrice + "$ cho " + autoBid.getBidder().getUsername());
                        changed = true;
                        break; // Có thay đổi, thoát vòng lặp for để xét lại từ đầu (đảm bảo tính ưu tiên)
                    } catch (Exception e) {
                        // Người dùng không đủ tiền hoặc lỗi gì đó -> Xóa cấu hình Auto-bid này
                        autoBids.remove(autoBid);
                        changed = true; // reset vòng lặp sau khi xóa phần tử
                        break;
                    }
                }
            }
        }
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
            winProcessed = true; // Đánh dấu đã xử lý để Monitor không lặp lại

            if (bidList.isEmpty()) {
                return;
            }

            BidTransaction winBid = bidList.get(bidList.size() - 1);
            seller.receiveBalance(currentPrice);
            winBid.getBidder().paid(currentPrice);
        }
    }

    /**
     * Trả về danh sách an toàn. Khi 1 thread gọi hàm này,
     * nó tạo ra 1 bản sao, không lo bị Exception nếu thread khác đang đặt giá.
     */
    public synchronized List<BidTransaction> getBidList() {
        return new ArrayList<>(bidList);
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

        if ("FINISHED".equals(getStatusDisplay()) && !bidList.isEmpty()) {
            BidTransaction winBid = bidList.get(bidList.size() - 1);
            sb.append("🏆 NGƯỜI CHIẾN THẮNG: ").append(winBid.getBidderName())
              .append(" với mức giá ").append(winBid.getAmount()).append(" $\n\n");
        }

        if (bidList.isEmpty()) {
            sb.append("- Chưa có người đặt giá cho sản phẩm này.\n");
        } else {
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
    public boolean isWinProcessed() { return winProcessed; }
}