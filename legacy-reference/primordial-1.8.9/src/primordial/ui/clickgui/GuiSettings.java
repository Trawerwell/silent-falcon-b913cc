/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.clickgui;

import primordial.ui.font.FontHelper;
import primordial.ui.font.FontUtil;

public final class GuiSettings {
    private static FontUtil categoryFont;
    private static FontUtil normalFont;

    private GuiSettings() {
    }

    public static void initFont() {
        normalFont = FontHelper.SIZE_15;
        categoryFont = FontHelper.SIZE_20;
    }

    public static FontUtil getCategoryFont() {
        return categoryFont;
    }

    public static FontUtil getNormalFont() {
        return normalFont;
    }
}
