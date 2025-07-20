package project_lostfound;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;

public class AdminMessagesPage extends JFrame {
    private static final Color BG_BEIGE = new Color(0xF5, 0xE8, 0xDA);
    private static final Color ACCENT_BROWN = new Color(0x8B, 0x5E, 0x3C);

    private JComboBox<ReporterItem> reporterCombo;
    private JTextArea messageArea;
    private final User adminUser;

    public AdminMessagesPage(User adminUser) {
        this.adminUser = adminUser;

        setTitle("📨 Admin Messages");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        // --- Title ---
        JLabel titleLbl = new JLabel("Send Message to Reporter", SwingConstants.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLbl.setOpaque(true);
        titleLbl.setBackground(ACCENT_BROWN);
        titleLbl.setForeground(Color.BLACK);
        titleLbl.setBorder(new EmptyBorder(10,10,10,10));
        add(titleLbl, BorderLayout.NORTH);

        // --- Form ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(BG_BEIGE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Select Reporter:"), gbc);

        reporterCombo = new JComboBox<>();
        loadReporters(); // load reporter list
        gbc.gridx = 1; gbc.gridy = 0;
        formPanel.add(reporterCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        formPanel.add(new JLabel("Message:"), gbc);

        messageArea = new JTextArea(5, 30);
        JScrollPane msgScroll = new JScrollPane(messageArea);
        gbc.gridy = 2;
        formPanel.add(msgScroll, gbc);
        add(formPanel, BorderLayout.CENTER);

        // --- Bottom ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sendBtn = new JButton("Send");
        JButton backBtn = new JButton("Back");
        bottomPanel.add(sendBtn);
        bottomPanel.add(backBtn);
        add(bottomPanel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        backBtn.addActionListener(e -> dispose());

        setVisible(true);
    }

    private void loadReporters() {
        try {
            URL url = new URL("http://localhost/lostfound/fetch_reporters.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (InputStream is = conn.getInputStream();
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                String json = sb.toString();
                // parse simple JSON manually
                List<Map<String, String>> reporters = parseJsonArray(json);
                reporterCombo.removeAllItems();
                for (Map<String, String> r : reporters) {
                    reporterCombo.addItem(new ReporterItem(
                        Integer.parseInt(r.get("id")),
                        r.get("name")
                    ));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load reporters: " + ex.getMessage());
        }
    }

    private void sendMessage() {
        ReporterItem selected = (ReporterItem) reporterCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a reporter.");
            return;
        }
        String msg = messageArea.getText().trim();
        if (msg.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Message cannot be empty.");
            return;
        }

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                String data = "reporter_id=" + URLEncoder.encode(String.valueOf(selected.id), "UTF-8")
                            + "&admin_id=" + URLEncoder.encode(String.valueOf(adminUser.getId()), "UTF-8")
                            + "&message=" + URLEncoder.encode(msg, "UTF-8");

                URL url = new URL("http://localhost/lostfound/send_message.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                }
                try (InputStream is = conn.getInputStream()) {
                    return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }

            @Override
            protected void done() {
                try {
                    String response = get();
                    JOptionPane.showMessageDialog(AdminMessagesPage.this, "Response: " + response);
                    messageArea.setText("");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(AdminMessagesPage.this, "Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    // Simple JSON parser (assumes array of objects like [{"id":"1","name":"x"}])
    private List<Map<String,String>> parseJsonArray(String json) {
        List<Map<String,String>> list = new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return list;
        json = json.substring(1, json.length()-1);
        String[] objs = json.split("\\},\\{");
        for (String obj : objs) {
            obj = obj.replace("{", "").replace("}", "");
            String[] pairs = obj.split(",");
            Map<String,String> map = new HashMap<>();
            for (String p : pairs) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    String key = kv[0].replace("\"", "").trim();
                    String val = kv[1].replace("\"", "").trim();
                    map.put(key, val);
                }
            }
            list.add(map);
        }
        return list;
    }

    private static class ReporterItem {
        int id;
        String name;
        ReporterItem(int id, String name) { this.id = id; this.name = name; }
        public String toString() { return name + " (ID: " + id + ")"; }
    }
}
