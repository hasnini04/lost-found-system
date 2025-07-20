package project_lostfound;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Entry landing page: themed with nude/brown colors, UTeM logo centered,
 * and consistent button styling used across the app.
 */
public class MainPage {

    // ------------------------------------------------------------------
    // Theme colors (reuse same tones for consistency)
    // ------------------------------------------------------------------
    private static final Color NUDE_BG        = new Color(245, 222, 179);  // wheat
    private static final Color PANEL_BG       = new Color(255, 248, 220);  // cornsilk-ish inner panel
    private static final Color BROWN_TEXT     = new Color( 92,  64,  51);  // dark brown
    private static final Color BROWN_BORDER   = new Color(160,  82,  45);  // sienna-ish
    private static final Color BTN_PRIMARY_BG = new Color(210, 180, 140);  // tan
    private static final Color BTN_SECOND_BG  = new Color(222, 184, 135);  // burlywood
    private static final Color BTN_HOVER_BG   = new Color(230, 200, 160);  // hover tint

    // ------------------------------------------------------------------
    // Logo config
    // ------------------------------------------------------------------
    // Put your logo in: src/main/resources/images/utem_logo.png
    // OR /images/utem_logo.png on your classpath.
    private static final String LOGO_PATH = "/images/utem_logo.png";
    private static final int LOGO_MAX_W = 160;
    private static final int LOGO_MAX_H = 160;

    public MainPage() {
        buildUI();
    }

    private void buildUI() {
        JFrame frame = new JFrame("Lost & Found Reporting System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(480, 420);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(NUDE_BG);

        // ---------- Center wrapper panel ----------
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        centerPanel.setBackground(PANEL_BG);

        // add a brown border around the inner panel for definition
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BROWN_BORDER, 3),
                BorderFactory.createEmptyBorder(30, 40, 30, 40)));

        // Logo
        JLabel logoLabel = new JLabel("", SwingConstants.CENTER);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        loadLogoIntoLabel(logoLabel);

        // Title below logo
        JLabel titleLabel = new JLabel("Welcome to Lost & Found System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 22));
        titleLabel.setForeground(BROWN_TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(16, 0, 20, 0));

        // Buttons
        JButton reporterBtn = new JButton("Login as Reporter");
        JButton adminBtn    = new JButton("Login as Admin");

        Dimension btnSize = new Dimension(220, 40);
        styleButton(reporterBtn, BTN_PRIMARY_BG);
        styleButton(adminBtn, BTN_SECOND_BG);
        reporterBtn.setMaximumSize(btnSize);
        adminBtn.setMaximumSize(btnSize);
        reporterBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add a little hover flair
        installHover(reporterBtn, BTN_PRIMARY_BG, BTN_HOVER_BG);
        installHover(adminBtn,    BTN_SECOND_BG,  BTN_HOVER_BG);

        // Button actions
        reporterBtn.addActionListener(e -> {
            frame.dispose();
            new ReporterLoginPage();
        });

        adminBtn.addActionListener(e -> {
            frame.dispose();
            new AdminLoginPage();
        });

        // Add components in order
        centerPanel.add(logoLabel);
        centerPanel.add(titleLabel);
        centerPanel.add(reporterBtn);
        centerPanel.add(Box.createVerticalStrut(12));
        centerPanel.add(adminBtn);

        frame.add(centerPanel, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // ------------------------------------------------------------------
    // Button styling helpers
    // ------------------------------------------------------------------
    private void styleButton(JButton btn, Color bg) {
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(bg);
        btn.setForeground(Color.BLACK); // requested black text
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BROWN_BORDER, 2),
                BorderFactory.createEmptyBorder(8, 24, 8, 24)
        ));
    }

    private void installHover(JButton btn, Color normal, Color hover) {
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(normal); }
        });
    }

    // ------------------------------------------------------------------
    // Logo loading
    // ------------------------------------------------------------------
    /**
     * Attempts to load the UTEM logo from the classpath and scale it into the label.
     * Falls back to text when missing.
     */
    private void loadLogoIntoLabel(JLabel label) {
        try {
            URL imgUrl = getClass().getResource(LOGO_PATH);
            if (imgUrl == null) {
                label.setText("[UTeM Logo]");
                label.setForeground(BROWN_TEXT);
                return;
            }
            BufferedImage img = ImageIO.read(imgUrl);
            if (img == null) {
                label.setText("[UTeM Logo]");
                label.setForeground(BROWN_TEXT);
                return;
            }
            Image scaled = scalePreserve(img, LOGO_MAX_W, LOGO_MAX_H);
            label.setIcon(new ImageIcon(scaled));
        } catch (Exception ex) {
            label.setText("[UTeM Logo]");
            label.setForeground(BROWN_TEXT);
        }
    }

    /** Aspect‑ratio scale w/out upscaling. */
    private Image scalePreserve(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth();
        int h = src.getHeight();
        double s = Math.min((double)maxW / w, (double)maxH / h);
        if (s >= 1.0) return src; // don't enlarge
        int nw = (int)Math.round(w * s);
        int nh = (int)Math.round(h * s);
        return src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
    }

    // ------------------------------------------------------------------
    // Entry
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        // Use system look & feel for nicer appearance (optional)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        new MainPage();
    }
}
