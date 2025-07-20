package project_lostfound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class UserMessagesPage extends JFrame {

    private static final Color BG_BEIGE       = new Color(0xF5, 0xE8, 0xDA);
    private static final Color PANEL_IVORY    = new Color(0xFF, 0xFB, 0xF5);
    private static final Color BORDER_TAN     = new Color(0xC9, 0xAA, 0x88);
    private static final Color ACCENT_BROWN   = new Color(0x8B, 0x5E, 0x3C);
    private static final Color ACCENT_BROWN_D = new Color(0x5C, 0x3B, 0x1E);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font MSG_FONT   = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BTN_FONT   = new Font("SansSerif", Font.BOLD, 15);

    private static final String BASE_ENDPOINT = "http://localhost/lostfound/fetch_messages.php?reporter_id=";

    private final User currentUser;
    private JPanel messageListPanel;

    public UserMessagesPage(User user) {
        this.currentUser = user;

        setTitle("📩 Notifications");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("Messages for " + user.getName(), SwingConstants.CENTER);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(Color.BLACK);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        // Scrollable message area
        messageListPanel = new JPanel();
        messageListPanel.setLayout(new BoxLayout(messageListPanel, BoxLayout.Y_AXIS));
        messageListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(messageListPanel);
        scrollPane.getViewport().setBackground(BG_BEIGE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Bottom nav
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(BG_BEIGE);
        JButton closeBtn = new JButton("✖ Close");
        styleButton(closeBtn);
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Initial load
        loadMessages();
        setVisible(true);
    }

    // Load messages from backend
    private void loadMessages() {
        messageListPanel.removeAll();
        new SwingWorker<List<Map<String, String>>, Void>() {
            @Override
            protected List<Map<String, String>> doInBackground() throws Exception {
                String json = fetchJson(BASE_ENDPOINT + currentUser.getId());
                return parseMessages(json);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, String>> messages = get();
                    if (messages.isEmpty()) {
                        JLabel none = new JLabel("No messages found.");
                        none.setFont(MSG_FONT);
                        none.setForeground(Color.GRAY);
                        none.setAlignmentX(Component.LEFT_ALIGNMENT);
                        messageListPanel.add(none);
                    } else {
                        for (Map<String, String> msg : messages) {
                            messageListPanel.add(buildMessageCard(msg));
                            messageListPanel.add(Box.createVerticalStrut(8));
                        }
                    }
                    messageListPanel.revalidate();
                    messageListPanel.repaint();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(UserMessagesPage.this,
                            "❌ Failed to load messages:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private JPanel buildMessageCard(Map<String, String> msg) {
        JPanel outer = new JPanel(new BorderLayout(10,10));
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                new EmptyBorder(8,8,8,8)));
        outer.setBackground(PANEL_IVORY);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel msgLbl = new JLabel("<html>" + msg.getOrDefault("message", "-") + "</html>");
        msgLbl.setFont(MSG_FONT);
        msgLbl.setForeground(Color.BLACK);
        outer.add(msgLbl, BorderLayout.CENTER);

        JLabel timeLbl = new JLabel(msg.getOrDefault("created_at", ""));
        timeLbl.setFont(new Font("SansSerif", Font.ITALIC, 12));
        timeLbl.setForeground(Color.DARK_GRAY);
        outer.add(timeLbl, BorderLayout.SOUTH);

        return outer;
    }

    private String fetchJson(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) baos.write(buf, 0, r);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private List<Map<String, String>> parseMessages(String json) {
        List<Map<String, String>> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        if (!json.contains("\"status\":\"success\"")) return list;
        int dataIdx = json.indexOf("\"data\":");
        if (dataIdx < 0) return list;
        int startArr = json.indexOf('[', dataIdx);
        if (startArr < 0) return list;
        int endArr = findMatchingBracket(json, startArr, '[', ']');
        if (endArr < 0) return list;
        String arr = json.substring(startArr + 1, endArr).trim();
        if (arr.isEmpty()) return list;
        int idx = 0;
        while (idx < arr.length()) {
            int objStart = arr.indexOf('{', idx);
            if (objStart < 0) break;
            int objEnd = findMatchingBracket(arr, objStart, '{', '}');
            if (objEnd < 0) break;
            String obj = arr.substring(objStart + 1, objEnd);
            list.add(parseObject(obj));
            idx = objEnd + 1;
        }
        return list;
    }

    private Map<String, String> parseObject(String obj) {
        Map<String, String> m = new LinkedHashMap<>();
        String[] parts = obj.split(",");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                m.put(key, val);
            }
        }
        return m;
    }

    private int findMatchingBracket(String s, int openIdx, char open, char close) {
        int depth = 0; boolean inStr = false;
        for (int i = openIdx; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') inStr = !inStr;
            if (inStr) continue;
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private void styleButton(AbstractButton b) {
        b.setFont(BTN_FONT);
        b.setForeground(Color.BLACK);
        b.setBackground(ACCENT_BROWN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN_D),
                BorderFactory.createEmptyBorder(6,14,6,14)));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN); }
        });
    }
}
