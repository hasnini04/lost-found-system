package project_lostfound;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;


public class ReportLostItemPage {

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------
    private static final Color BG_COLOR      = new Color(245, 235, 224);   // nude
    private static final Color PANEL_COLOR   = Color.WHITE;
    private static final Color ACCENT_BROWN  = new Color(181, 136, 99);
    private static final Color FIELD_BORDER  = new Color(181, 136, 99);
    private static final Font  LABEL_FONT    = new Font("Segoe UI Emoji", Font.BOLD, 16);
    private static final Font  FIELD_FONT    = new Font("Segoe UI", Font.PLAIN, 15);
    private static final Font  BTN_FONT      = new Font("Segoe UI Emoji", Font.BOLD, 18);

    // Endpoint (adjust if path differs)
    private static final String POST_URL = "http://localhost/lostfound/report_lost_item.php";

    private final int userId;

    // UI refs
    private JFrame frame;
    private JTextField itemNameField;
    private JTextArea  descriptionArea;
    private JTextField locationField;  // editable
    private JSpinner   dateSpinner;
    private JLabel     imagePreviewLabel;
    private JLabel     imageFilenameLabel;
    private File       selectedImageFile = null;

    public ReportLostItemPage(int userId) {
        this.userId = userId;
        buildUI();
    }

    // ------------------------------------------------------------------
    // UI
    // ------------------------------------------------------------------
    private void buildUI() {
        frame = new JFrame("🧍 Report Lost Item");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(700, 620);
        frame.setLayout(new BorderLayout(20, 20));
        frame.getContentPane().setBackground(BG_COLOR);

        // Heading
        JLabel heading = new JLabel("Report a Lost Item", SwingConstants.CENTER);
        heading.setFont(new Font("Segoe UI Emoji", Font.BOLD, 28));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        frame.add(heading, BorderLayout.NORTH);

        // Form panel inside scroll (in case small screens)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(PANEL_COLOR);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;

        // Fields
        itemNameField = createTextField();
        descriptionArea = createTextArea();
        locationField = createTextField();
        locationField.setToolTipText("Type the last place you remember having the item.");

        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(FIELD_FONT);

        JButton chooseImageButton = createMiniButton("📷 Choose Image...");
        chooseImageButton.addActionListener(e -> chooseImageFile());

        imageFilenameLabel = new JLabel("No image selected");
        imageFilenameLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        imageFilenameLabel.setForeground(Color.DARK_GRAY);

        imagePreviewLabel = new JLabel("Preview", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(160, 120));
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setBackground(Color.WHITE);

        int row = 0;
        addRow(formPanel, gbc, row++, "📝 Item Name:", itemNameField);
        addRow(formPanel, gbc, row++, "🧾 Description:", new JScrollPane(descriptionArea));
        addRow(formPanel, gbc, row++, "📍 Last Location:", locationField);
        addRow(formPanel, gbc, row++, "📅 Date Lost:", dateSpinner);

        // Image row (button + filename)
        JPanel imgButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        imgButtonPanel.setOpaque(false);
        imgButtonPanel.add(chooseImageButton);
        imgButtonPanel.add(imageFilenameLabel);
        addRow(formPanel, gbc, row++, "🖼 Image:", imgButtonPanel);

        // Image preview full width (indent under field col)
        gbc.gridx = 1; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 1;
        formPanel.add(imagePreviewLabel, gbc);
        row++;

        JScrollPane formScroll = new JScrollPane(formPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(formScroll, BorderLayout.CENTER);

        // Buttons bottom
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        btnPanel.setBackground(BG_COLOR);
        JButton submitButton = createMainButton("🚀 Submit");
        JButton backButton   = createMainButton("⬅ Back");
        btnPanel.add(backButton);
        btnPanel.add(submitButton);
        frame.add(btnPanel, BorderLayout.SOUTH);

        submitButton.addActionListener(e -> submitForm());
        backButton.addActionListener(e -> {
            frame.dispose();
            new ReporterDashboardPage(userId);
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** Add labeled field row to GridBag form. */
    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component fieldComp) {
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        panel.add(lbl, gbc);

        gbc.weightx = 1;
        gbc.gridx = 1;
        panel.add(fieldComp, gbc);
    }

    private JTextField createTextField() {
        JTextField tf = new JTextField();
        tf.setFont(FIELD_FONT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER),
                BorderFactory.createEmptyBorder(4,6,4,6)));
        return tf;
    }

    private JTextArea createTextArea() {
        JTextArea ta = new JTextArea(4, 20);
        ta.setFont(FIELD_FONT);
        ta.setWrapStyleWord(true);
        ta.setLineWrap(true);
        ta.setBorder(BorderFactory.createLineBorder(FIELD_BORDER));
        return ta;
    }

    private JButton createMainButton(String text) {
        JButton b = new JButton(text);
        b.setFont(BTN_FONT);
        b.setBackground(ACCENT_BROWN);
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(180, 50));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        return b;
    }

    private JButton createMiniButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        b.setBackground(new Color(230, 210, 195));
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ------------------------------------------------------------------
    // Image chooser + preview
    // ------------------------------------------------------------------
    private void chooseImageFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select image of lost item");
        chooser.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif", "bmp"));
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = chooser.getSelectedFile();
            imageFilenameLabel.setText(selectedImageFile.getName());
            loadImagePreview(selectedImageFile);
        }
    }

    private void loadImagePreview(File imgFile) {
        try {
            BufferedImage img = ImageIO.read(imgFile);
            if (img == null) {
                imagePreviewLabel.setIcon(null);
                imagePreviewLabel.setText("Preview not available");
                return;
            }
            int w = imagePreviewLabel.getWidth();
            int h = imagePreviewLabel.getHeight();
            if (w <= 0) w = 160;
            if (h <= 0) h = 120;
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            imagePreviewLabel.setText("");
            imagePreviewLabel.setIcon(new ImageIcon(scaled));
        } catch (IOException ex) {
            imagePreviewLabel.setIcon(null);
            imagePreviewLabel.setText("Error loading");
        }
    }

    // ------------------------------------------------------------------
    // Submit
    // ------------------------------------------------------------------
    private void submitForm() {
        String itemName = itemNameField.getText().trim();
        String desc     = descriptionArea.getText().trim();
        String loc      = locationField.getText().trim();
        String dateLost = new SimpleDateFormat("yyyy-MM-dd").format(dateSpinner.getValue());

        if (itemName.isEmpty() || loc.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "⚠ Item name and location are required.");
            return;
        }

        if (selectedImageFile != null) {
            submitMultipart(itemName, desc, loc, dateLost, selectedImageFile);
        } else {
            submitUrlEncoded(itemName, desc, loc, dateLost);
        }
    }

    /** x-www-form-urlencoded POST (no image). */
    private void submitUrlEncoded(String itemName, String desc, String loc, String dateLost) {
        try {
            URL url = new URL(POST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String postData =
                    "item_name="     + URLEncoder.encode(itemName, StandardCharsets.UTF_8) +
                    "&description="  + URLEncoder.encode(desc, StandardCharsets.UTF_8) +
                    "&location_lost="+ URLEncoder.encode(loc, StandardCharsets.UTF_8) +
                    "&date_lost="    + URLEncoder.encode(dateLost, StandardCharsets.UTF_8) +
                    "&reporter_id="  + userId;

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
            }

            handleServerResponse(conn);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "❌ Failed to connect to server.");
        }
    }

    /** multipart/form-data POST (with image). */
    private void submitMultipart(String itemName, String desc, String loc, String dateLost, File imageFile) {
        String boundary = "----LostFoundBoundary" + System.currentTimeMillis();
        try {
            URL url = new URL(POST_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                writeFormField(out, boundary, "reporter_id",   String.valueOf(userId));
                writeFormField(out, boundary, "item_name",     itemName);
                writeFormField(out, boundary, "description",   desc);
                writeFormField(out, boundary, "location_lost", loc);
                writeFormField(out, boundary, "date_lost",     dateLost);
                writeFileField(out, boundary, "image", imageFile);
                out.writeBytes("--" + boundary + "--\r\n");
            }

            handleServerResponse(conn);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "❌ Failed to upload image.");
        }
    }

    private void writeFormField(DataOutputStream out, String boundary, String name, String value) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.writeBytes(value + "\r\n");
    }

    private void writeFileField(DataOutputStream out, String boundary, String fieldName, File file) throws IOException {
        String fileName = file.getName();
        String mimeType = URLConnection.guessContentTypeFromName(fileName);
        if (mimeType == null) mimeType = "application/octet-stream";

        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        out.writeBytes("\r\n");
    }

    // ------------------------------------------------------------------
    // Response handling
    // ------------------------------------------------------------------
    private void handleServerResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) response.append(line);
        }
        String resp = response.toString();
        System.out.println("Server response: " + resp);

        if (resp.toLowerCase().contains("\"status\":\"success\"") || resp.contains("success")) {
            JOptionPane.showMessageDialog(frame, "✅ Lost item reported successfully!");
            frame.dispose();
            new ReporterDashboardPage(userId);
        } else {
            JOptionPane.showMessageDialog(frame, "❌ Error reporting item:\n" + resp);
        }
    }

    // ------------------------------------------------------------------
    // Quick test harness
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        new ReportLostItemPage(101);
    }
}
