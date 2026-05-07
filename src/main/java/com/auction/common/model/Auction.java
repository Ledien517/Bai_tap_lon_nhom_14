package com.auction.common.model;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
public class Auction {
    private final Seller seller;
    private final Item item;
    private final double startingPrice;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private String status;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private boolean isFinished = false;
    private double currentPrice;
    private final double minIncrement;
    private boolean winProcessed = false;
    private ArrayList<BidTransaction> BidList = new ArrayList<BidTransaction>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public Auction (Seller seller, Item item){
        if (seller == null || item == null) {
            throw new IllegalArgumentException("Seller và Item không được bỏ trống!");
        }
        this.seller = seller;
        this.item = item;
        this.startingPrice = this.item.getStartingPrice();
        this.minIncrement = this.item.getMinIncrement();
        this.currentPrice = this.item.getStartingPrice();
        this.startTime = this.item.startTime;
        this.endTime = this.item.endTime;
        startAutoTimer();
    }
    private String updateStatus(){
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)){
            status = "UPCOMING";
        }
        else if (now.isAfter(endTime)){
            status = "FINISHED";
        }
        else {
            status = "ACTIVE";
        }
        return status;
    }
    public void startAutoTimer() {
        long delayToStart = Duration.between(LocalDateTime.now(), startTime).toMillis();
        long delayToEnd = Duration.between(LocalDateTime.now(), endTime).toMillis();
        if (delayToStart > 0) {
            scheduler.schedule(() -> {
                this.status = "ACTIVE";
                System.out.println("--- PHIÊN ĐẤU GIÁ CHÍNH THỨC BẮT ĐẦU! ---");
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
            System.out.println("--- PHIÊN ĐẤU GIÁ ĐÃ TỰ ĐỘNG KẾT THÚC! ---");
            Winner();
            scheduler.shutdown();
        }
    }
    public synchronized void placeBid(BidTransaction newBid){
        String currentStatus = updateStatus();
        if (currentStatus.equals("UPCOMING")){
            throw new IllegalArgumentException("Phiên đấu giá chưa bắt đầu!");
        }
        else if (currentStatus.equals("FINISHED")){
            throw new IllegalArgumentException("Phiên đấu giá đã kết thúc!");
        }
        else if (currentStatus.equals("ACTIVE")){
            if (BidList.isEmpty() && newBid.getAmount() < startingPrice){
                throw new IllegalArgumentException("Giá khởi điểm là: " + startingPrice);
            }
            else if (BidList.isEmpty() && newBid.getAmount() >= startingPrice){
                newBid.frozen();
                BidList.add(newBid);
                currentPrice = newBid.getAmount();
            }
            else{
                BidTransaction lastBid = BidList.get(BidList.size() - 1);
                if (lastBid.getBidder().equals(newBid.getBidder())) {
                    throw new IllegalArgumentException("Bạn đang là người giữ giá cao nhất!");
                } else {
                    if (newBid.getAmount() >= currentPrice + minIncrement) {
                        lastBid.refund();
                        newBid.frozen();
                        BidList.add(newBid);
                        currentPrice = newBid.getAmount();
                    } else {
                        throw new IllegalArgumentException("Bạn phải đặt giá cao hơn giá hiện tại ít nhất: " + minIncrement);
                    }
                }
            }
        }
    }
    public synchronized void Winner(){
        String currentStatus = updateStatus();
        if (currentStatus.equals("UPCOMING")){
            throw new IllegalArgumentException("Phiên đấu giá chưa bắt đầu!");
        }
        else if (currentStatus.equals("ACTIVE")){
            throw new IllegalArgumentException("Phiên đấu giá đang diễn ra!");
        }
        else{
            if (BidList.isEmpty()){
                System.out.println("Phiên đấu giá này đã kết thúc, không có ai tham gia đấu giá!");
                return;
            }
            else{
                BidTransaction winBid = BidList.get(BidList.size() - 1);
                Bidder winBidder = winBid.getBidder();
                System.out.println("Người thắng cuộc: " + winBid.getBidderName() + " với giá: " + winBid.getAmount() + " !");
                if (!winProcessed) {
                    seller.receiveBalance(currentPrice);
                    winBidder.paid(currentPrice);
                    winProcessed = true;
                }
            }
        }
    }
    public double getCurrentPrice() {
        return this.currentPrice;
    }
    public String getStatusDisplay() {
        updateStatus();
        return this.status;
    }
    public String getInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Danh sách đặt giá:\n");
        if (BidList.isEmpty()) {
            sb.append("- Chưa có người đặt giá.\n");
        } else {
            for (BidTransaction bid : BidList) {
                sb.append("- ").append(bid.getBidderName())
                        .append(": ").append(bid.getAmount())
                        .append(" vào lúc: ").append(bid.getTime()).append("\n");
            }
        }
        return sb.toString();
    }
}
