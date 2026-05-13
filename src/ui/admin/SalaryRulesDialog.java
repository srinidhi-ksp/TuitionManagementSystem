package ui.admin;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import dao.SalaryRulesDAO;
import model.SalaryRules;

/**
 * Salary Rules Configuration dialog — spec §9.4.
 * Four spinners backed by the salary_rules MongoDB document.
 * Live preview panel recomputes in-memory with no DB call.
 */
public class SalaryRulesDialog extends JDialog {

    private static final Color NAV_BG   = new Color(10, 27, 63);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color TEXT_PRI = new Color(26, 35, 64);
    private static final Color TEXT_SEC = new Color(107, 122, 153);
    private static final Color ACCENT   = new Color(59, 130, 246);

    // Preview values (fixed so preview is consistent)
    private static final int PREVIEW_ABSENT = 4;
    private static final int PREVIEW_EXTRA  = 2;

    private JSpinner freeSpinner;
    private JSpinner baseDeductSpinner;
    private JSpinner incrSpinner;
    private JSpinner bonusSpinner;

    // Preview labels
    private JLabel previewDeductLbl;
    private JLabel previewBonusLbl;
    private JLabel previewNetLbl;

    private SalaryRulesDAO rulesDAO;

    public SalaryRulesDialog(Frame parent) {
        super(parent, "Salary Rules Configuration", true);
        this.rulesDAO = new SalaryRulesDAO();
        SalaryRules rules = rulesDAO.findDefault();

        setLayout(new BorderLayout());
        getContentPane().setBackground(CARD_BG);

        // ── Title bar ─────────────────────────────────────────────────────────
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(NAV_BG);
        titleBar.setBorder(new EmptyBorder(14, 20, 14, 20));
        JLabel titleLbl = new JLabel("⚙  Salary Rules Configuration");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(Color.WHITE);
        titleBar.add(titleLbl, BorderLayout.WEST);
        add(titleBar, BorderLayout.NORTH);

        // ── Form ──────────────────────────────────────────────────────────────
        JPanel form = new JPanel(new GridLayout(0, 2, 16, 14));
        form.setBorder(new EmptyBorder(20, 24, 8, 24));
        form.setBackground(CARD_BG);

        freeSpinner       = makeSpinner(rules.getFreeDaysAllowed(),          0, 30, 1);
        baseDeductSpinner = makeSpinner(rules.getBaseDeductionPerAbsentDay(), 0, 10000, 50);
        incrSpinner       = makeSpinner(rules.getDeductionIncrementPerDay(),  0, 5000, 50);
        bonusSpinner      = makeSpinner(rules.getBonusPerExtraSlot(),         0, 5000, 50);

        form.add(label("Free absent days (grace)"));
        form.add(freeSpinner);
        form.add(label("Deduction per absent day (₹)"));
        form.add(baseDeductSpinner);
        form.add(label("Increment per subsequent day (₹)"));
        form.add(incrSpinner);
        form.add(label("Bonus per extra slot (₹)"));
        form.add(bonusSpinner);

        // ── Live preview panel ─────────────────────────────────────────────────
        JPanel previewCard = new JPanel(new GridLayout(0, 1, 0, 6));
        previewCard.setBackground(new Color(241, 245, 249));
        previewCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
            new EmptyBorder(14, 16, 14, 16)
        ));
        JLabel previewTitle = new JLabel(
            "Preview (" + PREVIEW_ABSENT + " absent days, " + PREVIEW_EXTRA + " extra slots)");
        previewTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        previewTitle.setForeground(TEXT_PRI);

        previewDeductLbl = new JLabel("Deduction : —");
        previewBonusLbl  = new JLabel("Bonus     : —");
        previewNetLbl    = new JLabel("Net effect: —");

        for (JLabel l : new JLabel[]{previewDeductLbl, previewBonusLbl, previewNetLbl}) {
            l.setFont(new Font("Monospaced", Font.PLAIN, 12));
            l.setForeground(TEXT_PRI);
        }

        previewCard.add(previewTitle);
        previewCard.add(previewDeductLbl);
        previewCard.add(previewBonusLbl);
        previewCard.add(previewNetLbl);

        JPanel previewWrapper = new JPanel(new BorderLayout());
        previewWrapper.setBackground(CARD_BG);
        previewWrapper.setBorder(new EmptyBorder(0, 24, 16, 24));
        previewWrapper.add(previewCard, BorderLayout.CENTER);

        // ── Centre section ────────────────────────────────────────────────────
        JPanel centre = new JPanel(new BorderLayout());
        centre.setBackground(CARD_BG);
        centre.add(form, BorderLayout.NORTH);
        centre.add(previewWrapper, BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        // ── Buttons ───────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        btnRow.setBackground(new Color(248, 250, 253));
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 235, 245)));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        btnRow.add(cancelBtn);

        JButton saveBtn = new JButton("Save Rules");
        saveBtn.setBackground(ACCENT);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setFocusPainted(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> saveRules());
        btnRow.add(saveBtn);

        add(btnRow, BorderLayout.SOUTH);

        // Wire live preview update to all spinners
        ChangeListener preview = (ChangeEvent ce) -> updatePreview();
        freeSpinner.addChangeListener(preview);
        baseDeductSpinner.addChangeListener(preview);
        incrSpinner.addChangeListener(preview);
        bonusSpinner.addChangeListener(preview);
        updatePreview(); // initial render

        setSize(520, 480);
        setLocationRelativeTo(parent);
    }

    // ── Live preview ─────────────────────────────────────────────────────────

    private void updatePreview() {
        int freeDays   = (int) freeSpinner.getValue();
        int baseDeduct = (int) baseDeductSpinner.getValue();
        int increment  = (int) incrSpinner.getValue();
        int bonus      = (int) bonusSpinner.getValue();

        // Build a temporary rules object and calculate
        SalaryRules tmp = new SalaryRules();
        tmp.setFreeDaysAllowed(freeDays);
        tmp.setBaseDeductionPerAbsentDay(baseDeduct);
        tmp.setDeductionIncrementPerDay(increment);
        tmp.setBonusPerExtraSlot(bonus);

        double deduct    = tmp.calculateDeduction(PREVIEW_ABSENT);
        double bonusAmt  = tmp.calculateBonus(PREVIEW_EXTRA);
        double net       = bonusAmt - deduct;

        previewDeductLbl.setText(String.format("Deduction : ₹%,.0f", deduct));
        previewBonusLbl.setText( String.format("Bonus     : ₹%,.0f", bonusAmt));
        previewNetLbl.setText(   String.format("Net effect: %s₹%,.0f", net < 0 ? "−" : "+", Math.abs(net)));
        previewNetLbl.setForeground(net < 0 ? new Color(220, 38, 38) : new Color(22, 163, 74));
    }

    // ── Save to DB ────────────────────────────────────────────────────────────

    private void saveRules() {
        SalaryRules rules = new SalaryRules();
        rules.setFreeDaysAllowed((int) freeSpinner.getValue());
        rules.setBaseDeductionPerAbsentDay((int) baseDeductSpinner.getValue());
        rules.setDeductionIncrementPerDay((int) incrSpinner.getValue());
        rules.setBonusPerExtraSlot((int) bonusSpinner.getValue());
        rulesDAO.upsert(rules);

        JOptionPane.showMessageDialog(this,
            "Rules saved. Click 'Calculate All' to apply new rates to the current month.",
            "Saved", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JSpinner makeSpinner(int value, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 13));
        return spinner;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(TEXT_SEC);
        return l;
    }
}
