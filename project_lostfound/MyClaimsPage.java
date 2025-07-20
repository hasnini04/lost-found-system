package project_lostfound;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MyClaimsPage extends JFrame {

    // ------------------------------------------------------------------
    // Theme palette / fonts
    // ------------------------------------------------------------------
    private static final Color BG_BEIGE       = new Color(0xF5,0xE8,0xDA);
    private static final Color PANEL_IVORY    = new Color(0xFF,0xFB,0xF5);
    private static final Color BORDER_TAN     = new Color(0xC9,0xAA,0x88);
    private static final Color ACCENT_BROWN   = new Color(0x8B,0x5E,0x3C);
    private static final Color ACCENT_BROWN_D = new Color(0x5C,0x3B,0x1E);

    private static final Font  TITLE_FONT     = new Font("SansSerif", Font.BOLD, 22);
    private static final Font  HEADER_FONT    = new Font("SansSerif", Font.BOLD, 16);
    private static final Font  ROW_FONT       = new Font("SansSerif", Font.PLAIN, 15);
    private static final Font  BTN_FONT       = new Font("SansSerif", Font.BOLD, 15);

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------
    private JTable claimsTable;
    private DefaultTableModel tableModel;
    private final int userId;

    // we keep our own list to track found_item_id for each row
    private final List<RowData> rows = new ArrayList<>();

    // endpoints
    private static final String FETCH_ENDPOINT =
            "http://localhost/lostfound/fetch_user_claims.php?reporter_id=";
    private static final String MARK_CLAIMED_ENDPOINT =
            "http://localhost/lostfound/mark_found_item_claimed.php";

    public MyClaimsPage(int userId) {
        this.userId = userId;

        setTitle("📋 My Claim Requests");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 420);
        getContentPane().setBackground(BG_BEIGE);
        setLayout(new BorderLayout(10,10));

        // ----- Title strip -----
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ACCENT_BROWN);
        JLabel titleLbl = new JLabel("My Claim Requests", SwingConstants.CENTER);
        titleLbl.setFont(TITLE_FONT);
        titleLbl.setForeground(Color.BLACK); // <-- BLACK
        titleLbl.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        titlePanel.add(titleLbl, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        // ----- Table model -----
        tableModel = new DefaultTableModel(new String[]{"Item", "Status", "Pickup Note", "Action"}, 0) {
            @Override public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Action column clickable
            }
        };

        claimsTable = new JTable(tableModel);
        claimsTable.setRowHeight(32);
        claimsTable.setFont(ROW_FONT);
        claimsTable.setFillsViewportHeight(true);
        claimsTable.setGridColor(BORDER_TAN);

        // Header style (black text)
        JTableHeader th = claimsTable.getTableHeader();
        th.setFont(HEADER_FONT);
        th.setBackground(ACCENT_BROWN);
        th.setForeground(Color.BLACK); // <-- BLACK

        // --- Column renderers ---
        TableColumnModel cols = claimsTable.getColumnModel();

        // Item column (0) -> always black
        cols.getColumn(0).setCellRenderer(new GenericCellRenderer());

        // Status column (1) -> background by status, text black
        cols.getColumn(1).setCellRenderer(new StatusColorRenderer());

        // Pickup Note column (2) -> always black
        cols.getColumn(2).setCellRenderer(new GenericCellRenderer());

        // Action column (3) -> themed button (black text)
        TableColumn actionCol = cols.getColumn(3);
        actionCol.setCellRenderer(new ActionButtonRenderer());
        actionCol.setCellEditor(new ActionButtonEditor(new JCheckBox())); // JTable demands an editor component
        actionCol.setPreferredWidth(110);

        // Wrap table in themed panel
        JPanel tableHolder = new JPanel(new BorderLayout());
        tableHolder.setBackground(PANEL_IVORY);
        tableHolder.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_TAN),
                BorderFactory.createEmptyBorder(10,10,10,10)));

        JScrollPane scrollPane = new JScrollPane(claimsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_TAN));
        tableHolder.add(scrollPane, BorderLayout.CENTER);
        add(tableHolder, BorderLayout.CENTER);

        // ----- Bottom bar w/ Back -----
        JButton backButton = new JButton("⬅ Back to Dashboard");
        styleButton(backButton);
        backButton.addActionListener(e -> {
            dispose();
            new ReporterDashboardPage(userId);
        });

        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(BG_BEIGE);
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        fetchUserClaims();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ------------------------------------------------------------------
    // Data fetch
    // ------------------------------------------------------------------
    private void fetchUserClaims() {
        SwingWorker<Void, RowData> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                rows.clear();
                try {
                    URL url = new URL(FETCH_ENDPOINT + userId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);

                    try (BufferedReader reader =
                                 new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty()) continue;
                            // claim_id|item|status|pickup|created_at|found_item_id
                            String[] parts = line.split("\\|", -1);
                            RowData rd = new RowData();
                            rd.claimId     = parts.length > 0 ? safeInt(parts[0]) : 0;
                            rd.item        = parts.length > 1 ? parts[1].trim() : "";
                            rd.status      = parts.length > 2 ? parts[2].trim() : "";
                            rd.pickupNote  = parts.length > 3 ? parts[3].trim() : "";
                            rd.createdAt   = parts.length > 4 ? parts[4].trim() : "";
                            rd.foundItemId = parts.length > 5 ? safeInt(parts[5]) : 0;
                            publish(rd);
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            MyClaimsPage.this,
                            "Error fetching claims: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void process(List<RowData> chunk) {
                rows.addAll(chunk);
            }

            @Override
            protected void done() {
                reloadTableFromRows();
            }
        };
        worker.execute();
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception ignored) { return 0; }
    }

    /**
     * Rebuilds the JTable model from `rows`, but SKIPS any row whose status == "claimed".
     */
    private void reloadTableFromRows() {
        tableModel.setRowCount(0);
        for (RowData rd : rows) {
            String stLower = rd.status == null ? "" : rd.status.trim().toLowerCase();
            if (stLower.equals("claimed")) {
                continue; // hide
            }
            tableModel.addRow(new Object[]{
                    rd.item,
                    rd.status,
                    rd.pickupNote,
                    actionTextForStatus(rd.status)
            });
        }
    }

    private String actionTextForStatus(String status) {
        if (status == null) return "";
        String s = status.trim().toLowerCase();
        if (s.equals("approved") || s.equals("accepted")) {
            return "Claimed";   // user can click to mark as claimed
        } else {
            return "";          // nothing clickable
        }
    }

    // ------------------------------------------------------------------
    // Renderers / Editors
    // ------------------------------------------------------------------

    /** Generic renderer that forces black text & white background (unless selected). */
    private static class GenericCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setForeground(Color.BLACK);             // ALWAYS BLACK TEXT
            if (!isSelected) setBackground(Color.WHITE);
            return this;
        }
    }

    /** Status renderer: background by status, but text is BLACK. */
    private static class StatusColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setForeground(Color.BLACK);             // ALWAYS BLACK TEXT

            if (isSelected) {
                return this;
            }

            if (value == null) {
                setBackground(Color.WHITE);
                return this;
            }

            String s = value.toString().toLowerCase();
            switch (s) {
                case "approved":
                case "accepted":
                    setBackground(new Color(200, 255, 200)); // light green
                    break;
                case "rejected":
                case "denied":
                    setBackground(new Color(255, 200, 200)); // light red
                    break;
                case "pending":
                default:
                    setBackground(new Color(255, 240, 200)); // light orange
                    break;
            }
            return this;
        }
    }

    /** Renderer for the Action column. Shows a themed button when actionable. */
    private class ActionButtonRenderer extends DefaultTableCellRenderer {
        private final JButton btn = themedCellButton();
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            String label = (value == null) ? "" : value.toString();
            btn.setText(label);
            btn.setEnabled(isActionEnabledForRow(row));
            return btn;
        }
    }

    /** Editor for the Action column. Handles click -> mark claimed. */
    private class ActionButtonEditor extends DefaultCellEditor {
        private final JButton btn = themedCellButton();
        private int editingRow = -1;

        public ActionButtonEditor(JCheckBox dummy) {
            super(dummy);
            btn.addActionListener(e -> fireAction());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            editingRow = row;
            String label = (value == null) ? "" : value.toString();
            btn.setText(label);
            btn.setEnabled(isActionEnabledForRow(row));
            return btn;
        }

        @Override
        public Object getCellEditorValue() {
            return btn.getText();
        }

        private void fireAction() {
            if (editingRow >= 0) {
                int modelIndex = tableRowToRowsIndex(editingRow);
                if (modelIndex >= 0 && modelIndex < rows.size() &&
                        isActionEnabledForStatus(rows.get(modelIndex).status)) {
                    markItemClaimed(rows.get(modelIndex));
                }
            }
            fireEditingStopped();
        }
    }

    /** Create a small themed button for table cells (black text). */
    private JButton themedCellButton() {
        JButton b = new JButton();
        b.setFont(BTN_FONT);
        b.setForeground(Color.BLACK);  // BLACK TEXT
        b.setBackground(ACCENT_BROWN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(ACCENT_BROWN_D));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { if (b.isEnabled()) b.setBackground(ACCENT_BROWN); }
        });
        return b;
    }

    /**
     * Map visible table row to the corresponding index in `rows`,
     * skipping any rows that were filtered out (claimed).
     */
    private int tableRowToRowsIndex(int visibleRow) {
        int count = -1;
        for (int i = 0; i < rows.size(); i++) {
            String stLower = rows.get(i).status == null ? "" : rows.get(i).status.trim().toLowerCase();
            if (stLower.equals("claimed")) {
                continue; // filtered out
            }
            count++;
            if (count == visibleRow) return i;
        }
        return -1;
    }

    private boolean isActionEnabledForRow(int visibleRow) {
        int idx = tableRowToRowsIndex(visibleRow);
        if (idx < 0 || idx >= rows.size()) return false;
        return isActionEnabledForStatus(rows.get(idx).status);
    }

    private boolean isActionEnabledForStatus(String status) {
        if (status == null) return false;
        String s = status.trim().toLowerCase();
        return s.equals("approved") || s.equals("accepted");
    }

    // ------------------------------------------------------------------
    // Mark found_item as claimed (server call)
    // ------------------------------------------------------------------
    private void markItemClaimed(RowData rd) {
        if (rd.foundItemId <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Item ID missing; cannot mark claimed.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark this item as CLAIMED?\n\n" + rd.item,
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            boolean success = false;
            String message = "";
            @Override
            protected Void doInBackground() {
                try {
                    URL url = new URL(MARK_CLAIMED_ENDPOINT);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    String postData = "found_item_id=" + rd.foundItemId +
                                      "&reporter_id=" + userId;
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(postData.getBytes(StandardCharsets.UTF_8));
                    }

                    try (BufferedReader in = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) sb.append(line);
                        String resp = sb.toString().toLowerCase();
                        success = resp.contains("\"success\"") || resp.contains("success");
                        message = resp;
                    }
                } catch (Exception ex) {
                    message = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(MyClaimsPage.this,
                            "Item marked as claimed. Thank you!");
                    fetchUserClaims(); // refresh from server
                } else {
                    JOptionPane.showMessageDialog(MyClaimsPage.this,
                            "Failed to mark claimed:\n" + message,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ------------------------------------------------------------------
    // Style helper for bottom/back button
    // ------------------------------------------------------------------
    private void styleButton(AbstractButton b) {
        b.setFont(BTN_FONT);
        b.setForeground(Color.BLACK);     // BLACK TEXT
        b.setBackground(ACCENT_BROWN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_BROWN_D),
                BorderFactory.createEmptyBorder(6,14,6,14)));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(ACCENT_BROWN_D); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(ACCENT_BROWN); }
        });
    }

    // ------------------------------------------------------------------
    // Model row holder
    // ------------------------------------------------------------------
    private static class RowData {
        int claimId;
        String item = "";
        String status = "";
        String pickupNote = "";
        String createdAt = "";
        int foundItemId;
    }
}
