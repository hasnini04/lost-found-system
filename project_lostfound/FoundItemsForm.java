package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import javax.imageio.ImageIO;
import java.util.List;

/**
 * FoundItemsForm (Admin, Themed, Black Text)
 * ------------------------------------------
 * Admin-facing list of all FOUND items.
 *
 * *** FUNCTIONALITY UPDATED ***
 *  • Search "claimed" to display only claimed items
 *  • Search "unclaimed" to display only unclaimed items
 *  • Search any keyword to match item_name or description
 *
 * *** VISUAL ***
 *  • Beige / brown project theme
 *  • Black fonts everywhere
 *  • Title strip
 *  • Themed search bar + search button
 *  • Themed cards with mild hover
 */
public class FoundItemsForm extends JFrame {

    private static final Color BG_BEIGE       = new Color(0xF5,0xE8,0xDA);
    private static final Color PANEL_IVORY    = new Color(0xFF,0xFB,0xF5);
    private static final Color BORDER_TAN     = new Color(0xC9,0xAA,0x88);
    private static final Color ACCENT_BROWN   = new Color(0x8B,0x5E,0x3C);
    private static final Color ACCENT_BROWN_D = new Color(0x5C,0x3B,0x1E);
    private static final Color THUMB_BG       = new Color(245,245,245);
    private static final Color THUMB_BORDER   = new Color(220,220,220);

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 15);
    private static final Font VALUE_FONT = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font BTN_FONT   = new Font("SansSerif", Font.BOLD, 15);

    private User currentUser;
    private JPanel itemListPanel;
    private JTextField searchField;

    private static final String BASE_URL = "http://localhost/lostfound/";
    private static final int THUMB_W = 100;
    private static final int THUMB_H = 100;

    public FoundItemsForm(User user) {
        this.currentUser = user;

        setTitle("👜 Found Items");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("All Found Item Reports", SwingConstants.CENTER);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(Color.BLACK);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        JPanel topPanel = new JPanel(new BorderLayout(8,8));
        topPanel.setOpaque(true);
        topPanel.setBackground(PANEL_IVORY);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,BORDER_TAN),
                new javax.swing.border.EmptyBorder(8,8,8,8)));

        searchField = new JTextField();
        searchField.setFont(VALUE_FONT);
        searchField.setForeground(Color.BLACK);
        searchField.setBackground(Color.WHITE);
        searchField.setToolTipText("Search by item name, description, 'claimed' or 'unclaimed'");

        JButton searchBtn = new JButton("🔍 Search");
        styleButton(searchBtn);

        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchBtn, BorderLayout.EAST);
        add(topPanel, BorderLayout.BEFORE_FIRST_LINE);

        itemListPanel = new JPanel();
        itemListPanel.setLayout(new BoxLayout(itemListPanel, BoxLayout.Y_AXIS));
        itemListPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(itemListPanel);
        scrollPane.getViewport().setBackground(BG_BEIGE);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> reload());
        searchField.addActionListener(e -> reload());

        reload();
        setVisible(true);
    }

    private void reload() {
        String keyword = searchField.getText().trim();
        loadItems(keyword);
    }

    private void loadItems(String keyword) {
        itemListPanel.removeAll();

        new SwingWorker<List<Map<String,String>>, Void>() {
            @Override
            protected List<Map<String,String>> doInBackground() throws Exception {
                String json = fetchJson("http://localhost/lostfound/admin_fetch_found.php");
                return parseItems(json);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String,String>> items = get();
                    keywordRender(items, keyword);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(FoundItemsForm.this,
                            "❌ Failed to load items:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    /** Search & filter logic */
    private void keywordRender(List<Map<String,String>> items, String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        int shown = 0;

        for (Map<String,String> item : items) {
            String name = item.getOrDefault("item_name", "");
            String desc = item.getOrDefault("description", "");
            String status = item.getOrDefault("status", "").toLowerCase(Locale.ROOT);

            if (lower.equals("claimed") && !status.equals("claimed")) continue;
            if (lower.equals("unclaimed") && !status.equals("unclaimed")) continue;

            if (!keyword.isEmpty() && !lower.equals("claimed") && !lower.equals("unclaimed")) {
                if (!(name.toLowerCase(Locale.ROOT).contains(lower)
                    || desc.toLowerCase(Locale.ROOT).contains(lower))) {
                    continue;
                }
            }

            itemListPanel.add(buildItemCard(item));
            itemListPanel.add(Box.createVerticalStrut(8));
            shown++;
        }

        if (shown == 0) {
            JLabel none = new JLabel("❌ No items match.");
            none.setForeground(Color.RED);
            none.setFont(VALUE_FONT);
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemListPanel.add(none);
        }

        itemListPanel.revalidate();
        itemListPanel.repaint();
    }

    private JPanel buildItemCard(Map<String,String> item) {
        JPanel outer = new JPanel(new BorderLayout(10,10));
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(8,8,8,8)));
        outer.setBackground(PANEL_IVORY);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        outer.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { outer.setBackground(Color.WHITE); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { outer.setBackground(PANEL_IVORY); }
        });

        String relPath = item.get("image_path");
        JLabel imgLabel = new JLabel();
        imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imgLabel.setPreferredSize(new Dimension(THUMB_W, THUMB_H));
        imgLabel.setOpaque(true);
        imgLabel.setBackground(THUMB_BG);
        imgLabel.setBorder(BorderFactory.createLineBorder(THUMB_BORDER));

        if (relPath != null && !relPath.isEmpty() && !"null".equalsIgnoreCase(relPath)) {
            String norm = relPath.replace('\\','/');
            String fullUrl = BASE_URL + norm;
            loadThumbnailAsync(fullUrl, imgLabel);
            imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imgLabel.setToolTipText("Click to view image");
            imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    showFullImage(fullUrl, item.getOrDefault("item_name", "Item"));
                }
            });
        } else {
            imgLabel.setText("No Image");
            imgLabel.setForeground(Color.GRAY);
        }
        outer.add(imgLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(labelRow("🔖 Name: ",            item.getOrDefault("item_name", "-")));
        textPanel.add(labelRow("📝 Description: ",     item.getOrDefault("description", "-")));
        textPanel.add(labelRow("📍 Location Found: ",  item.getOrDefault("location_found", "-")));
        textPanel.add(labelRow("📅 Date Found: ",      item.getOrDefault("date_found", "-")));
        textPanel.add(labelRow("✅ Status: ",          item.getOrDefault("status", "-")));
        outer.add(textPanel, BorderLayout.CENTER);

        return outer;
    }

    private JPanel labelRow(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(LABEL_FONT);
        l.setForeground(Color.BLACK);

        JLabel v = new JLabel(value);
        v.setFont(VALUE_FONT);
        v.setForeground(Color.BLACK);

        p.add(l); p.add(v);
        return p;
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
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

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
                val = rawVal;
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
        int depth = 0;
        boolean inStr = false;
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
                    lbl.setText("Failed to load image");
                }
            }
        }.execute();

        dlg.setVisible(true);
    }

    private Image scaleImage(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min((double)maxW / w, (double)maxH / h);
        if (scale >= 1.0) return src;
        int nw = (int)Math.round(w * scale);
        int nh = (int)Math.round(h * scale);
        Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return out;
    }

    private void styleButton(AbstractButton b) {
        b.setFont(BTN_FONT);
        b.setForeground(Color.BLACK);
        b.setBackground(ACCENT_BROWN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN_D),
                BorderFactory.createEmptyBorder(6,14,6,14)));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN); }
        });
    }
}
