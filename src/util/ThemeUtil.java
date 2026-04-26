package util;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import javax.swing.border.LineBorder;

public class ThemeUtil {

    public static void apply(Component comp) {
        if (comp == null) return;

        // Determine if sidebar (always dark)
        boolean isSidebar = false;
        Component temp = comp;
        while (temp != null) {
            if ("sidebar".equals(temp.getName())) {
                isSidebar = true;
                break;
            }
            temp = temp.getParent();
        }

        if (isSidebar) {
            applySidebarTheme(comp);
        } else {
            applyGeneralTheme(comp);
        }

        // Recursive application
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                apply(child);
            }
        }
    }

    private static void applySidebarTheme(Component comp) {
        if (comp instanceof JPanel) {
            comp.setBackground(ThemeManager.DARK_SIDEBAR);
        } else if (comp instanceof JLabel) {
            comp.setForeground(Color.WHITE);
        } else if (comp instanceof JButton) {
            comp.setForeground(Color.WHITE);
            // Highlight active/hover item if sidebar
            if ("active".equals(comp.getName())) {
                comp.setBackground(ThemeManager.DARK_HOVER);
            }
        }
    }

    private static void applyGeneralTheme(Component comp) {
        if (comp instanceof JPanel) {
            comp.setBackground(ThemeManager.BG);
        } else if (comp instanceof JLabel) {
            comp.setForeground(ThemeManager.TEXT);
            String name = comp.getName();
            if ("secondary".equals(name)) {
                comp.setForeground(ThemeManager.SUB_TEXT);
            } else if ("success".equals(name) || "PAID".equals(comp.getAccessibleContext().getAccessibleName())) {
                comp.setForeground(ThemeManager.SUCCESS);
            } else if ("warning".equals(name) || "PARTIAL".equals(comp.getAccessibleContext().getAccessibleName())) {
                comp.setForeground(ThemeManager.WARNING);
            } else if ("error".equals(name) || "UNPAID".equals(comp.getAccessibleContext().getAccessibleName())) {
                comp.setForeground(ThemeManager.ERROR);
            }
        } else if (comp instanceof JButton) {
            String name = comp.getName();
            if ("primary".equals(name) || "accent".equals(name)) {
                JComponent jc = (JComponent) comp;
                jc.setBackground(ThemeManager.PRIMARY);
                jc.setForeground(Color.WHITE);
                jc.setOpaque(true);
                jc.setBorder(null);
            } else if (comp.getClass() == JButton.class) {
                comp.setBackground(ThemeManager.CARD);
                comp.setForeground(ThemeManager.TEXT);
            }
        } else if (comp instanceof JTable) {
            JTable table = (JTable) comp;
            table.setBackground(ThemeManager.TABLE_BG);
            table.setForeground(ThemeManager.TABLE_TEXT);
            table.setGridColor(ThemeManager.DIVIDER);
            table.setRowHeight(35);
            table.setSelectionBackground(ThemeManager.isDarkMode ? ThemeManager.DARK_HOVER : new Color(241, 245, 249));
            
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setBackground(ThemeManager.isDarkMode ? ThemeManager.DARK_TABLE_HEADER : new Color(248, 250, 252));
                header.setForeground(ThemeManager.isDarkMode ? ThemeManager.DARK_TEXT_PRI : ThemeManager.LIGHT_TEXT_SEC);
                header.setFont(header.getFont().deriveFont(Font.BOLD));
            }
        } else if (comp instanceof JScrollPane) {
            comp.setBackground(ThemeManager.BG);
            JScrollPane sp = (JScrollPane) comp;
            if (sp.getViewport() != null) {
                sp.getViewport().setBackground(ThemeManager.BG);
            }
            sp.setBorder(null);
        } else if (comp instanceof JComboBox) {
            comp.setBackground(ThemeManager.CARD);
            comp.setForeground(ThemeManager.TEXT);
        } else if (comp instanceof JTextComponent) {
            JTextComponent txt = (JTextComponent) comp;
            txt.setBackground(ThemeManager.CARD);
            txt.setForeground(ThemeManager.TEXT);
            txt.setCaretColor(ThemeManager.TEXT);
            txt.setBorder(new LineBorder(ThemeManager.DIVIDER, 1, true));
        } else if (comp instanceof JMenuItem || comp instanceof JMenu || comp instanceof JMenuBar) {
            comp.setBackground(ThemeManager.CARD);
            comp.setForeground(ThemeManager.TEXT);
        } else if (comp instanceof JCheckBox || comp instanceof JRadioButton) {
            comp.setBackground(ThemeManager.BG);
            comp.setForeground(ThemeManager.TEXT);
        }
    }
}
