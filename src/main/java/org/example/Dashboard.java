package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class Dashboard extends JFrame {
    private JTextArea displayArea;
    private int loggedInUserId;

    public Dashboard(int userId) {
        this.loggedInUserId = userId;
        setTitle("InstaTrack Dashboard");
        setSize(950, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));

        JPanel btnPanel = new JPanel(new GridLayout(8, 1, 10, 10));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));

        JButton btnUpFollowers = new JButton("1. Încarcă Followers (JSON)");
        JButton btnUpFollowing = new JButton("2. Încarcă Following (JSON)");
        JButton btnNoBack = new JButton("Analiză: No Follow Back");
        JButton btnUnfollow = new JButton("Detecție Unfollow (DB)");
        JButton btnSave = new JButton("Actualizează Snapshot DB");
        JButton btnExport = new JButton("Exportă Raport (.txt)");
        JButton btnClear = new JButton("Curăță Ecran");
        JButton btnExit = new JButton("Ieșire");

        btnPanel.add(btnUpFollowers);
        btnPanel.add(btnUpFollowing);
        btnPanel.add(btnNoBack);
        btnPanel.add(btnUnfollow);
        btnPanel.add(btnSave);
        btnPanel.add(btnExport);
        btnPanel.add(btnClear);
        btnPanel.add(btnExit);
        add(btnPanel, BorderLayout.WEST);

        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        displayArea.setMargin(new Insets(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 10, 20, 20),
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Consolă Rezultate", TitledBorder.LEFT, TitledBorder.TOP)
        ));
        add(scrollPane, BorderLayout.CENTER);

        btnUpFollowers.addActionListener(e -> uploadAction("FOLLOWERS"));
        btnUpFollowing.addActionListener(e -> uploadAction("FOLLOWING"));
        btnNoBack.addActionListener(e -> runNoBack());
        btnUnfollow.addActionListener(e -> runUnfollowDB());
        btnSave.addActionListener(e -> saveToDB());
        btnExport.addActionListener(e -> exportTxt());
        btnClear.addActionListener(e -> displayArea.setText(""));
        btnExit.addActionListener(e -> System.exit(0));
    }

    private void uploadAction(String type) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                if (type.equals("FOLLOWERS")) Main.loadFollowersFile(chooser.getSelectedFile());
                else Main.loadFollowingFile(chooser.getSelectedFile());
                displayArea.append("[OK] Fișier " + type + " încărcat.\n");
                JOptionPane.showMessageDialog(this, "Fișierul " + type + " a fost încărcat!", "Succes", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare: " + ex.getMessage(), "Eroare JSON", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void runNoBack() {
        if (!Main.currentFollowers.isEmpty() && !Main.currentFollowing.isEmpty()) {
            displayArea.append("\n--- ANALIZĂ: NO FOLLOW BACK (JSON Complet) ---\n");
            executaComparatie(Main.currentFollowers, Main.currentFollowing);
        } else if (Main.currentFollowers.isEmpty() && Main.currentFollowing.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Se folosesc datele anterioare din DB.", "Info DB", JOptionPane.INFORMATION_MESSAGE);
            displayArea.append("\n--- ANALIZĂ: NO FOLLOW BACK (Date din DB) ---\n");
            incarcaSiComparaDinDB();
        } else {
            incarcaHibrid();
        }
    }

    private void incarcaHibrid() {
        Set<String> followers = new HashSet<>(Main.currentFollowers);
        Set<String> following = new HashSet<>(Main.currentFollowing);

        try (Connection conn = DriverManager.getConnection(Main.URL, Main.USER_DB, Main.PASS_DB)) {
            boolean hasDBFollowers = false;
            boolean hasDBFollowing = false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT follower_name FROM followers_history WHERE user_id = ?")) {
                ps.setInt(1, loggedInUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    hasDBFollowers = true;
                    String name = rs.getString("follower_name");
                    if (followers.isEmpty()) {
                        followers.add(name);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT following_name FROM following_history WHERE user_id = ?")) {
                ps.setInt(1, loggedInUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    hasDBFollowing = true;
                    String name = rs.getString("following_name");
                    if (following.isEmpty()) {
                        following.add(name);
                    }
                }
            }

            boolean hasAnyDB = hasDBFollowers || hasDBFollowing;

            if (!hasAnyDB && (followers.isEmpty() || following.isEmpty())) {
                JOptionPane.showMessageDialog(this, "Date insuficiente în JSON și DB!", "Eroare", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (Main.currentFollowing.isEmpty()) {
                JOptionPane.showMessageDialog(this, "S-a folosit Following din DB. Sigur nu ai modificat lista recent?", "Info Hibrid", JOptionPane.WARNING_MESSAGE);
                displayArea.append("\n--- ANALIZĂ: NO FOLLOW BACK (JSON Followers + DB Following) ---\n");
            } else if (Main.currentFollowers.isEmpty()) {
                JOptionPane.showMessageDialog(this, "S-a folosit Followers din DB.", "Info Hibrid", JOptionPane.INFORMATION_MESSAGE);
                displayArea.append("\n--- ANALIZĂ: NO FOLLOW BACK (DB Followers + JSON Following) ---\n");
            } else {
                displayArea.append("\n--- ANALIZĂ: NO FOLLOW BACK (Hibrid) ---\n");
            }

            executaComparatie(followers, following);

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Eroare DB", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void incarcaSiComparaDinDB() {
        Set<String> followersDB = new HashSet<>();
        Set<String> followingDB = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(Main.URL, Main.USER_DB, Main.PASS_DB)) {

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT follower_name FROM followers_history WHERE user_id = ?")) {
                ps.setInt(1, loggedInUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    followersDB.add(rs.getString("follower_name"));
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT following_name FROM following_history WHERE user_id = ?")) {
                ps.setInt(1, loggedInUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    followingDB.add(rs.getString("following_name"));
                }
            }

            if (followersDB.isEmpty() || followingDB.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nu există Snapshot complet în DB!", "Atenție", JOptionPane.WARNING_MESSAGE);
            } else {
                executaComparatie(followersDB, followingDB);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Eroare DB", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void executaComparatie(Set<String> followers, Set<String> following) {
        int count = 0;
        for (String user : following) {
            if (!followers.contains(user)) {
                displayArea.append("[!] " + user + "\n");
                count++;
            }
        }
        displayArea.append("Total: " + count + " persoane.\n");
    }

    private void runUnfollowDB() {
        if (Main.currentFollowers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Încarcă fișierul Followers JSON!", "Fișier lipsă", JOptionPane.WARNING_MESSAGE);
            return;
        }

        displayArea.append("\n--- DETECȚIE UNFOLLOW (DB vs JSON) ---\n");

        Set<String> followersDB = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(Main.URL, Main.USER_DB, Main.PASS_DB)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT follower_name FROM followers_history WHERE user_id = ?")) {
                ps.setInt(1, loggedInUserId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    followersDB.add(rs.getString("follower_name"));
                }
            }

            if (followersDB.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Salvează un Snapshot mai întâi.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int count = 0;
            for (String n : followersDB) {
                if (!Main.currentFollowers.contains(n)) {
                    displayArea.append("[-] " + n + "\n");
                    count++;
                }
            }

            if (count == 0) {
                displayArea.append("[OK] 0 unfolloweri noi.\n");
                JOptionPane.showMessageDialog(this, "Nicio schimbare detectată.", "Rezultat", JOptionPane.INFORMATION_MESSAGE);
            } else {
                displayArea.append("\nTotal: " + count + "\n");
                JOptionPane.showMessageDialog(this, "S-au găsit " + count + " unfolloweri!", "Alertă", JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Eroare DB", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void saveToDB() {
        if (Main.currentFollowers.isEmpty() || Main.currentFollowing.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Încarcă ambele JSON-uri!", "Atenție", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Înlocuiești istoricul vechi?", "Confirmare", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DriverManager.getConnection(Main.URL, Main.USER_DB, Main.PASS_DB)) {
            conn.setAutoCommit(false);
            try (PreparedStatement delFollowers = conn.prepareStatement("DELETE FROM followers_history WHERE user_id = ?");
                 PreparedStatement delFollowing = conn.prepareStatement("DELETE FROM following_history WHERE user_id = ?")) {

                delFollowers.setInt(1, loggedInUserId);
                delFollowers.executeUpdate();

                delFollowing.setInt(1, loggedInUserId);
                delFollowing.executeUpdate();
            }

            try (PreparedStatement insFollowers = conn.prepareStatement(
                    "INSERT INTO followers_history (user_id, follower_name) VALUES (?, ?)");
                 PreparedStatement insFollowing = conn.prepareStatement(
                         "INSERT INTO following_history (user_id, following_name) VALUES (?, ?)")) {

                for (String n : Main.currentFollowers) {
                    insFollowers.setInt(1, loggedInUserId);
                    insFollowers.setString(2, n);
                    insFollowers.addBatch();
                }

                for (String n : Main.currentFollowing) {
                    insFollowing.setInt(1, loggedInUserId);
                    insFollowing.setString(2, n);
                    insFollowing.addBatch();
                }

                insFollowers.executeBatch();
                insFollowing.executeBatch();
            }

            conn.commit();
            displayArea.append("[OK] Snapshot nou salvat.\n");
            JOptionPane.showMessageDialog(this, "Istoric actualizat!", "Succes", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void exportTxt() {
        if (displayArea.getText().trim().isEmpty()) return;
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile() + ".txt")) {
                pw.println(displayArea.getText());
                JOptionPane.showMessageDialog(this, "Raport salvat!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}