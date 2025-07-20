package project_lostfound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;


public class LostItemsForm extends JFrame {

    // ------------------------------------------------------------------
    // Theme palette
    // ------------------------------------------------------------------
    private static final Color BG_BEIGE       = new Color(0xF5,0xE8,0xDA);
    private static final Color PANEL_IVORY    = new Color(0xFF,0xFB,0xF5);
    private static final Color BORDER_TAN     = new Color(0xC9,0xAA,0x88);
    private static final Color ACCENT_BROWN   = new Color(0x8B,0x5E,0x3C);
    private static final Color ACCENT_BROWN_D = new Color(0x5C,0x3B,0x1E);
    private static final Color THUMB_BG       = new Color(245,245,245);
    private static final Color THUMB_BORDER   = new Color(220,220,220);

    // Fonts
    private static final java.awt.Font TITLE_FONT = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 22);
    private static final java.awt.Font LABEL_FONT = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 15);
    private static final java.awt.Font VALUE_FONT = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 15);
    private static final java.awt.Font BTN_FONT   = new java.awt.Font("SansSerif", java.awt.Font.BOLD, 15);

    // ------------------------------------------------------------------
    // Config
    // ------------------------------------------------------------------
    private static final String BASE_ENDPOINT =
            "http://localhost/lostfound/admin_fetch_datalost.php?action=lost_items";
    private static final String BASE_URL =
            "http://localhost/lostfound/";
    private static final String MATCH_ENDPOINT =
            "http://localhost/lostfound/send_match_note.php";

    private static final int THUMB_W = 100;
    private static final int THUMB_H = 100;

    private final User currentUser;

    // UI
    private JPanel itemListPanel;
    private JTextField searchField;

    public LostItemsForm(User user) {
        this.currentUser = user;

        setTitle("\uD83D\uDCE6 Lost Items");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        // ---------- Title strip ----------
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("All Lost Item Reports", SwingConstants.CENTER);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(Color.BLACK);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        // ---------- Top search bar ----------
        JPanel topPanel = new JPanel(new BorderLayout(8,8));
        topPanel.setOpaque(true);
        topPanel.setBackground(PANEL_IVORY);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,BORDER_TAN),
                new EmptyBorder(8,8,8,8)));

        searchField = new JTextField();
        searchField.setFont(VALUE_FONT);
        searchField.setForeground(Color.BLACK);
        searchField.setBackground(Color.WHITE);
        searchField.setToolTipText("Search by item name or description...");

        JButton searchBtn = new JButton("\uD83D\uDD0D Search");
        styleButton(searchBtn);

        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.BEFORE_FIRST_LINE);

        // ---------- Center scroll area ----------
        itemListPanel = new JPanel();
        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));
        itemListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(itemListPanel);
        scrollPane.getViewport().setBackground(BG_BEIGE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // ---------- Bottom nav ----------
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(BG_BEIGE);
        JButton backBtn = new JButton("\u2B05 Back");
        styleButton(backBtn);
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
        searchBtn.addActionListener(e -> reload());
        searchField.addActionListener(e -> reload());
        backBtn.addActionListener(e -> {
            dispose();
            new AdminDashboardPage(currentUser);
        });

        // Initial load
        reload();
        setVisible(true);
    }

    private void reload() {
        loadItems(searchField.getText().trim());
    }

    // ------------------------------------------------------------------
    // Data fetch + render
    // ------------------------------------------------------------------
    private void loadItems(String keyword) {
        itemListPanel.removeAll();

        new SwingWorker<List<Map<String,String>>, Void>() {
            @Override
            protected List<Map<String,String>> doInBackground() throws Exception {
                String json = fetchJson(BASE_ENDPOINT);
                return parseItems(json);
            }
            @Override
            protected void done() {
                try {
                    List<Map<String,String>> maps = get();
                    keywordRender(maps, keyword);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(LostItemsForm.this,
                            "\u274C Failed to load items:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private void keywordRender(List<Map<String,String>> items, String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        int shown = 0;
        for (Map<String,String> m : items) {
            String name = m.getOrDefault("item_name", "");
            String desc = m.getOrDefault("description", "");
            if (!keyword.isEmpty()) {
                if (!(name.toLowerCase(Locale.ROOT).contains(lower)
                        || desc.toLowerCase(Locale.ROOT).contains(lower))) {
                    continue;
                }
            }
            itemListPanel.add(buildItemCard(m));
            itemListPanel.add(Box.createVerticalStrut(8));
            shown++;
        }
        if (shown == 0) {
            JLabel none = new JLabel("\u274C No items match.");
            none.setForeground(Color.RED);
            none.setFont(VALUE_FONT);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemListPanel.add(none);
        }
        itemListPanel.revalidate();
        itemListPanel.repaint();
    }

    // ------------------------------------------------------------------
    // Card builder
    // ------------------------------------------------------------------
    private JPanel buildItemCard(Map<String,String> m) {
        JPanel outer = new JPanel(new BorderLayout(10,10));
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(8,8,8,8)));
        outer.setBackground(PANEL_IVORY);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Hover highlight
        outer.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { outer.setBackground(Color.WHITE); }
            @Override public void mouseExited (MouseEvent e) { outer.setBackground(PANEL_IVORY); }
        });

        // Image thumbnail
        String imgUrl = bestImageUrl(m);
        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(THUMB_W, THUMB_H));
        imgLabel.setOpaque(true);
        imgLabel.setBackground(THUMB_BG);
        imgLabel.setBorder(BorderFactory.createLineBorder(THUMB_BORDER));

        if (imgUrl != null) {
            loadThumbnailAsync(imgUrl, imgLabel);
            imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imgLabel.setToolTipText("Click to view image");
            imgLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    showFullImage(imgUrl, m.getOrDefault("item_name", "Lost Item"));
                }
            });
        } else {
            imgLabel.setText("No Image");
            imgLabel.setForeground(Color.GRAY);
        }
        outer.add(imgLabel, BorderLayout.WEST);

        // Text details
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(labelRow("\uD83D\uDCE6 Item: ",       m.getOrDefault("item_name", "-")));
        textPanel.add(labelRow("\uD83D\uDCDD Description: ",m.getOrDefault("description", "-")));
        textPanel.add(labelRow("\uD83D\uDCCD Location Lost: ",m.getOrDefault("location_lost", "-")));
        textPanel.add(labelRow("\uD83D\uDCC5 Date Lost: ",  m.getOrDefault("date_lost", "-")));
        textPanel.add(labelRow("\uD83D\uDC64 Reporter: ",   m.getOrDefault("reporter_name", "-")));
        textPanel.add(labelRow("\u23F0 Reported On: ",      m.getOrDefault("created_at", "-")));
        outer.add(textPanel, BorderLayout.CENTER);

        // ---- Action buttons ----
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionPanel.setOpaque(false);

        JButton matchBtn = new JButton("Match");
        styleButton(matchBtn);
        matchBtn.addActionListener(e -> sendMatchMessage(m));

        actionPanel.add(matchBtn);
        outer.add(actionPanel, BorderLayout.SOUTH);

        return outer;
    }

    private JPanel labelRow(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(Color.BLACK);

        JLabel val = new JLabel(value);
        val.setFont(VALUE_FONT);
        val.setForeground(Color.BLACK);

        p.add(lbl);
        p.add(val);
        return p;
    }

    // ------------------------------------------------------------------
    // Match message sender
    // ------------------------------------------------------------------
    private void sendMatchMessage(Map<String,String> item) {
        String reporterId = item.getOrDefault("reporter_id", "");
        String itemId = item.getOrDefault("id", "");
        if (reporterId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠ Cannot send match note (missing reporter_id).");
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String message = "Your item might be found. Please check the browser to claim it.";
                String params = "reporter_id=" + reporterId +
                        "&item_id=" + itemId +
                        "&message=" + java.net.URLEncoder.encode(message, "UTF-8");

                URL url = new URL(MATCH_ENDPOINT);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));

                try (InputStream is = conn.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    JOptionPane.showMessageDialog(LostItemsForm.this,
                            "✅ Match note sent!\nServer: " + result);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(LostItemsForm.this,
                            "❌ Failed to send match note:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Fetch JSON
    // ------------------------------------------------------------------
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

    // ------------------------------------------------------------------
    // JSON parsing
    // ------------------------------------------------------------------
    private List<Map<String,String>> parseItems(String json) {
        List<Map<String,String>> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        if (!json.contains("\"status\":\"success\"")) return list;
        int dataIdx = json.indexOf("\"data\":");
        if (dataIdx < 0) return list;
        int startArr = json.indexOf('[', dataIdx);
        if (startArr < 0) return list;
        int endArr = findMatchingBracket(json, startArr, '[', ']');
        if (endArr < 0) return list;
        String arr = json.substring(startArr+1, endArr).trim();
        if (arr.isEmpty()) return list;
        int idx = 0;
        while (idx < arr.length()) {
            int objStart = arr.indexOf('{', idx);
            if (objStart < 0) break;
            int objEnd = findMatchingBracket(arr, objStart, '{', '}');
            if (objEnd < 0) break;
            String obj = arr.substring(objStart+1, objEnd);
            list.add(parseObject(obj));
            idx = objEnd + 1;
        }
        return list;
    }

    private Map<String,String> parseObject(String obj) {
        Map<String,String> m = new LinkedHashMap<>();
        Pattern p = Pattern.compile("\\\"(.*?)\\\"\\s*:\\s*(\\\"(.*?)\\\"|null|[0-9.]+)");
        Matcher mt = p.matcher(obj);
        while (mt.find()) {
            String key = mt.group(1);
            String rawVal = mt.group(2);
            String val;
            if (rawVal == null) {
                val = null;
            } else if (rawVal.startsWith("\"")) {
                val = unescape(mt.group(3));
            } else if ("null".equals(rawVal)) {
                val = null;
            } else {
                val = rawVal; // number
            }
            m.put(key, val);
        }
        return m;
    }

    private String unescape(String s) {
        if (s == null) return null;
        return s.replace("\\\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static int findMatchingBracket(String s, int openIdx, char open, char close) {
        int depth = 0; boolean inStr = false;
        for (int i=openIdx; i<s.length(); i++) {
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

    // ------------------------------------------------------------------
    // Image helpers
    // ------------------------------------------------------------------
    private String bestImageUrl(Map<String,String> m) {
        String full = m.get("image_url_full");
        if (full != null && !full.isEmpty() && !"null".equalsIgnoreCase(full)) return full;

        String rel = m.get("image_url");
        if (rel != null && !rel.isEmpty() && !"null".equalsIgnoreCase(rel)) {
            rel = rel.replace('\\', '/');
            if (rel.startsWith("/")) rel = rel.substring(1);
            return BASE_URL + rel;
        }
        return null;
    }

    private void loadThumbnailAsync(String url, JLabel label) {
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() throws Exception {
                BufferedImage img = ImageIO.read(new URL(url));
                if (img == null) return null;
                return new ImageIcon(scaleImage(img, THUMB_W, THUMB_H));
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setText(null);
                        label.setIcon(icon);
                    } else {
                        label.setText("(bad img)");
                        label.setForeground(Color.GRAY);
                    }
                } catch (Exception ex) {
                    label.setText("(load err)");
                    label.setForeground(Color.GRAY);
                }
            }
        }.execute();
    }

    private void showFullImage(String url, String title) {
        JDialog dlg = new JDialog(this, title, true);
        dlg.setLayout(new BorderLayout());
        JLabel lbl = new JLabel("Loading...", SwingConstants.CENTER);
        lbl.setForeground(Color.BLACK);
        dlg.add(new JScrollPane(lbl), BorderLayout.CENTER);
        dlg.setSize(600, 400);
        dlg.setLocationRelativeTo(this);

        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() throws Exception {
                BufferedImage img = ImageIO.read(new URL(url));
                if (img == null) return null;
                return new ImageIcon(img);
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        lbl.setText(null);
                        lbl.setIcon(icon);
                    } else {
                        lbl.setText("Failed to load image");
                    }
                } catch (Exception ex) {
                    lbl.setText("Error: " + ex.getMessage());
                }
            }
        }.execute();

        dlg.setVisible(true);
    }

    private Image scaleImage(BufferedImage src, int w, int h) {
        Image scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return out;
    }

    // ------------------------------------------------------------------
    // Buttons
    // ------------------------------------------------------------------
    private void styleButton(JButton btn) {
        btn.setFont(BTN_FONT);
        btn.setForeground(Color.BLACK);
        btn.setBackground(ACCENT_BROWN);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createLineBorder(ACCENT_BROWN_D, 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 32));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(ACCENT_BROWN); }
        });
    }

}