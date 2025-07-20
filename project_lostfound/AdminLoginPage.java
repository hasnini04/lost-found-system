package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AdminLoginPage {

    private JFrame frame;
    private JTextField emailField;
    private JPasswordField passwordField;

    public AdminLoginPage() {
        frame = new JFrame("Admin Login");
        frame.setSize(450, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Background Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(245, 222, 179)); // light nude

        // Title Label
        JLabel titleLabel = new JLabel("Admin Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 26));
        titleLabel.setForeground(new Color(92, 64, 51)); // dark brown
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        formPanel.setBackground(new Color(245, 222, 179));

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 14));
        emailLabel.setForeground(new Color(92, 64, 51));
        emailField = new JTextField();
        styleTextField(emailField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.BOLD, 14));
        passLabel.setForeground(new Color(92, 64, 51));
        passwordField = new JPasswordField();
        styleTextField(passwordField);

        formPanel.add(emailLabel);
        formPanel.add(emailField);
        formPanel.add(passLabel);
        formPanel.add(passwordField);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(245, 222, 179));

        JButton loginButton = new JButton("Login");
        styleButton(loginButton, new Color(210, 180, 140), Color.BLACK); // tan background, black text
        JButton backButton = new JButton("Back");
        styleButton(backButton, new Color(222, 184, 135), Color.BLACK); // lighter brown, black text

        buttonPanel.add(loginButton);
        buttonPanel.add(backButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);

        // Actions
        loginButton.addActionListener(e -> loginAction());
        backButton.addActionListener(e -> {
            frame.dispose();
            new MainPage();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setBackground(new Color(255, 248, 220));
        textField.setBorder(BorderFactory.createLineBorder(new Color(160, 82, 45), 2));
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setBackground(bgColor);
        button.setForeground(textColor); // ✅ black font color
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void loginAction() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter both email and password.");
            return;
        }

        try {
            URL url = new URL("http://localhost/lostfound/admin_login.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setDoOutput(true);

            String postData = "email=" + URLEncoder.encode(email, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            conn.disconnect();

            String responseStr = response.toString().trim();
            if (responseStr.contains("\"status\":\"success\"")) {
                JOptionPane.showMessageDialog(frame, "Login successful!");
                frame.dispose();
                User adminUser = new User();
                adminUser.setId(3);
                adminUser.setName("Admin");
                adminUser.setEmail("admin@lostfound.com");
                adminUser.setRole("admin");

                new AdminDashboardPage(adminUser);
            } else if (responseStr.contains("\"message\"")) {
                String errorMsg = responseStr.split("\"message\":\"")[1].split("\"")[0];
                JOptionPane.showMessageDialog(frame, errorMsg);
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid admin credentials.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error connecting to server.");
        }
    }
}
