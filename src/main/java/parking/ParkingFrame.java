package parking;

import parking.dao.TicketDAO;
import parking.model.ParkingSlot;
import parking.model.Ticket;
import parking.model.Vehicle;
import parking.service.ParkingService;
import parking.service.ReportService;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main window of the Smart Parking Slot Allocator.
 * All business logic stays in ParkingService; this class only draws the screen
 * and forwards button clicks to the service.
 */
public class ParkingFrame extends JFrame {

    // ---------- look and feel constants ----------
    private static final Color BG = new Color(0xF3F5F9);
    private static final Color HEADER = new Color(0x1F2A44);
    private static final Color ACCENT = new Color(0x2E6BE6);
    private static final Color DANGER = new Color(0xD9534F);
    private static final Color OK_GREEN = new Color(0x2E9E55);
    private static final Color CARD_BORDER = new Color(0xDFE4EE);
    private static final Color TEXT_DARK = new Color(0x1E2433);
    private static final Color TEXT_MUTED = new Color(0x6B7488);
    private static final Font BASE = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font BOLD = BASE.deriveFont(Font.BOLD);

    private static final int CAR_SLOTS = 5;
    private static final int BIKE_SLOTS = 5;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  hh:mm:ss a");

    // ---------- backend ----------
    private final ParkingService service;
    private final ReportService reportService;

    // ---------- components ----------
    private final JTextField txtNumber = new JTextField();
    private final JComboBox<String> cmbType = new JComboBox<>(new String[]{ParkingService.CAR, ParkingService.BIKE});
    private final JLabel lblMessage = new JLabel(" ");
    private final JLabel lblClock = new JLabel();
    private final StatCard statCarFree = new StatCard("Free car slots");
    private final StatCard statBikeFree = new StatCard("Free bike slots");
    private final StatCard statParked = new StatCard("Vehicles parked");
    private final StatCard statRevenue = new StatCard("Total collection");
    private final JPanel carGrid = new JPanel(new GridLayout(0, 5, 12, 12));
    private final JPanel bikeGrid = new JPanel(new GridLayout(0, 5, 12, 12));
    private final JTextArea txtReport = new JTextArea();
    private JTable tblActive;
    private JTable tblHistory;
    private DefaultTableModel activeModel;
    private DefaultTableModel historyModel;
    private boolean updatingTable = false;
    private int tick = 0;

    public ParkingFrame() {
        TicketDAO dao = null;
        try {
            dao = new TicketDAO();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage() + " - running without database");
        }
        service = new ParkingService(CAR_SLOTS, BIKE_SLOTS, dao);
        reportService = new ReportService(service);

        buildUi();
        refreshAll();
        startTimer();
    }

    // =====================================================================
    //  UI construction
    // =====================================================================

    private void buildUi() {
        setTitle("Smart Parking Slot Allocator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        add(buildHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(BG);
        body.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        body.add(buildSidePanel(), BorderLayout.WEST);
        body.add(buildTabs(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        setMinimumSize(new Dimension(1000, 640));
        setSize(1100, 700);
        setLocationRelativeTo(null);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Smart Parking Slot Allocator");
        title.setForeground(Color.WHITE);
        title.setFont(BASE.deriveFont(Font.BOLD, 20f));

        JLabel sub = new JLabel("CAR Rs 20/hour  |  BIKE Rs 10/hour  |  hours rounded up, minimum 1 hour");
        sub.setForeground(new Color(0xA9B4CC));
        sub.setFont(BASE.deriveFont(12f));

        JPanel left = new JPanel(new GridLayout(2, 1, 0, 2));
        left.setOpaque(false);
        left.add(title);
        left.add(sub);

        lblClock.setForeground(Color.WHITE);
        lblClock.setFont(BASE.deriveFont(Font.BOLD, 14f));

        header.add(left, BorderLayout.WEST);
        header.add(lblClock, BorderLayout.EAST);
        return header;
    }

    private JComponent buildSidePanel() {
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(290, 0));

        // --- entry / exit form ---
        JPanel form = card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;

        JLabel heading = new JLabel("Vehicle entry / exit");
        heading.setFont(BASE.deriveFont(Font.BOLD, 15f));
        heading.setForeground(TEXT_DARK);
        gc.gridy = 0;
        gc.insets = new Insets(0, 0, 12, 0);
        form.add(heading, gc);

        gc.gridy = 1;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(fieldLabel("Vehicle number"), gc);

        styleField(txtNumber);
        gc.gridy = 2;
        gc.insets = new Insets(0, 0, 10, 0);
        form.add(txtNumber, gc);

        gc.gridy = 3;
        gc.insets = new Insets(0, 0, 4, 0);
        form.add(fieldLabel("Vehicle type"), gc);

        cmbType.setFont(BASE);
        cmbType.setPreferredSize(new Dimension(0, 34));
        gc.gridy = 4;
        gc.insets = new Insets(0, 0, 14, 0);
        form.add(cmbType, gc);

        RoundButton btnPark = new RoundButton("Park", ACCENT);
        RoundButton btnExit = new RoundButton("Exit", DANGER);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(btnPark);
        buttons.add(btnExit);
        gc.gridy = 5;
        gc.insets = new Insets(0, 0, 12, 0);
        form.add(buttons, gc);

        lblMessage.setFont(BASE);
        lblMessage.setVerticalAlignment(SwingConstants.TOP);
        lblMessage.setPreferredSize(new Dimension(230, 40)); // room for two lines so the layout does not jump
        gc.gridy = 6;
        gc.insets = new Insets(0, 0, 0, 0);
        form.add(lblMessage, gc);
        showMessage("Ready. Enter a vehicle number.", true);

        btnPark.addActionListener(e -> onPark());
        btnExit.addActionListener(e -> onExit());
        txtNumber.addActionListener(e -> onPark()); // Enter key parks the vehicle

        // --- stat cards (2 x 2) ---
        JPanel stats = new JPanel(new GridLayout(2, 2, 10, 10));
        stats.setOpaque(false);
        stats.add(statCarFree);
        stats.add(statBikeFree);
        stats.add(statParked);
        stats.add(statRevenue);

        stats.setPreferredSize(new Dimension(0, 150));

        JPanel top = new JPanel(new BorderLayout(0, 14));
        top.setOpaque(false);
        top.add(form, BorderLayout.NORTH);
        top.add(stats, BorderLayout.CENTER);
        side.add(top, BorderLayout.NORTH);
        return side;
    }

    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BOLD);
        tabs.addTab("Slots", buildSlotsTab());
        tabs.addTab("Parked vehicles", buildActiveTab());
        tabs.addTab("History", buildHistoryTab());
        tabs.addTab("Report", buildReportTab());
        tabs.addChangeListener(e -> refreshAll());
        return tabs;
    }

    private JComponent buildSlotsTab() {
        carGrid.setOpaque(false);
        bikeGrid.setOpaque(false);

        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        content.add(sectionLabel("Car slots"));
        content.add(Box.createVerticalStrut(8));
        content.add(carGrid);
        content.add(Box.createVerticalStrut(22));
        content.add(sectionLabel("Bike slots"));
        content.add(Box.createVerticalStrut(8));
        content.add(bikeGrid);
        content.add(Box.createVerticalStrut(18));

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        legend.setOpaque(false);
        legend.add(legendItem(OK_GREEN, "Free"));
        legend.add(legendItem(DANGER, "Occupied"));
        JLabel hint = new JLabel("Click an occupied slot to fill its vehicle number for exit.");
        hint.setForeground(TEXT_MUTED);
        hint.setFont(BASE.deriveFont(12f));
        legend.add(hint);
        content.add(legend);

        for (Component c : content.getComponents()) {
            if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(content, BorderLayout.NORTH);
        wrap.setBackground(Color.WHITE);
        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        return sp;
    }

    private JComponent buildActiveTab() {
        activeModel = new DefaultTableModel(
                new String[]{"Vehicle no.", "Type", "Slot", "Entry time", "Duration", "Fee so far"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblActive = new JTable(activeModel);
        styleTable(tblActive);
        tblActive.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblActive.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || updatingTable) return;
            int row = tblActive.getSelectedRow();
            if (row >= 0) txtNumber.setText(String.valueOf(activeModel.getValueAt(row, 0)));
        });
        return tableScroll(tblActive);
    }

    private JComponent buildHistoryTab() {
        historyModel = new DefaultTableModel(
                new String[]{"Vehicle no.", "Type", "Slot", "Entry time", "Exit time", "Duration", "Fee"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblHistory = new JTable(historyModel);
        styleTable(tblHistory);
        return tableScroll(tblHistory);
    }

    private JComponent buildReportTab() {
        txtReport.setEditable(false);
        txtReport.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        txtReport.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        txtReport.setForeground(TEXT_DARK);
        JScrollPane sp = new JScrollPane(txtReport);
        sp.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        return sp;
    }

    // =====================================================================
    //  Button actions
    // =====================================================================

    private void onPark() {
        try {
            String number = txtNumber.getText().trim();
            Vehicle v = new Vehicle(number, String.valueOf(cmbType.getSelectedItem()));
            Ticket t = service.park(v);
            showMessage(t.getVehicle().getNumber() + " parked in slot " + t.getSlot().getSlotId(), true);
            txtNumber.setText("");
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), false);
        }
        refreshAll();
        txtNumber.requestFocusInWindow();
    }

    private void onExit() {
        try {
            if (txtNumber.getText().trim().isEmpty()) {
                throw new IllegalArgumentException("Enter or select the vehicle that is leaving");
            }
            Ticket t = service.exit(txtNumber.getText());
            showMessage(t.getVehicle().getNumber() + " left slot " + t.getSlot().getSlotId()
                    + " | Fee Rs " + String.format("%.0f", t.getFee()), true);
            txtNumber.setText("");
            refreshAll();
            showReceipt(t);
        } catch (RuntimeException ex) {
            showMessage(ex.getMessage(), false);
            refreshAll();
        }
        txtNumber.requestFocusInWindow();
    }

    private void showReceipt(Ticket t) {
        String text = String.format(
                "Vehicle   : %s (%s)%nSlot      : %d%nEntry     : %s%nExit      : %s%nDuration  : %s%n%nAMOUNT    : Rs %.0f",
                t.getVehicle().getNumber(), t.getVehicle().getType(), t.getSlot().getSlotId(),
                t.getInTime().format(DATE_TIME), t.getOutTime().format(DATE_TIME),
                formatDuration(t.getInTime(), t.getOutTime()), t.getFee());
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, area, "Exit receipt", JOptionPane.INFORMATION_MESSAGE);
    }

    // =====================================================================
    //  Refreshing the screen from the service
    // =====================================================================

    private void refreshAll() {
        List<ParkingSlot> slots = service.getSlots();
        List<Ticket> active = service.getActiveTickets();
        List<Ticket> done = service.getCompletedTickets();

        Map<Integer, Ticket> bySlot = new HashMap<>();
        for (Ticket t : active) bySlot.put(t.getSlot().getSlotId(), t);

        carGrid.removeAll();
        bikeGrid.removeAll();
        int carTotal = 0, bikeTotal = 0, carFree = 0, bikeFree = 0;
        for (ParkingSlot s : slots) {
            SlotCard card = new SlotCard(s, bySlot.get(s.getSlotId()));
            boolean isCar = ParkingService.CAR.equals(s.getSupportedType());
            if (isCar) {
                carTotal++;
                if (!s.isOccupied()) carFree++;
                carGrid.add(card);
            } else {
                bikeTotal++;
                if (!s.isOccupied()) bikeFree++;
                bikeGrid.add(card);
            }
        }
        carGrid.revalidate();
        carGrid.repaint();
        bikeGrid.revalidate();
        bikeGrid.repaint();

        double revenue = 0;
        for (Ticket t : done) revenue += t.getFee();

        statCarFree.setValue(carFree + " / " + carTotal, carFree == 0 ? DANGER : OK_GREEN);
        statBikeFree.setValue(bikeFree + " / " + bikeTotal, bikeFree == 0 ? DANGER : OK_GREEN);
        statParked.setValue(String.valueOf(active.size()), ACCENT);
        statRevenue.setValue("Rs " + String.format("%.0f", revenue), TEXT_DARK);

        refreshActiveTable(active);
        refreshHistoryTable(done);
        txtReport.setText(reportService.buildReport());
        txtReport.setCaretPosition(0);
    }

    private void refreshActiveTable(List<Ticket> active) {
        String selected = null;
        int row = tblActive.getSelectedRow();
        if (row >= 0) selected = String.valueOf(activeModel.getValueAt(row, 0));

        updatingTable = true;
        activeModel.setRowCount(0);
        List<Ticket> sorted = new ArrayList<>(active);
        sorted.sort(Comparator.comparing(Ticket::getInTime));
        LocalDateTime now = LocalDateTime.now();
        for (Ticket t : sorted) {
            activeModel.addRow(new Object[]{
                    t.getVehicle().getNumber(), t.getVehicle().getType(), t.getSlot().getSlotId(),
                    t.getInTime().format(DATE_TIME), formatDuration(t.getInTime(), now),
                    "Rs " + String.format("%.0f", service.currentFee(t))});
        }
        if (selected != null) {
            for (int i = 0; i < activeModel.getRowCount(); i++) {
                if (selected.equals(activeModel.getValueAt(i, 0))) {
                    tblActive.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        updatingTable = false;
    }

    private void refreshHistoryTable(List<Ticket> done) {
        historyModel.setRowCount(0);
        for (int i = done.size() - 1; i >= 0; i--) { // newest first
            Ticket t = done.get(i);
            historyModel.addRow(new Object[]{
                    t.getVehicle().getNumber(), t.getVehicle().getType(), t.getSlot().getSlotId(),
                    t.getInTime().format(DATE_TIME), t.getOutTime().format(DATE_TIME),
                    formatDuration(t.getInTime(), t.getOutTime()),
                    "Rs " + String.format("%.0f", t.getFee())});
        }
    }

    private void startTimer() {
        lblClock.setText(LocalDateTime.now().format(CLOCK));
        new Timer(1000, e -> {
            lblClock.setText(LocalDateTime.now().format(CLOCK));
            if (++tick % 30 == 0) refreshActiveTable(service.getActiveTickets()); // keep durations/fees current
        }).start();
    }

    // =====================================================================
    //  Small helpers
    // =====================================================================

    private void showMessage(String text, boolean ok) {
        lblMessage.setForeground(ok ? OK_GREEN : DANGER);
        lblMessage.setText("<html><body style='width:230px'>" + escape(text) + "</body></html>");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String formatDuration(LocalDateTime from, LocalDateTime to) {
        long minutes = Math.max(0, Duration.between(from, to).toMinutes());
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    private static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        return p;
    }

    private static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(12f));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(Font.BOLD, 15f));
        l.setForeground(TEXT_DARK);
        return l;
    }

    private static JComponent legendItem(Color c, String text) {
        JLabel l = new JLabel(text);
        l.setFont(BASE.deriveFont(12f));
        l.setForeground(TEXT_MUTED);
        l.setIcon(new Icon() {
            public void paintIcon(Component comp, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillOval(x, y, 10, 10);
                g2.dispose();
            }
            public int getIconWidth() { return 10; }
            public int getIconHeight() { return 10; }
        });
        l.setIconTextGap(6);
        return l;
    }

    private static void styleField(JTextField f) {
        f.setFont(BASE.deriveFont(14f));
        Border line = BorderFactory.createLineBorder(new Color(0xC9D0DE));
        f.setBorder(BorderFactory.createCompoundBorder(line, BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setPreferredSize(new Dimension(0, 36));
    }

    private static void styleTable(JTable t) {
        t.setFont(BASE);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(new Color(0xDCE7FB));
        t.setSelectionForeground(TEXT_DARK);
        t.setFillsViewportHeight(true);
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        t.setDefaultRenderer(Object.class, r);
        JTableHeader h = t.getTableHeader();
        h.setFont(BOLD);
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(0, 34));
        // give the date/time columns enough room, keep the others narrow
        for (int i = 0; i < t.getColumnCount(); i++) {
            boolean isTime = t.getColumnName(i).toLowerCase().contains("time");
            t.getColumnModel().getColumn(i).setPreferredWidth(isTime ? 200 : (i == 0 ? 130 : 80));
        }
    }

    private static JScrollPane tableScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    // =====================================================================
    //  Custom components
    // =====================================================================

    /** Rounded flat button that looks the same on every look and feel. */
    private static class RoundButton extends JButton {
        private final Color base;

        RoundButton(String text, Color base) {
            super(text);
            this.base = base;
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(BOLD);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(100, 38));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = base;
            if (getModel().isPressed()) c = base.darker();
            else if (getModel().isRollover()) c = base.brighter();
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small card with a caption and a big number. */
    private static class StatCard extends JPanel {
        private final JLabel value = new JLabel("-");

        StatCard(String caption) {
            setLayout(new GridLayout(2, 1, 0, 2));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(CARD_BORDER),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)));
            JLabel cap = new JLabel(caption);
            cap.setFont(BASE.deriveFont(11.5f));
            cap.setForeground(TEXT_MUTED);
            value.setFont(BASE.deriveFont(Font.BOLD, 20f));
            add(cap);
            add(value);
        }

        void setValue(String text, Color color) {
            value.setText(text);
            value.setForeground(color);
        }
    }

    /** One parking slot drawn as a rounded tile: green when free, red when occupied. */
    private class SlotCard extends JPanel {
        private final ParkingSlot slot;
        private final Ticket ticket;

        SlotCard(ParkingSlot slot, Ticket ticket) {
            this.slot = slot;
            this.ticket = ticket;
            setOpaque(false);
            setPreferredSize(new Dimension(110, 88));
            if (ticket != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText(ticket.getVehicle() + " | in " + ticket.getInTime().format(DATE_TIME));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        txtNumber.setText(ticket.getVehicle().getNumber());
                        txtNumber.requestFocusInWindow();
                    }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            boolean occupied = ticket != null;
            Color main = occupied ? DANGER : OK_GREEN;
            int w = getWidth(), h = getHeight();

            g2.setColor(new Color(main.getRed(), main.getGreen(), main.getBlue(), 28));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            g2.setColor(main);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

            g2.setFont(BASE.deriveFont(Font.BOLD, 15f));
            g2.setColor(TEXT_DARK);
            g2.drawString("Slot " + slot.getSlotId(), 12, 26);

            g2.setFont(BASE.deriveFont(11f));
            g2.setColor(TEXT_MUTED);
            g2.drawString(slot.getSupportedType(), 12, 44);

            g2.setFont(BASE.deriveFont(Font.BOLD, 12f));
            g2.setColor(main);
            String line = occupied ? ticket.getVehicle().getNumber() : "FREE";
            FontMetrics fm = g2.getFontMetrics();
            while (fm.stringWidth(line) > w - 20 && line.length() > 3) line = line.substring(0, line.length() - 2);
            g2.drawString(line, 12, h - 16);
            g2.dispose();
        }
    }
}
