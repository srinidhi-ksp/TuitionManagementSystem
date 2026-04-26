package util;

import java.awt.*;

public class ThemeManager {
    public static boolean isDarkMode = false;

    // --- DARK THEME PALETTE (WhatsApp/Discord Style) ---
    public static final Color DARK_BG          = new Color(15, 23, 42);   // #0f172a
    public static final Color DARK_CARD        = new Color(17, 24, 39);   // #111827
    public static final Color DARK_SIDEBAR     = new Color(2, 6, 23);     // #020617
    public static final Color DARK_TABLE_HEADER= new Color(31, 41, 55);   // #1f2937
    public static final Color DARK_BORDER       = new Color(55, 65, 81);   // #374151
    public static final Color DARK_TEXT_PRI    = new Color(249, 250, 251);// #f9fafb
    public static final Color DARK_TEXT_SEC    = new Color(156, 163, 175);// #9ca3af
    public static final Color DARK_HOVER       = new Color(30, 41, 59);   // #1e293b

    // --- LIGHT THEME PALETTE ---
    public static final Color LIGHT_BG         = new Color(248, 250, 252);
    public static final Color LIGHT_CARD       = Color.WHITE;
    public static final Color LIGHT_TEXT_PRI   = new Color(30, 41, 59);
    public static final Color LIGHT_TEXT_SEC   = new Color(100, 116, 139);
    public static final Color LIGHT_BORDER     = new Color(226, 232, 240);

    // --- ACCENTS ---
    public static final Color SUCCESS = new Color(34, 197, 94);  // #22c55e
    public static final Color WARNING = new Color(245, 158, 11); // #f59e0b
    public static final Color ERROR   = new Color(239, 68, 68);   // #ef4444
    public static final Color PRIMARY = new Color(59, 130, 246); // #3b82f6

    // Dynamic Variables
    public static Color BG;
    public static Color CARD;
    public static Color TEXT;
    public static Color SUB_TEXT;
    public static Color TABLE_BG;
    public static Color TABLE_TEXT;
    public static Color DIVIDER;

    static {
        applyThemeValues();
    }

    public static void applyThemeValues() {
        if (isDarkMode) {
            BG = DARK_BG;
            CARD = DARK_CARD;
            TEXT = DARK_TEXT_PRI;
            SUB_TEXT = DARK_TEXT_SEC;
            TABLE_BG = DARK_CARD;
            TABLE_TEXT = DARK_TEXT_PRI;
            DIVIDER = DARK_BORDER;
        } else {
            BG = LIGHT_BG;
            CARD = LIGHT_CARD;
            TEXT = LIGHT_TEXT_PRI;
            SUB_TEXT = LIGHT_TEXT_SEC;
            TABLE_BG = LIGHT_CARD;
            TABLE_TEXT = LIGHT_TEXT_PRI;
            DIVIDER = LIGHT_BORDER;
        }
        
        // Aliases
        BACKGROUND = BG;
        TEXT_PRIMARY = TEXT;
        TEXT_SECONDARY = SUB_TEXT;
    }

    public static Color BACKGROUND;
    public static Color TEXT_PRIMARY;
    public static Color TEXT_SECONDARY;

    public static void toggleTheme() {
        isDarkMode = !isDarkMode;
        System.out.println("Theme: " + (isDarkMode ? "DARK" : "LIGHT"));
        applyThemeValues();
    }
}
