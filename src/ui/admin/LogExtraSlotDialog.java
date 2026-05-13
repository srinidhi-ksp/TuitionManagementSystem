package ui.admin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Calendar;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import dao.BatchDAO;
import dao.SalaryRulesDAO;
import dao.TeacherExtraSlotDAO;
import dao.TimeslotDAO;
import model.Batch;
import model.SalaryRules;
import model.TeacherExtraSlot;
import model.Teacher;
import model.Timeslot;
import service.SalaryService;

/**
 * Dialog for manually logging an extra slot taught by a teacher — spec §9.3.
 * After saving, triggers recalculation for this teacher and notifies the caller.
 */
public class LogExtraSlotDialog extends JDialog {

    private static final Color NAV_BG   = new Color(10, 27, 63);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color TEXT_SEC = new Color(107, 122, 153);
    private static final Color ACCENT   = new Color(59, 130, 246);

    private final Teacher teacher;
    private final String month;
    private final String year;
    private final Runnable onSaved; // callback to refresh the salary table row

    public LogExtraSlotDialog(Frame parent, Teacher teacher, String month, String year, Runnable onSaved) {
        super(parent, "Log Extra Slot — " + teacher.getName() + " (" + teacher.getUserId() + ")", true);
        this.teacher = teacher;
        this.month   = month;
        this.year    = year;
        this.onSaved = onSaved;

        setLayout(new BorderLayout());
        getContentPane().setBackground(CARD_BG);

        // ── Title bar ─────────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(NAV_BG);
        titleBar.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel titleLbl = new JLabel("Log Extra Slot — " + teacher.getName() + " (" + teacher.getUserId() + ")");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);
        add(titleBar, BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridLayout(0, 2, 16, 12));
        form.setBorder(new EmptyBorder(24, 24, 16, 24));
        form.setBackground(CARD_BG);

        // Date field
        JTextField dateField = new JTextField(
            year + "-" + month + "-" + String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        styleTextField(dateField);

        // Batch combo — all active batches
        JComboBox<String> batchCombo = new JComboBox<>();
        batchCombo.addItem("Select Batch");
        List<Batch> allBatches = new BatchDAO().findAllActive();
        for (Batch b : allBatches) {
            batchCombo.addItem(b.getBatchId() + " – " + b.getBatchName());
        }

        // Timeslot combo
        List<Timeslot> slots = new TimeslotDAO().findAllOrderedByStart();
        JComboBox<Timeslot> timeslotCombo = new JComboBox<>(slots.toArray(new Timeslot[0]));
        timeslotCombo.setRenderer((list, value, index, isSelected, hasFocus) -> {
            JLabel lbl = new JLabel(value != null ? value.getLabel() : "");
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            return lbl;
        });

        form.add(label("Date (YYYY-MM-DD)")); form.add(dateField);
        form.add(label("Batch"));             form.add(batchCombo);
        form.add(label("Time Slot"));         form.add(timeslotCombo);

        add(form, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnRow.setBackground(new Color(248, 250, 253));
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 245)));

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        btnRow.add(cancel);

        JButton save = new JButton("Log Extra Slot");
        save.setBackground(ACCENT);
        save.setForeground(Color.WHITE);
        save.setFocusPainted(false);
        save.setBorderPainted(false);
        save.setFont(new Font("SansSerif", Font.BOLD, 13));
        save.setCursor(new Cursor(Cursor.HAND_CURSOR));
        save.addActionListener((ActionEvent e) -> {
            if (batchCombo.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Please select a batch.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String dateStr = dateField.getText().trim();
            if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                JOptionPane.showMessageDialog(this, "Date must be in YYYY-MM-DD format.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Timeslot ts = (Timeslot) timeslotCombo.getSelectedItem();

            // Parse batch ID
            String selBatch = batchCombo.getSelectedItem().toString();
            String batchIdStr = selBatch.split(" – ")[0];
            String batchName  = selBatch.contains(" – ") ? selBatch.split(" – ")[1] : selBatch;

            // Load bonus rate from rules
            SalaryRules rules = new SalaryRulesDAO().findDefault();

            TeacherExtraSlot slot = new TeacherExtraSlot();
            slot.setTeacherId(teacher.getUserId());
            slot.setBatchId(batchIdStr);
            slot.setBatchName(batchName);
            slot.setDay(""); // not enforced for manual log
            slot.setTimeslotId(ts != null ? ts.getId() : "");
            slot.setDate(dateStr);
            slot.setMonth(month);
            slot.setYear(year);
            slot.setBonusAmount(rules.getBonusPerExtraSlot());

            boolean ok = new TeacherExtraSlotDAO().insert(slot);
            if (ok) {
                // Recalculate salary for this teacher only
                new SalaryService().calculateSalary(teacher.getUserId(),
                    Integer.parseInt(month), Integer.parseInt(year));
                JOptionPane.showMessageDialog(this,
                    "✅ Extra slot logged and salary updated.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                if (onSaved != null) onSaved.run();
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to log extra slot.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnRow.add(save);

        add(btnRow, BorderLayout.SOUTH);

        setSize(480, 300);
        setLocationRelativeTo(parent);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(TEXT_SEC);
        return l;
    }

    private static void styleTextField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
            new EmptyBorder(6, 10, 6, 10)));
    }
}
