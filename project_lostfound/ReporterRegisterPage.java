package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ReporterRegisterPage {

    private static final Color NUDE_BG          = new Color(245, 222, 179);  // wheat-ish
    private static final Color TEXTFIELD_BG     = new Color(255, 248, 220);  // cornsilk
    private static final Color BROWN_BORDER     = new Color(160,  82,  45);  // sienna
    private static final Color BROWN_TEXT       = new Color( 92,  64,  51);  // dark brown
    private static final Color BTN_PRIMARY_BG   = new Color(210, 180, 140);  // tan
    private static final Color BTN_SECOND_BG    = new Color(222, 184, 135);  // burlywood-ish

    private JFrame frame;
    private JTextField nameField, matricField, facultyField, emailField;
    private JPasswordField passwordField;

    public ReporterRegisterPage() {
        buildUI();
    }

    private void buildUI() {
        frame = new JFrame("Reporter Registration");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 420);
        frame.setLayout(new BorderLayout());

        // ---------- Main Panel ----------
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(NUDE_BG);
        frame.add(mainPanel, BorderLayout.CENTER);

        // ---------- Title ----------
        JLabel titleLabel = new JLabel("Reporter Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 26));
        titleLabel.setForeground(BROWN_TEXT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // ---------- Form ----------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(NUDE_BG);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;

        // Fields
        nameField    = addFormRow(formPanel, gbc, 0, "Name:");
        matricField  = addFormRow(formPanel, gbc, 1, "Matric Number:");
        facultyField = addFormRow(formPanel, gbc, 2, "Faculty:");
        emailField   = addFormRow(formPanel, gbc, 3, "Email:");
        passwordField = new JPasswordField();
        styleTextField(passwordField);
        gbc.gridx = 1; gbc.gridy = 4;
        formPanel.add(passwordField, gbc);
        gbc.gridx = 0;
        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(new Font("Arial", Font.BOLD, 14));
        passLbl.setForeground(BROWN_TEXT);
        gbc.gridy = 4;
        formPanel.add(passLbl, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ---------- Buttons ----------
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(NUDE_BG);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        JButton registerButton = new JButton("Register");
        styleButton(registerButton, BTN_PRIMARY_BG, Color.BLACK);
        JButton backButton = new JButton("Back");
        styleButton(backButton, BTN_SECOND_BG, Color.BLACK);

        buttonPanel.add(registerButton);
        buttonPanel.add(backButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // ---------- Actions ----------
        registerButton.addActionListener(e -> handleRegister());
        backButton.addActionListener(e -> {
            frame.dispose();
            new ReporterLoginPage();
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Add a labeled text field
    private JTextField addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(BROWN_TEXT);
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(label, gbc);

        JTextField textField = new JTextField();
        styleTextField(textField);
        gbc.gridx = 1;
        gbc.gridy = row;
        panel.add(textField, gbc);

        return textField;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(new Font("Arial", Font.PLAIN, 14));
        tf.setBackground(TEXTFIELD_BG);
        tf.setBorder(BorderFactory.createLineBorder(BROWN_BORDER, 2));
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(fg); // black text
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 24, 8, 24));
    }

    private void handleRegister() {
        String name = nameField.getText().trim();
        String matric = matricField.getText().trim();
        String faculty = facultyField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (name.isEmpty() || matric.isEmpty() || faculty.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill in all fields.");
            return;
        }

        try {
            URL url = new URL("http://localhost/lostfound/register_reporter.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setDoOutput(true);

            String postData = "name=" + URLEncoder.encode(name, "UTF-8") +
                    "&matric=" + URLEncoder.encode(matric, "UTF-8") +
                    "&faculty=" + URLEncoder.encode(faculty, "UTF-8") +
                    "&email=" + URLEncoder.encode(email, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                JOptionPane.showMessageDialog(frame, "Registration successful!");
                frame.dispose();
                new ReporterLoginPage();
            } else {
                JOptionPane.showMessageDialog(frame, "Registration failed. Server error.");
            }

            conn.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error connecting to server.");
        }
    }
}
