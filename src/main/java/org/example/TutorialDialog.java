package org.example;

import javax.swing.*;
import java.awt.*;

public class TutorialDialog extends JDialog {
    public TutorialDialog(JFrame parent) {
        super(parent, "Ghid Descărcare Date Instagram", true);
        setSize(500, 450);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JEditorPane tutorialText = new JEditorPane();
        tutorialText.setContentType("text/html");
        tutorialText.setEditable(false);
        tutorialText.setText(
                "<html><body style='font-family: sans-serif; font-size: 11pt; color: #333;'>" +
                        "<h2 style='color: #E1306C;'>Cum obții fișierele JSON:</h2>" +
                        "<ol>" +
                        "<li>Intră pe Instagram -> <b>Settings</b> -> <b>Accounts Center</b> -> <b>Your information and permissions</b> -> <b>Export your information</b>.</li>" +
                        "<li>Apasă pr <b>Create export</b>.</li>" +
                        "<li>Alege contul tău de Instagram</li>" +
                        "<li>Alege <b>Export to device</b>.</li>" +
                        "<li>Selectează doar <b>Followers and Following</b> (pentru viteză).</li>" +
                        "<li style='color: #ff0000; font-weight: bold;'>IMPORTANT: La 'Format', bifează JSON în loc de HTML.</li>" +
                        "<li>La 'Date Range', alege <b>All Time</b>.</li>" +
                        "</ol>" +
                        "<p style='background-color: #f8f8f8; padding: 10px;'>" +
                        "<i>Notă: Procesarea durează de obicei în jur de <b>6 minute</b>. Vei primi un link de descărcare pe e-mail.</i></p>" +
                        "</body></html>"
        );

        panel.add(new JScrollPane(tutorialText), BorderLayout.CENTER);


        JButton btnClose = new JButton("Am înțeles, mergi la Dashboard");
        btnClose.addActionListener(e -> dispose());
        panel.add(btnClose, BorderLayout.SOUTH);

        add(panel);
    }
}