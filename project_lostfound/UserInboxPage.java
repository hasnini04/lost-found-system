package project_lostfound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * UserInboxPage
 * -----------------
 * Displays all messages sent by admin to this reporter.
 * Messages are fetched from a PHP endpoint (e.g., get_messages.php?userId=XX).
 */
public class UserInboxPage extends JFrame {

    private int userId;
    private JTable messageTable;
    private DefaultTableModel tableModel;

    public UserInboxPage(int userId) {
        this.userId = userId;
        setTitle("📨 My Inbox");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout
        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        contentPane.setBackground(new Color(245, 235, 224));
        setContentPane(contentPane);

        JLabel heading = new JLabel("Your Messages", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        contentPane.add(heading, BorderLayout.NORTH);

        // Table for messages
        String[] columns = {"Date", "Message"};
        tableModel = new DefaultTableModel(columns, 0);
        messageTable = new JTable(tableModel);
        messageTable.setRowHeight(30);
        JScrollPane scrollPane = new JScrollPane(messageTable);
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Back button
        JButton backBtn = new JButton("⬅ Back");
        backBtn.setBackground(new Color(181, 136, 99));
        backBtn.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
        backBtn.addActionListener(e -> {
            dispose();
            new ReporterDashboardPage(userId);
        });
        contentPane.add(backBtn, BorderLayout.SOUTH);

        // Load messages from server
        loadMessages();

        setVisible(true);
    }

    private void loadMessages() {
        try {
            // Replace with your PHP endpoint
            URL url = new URL("http://localhost/lostfound/get_messages.php?userId=" + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            tableModel.setRowCount(0); // clear table

            while ((inputLine = in.readLine()) != null) {
                // For simplicity, assume server returns lines like "2025-07-19|Hello there!"
                String[] parts = inputLine.split("\\|", 2);
                if (parts.length == 2) {
                    tableModel.addRow(new Object[]{parts[0], parts[1]});
                }
            }
            in.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load messages: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserInboxPage(4)); // test with user ID 4
    }
}
