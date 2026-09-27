/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.wyvern;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import primordial.ui.clickgui.WyvernBlur;
import primordial.ui.wyvern.WBlur;

public final class WDraw {
    private static int program;
    private static int uSize;
    private static int uRadii;
    private static int uBorder;
    private static boolean failed;
    private static int blurPasses;
    private static boolean usingFallbackBlur;
    private static long lastPrepareNanos;
    private static boolean blurReady;
    private static int frameCounter;
    private static float borderWidth;
    private static long frameSeq;
    private static long capturedFrame;

    private WDraw() {
    }

    private static void doPrepare() {
        lastPrepareNanos = System.nanoTime();
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft == null || minecraft.displayWidth <= 1 || minecraft.displayHeight <= 1 || !Display.isActive() || !Display.isVisible()) {
                blurReady = false;
                return;
            }
            blurReady = WBlur.prepare(blurPasses);
            usingFallbackBlur = !blurReady ? (blurReady = WyvernBlur.prepare()) : false;
        }
        catch (Throwable throwable) {
            blurReady = false;
        }
    }

    public static void nextFrame() {
        capturedFrame = ++frameSeq;
        WDraw.doPrepare();
    }

    public static void captureFrame() {
        capturedFrame = ++frameSeq;
        WDraw.doPrepare();
    }

    public static void prepareNow() {
        capturedFrame = ++frameSeq;
        WDraw.doPrepare();
    }

    public static void setBlurPasses(int n) {
        blurPasses = Math.max(2, Math.min(6, n));
    }

    public static void beginFrame() {
        if (capturedFrame == frameSeq) {
            return;
        }
        capturedFrame = ++frameSeq;
        WDraw.doPrepare();
    }

    public static void blur(float f, float f2, float f3, float f4, float f5, float f6) {
        if (!blurReady || f6 <= 0.01f || f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        try {
            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            if (usingFallbackBlur) {
                WyvernBlur.draw(f, f2, f3, f4, f5, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), f6);
            } else {
                WBlur.draw(f, f2, f3, f4, f5, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), f6);
            }
        }
        catch (Throwable throwable) {
            blurReady = false;
        }
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, Color color) {
        WDraw.rect(f, f2, f3, f4, f5, f5, f5, f5, color, color, color, color);
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, Color color) {
        WDraw.rect(f, f2, f3, f4, f5, f6, f7, f8, color, color, color, color);
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, Color color, Color color2, Color color3, Color color4) {
        WDraw.rect(f, f2, f3, f4, f5, f5, f5, f5, color, color2, color3, color4);
    }

    public static void rectGradientH(float f, float f2, float f3, float f4, float f5, Color color, Color color2) {
        WDraw.rect(f, f2, f3, f4, f5, f5, f5, f5, color, color2, color2, color);
    }

    public static void rectGradientV(float f, float f2, float f3, float f4, float f5, Color color, Color color2) {
        WDraw.rect(f, f2, f3, f4, f5, f5, f5, f5, color, color, color2, color2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void outline(float f, float f2, float f3, float f4, float f5, float f6, Color color) {
        borderWidth = Math.max(0.4f, f6);
        try {
            WDraw.rect(f, f2, f3, f4, f5, f5, f5, f5, color, color, color, color);
        }
        finally {
            borderWidth = 0.0f;
        }
    }

    public static void rect(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, Color color, Color color2, Color color3, Color color4) {
        if (f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        WDraw.ensureProgram();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        boolean bl = program != 0;
        boolean bl2 = bl;
        if (bl) {
            GlStateManager.disableTexture2D();
            float f9 = Math.min(f3, f4) * 0.5f;
            GL20.glUseProgram(program);
            GL20.glUniform2f(uSize, f3, f4);
            GL20.glUniform1f(uBorder, borderWidth);
            GL20.glUniform4f(uRadii, Math.max(0.0f, Math.min(f5, f9)), Math.max(0.0f, Math.min(f6, f9)), Math.max(0.0f, Math.min(f7, f9)), Math.max(0.0f, Math.min(f8, f9)));
        } else {
            GlStateManager.disableTexture2D();
        }
        GL11.glBegin(7);
        WDraw.vertex(f, f2, 0.0f, 0.0f, color);
        WDraw.vertex(f, f2 + f4, 0.0f, 1.0f, color4);
        WDraw.vertex(f + f3, f2 + f4, 1.0f, 1.0f, color3);
        WDraw.vertex(f + f3, f2, 1.0f, 0.0f, color2);
        GL11.glEnd();
        if (bl) {
            GL20.glUseProgram(0);
        }
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void vertex(float f, float f2, float f3, float f4, Color color) {
        GL11.glColor4f((float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
        GL11.glTexCoord2f(f3, f4);
        GL11.glVertex2f(f, f2);
    }

    public static void card(float f, float f2, float f3, float f4, float f5, int n, float f6) {
        if (f6 <= 0.01f) {
            return;
        }
        WDraw.blur(f, f2, f3, f4, f5, f6);
        WDraw.rect(f, f2, f3, f4, f5, new Color(0, 0, 0, (int)((float)n * f6)));
    }

    public static void glass(float f, float f2, float f3, float f4, float f5, float f6) {
        WDraw.glass(f, f2, f3, f4, f5, f6, 92);
    }

    public static void glass(float f, float f2, float f3, float f4, float f5, float f6, int n) {
        if (f6 <= 0.01f || f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        WDraw.blur(f, f2, f3, f4, f5, f6);
        WDraw.rect(f, f2, f3, f4, f5, new Color(0, 0, 0, (int)((float)n * f6)));
        WDraw.gloss(f, f2, f3, f4, f5, f6);
    }

    public static void gloss(float f, float f2, float f3, float f4, float f5, float f6) {
        if (f6 <= 0.01f || f3 <= 0.0f || f4 <= 0.0f) {
            return;
        }
        WDraw.outline(f, f2, f3, f4, f5, 0.7f, new Color(255, 255, 255, (int)(26.0f * f6)));
        float f7 = Math.min(f5, 2.0f);
        WDraw.rect(f + f7, f2 + 0.55f, Math.max(0.0f, f3 - f7 * 2.0f), 0.5f, 0.25f, new Color(255, 255, 255, (int)(38.0f * f6)));
    }

    public static void wifi(float f, float f2, float f3, Color color, int n) {
        float f4 = f + f3 * 0.22f;
        float f5 = f2 + f3 * 0.72f;
        float f6 = Math.max(0.7f, f3 * 0.115f);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GL11.glEnable(2848);
        GL11.glHint(3154, 4354);
        for (int i = 0; i < 3; ++i) {
            float f7 = f3 * (0.32f + (float)i * 0.27f);
            boolean bl = i < Math.max(0, Math.min(3, n));
            Color color2 = bl ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), 55);
            GL11.glColor4f((float)color2.getRed() / 255.0f, (float)color2.getGreen() / 255.0f, (float)color2.getBlue() / 255.0f, (float)color2.getAlpha() / 255.0f);
            GL11.glLineWidth(f6);
            GL11.glBegin(3);
            for (int j = 0; j <= 16; ++j) {
                double d = Math.toRadians(-45.0) + Math.toRadians(-58.0 + 116.0 * ((double)j / 16.0));
                float f8 = f4 + (float)Math.cos(d) * f7;
                float f9 = f5 + (float)Math.sin(d) * f7;
                GL11.glVertex2f(f8, f9);
            }
            GL11.glEnd();
        }
        GL11.glLineWidth(1.0f);
        GL11.glDisable(2848);
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        float f10 = Math.max(1.0f, f3 * 0.17f);
        WDraw.rect(f4 - f10 * 0.35f, f5 - f10 * 0.5f, f10, f10, f10 * 0.5f, color);
    }

    public static Color alpha(Color color, float f) {
        int n = (int)((float)color.getAlpha() * Math.max(0.0f, Math.min(1.0f, f)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, n)));
    }

    public static Color withAlpha(Color color, int n) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, n)));
    }

    public static Color darker(Color color, float f) {
        return new Color((int)Math.max(0.0f, (float)color.getRed() * f), (int)Math.max(0.0f, (float)color.getGreen() * f), (int)Math.max(0.0f, (float)color.getBlue() * f), color.getAlpha());
    }

    private static void ensureProgram() {
        if (program != 0 || failed || !OpenGlHelper.shadersSupported) {
            return;
        }
        try {
            String string = "#version 120\nvarying vec2 uv;\nvarying vec4 col;\nvoid main(){\n gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n uv = gl_MultiTexCoord0.xy;\n col = gl_Color;\n}";
            String string2 = "#version 120\nuniform vec2 size;\nuniform vec4 radii;\nuniform float border;\nvarying vec2 uv;\nvarying vec4 col;\nvoid main(){\n vec2 p = uv * size - size * 0.5;\n float r = (p.x < 0.0) ? ((p.y < 0.0) ? radii.x : radii.w) : ((p.y < 0.0) ? radii.y : radii.z);\n vec2 q = abs(p) - size * 0.5 + r;\n float d = min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;\n if (border > 0.0) { d = abs(d + border * 0.5) - border * 0.5; }\n float a = 1.0 - smoothstep(-0.7, 0.7, d);\n if (a <= 0.001) discard;\n gl_FragColor = vec4(col.rgb, col.a * a);\n}";
            int n = WDraw.compile(35633, string);
            int n2 = WDraw.compile(35632, string2);
            int n3 = GL20.glCreateProgram();
            GL20.glAttachShader(n3, n);
            GL20.glAttachShader(n3, n2);
            GL20.glLinkProgram(n3);
            GL20.glDeleteShader(n);
            GL20.glDeleteShader(n2);
            if (GL20.glGetProgrami(n3, 35714) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(n3, 4096));
            }
            program = n3;
            uSize = GL20.glGetUniformLocation(program, "size");
            uRadii = GL20.glGetUniformLocation(program, "radii");
            uBorder = GL20.glGetUniformLocation(program, "border");
        }
        catch (Throwable throwable) {
            failed = true;
            System.err.println("[primordial] rounded rect shader failed: " + String.valueOf(throwable));
        }
    }

    private static int compile(int n, String string) {
        int n2 = GL20.glCreateShader(n);
        GL20.glShaderSource(n2, string);
        GL20.glCompileShader(n2);
        if (GL20.glGetShaderi(n2, 35713) == 0) {
            throw new IllegalStateException(GL20.glGetShaderInfoLog(n2, 4096));
        }
        return n2;
    }

    static {
        capturedFrame = -1L;
        blurPasses = 5;
    }
}
