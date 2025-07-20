package project_lostfound;

import javax.swing.*;
import java.awt.*;

public class AdminDashboardPage extends JFrame {

    private User adminUser;
    private static final Color BG_COLOR = new Color(245, 235, 224); // Nude
    private static final Color BTN_COLOR = new Color(181, 136, 99); // Brown

    public AdminDashboardPage(User adminUser) {
        this.adminUser = adminUser;

        setTitle("Admin Dashboard");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);

        // Heading
        JLabel heading = new JLabel("Welcome, Admin", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI Emoji", Font.BOLD, 28));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(heading, BorderLayout.NORTH);

        // Buttons Panel
        JPanel panel = new JPanel(new GridLayout(6, 1, 15, 15)); // Changed 5 -> 6
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 100));

        JButton viewLostItemsButton = createButton("📦  View Lost Items");
        JButton viewFoundItemsButton = createButton("👜  View Found Items");
        JButton approveClaimsButton = createButton("✅  Approve/Reject Claims");
        JButton manageUsersButton = createButton("👥  Manage Users");
        JButton sendMessagesButton = createButton("📨  Send Messages");
        JButton logoutButton = createButton("🚪  Logout");

        // Add listeners
        viewLostItemsButton.addActionListener(e -> new LostItemsForm(adminUser));
        viewFoundItemsButton.addActionListener(e -> new FoundItemsForm(adminUser));
        approveClaimsButton.addActionListener(e -> new AdminClaimsPanel());
        manageUsersButton.addActionListener(e -> new ManageUsersForm(adminUser));
        sendMessagesButton.addActionListener(e -> new AdminMessagesPage(adminUser));
        logoutButton.addActionListener(e -> {
            dispose();
            new MainPage();
        });

        // Add buttons to panel
        panel.add(viewLostItemsButton);
        panel.add(viewFoundItemsButton);
        panel.add(approveClaimsButton);
        panel.add(manageUsersButton);
        panel.add(sendMessagesButton);
        panel.add(logoutButton);

        add(panel, BorderLayout.CENTER);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18)); // Supports emoji
        button.setBackground(BTN_COLOR);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(300, 70));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        new AdminDashboardPage(new User());
    }
}
