/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.menu;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;
import primordial.ui.wyvern.WTexture;

public final class PrimordialTitleRenderer {
    private static final ResourceLocation HOURGLASS = new ResourceLocation("primordial/title_hourglass.png");
    private static final ResourceLocation TEXT = new ResourceLocation("primordial/title_text.png");
    private static final long CYCLE_MS = 7600L;
    private static final float TEXT_ASPECT = 4.6666665f;
    private static final float HG_ASPECT = 0.6296296f;
    private static long cycleStart = System.currentTimeMillis();
    private static float lastLeft;
    private static float lastTop;
    private static float lastWidth;
    private static float lastHeight;
    private static float tintR;
    private static float tintG;
    private static float tintB;
    private static boolean hgMipmaps;
    private static boolean textMipmaps;

    private PrimordialTitleRenderer() {
    }

    public static void restart() {
        cycleStart = System.currentTimeMillis();
    }

    public static boolean isHovered(int n, int n2) {
        return (float)n >= lastLeft && (float)n <= lastLeft + lastWidth && (float)n2 >= lastTop && (float)n2 <= lastTop + lastHeight;
    }

    public static void render(int n, int n2) {
        long l = System.currentTimeMillis() - cycleStart;
        if (l < 0L) {
            cycleStart = System.currentTimeMillis();
            l = 0L;
        }
        float f = (float)(l %= 7600L) / 7600.0f;
        float f2 = PrimordialTitleRenderer.clamp((float)n2 / 26.0f, 1.7f, 3.4f);
        float f3 = 17.0f * f2;
        float f4 = f3 / 0.6296296f;
        float f5 = 126.0f * f2;
        float f6 = f5 / 4.6666665f;
        float f7 = 5.5f * f2;
        float f8 = f3 + f7 + f5;
        float f9 = ((float)n - f3) * 0.5f;
        float f10 = ((float)n - f8) * 0.5f;
        float f11 = (float)n2 - 16.0f - f4 * 0.5f;
        float f12 = f4 * 0.5f + 6.0f;
        if (f11 < f12) {
            f11 = f12;
        }
        float f13 = 0.0f;
        f13 = f < 0.28f ? PrimordialTitleRenderer.easeInOutQuart(f / 0.28f) : 1.0f;
        float f14 = f13 * 360.0f;
        float f15 = f < 0.34f ? 0.0f : (f < 0.54f ? PrimordialTitleRenderer.smootherstep((f - 0.34f) / 0.2f) : (f < 0.76f ? 1.0f : (f < 0.94f ? 1.0f - PrimordialTitleRenderer.smootherstep((f - 0.76f) / 0.18f) : 0.0f)));
        float f16 = PrimordialTitleRenderer.lerp(f9, f10, f15);
        float f17 = f16 + f3 + f7;
        float f18 = f15 * f5;
        lastLeft = f16;
        lastTop = f11 - f4 * 0.5f;
        lastWidth = f3 + (f18 > 0.5f ? f7 + f18 : 0.0f);
        lastHeight = f4;
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(7425);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        PrimordialTitleRenderer.drawHourglass(f16, f11, f3, f4, f14);
        if (f15 > 0.002f) {
            PrimordialTitleRenderer.drawText(f17, f11 - f6 * 0.5f, f5, f6, f15);
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableDepth();
        for (int i = 0; i < 8 && GL11.glGetError() != 0; ++i) {
        }
    }

    public static float compositionWidth(float f) {
        return 148.5f * (f / 27.0f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void renderScaled(float f, float f2, float f3, Color color) {
        if (color != null) {
            tintR = (float)color.getRed() / 255.0f;
            tintG = (float)color.getGreen() / 255.0f;
            tintB = (float)color.getBlue() / 255.0f;
        }
        try {
            PrimordialTitleRenderer.renderScaled(f, f2, f3);
        }
        finally {
            tintR = 1.0f;
            tintG = 1.0f;
            tintB = 1.0f;
        }
    }

    public static void renderScaled(float f, float f2, float f3) {
        long l = System.currentTimeMillis() - cycleStart;
        if (l < 0L) {
            cycleStart = System.currentTimeMillis();
            l = 0L;
        }
        float f4 = (float)(l % 7600L) / 7600.0f;
        float f5 = f3 / 27.0f;
        float f6 = 17.0f * f5;
        float f7 = f6 / 0.6296296f;
        float f8 = 126.0f * f5;
        float f9 = f8 / 4.6666665f;
        float f10 = 5.5f * f5;
        float f11 = f4 < 0.28f ? PrimordialTitleRenderer.easeInOutQuart(f4 / 0.28f) : 1.0f;
        float f12 = f11 * 360.0f;
        float f13 = f4 < 0.34f ? 0.0f : (f4 < 0.54f ? PrimordialTitleRenderer.smootherstep((f4 - 0.34f) / 0.2f) : (f4 < 0.76f ? 1.0f : (f4 < 0.94f ? 1.0f - PrimordialTitleRenderer.smootherstep((f4 - 0.76f) / 0.18f) : 0.0f)));
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.shadeModel(7425);
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        PrimordialTitleRenderer.drawHourglass(f, f2, f6, f7, f12);
        if (f13 > 0.002f) {
            PrimordialTitleRenderer.drawText(f + f6 + f10, f2 - f9 * 0.5f, f8, f9, f13);
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableDepth();
        for (int i = 0; i < 8 && GL11.glGetError() != 0; ++i) {
        }
    }

    private static void drawHourglass(float f, float f2, float f3, float f4, float f5) {
        PrimordialTitleRenderer.bindLinear(HOURGLASS);
        GlStateManager.pushMatrix();
        GlStateManager.translate(f + f3 * 0.5f, f2, 0.0f);
        GlStateManager.rotate(f5, 0.0f, 0.0f, 1.0f);
        PrimordialTitleRenderer.drawRect(-f3 * 0.5f, -f4 * 0.5f, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    private static void drawText(float f, float f2, float f3, float f4, float f5) {
        float f6;
        PrimordialTitleRenderer.bindLinear(TEXT);
        float f7 = PrimordialTitleRenderer.clamp01(f5) * f3;
        float f8 = Math.min(f7, (1.0f - f5) * f3);
        f8 = Math.min(f8, f3 * 0.08f);
        float f9 = Math.max(0.0f, f7 - f8);
        if (f9 > 0.2f) {
            f6 = f9 / f3;
            PrimordialTitleRenderer.drawRect(f, f2, f9, f4, 0.0f, 0.0f, f6, 1.0f, 1.0f);
        }
        if (f8 > 0.2f) {
            f6 = f9 / f3;
            float f10 = f7 / f3;
            PrimordialTitleRenderer.drawRectFade(f + f9, f2, f8, f4, f6, 0.0f, f10, 1.0f, 1.0f, 0.0f);
        }
    }

    private static void bindLinear(ResourceLocation resourceLocation) {
        boolean bl;
        if (WTexture.bind(resourceLocation, 1024)) {
            return;
        }
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
        boolean bl2 = GLContext.getCapabilities().OpenGL30;
        boolean bl3 = HOURGLASS.equals(resourceLocation);
        boolean bl4 = bl3 ? !hgMipmaps : (bl = !textMipmaps);
        if (bl) {
            try {
                if (bl2) {
                    GL30.glGenerateMipmap(3553);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (bl3) {
                hgMipmaps = true;
            } else {
                textMipmaps = true;
            }
        }
        GL11.glTexParameteri(3553, 10241, bl2 ? 9987 : 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
    }

    private static void drawRect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        if (f3 <= 0.0f || f4 <= 0.0f || f9 <= 0.0f) {
            return;
        }
        GlStateManager.color(tintR, tintG, tintB, PrimordialTitleRenderer.clamp01(f9));
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(f, f2 + f4, 0.0).tex(f5, f8).endVertex();
        worldRenderer.pos(f + f3, f2 + f4, 0.0).tex(f7, f8).endVertex();
        worldRenderer.pos(f + f3, f2, 0.0).tex(f7, f6).endVertex();
        worldRenderer.pos(f, f2, 0.0).tex(f5, f6).endVertex();
        tessellator.draw();
    }

    private static void drawRectFade(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10) {
        if (f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        float f11 = PrimordialTitleRenderer.clamp01(f9);
        float f12 = PrimordialTitleRenderer.clamp01(f10);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        worldRenderer.pos(f, f2 + f4, 0.0).tex(f5, f8).color(tintR, tintG, tintB, f11).endVertex();
        worldRenderer.pos(f + f3, f2 + f4, 0.0).tex(f7, f8).color(tintR, tintG, tintB, f12).endVertex();
        worldRenderer.pos(f + f3, f2, 0.0).tex(f7, f6).color(tintR, tintG, tintB, f12).endVertex();
        worldRenderer.pos(f, f2, 0.0).tex(f5, f6).color(tintR, tintG, tintB, f11).endVertex();
        tessellator.draw();
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    private static float clamp01(float f) {
        return PrimordialTitleRenderer.clamp(f, 0.0f, 1.0f);
    }

    private static float lerp(float f, float f2, float f3) {
        return f + (f2 - f) * PrimordialTitleRenderer.clamp01(f3);
    }

    private static float easeInOutQuart(float f) {
        if ((f = PrimordialTitleRenderer.clamp01(f)) < 0.5f) {
            float f2 = f * f;
            return 8.0f * f2 * f2;
        }
        float f3 = -2.0f * f + 2.0f;
        float f4 = f3 * f3;
        return 1.0f - f4 * f4 / 2.0f;
    }

    private static float smootherstep(float f) {
        f = PrimordialTitleRenderer.clamp01(f);
        return f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f);
    }

    static {
        tintR = 1.0f;
        tintG = 1.0f;
        tintB = 1.0f;
    }
}
