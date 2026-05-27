package com.auction.server;

import com.auction.dao.DatabaseManager;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

public class DBConnectionTest {
    @Test
    public void testConnection() {
        try (Connection conn = DatabaseManager.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            System.out.println(">>> DATABASE CONNECTION SUCCESSFUL! <<<");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Database connection failed: " + e.getMessage());
        }
    }
}
