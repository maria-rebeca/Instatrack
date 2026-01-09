package org.example;

import javax.swing.*;
import java.awt.*;

public class LoginForm extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginForm() {
        setTitle("InstaTrack - Login");
        setSize(400, 260);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        Box container = Box.createVerticalBox();
        container.add(Box.createVerticalStrut(30));

        JPanel uRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        uRow.add(new JLabel("Utilizator: "));
        userField = new JTextField(15);
        uRow.add(userField);
        container.add(uRow);

        JPanel pRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pRow.add(new JLabel("   Parolă: "));
        passField = new JPasswordField(15);
        pRow.add(passField);
        container.add(pRow);

        JPanel bRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton loginBtn = new JButton("Logare");
        JButton openRegBtn = new JButton("Cont nou");
        loginBtn.setPreferredSize(new Dimension(100, 35));
        openRegBtn.setPreferredSize(new Dimension(100, 35));
        bRow.add(loginBtn);
        bRow.add(openRegBtn);
        container.add(bRow);

        add(container);

        openRegBtn.addActionListener(e -> {
            new RegisterForm().setVisible(true);
        });

        loginBtn.addActionListener(e -> {
            int userId = Main.checkLogin(userField.getText(), new String(passField.getPassword()));
            if (userId != -1) {
                this.dispose();
                TutorialDialog tutorial = new TutorialDialog(null);
                tutorial.setVisible(true);
                new Dashboard(userId).setVisible(true);
            } else {
                JOptionPane.showMessageDialog(this, "Date incorecte!", "Eroare", JOptionPane.ERROR_MESSAGE);
            }
        });


    }
}