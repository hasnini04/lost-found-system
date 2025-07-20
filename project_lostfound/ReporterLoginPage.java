package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReporterLoginPage {

    private static final Color NUDE_BG          = new Color(245, 222, 179);  // wheat-ish
    private static final Color TEXTFIELD_BG     = new Color(255, 248, 220);  // cornsilk / light cream
    private static final Color BROWN_BORDER     = new Color(160,  82,  45);  // sienna
    private static final Color BROWN_DARK_TEXT  = new Color( 92,  64,  51);  // dark brown text
    private static final Color BTN_PRIMARY_BG   = new Color(210, 180, 140);  // tan
    private static final Color BTN_SECOND_BG    = new Color(222, 184, 135);  // burlywood-ish
    private static final Color BTN_TERTIARY_BG  = new Color(244, 164,  96);  // sandy (for Back if you want contrast)

    private JFrame frame;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JLabel statusLabel;

    // Simple regex to pull out numeric user_id from JSON-like response: "user_id":123
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("\"user_id\"\\s*:\\s*\"?(\\d+)\"?");

    public ReporterLoginPage() {
        buildUI();
    }

    private void buildUI() {
        frame = new JFrame("Reporter Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(480, 340);
        frame.setLayout(new BorderLayout());

        // ---------- Main wrapper ----------
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(NUDE_BG);
        frame.add(mainPanel, BorderLayout.CENTER);

        // ---------- Title ----------
        JLabel titleLabel = new JLabel("Reporter Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 26));
        titleLabel.setForeground(BROWN_DARK_TEXT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // ---------- Form ----------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(NUDE_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;

        // Email
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        JLabel emailLbl = new JLabel("Email:");
        emailLbl.setFont(new Font("Arial", Font.BOLD, 14));
        emailLbl.setForeground(BROWN_DARK_TEXT);
        formPanel.add(emailLbl, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        emailField = new JTextField();
        styleTextField(emailField);
        formPanel.add(emailField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(new Font("Arial", Font.BOLD, 14));
        passLbl.setForeground(BROWN_DARK_TEXT);
        formPanel.add(passLbl, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        passwordField = new JPasswordField();
        styleTextField(passwordField);
        formPanel.add(passwordField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ---------- Action buttons ----------
        JPanel actionPanel = new JPanel();
        actionPanel.setBackground(NUDE_BG);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton loginButton = new JButton("Login");
        styleButton(loginButton, BTN_PRIMARY_BG, Color.BLACK);
        JButton registerButton = new JButton("Register");
        styleButton(registerButton, BTN_SECOND_BG, Color.BLACK);
        JButton backButton = new JButton("Back");
        styleButton(backButton, BTN_PRIMARY_BG, Color.BLACK);

        actionPanel.add(loginButton);
        actionPanel.add(registerButton);
        actionPanel.add(backButton);

        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        // ---------- Status Label ----------
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        statusLabel.setForeground(BROWN_DARK_TEXT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        frame.add(statusLabel, BorderLayout.SOUTH); // sits below mainPanel

        // ---------- Actions ----------
        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin()); // Enter key
        registerButton.addActionListener(e -> {
            frame.dispose();
            new ReporterRegisterPage();
        });
        backButton.addActionListener(e -> {
            frame.dispose();
            new MainPage();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Style helpers
    // ------------------------------------------------------------------
    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Arial", Font.PLAIN, 14));
        tf.setBackground(TEXTFIELD_BG);
        tf.setBorder(BorderFactory.createLineBorder(BROWN_BORDER, 2));
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg); // black per your request
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
    }

    // ------------------------------------------------------------------
    // Login flow
    // ------------------------------------------------------------------
    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both email and password.");
            return;
        }

        statusLabel.setText("Logging in...");
        statusLabel.setForeground(BROWN_DARK_TEXT);

        // Do network on a background thread
        new SwingWorker<LoginResult, Void>() {
            @Override
            protected LoginResult doInBackground() {
                return callLoginEndpoint(email, password);
            }
            @Override
            protected void done() {
                try {
                    LoginResult res = get();
                    if (res.success) {
                        JOptionPane.showMessageDialog(frame, "Login successful!");
                        frame.dispose();
                        new ReporterDashboardPage(res.userId);
                    } else {
                        JOptionPane.showMessageDialog(frame,
                                "Login failed: " + res.message,
                                "Login Failed", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText(res.message);
                        statusLabel.setForeground(Color.RED);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame,
                            "Unexpected error: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    statusLabel.setText("Unexpected error.");
                    statusLabel.setForeground(Color.RED);
                }
            }
        }.execute();
    }

    // ------------------------------------------------------------------
    // Call PHP reporter_login.php
    // ------------------------------------------------------------------
    private LoginResult callLoginEndpoint(String email, String password) {
        LoginResult result = new LoginResult();
        try {
            URL url = new URL("http://localhost/lostfound/reporter_login.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String postData = "email=" + URLEncoder.encode(email, "UTF-8") +
                              "&password=" + URLEncoder.encode(password, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
            }
            conn.disconnect();

            String resp = response.toString().trim();
            // quick check for success
            boolean ok = resp.contains("\"status\":\"success\"");
            if (ok) {
                result.success = true;
                // parse user_id
                Integer uid = parseUserId(resp);
                if (uid != null) {
                    result.userId = uid;
                } else {
                    // fallback if not found; you might want to fail instead
                    result.userId = 1;
                }
            } else {
                result.success = false;
                result.message = parseServerMessage(resp);
            }
        } catch (Exception ex) {
            result.success = false;
            result.message = ex.getMessage();
        }
        return result;
    }

    // ------------------------------------------------------------------
    // Helpers: parse user_id & message from JSON-ish text
    // ------------------------------------------------------------------
    private Integer parseUserId(String json) {
        Matcher m = USER_ID_PATTERN.matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    /** Try to pull "message":"...". */
    private String parseServerMessage(String json) {
        int idx = json.indexOf("\"message\"");
        if (idx >= 0) {
            int colon = json.indexOf(':', idx);
            if (colon > 0) {
                int q1 = json.indexOf('"', colon + 1);
                int q2 = (q1 >= 0) ? json.indexOf('"', q1 + 1) : -1;
                if (q1 >= 0 && q2 > q1) {
                    return json.substring(q1 + 1, q2);
                }
            }
        }
        return "Unknown error.";
    }

    // ------------------------------------------------------------------
    // Model: login result
    // ------------------------------------------------------------------
    private static class LoginResult {
        boolean success;
        int userId;
        String message = "";
    }
}
