package parking;

import parking.dao.TicketDAO;
import parking.model.ParkingSlot;
import parking.model.Ticket;
import parking.model.Vehicle;
import parking.service.ParkingService;
import parking.service.ReportService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

public class ParkingFrame extends JFrame {

    private final ParkingService service;
    private final ReportService reportService;

    private JPanel pnlEntry, pnlSlots, pnlStatus;
    private JScrollPane jScrollPane1;
    private JTable tblSlots;
    private JLabel lblNumber, lblType, lblMessage, lblFree, lblFee;
    private JTextField txtNumber;
    private JComboBox<String> cmbType;
    private JButton btnPark, btnExit, btnReport;

    public ParkingFrame() {
        TicketDAO dao = null;
        try {
            dao = new TicketDAO();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage() + " - running without database");
        }
        service = new ParkingService(5, 5, dao);
        reportService = new ReportService(service);
        initComponents();
        refreshTable();
    }

    private void initComponents() {
        setTitle("Smart Parking Slot Allocator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(null);
        setResizable(false);

        pnlEntry = new JPanel(null);
        pnlEntry.setBorder(new TitledBorder("Vehicle entry"));
        pnlEntry.setBounds(10, 10, 590, 120);

        lblNumber = new JLabel("Vehicle number");
        lblNumber.setBounds(16, 30, 100, 20);
        txtNumber = new JTextField();
        txtNumber.setBounds(120, 26, 160, 28);
        lblType = new JLabel("Type");
        lblType.setBounds(300, 30, 40, 20);
        cmbType = new JComboBox<>(new String[]{"CAR", "BIKE"});
        cmbType.setBounds(345, 26, 100, 28);
        btnPark = new JButton("Park");
        btnPark.setBounds(16, 75, 110, 30);
        btnExit = new JButton("Exit");
        btnExit.setBounds(140, 75, 110, 30);
        btnReport = new JButton("Report");
        btnReport.setBounds(264, 75, 110, 30);

        for (JComponent c : new JComponent[]{lblNumber, txtNumber, lblType, cmbType, btnPark, btnExit, btnReport}) {
            pnlEntry.add(c);
        }

        pnlSlots = new JPanel(null);
        pnlSlots.setBorder(new TitledBorder("Slot status"));
        pnlSlots.setBounds(10, 140, 590, 190);
        tblSlots = new JTable(new DefaultTableModel(new String[]{"Slot ID", "Type", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        });
        jScrollPane1 = new JScrollPane(tblSlots);
        jScrollPane1.setBounds(10, 20, 570, 160);
        pnlSlots.add(jScrollPane1);

        pnlStatus = new JPanel(null);
        pnlStatus.setBorder(new TitledBorder("Status"));
        pnlStatus.setBounds(10, 340, 590, 100);
        lblMessage = new JLabel("Ready");
        lblMessage.setBounds(16, 25, 560, 25);
        lblFree = new JLabel("Free slots: 0");
        lblFree.setBounds(16, 60, 270, 25);
        lblFee = new JLabel("Last fee: 0");
        lblFee.setBounds(300, 60, 270, 25);
        pnlStatus.add(lblMessage);
        pnlStatus.add(lblFree);
        pnlStatus.add(lblFee);

        add(pnlEntry);
        add(pnlSlots);
        add(pnlStatus);

        btnPark.addActionListener(evt -> btnParkActionPerformed());
        btnExit.addActionListener(evt -> btnExitActionPerformed());
        btnReport.addActionListener(evt -> btnReportActionPerformed());

        getContentPane().setPreferredSize(new java.awt.Dimension(610, 450));
        pack();
        setLocationRelativeTo(null);
    }

    private void btnParkActionPerformed() {
        try {
            Vehicle v = new Vehicle(txtNumber.getText().trim(), cmbType.getSelectedItem().toString());
            Ticket t = service.park(v);
            lblMessage.setText(t.getVehicle().getNumber() + " parked in slot " + t.getSlot().getSlotId());
            txtNumber.setText("");
        } catch (RuntimeException e) {
            lblMessage.setText(e.getMessage());
        }
        refreshTable();
    }

    private void btnExitActionPerformed() {
        try {
            Ticket t = service.exit(txtNumber.getText());
            lblMessage.setText(t.getVehicle().getNumber() + " left slot " + t.getSlot().getSlotId());
            lblFee.setText("Last fee: Rs " + String.format("%.0f", t.getFee()));
            txtNumber.setText("");
        } catch (RuntimeException e) {
            lblMessage.setText(e.getMessage());
        }
        refreshTable();
    }

    private void btnReportActionPerformed() {
        JOptionPane.showMessageDialog(this, reportService.buildReport(), "Daily report",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void refreshTable() {
        DefaultTableModel model = (DefaultTableModel) tblSlots.getModel();
        model.setRowCount(0);
        for (ParkingSlot s : service.getSlots()) {
            model.addRow(new Object[]{s.getSlotId(), s.getSupportedType(), s.isOccupied() ? "Occupied" : "Free"});
        }
        lblFree.setText("Free slots: " + service.freeCount());
    }
}
