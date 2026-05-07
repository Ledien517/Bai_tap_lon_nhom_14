package com.auction.common.model;

import com.auction.common.observer.AuctionObserver;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Quản lý một phiên đấu giá.
 *
 * THAY ĐỔI SO VỚI BẢN CŨ:
 * 1. Observer Pattern (CopyOnWriteArrayList = thread-safe)
 * 2. Anti-sniping: gia hạn 60s nếu có bid trong 30s cuối
 * 3. getBidList() để Controller đọc lịch sử
 * 4. endTime không còn final
 * 5. item.setCurrentHighestBid() sau mỗi bid thành công
 */
public class Auction {
    private final Seller seller;
    private final Item item;
    private final double startingPrice;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isFinished = false;
    private double currentPrice;
    private final double minIncrement;
    private boolean winProcessed = false;
    private final ArrayList<BidTransaction> bidList = new ArrayList<>();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Observer list — CopyOnWriteArrayList tránh ConcurrentModificationException
    private final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    public Auction(Seller seller, Item item) {
        if (seller == null || item == null) {
            throw new IllegalArgumentException("Seller và Item không được bỏ trống!");
        }
        this.seller = seller;
        this.item = item;
        this.startingPrice = item.getStartingPrice();
        this.minIncrement = item.getMinIncrement();
        this.currentPrice = item.getStartingPrice();
        this.startTime = item.startTime;
        this.endTime = item.endTime;
        startAutoTimer();
    }

    // ===== Observer Pattern =====
    public void addObserver(AuctionObserver observer) {
        if (observer != null) observers.add(observer);
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyBidPlaced(BidTransaction bid) {
        for (AuctionObserver obs : observers) obs.onBidPlaced(bid);
    }

    private void notifyPriceUpdated(double price) {
        for (AuctionObserver obs : observers) obs.onPriceUpdated(price);
    }

    private void notifyAuctionEnded() {
        if (bidList.isEmpty()) return;
        BidTransaction win = bidList.get(bidList.size() - 1);
        for (AuctionObserver obs : observers) obs.onAuctionEnded(win.getBidderName(), currentPrice);
    }

    // ===== Status =====
    private String updateStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime))      status = "UPCOMING";
        else if (now.isAfter(endTime))    status = "FINISHED";
        else                              status = "ACTIVE";
        return status;
    }

    public void startAutoTimer() {
        long delayToStart = Duration.between(LocalDateTime.now(), startTime).toMillis();
        long delayToEnd   = Duration.between(LocalDateTime.now(), endTime).toMillis();

        if (delayToStart > 0) {
            scheduler.schedule(() -> {
                this.status = "ACTIVE";
                System.out.println("--- PHIÊN ĐẤU GIÁ CHÍNH THỨC BẮT ĐẦU! ---");
            }, delayToStart, TimeUnit.MILLISECONDS);
        }
        if (delayToEnd > 0) {
            scheduler.schedule(this::endAuction, delayToEnd, TimeUnit.MILLISECONDS);
        } else {
            endAuction();
        }
    }

    private synchronized void endAuction() {
        if (!isFinished) {
            isFinished = true;
            this.status = "FINISHED";
            System.out.println("--- PHIÊN ĐẤU GIÁ ĐÃ TỰ ĐỘNG KẾT THÚC! ---");
            notifyAuctionEnded();
            Winner();
            scheduler.shutdown();
        }
    }

    // ===== Core: đặt giá =====
    public synchronized void placeBid(BidTransaction newBid) {
        String currentStatus = updateStatus();

        if (currentStatus.equals("UPCOMING")) {
            throw new IllegalArgumentException("Phiên đấu giá chưa bắt đầu!");
        } else if (currentStatus.equals("FINISHED")) {
            throw new IllegalArgumentException("Phiên đấu giá đã kết thúc!");
        } else {
            if (bidList.isEmpty() && newBid.getAmount() < startingPrice) {
                throw new IllegalArgumentException(
                    "Giá đặt phải từ " + String.format("%,.0f", startingPrice) + " $");
            } else if (bidList.isEmpty()) {
                newBid.frozen();
                bidList.add(newBid);
                currentPrice = newBid.getAmount();
            } else {
                BidTransaction lastBid = bidList.get(bidList.size() - 1);
                if (lastBid.getBidder().equals(newBid.getBidder())) {
                    throw new IllegalArgumentException("Bạn đang là người giữ giá cao nhất!");
                }
                if (newBid.getAmount() < currentPrice + minIncrement) {
                    throw new IllegalArgumentException(
                        "Giá đặt phải cao hơn giá hiện tại ít nhất " + String.format("%,.0f", minIncrement) + " $");
                }
                lastBid.refund();
                newBid.frozen();
                bidList.add(newBid);
                currentPrice = newBid.getAmount();
            }

            // Đồng bộ giá lên Item để TableView refresh đúng
            item.setCurrentHighestBid(currentPrice);

            // Anti-sniping: gia hạn nếu bid trong 30 giây cuối
            long secondsLeft = Duration.between(LocalDateTime.now(), endTime).getSeconds();
            if (secondsLeft >= 0 && secondsLeft < 30) {
                endTime = endTime.plusSeconds(60);
                System.out.println("⏱️ Anti-sniping: Phiên được gia hạn thêm 60 giây!");
            }

            // Notify observers để update UI realtime
            notifyBidPlaced(newBid);
            notifyPriceUpdated(currentPrice);
        }
    }

    public synchronized void Winner() {
        String s = updateStatus();
        if (s.equals("UPCOMING")) {
            throw new IllegalArgumentException("Phiên đấu giá chưa bắt đầu!");
        } else if (s.equals("ACTIVE")) {
            throw new IllegalArgumentException("Phiên đấu giá đang diễn ra!");
        } else {
            if (bidList.isEmpty()) {
                System.out.println("Phiên đấu giá kết thúc, không có người tham gia!");
                return;
            }
            BidTransaction winBid = bidList.get(bidList.size() - 1);
            System.out.println("Người thắng: " + winBid.getBidderName()
                + " — Giá: " + String.format("%,.0f $", winBid.getAmount()));
            if (!winProcessed) {
                seller.receiveBalance(currentPrice);
                winBid.getBidder().paid(currentPrice);
                winProcessed = true;
            }
        }
    }

    // ===== Getters =====
    public double getCurrentPrice() { return currentPrice; }

    public String getStatusDisplay() {
        updateStatus();
        return status;
    }

    /** Trả về bản copy để tránh race condition khi iterate từ ngoài. */
    public synchronized List<BidTransaction> getBidList() {
        return new ArrayList<>(bidList);
    }

    public Seller getSeller()          { return seller; }
    public Item getItem()              { return item; }
    public LocalDateTime getEndTime()  { return endTime; }

    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- THÔNG TIN PHIÊN ĐẤU GIÁ ---\n");
        sb.append("Người bán: ").append(seller.getUsername()).append("\n");
        sb.append("Sản phẩm : ").append(item.getName()).append("\n");
        sb.append("Thời gian: ").append(startTime.format(FORMATTER))
          .append(" → ").append(endTime.format(FORMATTER)).append("\n");
        sb.append("Trạng thái: ").append(getStatusDisplay()).append("\n");
        sb.append("Giá khởi điểm: ").append(String.format("%,.0f $", startingPrice)).append("\n");
        sb.append("Giá hiện tại : ").append(String.format("%,.0f $", currentPrice)).append("\n");
        sb.append("Bước giá tối thiểu: ").append(String.format("%,.0f $", minIncrement)).append("\n");
        sb.append("Lịch sử đặt giá:\n");
        if (bidList.isEmpty()) {
            sb.append("  (Chưa có người đặt giá)\n");
        } else {
            for (BidTransaction bid : bidList) {
                sb.append("  • ").append(bid.getBidderName())
                  .append(": ").append(String.format("%,.0f $", bid.getAmount()))
                  .append(" — ").append(bid.getTime()).append("\n");
            }
        }
        return sb.toString();
    }
}
