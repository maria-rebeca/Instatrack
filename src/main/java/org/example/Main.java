package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.nio.file.Files;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static String URL;
    public static String USER_DB;
    public static String PASS_DB;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        Properties prop = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            URL = prop.getProperty("db.url");
            USER_DB = prop.getProperty("db.user");
            PASS_DB = prop.getProperty("db.pass");
        } catch (IOException ex) {
            System.err.println("Nu s-a găsit fișierul config.properties!");
        }
    }

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