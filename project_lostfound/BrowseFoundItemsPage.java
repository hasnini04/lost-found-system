package project_lostfound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BrowseFoundItemsPage {

    private static final String PIPE_ENDPOINT     = "http://localhost/lostfound/get_found_items.php";
    private static final String BASE_URL          = "http://localhost/lostfound/";
    private static final int    DETAIL_IMG_MAX_W  = 400;
    private static final int    DETAIL_IMG_MAX_H  = 300;

    private static final Color BG_BEIGE       = new Color(0xF5,0xE8,0xDA);
    private static final Color PANEL_IVORY    = new Color(0xFF,0xFB,0xF5);
    private static final Color BORDER_TAN     = new Color(0xC9,0xAA,0x88);
    private static final Color ACCENT_BROWN   = new Color(0x8B,0x5E,0x3C);
    private static final Color ACCENT_BROWN_D = new Color(0x5C,0x3B,0x1E);
    private static final Color FIELD_BG       = Color.WHITE;

    private static final Font TITLE_FONT    = new Font("SansSerif", Font.BOLD, 22);
    private static final Font LABEL_FONT    = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BUTTON_FONT   = new Font("SansSerif", Font.BOLD, 16);
    private static final Font ITEM_BTN_FONT = new Font("SansSerif", Font.BOLD, 15);
    private static final Font TEXT_FONT     = new Font("SansSerif", Font.PLAIN, 15);

    private JFrame frame;
    private JPanel itemsPanel;
    private JTextField searchField;
    private final List<Item> allItems = new ArrayList<>();
    private final int userId;

    public BrowseFoundItemsPage(int userId) {
        this.userId = userId;

        frame = new JFrame("🔍 Browse Found Items");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(720, 600);
        frame.setLayout(new BorderLayout(10,10));
        frame.getContentPane().setBackground(BG_BEIGE);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("Browse Found Items", SwingConstants.CENTER);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new BorderLayout(8,0));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        searchPanel.setBackground(PANEL_IVORY);
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(LABEL_FONT);
        searchLbl.setForeground(Color.BLACK);

        searchField = new JTextField();
        searchField.setFont(TEXT_FONT);
        searchField.setToolTipText("Search item by keyword...");
        searchField.setBackground(FIELD_BG);
        searchField.setForeground(Color.BLACK);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(4,6,4,6)));

        JButton searchBtn = new JButton("Go");
        styleButton(searchBtn);
        searchBtn.setFont(BUTTON_FONT.deriveFont(Font.PLAIN, 14f));

        searchPanel.add(searchLbl, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);
        frame.add(searchPanel, BorderLayout.BEFORE_FIRST_LINE);

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(PANEL_IVORY);
        itemsPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

        JScrollPane scrollPane = new JScrollPane(itemsPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_TAN));
        frame.add(scrollPane, BorderLayout.CENTER);

        JButton backButton = new JButton("⬅ Back");
        styleButton(backButton);
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(BG_BEIGE);
        bottomPanel.add(backButton);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> filterItems());
        searchField.addActionListener(e -> filterItems());
        backButton.addActionListener(e -> {
            frame.dispose();
            new ReporterDashboardPage(userId);
        });

        fetchData();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void fetchData() {
        try {
            URL url = new URL(PIPE_ENDPOINT);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                allItems.clear();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 9) {
                        Item item = new Item();
                        try { item.id = Integer.parseInt(parts[0]); } catch (Exception ignored) {}
                        item.name        = parts[1];
                        item.description = parts[2];
                        item.location    = parts[3];
                        item.date        = parts[4];
                        item.status      = parts[5];
                        item.created     = parts[6];
                        item.imagePath   = parts[7];
                        item.claimStatus = parts[8]; // New claim status
                        allItems.add(item);
                    }
                }
            }

            filterItems();

        } catch (Exception ex) {
            itemsPanel.removeAll();
            JLabel error = new JLabel("⚠️ Failed to load items.");
            error.setForeground(Color.RED);
            error.setFont(LABEL_FONT);
            itemsPanel.add(error);
            itemsPanel.revalidate();
            itemsPanel.repaint();
            ex.printStackTrace();
        }
    }

    private void filterItems() {
        String keyword = searchField.getText().toLowerCase().trim();
        itemsPanel.removeAll();
        boolean foundAny = false;

        for (Item item : allItems) {
            String st = item.status == null ? "" : item.status.trim().toLowerCase();
            String claimSt = item.claimStatus == null ? "" : item.claimStatus.trim().toLowerCase();

            // Only skip items that are already claimed or approved
            if (st.equals("claimed") || claimSt.equals("approved")) {
                continue;
            }

            if (!(item.name.toLowerCase().contains(keyword) ||
                  item.description.toLowerCase().contains(keyword) ||
                  item.location.toLowerCase().contains(keyword))) {
                continue;
            }

            itemsPanel.add(buildItemRow(item));
            itemsPanel.add(Box.createVerticalStrut(10));
            foundAny = true;
        }

        if (!foundAny) {
            JLabel noMatch = new JLabel("❌ No items matched your search.");
            noMatch.setForeground(Color.RED);
            noMatch.setFont(LABEL_FONT.deriveFont(Font.ITALIC, 16f));
            itemsPanel.add(noMatch);
        }

        itemsPanel.revalidate();
        itemsPanel.repaint();
    }


    private Component buildItemRow(Item item) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(FIELD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(8,12,8,12)));

        JLabel nameLbl = new JLabel("📦 " + item.name);
        nameLbl.setFont(ITEM_BTN_FONT);
        nameLbl.setForeground(Color.BLACK);

        card.add(nameLbl, BorderLayout.WEST);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            Color orig = card.getBackground();
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(PANEL_IVORY); }
            @Override public void mouseExited (MouseEvent e) { card.setBackground(orig); }
            @Override public void mouseClicked(MouseEvent e) { showItemDetails(item); }
        });

        return card;
    }

    private void showItemDetails(Item item) {
        JDialog dlg = new JDialog(frame, "Item Details: " + item.name, true);
        dlg.setLayout(new BorderLayout(10,10));
        dlg.getContentPane().setBackground(BG_BEIGE);
        dlg.setSize(640, 500);

        JPanel head = new JPanel(new BorderLayout());
        head.setBackground(ACCENT_BROWN);
        JLabel headLbl = new JLabel("Item Details", SwingConstants.CENTER);
        headLbl.setFont(TITLE_FONT.deriveFont(20f));
        headLbl.setForeground(Color.WHITE);
        headLbl.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        head.add(headLbl, BorderLayout.CENTER);
        dlg.add(head, BorderLayout.NORTH);

        JLabel imgLabel = new JLabel("Loading image...", SwingConstants.CENTER);
        imgLabel.setOpaque(true);
        imgLabel.setBackground(PANEL_IVORY);
        imgLabel.setBorder(BorderFactory.createLineBorder(BORDER_TAN));
        imgLabel.setPreferredSize(new Dimension(DETAIL_IMG_MAX_W, DETAIL_IMG_MAX_H));
        dlg.add(imgLabel, BorderLayout.BEFORE_FIRST_LINE);

        String imgUrl = resolveImageUrl(item.imagePath, item.imageUrlFull);
        if (imgUrl == null) {
            imgLabel.setText("No image available.");
        } else {
            loadImageAsync(imgUrl, imgLabel, DETAIL_IMG_MAX_W, DETAIL_IMG_MAX_H);
            imgLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            imgLabel.setToolTipText("Click to open full image");
            imgLabel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    showFullImage(imgUrl, item.name);
                }
            });
        }

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(TEXT_FONT);
        textArea.setForeground(Color.BLACK);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(PANEL_IVORY);
        textArea.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        textArea.setText(
                "📦 Item Name: " + item.name + "\n" +
                "📝 Description: " + item.description + "\n" +
                "📍 Location: " + item.location + "\n" +
                "📅 Date Found: " + item.date + "\n" +
                "⏰ Reported: " + item.created
        );
        JScrollPane textScroll = new JScrollPane(textArea);
        textScroll.setBorder(BorderFactory.createLineBorder(BORDER_TAN));
        dlg.add(textScroll, BorderLayout.CENTER);

        JButton requestButton = new JButton("Request to Claim");
        styleButton(requestButton);
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_BEIGE);
        btnPanel.add(requestButton);
        dlg.add(btnPanel, BorderLayout.SOUTH);

        requestButton.addActionListener(e -> {
            dlg.dispose();
            sendClaimRequest(item);
        });

        dlg.setLocationRelativeTo(frame);
        dlg.setVisible(true);
    }

    private void sendClaimRequest(Item item) {
        JTextArea detailsField = new JTextArea(6, 30);
        detailsField.setLineWrap(true);
        detailsField.setWrapStyleWord(true);
        detailsField.setFont(TEXT_FONT);
        detailsField.setForeground(Color.BLACK);

        JScrollPane scrollPane = new JScrollPane(detailsField);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_TAN));

        int result = JOptionPane.showConfirmDialog(frame, new Object[]{
                "Please describe the item to prove ownership (color, brand, markings, where you lost it):",
                scrollPane
        }, "Claim Verification", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String claimMessage = detailsField.getText().trim();

            if (claimMessage.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "⚠️ Description is required to claim the item.");
                return;
            }

            try {
                String urlStr = "http://localhost/lostfound/submit_claim_request.php";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "reporter_id=" + userId +
                        "&found_item_id=" + item.id +
                        "&item_name=" + URLEncoder.encode(item.name, "UTF-8") +
                        "&message=" + URLEncoder.encode("Auto-claim request", "UTF-8") +
                        "&claim_message=" + URLEncoder.encode(claimMessage, "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes(StandardCharsets.UTF_8));
                }

                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    while (in.readLine() != null) { }
                }

                JOptionPane.showMessageDialog(frame, "✅ Claim request with description sent to admin.");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "❌ Failed to send claim request.");
                ex.printStackTrace();
            }
        }
    }

    private String resolveImageUrl(String rel, String abs) {
        if (abs != null && !abs.isEmpty() && !"null".equalsIgnoreCase(abs)) return abs;
        if (rel == null || rel.isEmpty() || "null".equalsIgnoreCase(rel)) return null;
        rel = rel.replace('\\', '/');
        if (rel.startsWith("/")) rel = rel.substring(1);
        return BASE_URL + rel;
    }

    private void loadImageAsync(String url, JLabel label, int maxW, int maxH) {
        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() {
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new URL(url));
                    if (img == null) return null;
                    return new ImageIcon(scaleImage(img, maxW, maxH));
                } catch (Exception ignored) { return null; }
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setText(null);
                        label.setIcon(icon);
                    } else {
                        label.setText("Image load failed.");
                    }
                } catch (Exception e) {
                    label.setText("Image load failed.");
                }
            }
        }.execute();
    }

    private void showFullImage(String url, String title) {
        JDialog dlg = new JDialog(frame, title, true);
        dlg.setLayout(new BorderLayout());
        dlg.getContentPane().setBackground(BG_BEIGE);
        JLabel lbl = new JLabel("Loading...", SwingConstants.CENTER);
        lbl.setForeground(Color.BLACK);
        dlg.add(new JScrollPane(lbl), BorderLayout.CENTER);
        dlg.setSize(700, 500);
        dlg.setLocationRelativeTo(frame);

        new SwingWorker<ImageIcon, Void>() {
            @Override protected ImageIcon doInBackground() {
                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new URL(url));
                    if (img == null) return null;
                    return new ImageIcon(img);
                } catch (Exception ignored) { return null; }
            }
            @Override protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        lbl.setText(null);
                        lbl.setIcon(icon);
                    } else {
                        lbl.setText("Failed to load image.");
                    }
                } catch (Exception e) {
                    lbl.setText("Failed to load image.");
                }
            }
        }.execute();

        dlg.setVisible(true);
    }

    private static Image scaleImage(java.awt.image.BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.min((double)maxW / w, (double)maxH / h);
        if (scale >= 1.0) return src;
        int nw = (int)Math.round(w * scale);
        int nh = (int)Math.round(h * scale);
        Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(
                nw, nh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return out;
    }

    private void styleButton(AbstractButton b) {
        b.setFont(BUTTON_FONT);
        b.setFocusPainted(false);
        b.setBackground(ACCENT_BROWN);
        b.setForeground(Color.BLACK);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN_D),
                BorderFactory.createEmptyBorder(6,14,6,14)));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (MouseEvent e) { b.setBackground(ACCENT_BROWN); }
        });
    }

    class Item {
        int    id;
        String name        = "";
        String description = "";
        String location    = "";
        String date        = "";
        String status      = "";
        String created     = "";
        String imagePath   = "";
        String imageUrlFull= "";
        String claimStatus = ""; // new
    }
}
