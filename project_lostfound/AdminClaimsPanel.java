package project_lostfound;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class AdminClaimsPanel extends JFrame {

    // ------------------------------------------------------------------
    // Theme palette
    // ------------------------------------------------------------------
    private static final Color BG_BEIGE       = new Color(0xF5,0xE8,0xDA);
    private static final Color PANEL_IVORY    = new Color(0xFF,0xFB,0xF5);
    private static final Color BORDER_TAN     = new Color(0xC9,0xAA,0x88);
    private static final Color ACCENT_BROWN   = new Color(0x8B,0x5E,0x3C);
    private static final Color ACCENT_BROWN_D = new Color(0x5C,0x3B,0x1E);

    // Status backgrounds (claim status)
    private static final Color ST_APPROVED_BG = new Color(200,255,200);
    private static final Color ST_REJECT_BG   = new Color(255,200,200);
    private static final Color ST_OTHER_BG    = new Color(255,240,200);

    // Item status backgrounds (found_item status)
    private static final Color IT_CLAIMED_BG   = new Color(210,255,210);
    private static final Color IT_UNCLAIMED_BG = new Color(255,250,210);
    private static final Color IT_UNKNOWN_BG   = Color.WHITE;

    // Fonts
    private static final Font  TITLE_FONT  = new Font("SansSerif", Font.BOLD, 22);
    private static final Font  HEADER_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font  ROW_FONT    = new Font("SansSerif", Font.PLAIN, 14);
    private static final Font  BTN_FONT    = new Font("SansSerif", Font.BOLD, 14);

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------
    JTable claimsTable;
    DefaultTableModel tableModel;

    public AdminClaimsPanel() {
        setTitle("Admin - Claim Requests");
        setSize(1100, 520);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        // ----- Title strip -----
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("Claim Requests (Admin)", SwingConstants.CENTER);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(Color.BLACK);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        // ----- Table setup -----
        tableModel = new DefaultTableModel(new String[]{
                "ID", "User", "Email", "Item", "Item Status", "Claim Message", "Claim Status", "Date", "Action"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 8; // Only Action column is editable (for buttons)
            }
        };

        claimsTable = new JTable(tableModel);
        claimsTable.setRowHeight(35);
        claimsTable.setFont(ROW_FONT);
        claimsTable.setGridColor(BORDER_TAN);
        claimsTable.setFillsViewportHeight(true);
        claimsTable.setSelectionBackground(new Color(220,235,255));
        claimsTable.setSelectionForeground(Color.BLACK);

        // Header styling
        JTableHeader hdr = claimsTable.getTableHeader();
        hdr.setFont(HEADER_FONT);
        hdr.setBackground(ACCENT_BROWN);
        hdr.setForeground(Color.BLACK);

        // Column widths
        TableColumnModel colModel = claimsTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(50);    // ID
        colModel.getColumn(1).setPreferredWidth(120);   // User
        colModel.getColumn(2).setPreferredWidth(180);   // Email
        colModel.getColumn(3).setPreferredWidth(140);   // Item
        colModel.getColumn(4).setPreferredWidth(100);   // Item Status
        colModel.getColumn(5).setPreferredWidth(250);   // Claim Message
        colModel.getColumn(6).setPreferredWidth(100);   // Claim Status
        colModel.getColumn(7).setPreferredWidth(160);   // Date
        colModel.getColumn(8).setPreferredWidth(150);   // Action buttons

        // Renderers
        colModel.getColumn(0).setCellRenderer(new GenericCellRenderer());           // ID
        colModel.getColumn(1).setCellRenderer(new GenericCellRenderer());           // User
        colModel.getColumn(2).setCellRenderer(new GenericCellRenderer());           // Email
        colModel.getColumn(3).setCellRenderer(new GenericCellRenderer());           // Item
        colModel.getColumn(4).setCellRenderer(new ItemStatusCellRenderer());        // Item Status
        colModel.getColumn(5).setCellRenderer(new GenericCellRenderer());           // Claim Message
        colModel.getColumn(6).setCellRenderer(new ClaimStatusCellRenderer());       // Claim Status
        colModel.getColumn(7).setCellRenderer(new GenericCellRenderer());           // Date

        // Action column
        colModel.getColumn(8).setCellRenderer(new ButtonRenderer());
        colModel.getColumn(8).setCellEditor(new ButtonEditor());

        // Wrap table
        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setBackground(PANEL_IVORY);
        tableWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(10,10,10,10)));
        JScrollPane scrollPane = new JScrollPane(claimsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_TAN));
        tableWrap.add(scrollPane, BorderLayout.CENTER);
        add(tableWrap, BorderLayout.CENTER);

        // Initial load
        fetchClaims();

        setVisible(true);
    }

    // ------------------------------------------------------------------
    // Fetch data from server
    // ------------------------------------------------------------------
    private void fetchClaims() {
        try {
            URL url = new URL("http://localhost/lostfound/fetch_claims.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            parseAndLoad(responseBuilder.toString());

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error fetching claims: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ------------------------------------------------------------------
    // Parse JSON (loose parsing consistent w/ existing style) and load table
    // ------------------------------------------------------------------
    private void parseAndLoad(String json) {
        tableModel.setRowCount(0);
        if (json == null || json.isEmpty()) return;

        int arrStart = json.indexOf("[");
        int arrEnd   = json.lastIndexOf("]");
        if (arrStart < 0 || arrEnd < 0) return;

        String array = json.substring(arrStart + 1, arrEnd);
        String[] objects = array.split("\\},\\{");

        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "").replace("\"", "");
            String[] fields = obj.split(",");

            String id = "", user = "", email = "", item = "",
                   itemStatus = "", claimMessage = "", claimStatus = "", date = "";

            for (String field : fields) {
                String[] keyVal = field.split(":", 2);
                if (keyVal.length < 2) continue;
                String key = keyVal[0].trim();
                String val = keyVal[1].trim();

                switch (key) {
                    case "id": id = val; break;
                    case "user_name": user = val; break;
                    case "email": email = val; break;
                    case "item_name": item = val; break;
                    case "item_status": itemStatus = val; break;
                    case "claim_message":
                        try {
                            claimMessage = new String(Base64.getDecoder().decode(val), StandardCharsets.UTF_8);
                        } catch (Exception e) {
                            claimMessage = val;
                        }
                        break;
                    // legacy "message" fallback if server returns it
                    case "message":
                        if (claimMessage.isEmpty()) {
                            try {
                                claimMessage = new String(Base64.getDecoder().decode(val), StandardCharsets.UTF_8);
                            } catch (Exception e) {
                                claimMessage = val;
                            }
                        }
                        break;
                    case "status": claimStatus = val; break;
                    case "created_at": date = val; break;
                }
            }

            tableModel.addRow(new Object[]{
                    id, user, email, item, itemStatus, claimMessage, claimStatus, date, "Action"
            });
        }
    }

    // ------------------------------------------------------------------
    // Generic black‑text renderer
    // ------------------------------------------------------------------
    private static class GenericCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(Color.BLACK);
            if (!isSelected) setBackground(Color.WHITE);
            setToolTipText(value == null ? "" : value.toString());
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Item Status renderer (claimed/unclaimed from found_item)
    // ------------------------------------------------------------------
    private static class ItemStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(Color.BLACK);
            if (isSelected) return this;

            if (value == null) {
                setBackground(IT_UNKNOWN_BG);
                return this;
            }
            String s = value.toString().trim().toLowerCase();
            switch (s) {
                case "claimed":   setBackground(IT_CLAIMED_BG);   break;
                case "unclaimed": setBackground(IT_UNCLAIMED_BG); break;
                default:          setBackground(IT_UNKNOWN_BG);   break;
            }
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Claim Status renderer (pending/approved/rejected from claim)
    // ------------------------------------------------------------------
    private static class ClaimStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(Color.BLACK);
            if (isSelected) return this;

            if (value == null) {
                setBackground(Color.WHITE);
                return this;
            }
            String s = value.toString().trim().toLowerCase();
            switch (s) {
                case "approved":
                case "accepted":
                    setBackground(ST_APPROVED_BG);
                    break;
                case "rejected":
                case "denied":
                    setBackground(ST_REJECT_BG);
                    break;
                default: // pending, etc.
                    setBackground(ST_OTHER_BG);
                    break;
            }
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Renderer for Action column – themed buttons
    // ------------------------------------------------------------------
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        JButton approveBtn = themedSmallButton("Approve");
        JButton rejectBtn  = themedSmallButton("Reject");

        public ButtonRenderer() {
            setOpaque(true);
            setBackground(PANEL_IVORY);
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            add(approveBtn);
            add(rejectBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            // Enable/disable based on claim status
            String s = (String) table.getValueAt(row, 6); // claim status column
            s = (s == null) ? "" : s.trim().toLowerCase();
            boolean done = s.equals("approved") || s.equals("rejected") || s.equals("accepted") || s.equals("denied");
            approveBtn.setEnabled(!done);
            rejectBtn.setEnabled(!done);
            return this;
        }
    }

    // ------------------------------------------------------------------
    // Editor for Action column – perform approve/reject
    // ------------------------------------------------------------------
    class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        JPanel panel       = new JPanel();
        JButton approveBtn = themedSmallButton("Approve");
        JButton rejectBtn  = themedSmallButton("Reject");

        public ButtonEditor() {
            panel.setOpaque(true);
            panel.setBackground(PANEL_IVORY);
            panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.add(approveBtn);
            panel.add(rejectBtn);

            approveBtn.addActionListener(e -> performAction("approved"));
            rejectBtn.addActionListener(e -> performAction("rejected"));
        }

        private void performAction(String newStatus) {
            int row = claimsTable.getSelectedRow();
            if (row == -1) return;

            String claimId = tableModel.getValueAt(row, 0).toString();
            String pickupNote = "";

            if ("approved".equals(newStatus)) {
                pickupNote = JOptionPane.showInputDialog(AdminClaimsPanel.this, "Enter pickup note:");
                if (pickupNote == null || pickupNote.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(AdminClaimsPanel.this, "Pickup note required.");
                    fireEditingStopped();
                    return;
                }
            }

            try {
                URL url = new URL("http://localhost/lostfound/update_claim_status.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String data = "claim_id=" + URLEncoder.encode(claimId, "UTF-8")
                        + "&status=" + URLEncoder.encode(newStatus, "UTF-8")
                        + "&pickup_note=" + URLEncoder.encode(pickupNote, "UTF-8");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(data.getBytes(StandardCharsets.UTF_8));
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String response = in.readLine();
                in.close();

                if (response != null && response.contains("success")) {
                    JOptionPane.showMessageDialog(AdminClaimsPanel.this, "Claim " + newStatus + " successfully.");
                    fetchClaims(); // refresh
                } else {
                    JOptionPane.showMessageDialog(AdminClaimsPanel.this, "Error: " + response);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(AdminClaimsPanel.this, "Error updating claim: " + ex.getMessage());
            }

            fireEditingStopped();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            String s = (String) table.getValueAt(row, 6); // claim status column
            s = (s == null) ? "" : s.trim().toLowerCase();
            boolean done = s.equals("approved") || s.equals("rejected") || s.equals("accepted") || s.equals("denied");
            approveBtn.setEnabled(!done);
            rejectBtn.setEnabled(!done);
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    // ------------------------------------------------------------------
    // Small themed button helper (black text)
    // ------------------------------------------------------------------
    private JButton themedSmallButton(String text) {
        JButton b = new JButton(text);
        b.setFont(BTN_FONT);
        b.setForeground(Color.BLACK);
        b.setBackground(ACCENT_BROWN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN_D),
                BorderFactory.createEmptyBorder(4,10,4,10)));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN); }
        });
        return b;
    }
}
