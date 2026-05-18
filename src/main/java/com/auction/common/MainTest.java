package com.auction.common;

import com.auction.common.factory.ItemFactory;
import com.auction.common.model.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;

public class MainTest {
  public static void main(String[] args) throws InterruptedException {
    System.out.println("=== BẮT ĐẦU TEST ĐA LUỒNG (CONCURRENCY BIDDING) ===");

    // 1. Khởi tạo dữ liệu
    Seller seller = new Seller("nguoiban", "123456");

    // Tạo sản phẩm qua Factory. Cài đặt thời gian hợp lệ (kéo dài 1 ngày)
    Item laptop = ItemFactory.createItem(
        seller, "electronics", "SP01", "MacBook Pro", "M3 Max",
        1000.0, // Giá khởi điểm: 1000
        LocalDateTime.now(), LocalDateTime.now().plusDays(1),
        50.0, // Bước giá: 50
        "12"
    );

    Auction auction = new Auction(seller, laptop);

    // 2. Tạo 3 người chơi và nạp tiền cho họ
    Bidder alice = new Bidder("Alice", "pass");
    alice.deposit(5000.0); // Nạp 5000$

    Bidder bob = new Bidder("Bob", "pass");
    bob.deposit(5000.0);

    Bidder charlie = new Bidder("Charlie", "pass");
    charlie.deposit(5000.0);

    // 3. Chuẩn bị cho bài Test Đa Luồng
    // CountDownLatch(1) giống như chốt cửa. Khi đếm ngược về 0, cửa mở, mọi Thread cùng chạy.
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch endGate = new CountDownLatch(3); // Đợi 3 luồng chạy xong để tổng hợp kết quả

    // Tạo Luồng 1 (Alice đặt 1100)
    Thread t1 = new Thread(() -> {
      try {
        startGate.await(); // Chờ tiếng súng lệnh
        auction.placeBid(new BidTransaction(alice, 1100.0));
        System.out.println("[Thread 1] Alice đã gửi lệnh đặt giá 1100.");
      } catch (Exception e) {
        System.out.println("[Thread 1 - Lỗi] Alice: " + e.getMessage());
      } finally {
        endGate.countDown();
      }
    });

    // Tạo Luồng 2 (Bob đặt 1100 - Cố tình đặt bằng giá Alice cùng lúc)
    Thread t2 = new Thread(() -> {
      try {
        startGate.await();
        auction.placeBid(new BidTransaction(bob, 1100.0));
        System.out.println("[Thread 2] Bob đã gửi lệnh đặt giá 1100.");
      } catch (Exception e) {
        System.out.println("[Thread 2 - Lỗi] Bob: " + e.getMessage());
      } finally {
        endGate.countDown();
      }
    });

    // Tạo Luồng 3 (Charlie đặt 1200)
    Thread t3 = new Thread(() -> {
      try {
        startGate.await();
        auction.placeBid(new BidTransaction(charlie, 1200.0));
        System.out.println("[Thread 3] Charlie đã gửi lệnh đặt giá 1200.");
      } catch (Exception e) {
        System.out.println("[Thread 3 - Lỗi] Charlie: " + e.getMessage());
      } finally {
        endGate.countDown();
      }
    });

    // Bắt đầu cho các luồng vào vị trí sẵn sàng
    t1.start();
    t2.start();
    t3.start();

    System.out.println("3... 2... 1... BẮT ĐẦU!");
    // PHÁT SÚNG LỆNH: Giảm startGate về 0 để 3 luồng đồng loạt lao vào hàm placeBid
    startGate.countDown();

    // Chờ cả 3 luồng thực hiện xong
    endGate.await();

    // 4. In kết quả để kiểm chứng
    System.out.println("\n=== KẾT QUẢ PHIÊN ĐẤU GIÁ ===");
    System.out.println(auction.getInfo());
    System.out.println("Giá hiện tại của phiên: " + auction.getCurrentPrice());

    System.out.println("\n=== KIỂM TRA VÍ TIỀN (CHỐNG THẤT THOÁT) ===");
    System.out.println("Alice   - Khả dụng: " + alice.getAvailableBalance() + " | Đóng băng: " + alice.getFrozenBalance());
    System.out.println("Bob     - Khả dụng: " + bob.getAvailableBalance() + " | Đóng băng: " + bob.getFrozenBalance());
    System.out.println("Charlie - Khả dụng: " + charlie.getAvailableBalance() + " | Đóng băng: " + charlie.getFrozenBalance());
  }
}