package project_lostfound;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.SpinnerDateModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class ReportFoundItemPage {

    private static final Color BG_BEIGE          = new Color(0xF5,0xE8,0xDA);
    private static final Color PANEL_IVORY       = new Color(0xFF,0xFB,0xF5);
    private static final Color BORDER_TAN        = new Color(0xC9,0xAA,0x88);
    private static final Color ACCENT_BROWN      = new Color(0x8B,0x5E,0x3C);
    private static final Color ACCENT_BROWN_DARK = new Color(0x5C,0x3B,0x1E);
    private static final Color FIELD_BG          = Color.WHITE;

    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 22);
    private static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 16);
    private static final Font BTN_FONT   = new Font("SansSerif", Font.BOLD, 16);

    private final int userId;
    private boolean stopPolling = false;

    private JFrame frame;
    private JTextField itemNameField;
    private JTextArea  descriptionArea;
    private JTextField locationField;
    private JSpinner   dateSpinner;
    private JLabel     imagePreviewLabel;
    private File       selectedImageFile = null;

    public ReportFoundItemPage(int userId) {
        this.userId = userId;
        buildUI();
    }

    public ReportFoundItemPage() {
        this(1);
    }

    private void buildUI() {
        frame = new JFrame("📦 Report Found Item");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(640, 620);
        frame.setLayout(new BorderLayout(10,10));
        frame.getContentPane().setBackground(BG_BEIGE);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("Report Found Item", SwingConstants.CENTER);
        titleLbl.setForeground(Color.WHITE);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        frame.add(titlePanel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(true);
        formPanel.setBackground(PANEL_IVORY);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(20,26,20,26)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        itemNameField = new JTextField();
        styleField(itemNameField);

        descriptionArea = new JTextArea(4,20);
        descriptionArea.setBorder(BorderFactory.createLineBorder(BORDER_TAN));
        descriptionArea.setFont(FIELD_FONT);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        locationField = new JTextField();
        styleField(locationField);
        locationField.setEditable(true);
        locationField.setToolTipText("You can type or paste the location here, or use the map.");

        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setFont(FIELD_FONT);

        JButton selectLocationButton = new JButton("📍 Select Location on Map");
        styleButton(selectLocationButton);

        JButton chooseImageButton = new JButton("🖼 Choose Image...");
        styleButton(chooseImageButton);

        imagePreviewLabel = new JLabel("No image selected", SwingConstants.CENTER);
        imagePreviewLabel.setPreferredSize(new Dimension(160,110));
        imagePreviewLabel.setOpaque(true);
        imagePreviewLabel.setBackground(FIELD_BG);
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(BORDER_TAN));

        int row = 0;
        addLabeledField(formPanel, gbc, row++, "📛 Item Name:", itemNameField);
        addLabeledField(formPanel, gbc, row++, "📝 Description:", new JScrollPane(descriptionArea));

        // Location field + button below it
        JPanel locationPanel = new JPanel(new BorderLayout(5, 5));
        locationPanel.setOpaque(false);
        locationPanel.add(locationField, BorderLayout.CENTER);
        locationPanel.add(selectLocationButton, BorderLayout.SOUTH);
        addLabeledField(formPanel, gbc, row++, "📍 Location Found:", locationPanel);

        addLabeledField(formPanel, gbc, row++, "📅 Date Found:", dateSpinner);

        gbc.gridx = 0; gbc.gridy = row;
        JLabel imgLbl = new JLabel("🖼 Image (optional):");
        imgLbl.setFont(LABEL_FONT);
        formPanel.add(imgLbl, gbc);
        JPanel imgBtnWrap = new JPanel(new BorderLayout(5,5));
        imgBtnWrap.setOpaque(false);
        imgBtnWrap.add(imagePreviewLabel, BorderLayout.CENTER);
        imgBtnWrap.add(chooseImageButton, BorderLayout.SOUTH);
        gbc.gridx = 1;
        formPanel.add(imgBtnWrap, gbc);
        row++;

        frame.add(formPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
        bottomPanel.setBackground(BG_BEIGE);
        JButton backButton   = new JButton("⬅ Back");
        JButton submitButton = new JButton("🚀 Submit");
        styleButton(backButton);
        styleButton(submitButton);
        bottomPanel.add(backButton);
        bottomPanel.add(submitButton);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        chooseImageButton.addActionListener(evt -> chooseImage());
        selectLocationButton.addActionListener(e -> openMapLocation());
        submitButton.addActionListener(e -> submitForm());
        backButton.addActionListener(e -> {
            stopPolling = true;
            frame.dispose();
            new ReporterDashboardPage(userId);
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void chooseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Image of Found Item");
        fc.setFileFilter(new FileNameExtensionFilter("Image Files", "jpg","jpeg","png","gif","bmp"));
        int result = fc.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (f != null && f.isFile()) {
                selectedImageFile = f;
                updateImagePreview();
            }
        }
    }

    private void openMapLocation() {
        try {
            Desktop.getDesktop().browse(new URI("http://localhost/lostfound/map_found.html"));
            stopPolling = false;
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                while (!stopPolling && (System.currentTimeMillis() - startTime) < 20000) {
                    try {
                        URL url = new URL("http://localhost/lostfound/get_latest_location.php");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                            String address = reader.readLine();
                            if (address != null && !address.trim().isEmpty()) {
                                SwingUtilities.invokeLater(() -> locationField.setText(address.trim()));
                            }
                        }
                        Thread.sleep(2000);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        break;
                    }
                }
            }).start();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "❌ Failed to open map.");
        }
    }

    private void submitForm() {
        stopPolling = true;
        try {
            String itemName  = itemNameField.getText().trim();
            String desc      = descriptionArea.getText().trim();
            String location  = locationField.getText().trim();
            String dateValue = new SimpleDateFormat("yyyy-MM-dd").format(dateSpinner.getValue());

            if (itemName.isEmpty() || desc.isEmpty() || location.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "❗Please fill in all required fields.");
                return;
            }

            String response;
            if (selectedImageFile != null) {
                Map<String,String> fields = new HashMap<>();
                fields.put("item_name", itemName);
                fields.put("description", desc);
                fields.put("location", location);
                fields.put("date", dateValue);
                fields.put("reporter_id", String.valueOf(userId));

                response = sendMultipartPost(
                        "http://localhost/lostfound/report_found_item.php",
                        fields,
                        "image",
                        selectedImageFile
                );
            } else {
                response = sendFormPost(
                        "http://localhost/lostfound/report_found_item.php",
                        itemName,
                        desc,
                        location,
                        dateValue,
                        userId
                );
            }

            if (response.toLowerCase().contains("\"status\":\"success\"")) {
                JOptionPane.showMessageDialog(frame,
                        "✅ Found item reported!\nCurrent status: unclaimed");
                frame.dispose();
                new ReporterDashboardPage(userId);
            } else {
                JOptionPane.showMessageDialog(frame, "❌ Error reporting item:\n" + response);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "❌ Failed to connect to server.");
        }
    }

    private void addLabeledField(JPanel parent, GridBagConstraints gbc, int row,
                                 String labelText, Component fieldComp) {
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(LABEL_FONT);
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        parent.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        parent.add(fieldComp, gbc);
    }

    private void styleField(JTextField f) {
        f.setFont(FIELD_FONT);
        f.setBackground(FIELD_BG);
        f.setForeground(Color.BLACK);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(4,6,4,6)));
    }

    private void styleButton(AbstractButton b) {
        b.setFont(BTN_FONT);
        b.setFocusPainted(false);
        b.setBackground(ACCENT_BROWN);
        b.setForeground(Color.BLACK);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN_DARK),
                BorderFactory.createEmptyBorder(6,14,6,14)));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(ACCENT_BROWN_DARK); }
            @Override public void mouseExited (MouseEvent e) { b.setBackground(ACCENT_BROWN); }
        });
    }

    private void updateImagePreview() {
        if (selectedImageFile == null) {
            imagePreviewLabel.setText("No image selected");
            imagePreviewLabel.setIcon(null);
            return;
        }
        try {
            BufferedImage img = ImageIO.read(selectedImageFile);
            if (img != null) {
                Image scaled = img.getScaledInstance(
                        imagePreviewLabel.getWidth()  > 0 ? imagePreviewLabel.getWidth()  : 160,
                        imagePreviewLabel.getHeight() > 0 ? imagePreviewLabel.getHeight() : 110,
                        Image.SCALE_SMOOTH);
                imagePreviewLabel.setIcon(new ImageIcon(scaled));
                imagePreviewLabel.setText("");
            } else {
                imagePreviewLabel.setText(selectedImageFile.getName());
                imagePreviewLabel.setIcon(null);
            }
        } catch (IOException ex) {
            imagePreviewLabel.setText(selectedImageFile.getName());
            imagePreviewLabel.setIcon(null);
        }
    }

    private String sendMultipartPost(String urlString,
                                     Map<String, String> fields,
                                     String fileFieldName,
                                     File file) throws IOException {
        String boundary = "----LostFoundBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream();
             DataOutputStream writer = new DataOutputStream(output)) {

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                writer.writeBytes("--" + boundary + "\r\n");
                writer.writeBytes("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"\r\n\r\n");
                writer.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                writer.writeBytes("\r\n");
            }

            if (file != null && file.isFile()) {
                String fileName = file.getName();
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType == null) mimeType = "application/octet-stream";

                writer.writeBytes("--" + boundary + "\r\n");
                writer.writeBytes("Content-Disposition: form-data; name=\"" + fileFieldName + "\"; filename=\"" + fileName + "\"\r\n");
                writer.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = fis.read(buf)) != -1) {
                        writer.write(buf, 0, len);
                    }
                }
                writer.writeBytes("\r\n");
            }
            writer.writeBytes("--" + boundary + "--\r\n");
            writer.flush();
        }
        return readResponse(conn);
    }

    private String sendFormPost(String urlString,
                                String itemName,
                                String description,
                                String location,
                                String formattedDate,
                                int userId) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String postData = "item_name=" + URLEncoder.encode(itemName, StandardCharsets.UTF_8) +
                "&description=" + URLEncoder.encode(description, StandardCharsets.UTF_8) +
                "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8) +
                "&date=" + URLEncoder.encode(formattedDate, StandardCharsets.UTF_8) +
                "&reporter_id=" + userId;

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString().trim();
        }
    }
}
