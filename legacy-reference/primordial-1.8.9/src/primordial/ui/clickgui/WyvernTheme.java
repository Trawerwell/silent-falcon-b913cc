/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.clickgui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public final class WyvernTheme {
    private String name;
    private final List<Color> colors;

    public WyvernTheme(String string, List<Color> list) {
        this.name = string;
        this.colors = new ArrayList<Color>(list);
        if (this.colors.isEmpty()) {
            this.colors.add(Color.WHITE);
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String string) {
        this.name = string;
    }

    public List<Color> getColors() {
        return this.colors;
    }

    public Color primary() {
        return this.colors.get(0);
    }

    public Color secondary() {
        return this.colors.size() > 1 ? this.colors.get(1) : this.primary().darker();
    }
}
