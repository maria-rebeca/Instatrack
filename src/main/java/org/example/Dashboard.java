package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
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
                displayArea.append("[OK] Fișier " + type + " încărcat cu succes.\n");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Eroare la procesarea JSON: " + ex.getMessage());
            }
        }
    }

    private void runNoBack() {
        if (Main.currentFollowers.isEmpty() || Main.currentFollowing.isEmpty()) {
            displayArea.append("[!] Te rugăm să încarci ambele fișiere JSON mai întâi.\n");
            return;
        }
        displayArea.append("\n--- CINE NU ÎȚI DĂ FOLLOW BACK ---\n");
        int count = 0;
        for (String u : Main.currentFollowing) {
            if (!Main.currentFollowers.contains(u)) {
                displayArea.append("[!] " + u + "\n");
                count++;
            }
        }
        displayArea.append("Total: " + count + "\n");
    }

    private void runUnfollowDB() {
        displayArea.append("\n--- DETECȚIE UNFOLLOW DIN ISTORIC DB ---\n");
        Set<String> dbData = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(Main.URL, Main.USER_DB, Main.PASS_DB)) {
            PreparedStatement ps = conn.prepareStatement("SELECT follower_name FROM followers_history WHERE user_id = ?");
            ps.setInt(1, loggedInUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) dbData.add(rs.getString("follower_name"));

            int count = 0;
            for (String s : dbData) {
                if (!Main.currentFollowers.contains(s)) {
                    displayArea.append("[-] UNFOLLOW: " + s + "\n");
                    count++;
                }
            }
            displayArea.append("Total: " + count + "\n");
        } catch (Exception ex) { displayArea.append("Eroare DB: " + ex.getMessage() + "\n"); }
    }

    private void saveToDB() {
        int confirm = JOptionPane.showConfirmDialog(this, "Vrei să salvezi lista actuală ca nou snapshot?", "Confirmare", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(Main.URL, Main.USER_DB, Main.PASS_DB)) {
                conn.setAutoCommit(false);
                PreparedStatement del = conn.prepareStatement("DELETE FROM followers_history WHERE user_id = ?");
                del.setInt(1, loggedInUserId); del.executeUpdate();
                PreparedStatement ins = conn.prepareStatement("INSERT INTO followers_history (user_id, follower_name) VALUES (?, ?)");
                for (String n : Main.currentFollowers) {
                    ins.setInt(1, loggedInUserId); ins.setString(2, n); ins.addBatch();
                }
                ins.executeBatch(); conn.commit();
                displayArea.append("[OK] Baza de date a fost actualizată.\n");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Eroare la salvare: " + ex.getMessage()); }
        }
    }

    private void exportTxt() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter pw = new PrintWriter(chooser.getSelectedFile() + ".txt")) {
                pw.println(displayArea.getText());
                JOptionPane.showMessageDialog(this, "Raport exportat!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Eroare: " + ex.getMessage()); }
        }
    }
}