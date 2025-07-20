package project_lostfound;

import javax.swing.*;
import java.awt.*;

public class EntryPage {

    private JFrame frame;

    public EntryPage() {
        frame = new JFrame("Lost & Found System");
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(3, 1, 10, 10));

        JLabel welcomeLabel = new JLabel("Select User Type", SwingConstants.CENTER);
        frame.add(welcomeLabel);

        JButton reporterButton = new JButton("Login as Reporter");
        JButton adminButton = new JButton("Login as Admin");

        frame.add(reporterButton);
        frame.add(adminButton);

        // Reporter button action
        reporterButton.addActionListener(e -> {
            frame.dispose();
            new ReporterLoginPage(); // go to reporter login
        });

        // Admin button action
        adminButton.addActionListener(e -> {
            frame.dispose();
            new AdminLoginPage(); // go to admin login
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new EntryPage(); // Launch app from here
    }
}
