package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ClaimItemPage {
    private int userId;

    public ClaimItemPage(int userId) {
        this.userId = userId;

        JFrame frame = new JFrame("✅ Claim Found Item");
        frame.setSize(450, 350);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JTextField itemIdField = new JTextField();
        JTextArea verificationDetailsArea = new JTextArea(3, 20);
        JScrollPane scrollPane = new JScrollPane(verificationDetailsArea);

        JButton claimButton = new JButton("🚀 Submit Claim");
        JButton backButton = new JButton("🔙 Back");

        formPanel.add(new JLabel("🔢 Item ID:"));
        formPanel.add(itemIdField);
        formPanel.add(new JLabel("📝 Verification Info:"));
        formPanel.add(scrollPane);
        formPanel.add(new JLabel(""));
        formPanel.add(claimButton);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(backButton);

        frame.add(formPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // 🔁 Submit claim to server
        claimButton.addActionListener(e -> {
            String itemId = itemIdField.getText().trim();
            String verification = verificationDetailsArea.getText().trim();

            if (itemId.isEmpty() || verification.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "❗Please fill in all fields.");
                return;
            }

            try {
                URL url = new URL("http://localhost/lostfound/submit_claim.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "item_id=" + URLEncoder.encode(itemId, StandardCharsets.UTF_8) +
                                  "&verification=" + URLEncoder.encode(verification, StandardCharsets.UTF_8) +
                                  "&reporter_id=" + userId;

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes());
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                if (response.toString().contains("success")) {
                    JOptionPane.showMessageDialog(frame, "✅ Claim submitted successfully!");
                    frame.dispose();
                    new ReporterDashboardPage(userId);
                } else {
                    JOptionPane.showMessageDialog(frame, "❌ Failed to submit claim:\n" + response);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "❌ Connection to server failed.");
            }
        });

        // ⬅️ Back button
        backButton.addActionListener(e -> {
            frame.dispose();
            new ReporterDashboardPage(userId);
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
