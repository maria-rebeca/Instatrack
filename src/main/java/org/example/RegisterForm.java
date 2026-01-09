package org.example;

import javax.swing.*;
import java.awt.*;

public class RegisterForm extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public RegisterForm() {
        setTitle("InstaTrack - Creare Cont Nou");
        setSize(400, 260);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        Box container = Box.createVerticalBox();
        container.add(Box.createVerticalStrut(30));

        JPanel uRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        uRow.add(new JLabel("Utilizator Nou: "));
        userField = new JTextField(15);
        uRow.add(userField);
        container.add(uRow);

        JPanel pRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pRow.add(new JLabel("Parolă Nouă:   "));
        passField = new JPasswordField(15);
        pRow.add(passField);
        container.add(pRow);

        JPanel bRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton regBtn = new JButton("Înregistrare");
        regBtn.setPreferredSize(new Dimension(120, 35));
        bRow.add(regBtn);
        container.add(bRow);

        add(container);

        regBtn.addActionListener(e -> {
            String user = userField.getText();
            String pass = new String(passField.getPassword());

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Te rugăm să completezi toate câmpurile!");
                return;
            }

            if (Main.registerUser(user, pass)) {
                JOptionPane.showMessageDialog(this, "Cont creat cu succes! Acum te poți loga.");
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Eroare: Utilizatorul există deja sau conexiunea a eșuat.");
            }
        });
    }
}