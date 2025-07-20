package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class ManageUsersForm extends JFrame {
    private User adminUser;
    private JPanel usersPanel;
    private static final String API_URL = "http://localhost/lostfound/get_users.php";

    // Theme Colors
    private static final Color BG_BEIGE = new Color(0xF5E8DA);
    private static final Color PANEL_IVORY = new Color(0xFFFBF5);
    private static final Color BORDER_TAN = new Color(0xC9AA88);
    private static final Color ACCENT_BROWN = new Color(0x8B5E3C);
    private static final Color ACCENT_BROWN_DARK = new Color(0x5C3B1E);

    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font TEXT_FONT = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    public ManageUsersForm(User adminUser) {
        this.adminUser = adminUser;
        initialize();
    }

    private void initialize() {
        setTitle("Manage Users");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(ACCENT_BROWN);
        headerPanel.setPreferredSize(new Dimension(600, 60));
        JLabel headerLabel = new JLabel("Manage Users", SwingConstants.CENTER);
        headerLabel.setFont(HEADER_FONT);
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        // Scrollable Users Panel
        usersPanel = new JPanel();
        usersPanel.setLayout(new BoxLayout(usersPanel, BoxLayout.Y_AXIS));
        usersPanel.setBackground(BG_BEIGE);

        JScrollPane scrollPane = new JScrollPane(usersPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_BEIGE);
        add(scrollPane, BorderLayout.CENTER);

        fetchAndDisplayUsers();
        setVisible(true);
    }

    private void fetchAndDisplayUsers() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseText = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                responseText.append(line);
            }
            reader.close();

            String json = responseText.toString();

            if (json.contains("\"status\":\"success\"")) {
                String usersJson = json.split("\"users\":\\[")[1].split("]}")[0] + "}";
                String[] userJsonArray = usersJson.split("\\},\\{");

                usersPanel.removeAll();

                for (String userRaw : userJsonArray) {
                    Map<String, String> map = parseUserJson(userRaw);

                    JPanel userCard = new JPanel(new BorderLayout(10,10));
                    userCard.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER_TAN),
                            BorderFactory.createEmptyBorder(10,10,10,10)));
                    userCard.setBackground(PANEL_IVORY);
                    userCard.setMaximumSize(new Dimension(550, 100));

                    String info = "<html><b>" + map.get("name") + "</b><br>" +
                            "Email: " + map.get("email") + "<br>" +
                            "Matric: " + map.get("matric") + ", Faculty: " +
                            map.get("faculty") + "</html>";
                    JLabel infoLabel = new JLabel(info);
                    infoLabel.setFont(TEXT_FONT);

                    JPanel leftPanel = new JPanel(new BorderLayout());
                    leftPanel.setBackground(PANEL_IVORY);
                    leftPanel.add(infoLabel, BorderLayout.CENTER);

                    JPanel rightPanel = new JPanel(new GridLayout(2, 1, 5, 5));
                    rightPanel.setBackground(PANEL_IVORY);
                    JButton editBtn = createStyledButton("✏️ Edit");
                    JButton deleteBtn = createStyledButton("🗑️ Delete");

                    int userId = Integer.parseInt(map.get("id"));
                    String name = map.get("name");
                    String email = map.get("email");
                    String matric = map.get("matric");
                    String faculty = map.get("faculty");

                    editBtn.addActionListener(e -> {
                        new EditUserForm(userId, name, email, matric, faculty);
                    });

                    deleteBtn.addActionListener(e -> {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                "Are you sure you want to delete " + name + "?",
                                "Confirm Delete", JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            deleteUser(userId);
                        }
                    });

                    rightPanel.add(editBtn);
                    rightPanel.add(deleteBtn);

                    userCard.add(leftPanel, BorderLayout.CENTER);
                    userCard.add(rightPanel, BorderLayout.EAST);

                    usersPanel.add(userCard);
                    usersPanel.add(Box.createVerticalStrut(10));
                }

                usersPanel.revalidate();
                usersPanel.repaint();

            } else {
                JOptionPane.showMessageDialog(this, "❌ Failed to load users.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "❌ Error loading users:\n" + e.getMessage());
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BUTTON_FONT);
        button.setForeground(Color.BLACK); // <-- Changed to black text
        button.setBackground(ACCENT_BROWN);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8,12,8,12));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ACCENT_BROWN_DARK);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ACCENT_BROWN);
            }
        });

        return button;
    }

    private Map<String, String> parseUserJson(String raw) {
        Map<String, String> map = new HashMap<>();
        raw = raw.replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = raw.split(",");

        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }

        // Ensure nulls are readable
        for (String key : new String[]{"matric", "faculty"}) {
            if (!map.containsKey(key)) map.put(key, "N/A");
        }

        return map;
    }

    private void deleteUser(int userId) {
        try {
            URL url = new URL("http://localhost/lostfound/user/delete_user.php?id=" + userId);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String response = reader.readLine();
            reader.close();

            if (response.contains("success")) {
                JOptionPane.showMessageDialog(this, "✅ User deleted.");
                fetchAndDisplayUsers(); // refresh
            } else {
                JOptionPane.showMessageDialog(this, "❌ Failed to delete.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error deleting user:\n" + e.getMessage());
        }
    }
}
