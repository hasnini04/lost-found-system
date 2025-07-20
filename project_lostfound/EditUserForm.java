package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class EditUserForm extends JFrame {

    public EditUserForm(int userId, String name, String email, String matric, String faculty) {
        setTitle("✏️ Edit User");
        setSize(400, 300);
        setLayout(new GridLayout(5, 2, 10, 10));
        setLocationRelativeTo(null);

        JTextField nameField = new JTextField(name);
        JTextField emailField = new JTextField(email);
        JTextField matricField = new JTextField(matric != null ? matric : "");
        JTextField facultyField = new JTextField(faculty != null ? faculty : "");

        add(new JLabel("Name:")); add(nameField);
        add(new JLabel("Email:")); add(emailField);
        add(new JLabel("Matric:")); add(matricField);
        add(new JLabel("Faculty:")); add(facultyField);

        JButton saveBtn = new JButton("💾 Save");
        add(new JLabel(""));
        add(saveBtn);

        saveBtn.addActionListener(e -> {
            try {
                URL url = new URL("http://localhost/lostfound/update_user.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "id=" + userId +
                        "&name=" + URLEncoder.encode(nameField.getText(), StandardCharsets.UTF_8) +
                        "&email=" + URLEncoder.encode(emailField.getText(), StandardCharsets.UTF_8) +
                        "&matric=" + URLEncoder.encode(matricField.getText(), StandardCharsets.UTF_8) +
                        "&faculty=" + URLEncoder.encode(facultyField.getText(), StandardCharsets.UTF_8);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(postData.getBytes());
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String response = reader.readLine();
                reader.close();

                if (response != null && response.contains("success")) {
                    JOptionPane.showMessageDialog(this, "✅ User updated!");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Update failed.");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
            }
        });

        setVisible(true);
    }
}
