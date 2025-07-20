package project_lostfound;

import javax.swing.*;
import java.awt.*;

public class ReporterDashboardPage {

    private JFrame frame;
    private int userId;

    private static final Color BG_COLOR = new Color(245, 235, 224); // Nude background
    private static final Color BTN_COLOR = new Color(181, 136, 99); // Brown buttons

    public ReporterDashboardPage(int userId) {
        this.userId = userId;

        frame = new JFrame("🎒 Reporter Dashboard - Lost & Found");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(20, 20));
        frame.getContentPane().setBackground(BG_COLOR);

        // 🌟 Heading
        JLabel heading = new JLabel("Welcome, Reporter!", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI Emoji", Font.BOLD, 28));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));

        // Panel for buttons (6 rows now)
        JPanel buttonPanel = new JPanel(new GridLayout(6, 1, 15, 15));
        buttonPanel.setBackground(BG_COLOR);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 150, 20, 150));

        // Create Buttons with Emojis
        JButton reportLostButton = createButton("🛑 Report Lost Item");
        JButton reportFoundButton = createButton("📦 Report Found Item");
        JButton browseButton = createButton("🔍 Browse Found Items");
        JButton viewMyClaimsButton = createButton("📋 My Claim Requests");
        JButton inboxButton = createButton("📨 My Inbox");
        JButton backButton = createButton("⬅ Back to Main Page");

        // Add Actions
        reportLostButton.addActionListener(e -> {
            frame.dispose();
            new ReportLostItemPage(userId);
        });

        reportFoundButton.addActionListener(e -> {
            frame.dispose();
            new ReportFoundItemPage(userId);
        });

        browseButton.addActionListener(e -> {
            frame.dispose();
            new BrowseFoundItemsPage(userId);
        });

        viewMyClaimsButton.addActionListener(e -> {
            frame.dispose();
            new MyClaimsPage(userId);
        });

        inboxButton.addActionListener(e -> {
            frame.dispose();
            new UserInboxPage(userId);  // Open the new inbox page
        });

        backButton.addActionListener(e -> {
            frame.dispose();
            new MainPage();
        });

        // Add Buttons to Panel
        buttonPanel.add(reportLostButton);
        buttonPanel.add(reportFoundButton);
        buttonPanel.add(browseButton);
        buttonPanel.add(viewMyClaimsButton);
        buttonPanel.add(inboxButton);
        buttonPanel.add(backButton);

        // Add components to Frame
        frame.add(heading, BorderLayout.NORTH);
        frame.add(buttonPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Reusable button styling method
    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
        button.setBackground(BTN_COLOR);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(300, 60));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        new ReporterDashboardPage(101); // Test with sample user ID
    }
}
