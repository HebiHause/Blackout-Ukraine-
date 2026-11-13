package ua.blackout.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class UserStore {

    private final String url;

    public UserStore(String url) throws SQLException {
        this.url = url;
        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        chat_id INTEGER PRIMARY KEY,
                        queue TEXT NOT NULL)""");
        }
    }

    public void saveQueue(long chatId, String queue) throws SQLException {
        String sql = """
                INSERT INTO users (chat_id, queue) VALUES (?, ?)
                ON CONFLICT(chat_id) DO UPDATE SET queue = excluded.queue""";
        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, chatId);
            ps.setString(2, queue);
            ps.executeUpdate();
        }
    }

    public Optional<String> findQueue(long chatId) throws SQLException {
        String sql = "SELECT queue FROM users WHERE chat_id = ?";
        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, chatId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("queue"));
                }
                return Optional.empty();
            }
        }
    }
}