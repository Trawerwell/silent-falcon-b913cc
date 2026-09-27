/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.wyvern;

import primordial.ui.wyvern.WMsdfFont;

public final class WFonts {
    public static final WMsdfFont REGULAR = new WMsdfFont("regular");
    public static final WMsdfFont MEDIUM = new WMsdfFont("medium");
    public static final WMsdfFont SEMIBOLD = new WMsdfFont("semibold");
    public static final WMsdfFont ICONS = new WMsdfFont("icons");
    public static final WMsdfFont HUD_ICONS = new WMsdfFont("hud_icons");
    public static final WMsdfFont NURIKI = new WMsdfFont("nuriki");
    public static final WMsdfFont FONT = new WMsdfFont("font");

    private WFonts() {
    }

    public static boolean ready() {
        return REGULAR.isUsable() && MEDIUM.isUsable();
    }
}
