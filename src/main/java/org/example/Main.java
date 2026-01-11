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
            System.err.println("Eroare config.properties!");
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
            if (checkStmt.executeQuery().next()) return false;
            PreparedStatement insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, user);
            insertStmt.setString(2, pass);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public static void loadFollowersFile(File file) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        JSONArray arr;
        if (content.trim().startsWith("{")) {
            JSONObject obj = new JSONObject(content);
            if (obj.has("relationships_followers")) arr = obj.getJSONArray("relationships_followers");
            else arr = obj.getJSONArray("followers");
        } else {
            arr = new JSONArray(content);
        }
        currentFollowers.clear();
        currentFollowers.addAll(extractNames(arr));
    }

    public static void loadFollowingFile(File file) throws Exception {
        String content = new String(Files.readAllBytes(file.toPath()));
        String trimmed = content.trim();

        JSONArray arr;

        if (trimmed.startsWith("{")) {
            JSONObject obj = new JSONObject(trimmed);
            if (obj.has("relationships_following")) {
                arr = obj.getJSONArray("relationships_following");
            } else if (obj.has("following")) {
                arr = obj.getJSONArray("following");
            } else {
                throw new RuntimeException(
                        "Nu am găsit nici 'relationships_following' nici 'following' în JSON-ul de following."
                );
            }
        } else {
            arr = new JSONArray(trimmed);
        }

        currentFollowing.clear();
        currentFollowing.addAll(extractNames(arr));
    }


    public static Set<String> extractNames(JSONArray array) {
        Set<String> names = new HashSet<>();

        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject item = array.getJSONObject(i);
                String name = null;
                if (item.has("string_list_data")) {
                    JSONObject first = item.getJSONArray("string_list_data").getJSONObject(0);

                    if (first.has("value")) {
                        name = first.getString("value");
                    }
                    else if (first.has("href")) {
                        String href = first.getString("href");
                        int lastSlash = href.lastIndexOf('/');
                        if (lastSlash != -1 && lastSlash + 1 < href.length()) {
                            name = href.substring(lastSlash + 1);
                        }
                    }
                }

                if (name == null && item.has("title")) {
                    name = item.getString("title");
                }

                if (name == null && item.has("value")) {
                    name = item.getString("value");
                }

                if (name != null && !name.isBlank()) {
                    names.add(name);
                }

            } catch (Exception e) {
            }
        }

        return names;
    }

}