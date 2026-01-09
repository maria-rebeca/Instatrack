package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;

public class Main {
    public static final String URL = "jdbc:postgresql://localhost:5432/postgres";
    public static final String USER_DB = "postgres";
    public static final String PASS_DB = "maria20cz";

    public static Set<String> currentFollowers = new HashSet<>();
    public static Set<String> currentFollowing = new HashSet<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }

    public static int checkLogin(String user, String pass) {
        String sql = "SELECT id FROM users WHERE username = ? AND password_text = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER_DB, PASS_DB);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) { return -1; }
        return -1;
    }

    public static boolean registerUser(String user, String pass) {
        String checkSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password_text) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER_DB, PASS_DB)) {
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, user);
            if (checkStmt.executeQuery().next()) {
                return false;
            }

            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, user);
            insertStmt.setString(2, pass);
            insertStmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Eroare Register: " + e.getMessage());
            return false;
        }
    }

    public static void loadFollowersFile(File file) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        JSONArray arr = new JSONArray(content);
        currentFollowers.clear();
        for (int i = 0; i < arr.length(); i++) {
            currentFollowers.add(arr.getJSONObject(i).getJSONArray("string_list_data").getJSONObject(0).getString("value"));
        }
    }

    public static void loadFollowingFile(File file) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        JSONObject obj = new JSONObject(content);
        JSONArray arr = obj.getJSONArray("relationships_following");
        currentFollowing.clear();
        for (int i = 0; i < arr.length(); i++) {
            currentFollowing.add(arr.getJSONObject(i).getString("title"));
        }
    }
}