/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import primordial.ui.font.FontUtil;

public class FontHelper {
    public static FontUtil SIZE_18;
    public static FontUtil SIZE_12;
    public static FontUtil SIZE_48;
    public static FontUtil SIZE_18_BOLD;
    public static FontUtil SIZE_15;
    public static FontUtil SIZE_24_BOLD;
    public static FontUtil SIZE_20;
    public static FontUtil SIZE_19;
    public static FontUtil eras;
    private static ResourceLocation location;

    public static void setupFont() {
        try {
            location = new ResourceLocation("primordial/font.ttf");
            SIZE_12 = new FontUtil(FontHelper.createFont(12), 1330, true, FontUtil.Style.REGULAR, 2.0f);
            SIZE_15 = new FontUtil(FontHelper.createFont(15), 10176, true, FontUtil.Style.REGULAR, 2.0f);
            SIZE_18 = new FontUtil(FontHelper.createFont(18), 1330, true, FontUtil.Style.REGULAR, 2.0f);
            SIZE_18_BOLD = new FontUtil(FontHelper.createFont(18), 1330, true, FontUtil.Style.BOLD, 2.0f);
            SIZE_19 = new FontUtil(FontHelper.createFont(19), 10176, true, FontUtil.Style.REGULAR, 4.0f);
            SIZE_20 = new FontUtil(FontHelper.createFont(20), 1330, true, FontUtil.Style.REGULAR, 2.0f);
            SIZE_24_BOLD = new FontUtil(FontHelper.createFont(24), 1330, true, FontUtil.Style.BOLD, 2.0f);
            SIZE_48 = new FontUtil(FontHelper.createFont(48), true, FontUtil.Style.BOLD);
            location = new ResourceLocation("primordial/eras.ttf");
            eras = new FontUtil(FontHelper.createFont(96), 512, true, FontUtil.Style.ITALIC);
            System.out.println("Fonts loaded.");
        }
        catch (FontFormatException | IOException exception) {
            exception.printStackTrace();
        }
    }

    public static Font createFont(int n) throws IOException, FontFormatException {
        IResource iResource = Minecraft.getMinecraft().getTextureManager().theResourceManager.getResource(location);
        Font font = Font.createFont(0, iResource.getInputStream()).deriveFont(0, n);
        iResource.getInputStream().close();
        return font;
    }
}
