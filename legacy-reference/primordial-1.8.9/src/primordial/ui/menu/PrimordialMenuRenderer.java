/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.menu;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class PrimordialMenuRenderer {
    private static final ResourceLocation VERTEX = new ResourceLocation("shaders/program/primordial_menu.vsh");
    private static final ResourceLocation FRAGMENT = new ResourceLocation("shaders/program/primordial_menu.fsh");
    private static final long START_NANOS = System.nanoTime();
    private static int program;
    private static int timeUniform;
    private static int resolutionUniform;
    private static boolean failed;

    private PrimordialMenuRenderer() {
    }

    public static void render(int n, int n2, int n3, int n4) {
        PrimordialMenuRenderer.renderInternal(n, n2);
    }

    public static void render(int n, int n2) {
        PrimordialMenuRenderer.renderInternal(n, n2);
    }

    public static void drainGlErrors() {
        for (int i = 0; i < 16 && GL11.glGetError() != 0; ++i) {
        }
    }

    private static void renderInternal(int n, int n2) {
        PrimordialMenuRenderer.ensureProgram();
        if (program == 0) {
            PrimordialMenuRenderer.renderFallback(n, n2);
            return;
        }
        float f = (float)((double)(System.nanoTime() - START_NANOS) / 1.0E9);
        float f2 = Math.max(1, n);
        float f3 = Math.max(1, n2);
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableTexture2D();
        GL20.glUseProgram(program);
        if (timeUniform >= 0) {
            GL20.glUniform1f(timeUniform, f);
        }
        if (resolutionUniform >= 0) {
            GL20.glUniform2f(resolutionUniform, f2, f3);
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(0.0, n2, 0.0).tex(0.0, 1.0).endVertex();
        worldRenderer.pos(n, n2, 0.0).tex(1.0, 1.0).endVertex();
        worldRenderer.pos(n, 0.0, 0.0).tex(1.0, 0.0).endVertex();
        worldRenderer.pos(0.0, 0.0, 0.0).tex(0.0, 0.0).endVertex();
        tessellator.draw();
        GL20.glUseProgram(0);
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.disableDepth();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
        PrimordialMenuRenderer.drainGlErrors();
    }

    private static void renderFallback(int n, int n2) {
        Gui.drawRect(0, 0, n, n2, -15593708);
        Gui.drawRect(0, 0, n, n2 / 2, -15067876);
        Gui.drawRect(0, n2 - Math.max(28, n2 / 5), n, n2, -16054004);
    }

    private static void ensureProgram() {
        if (program != 0 || failed) {
            return;
        }
        try {
            int n = PrimordialMenuRenderer.compile(35633, PrimordialMenuRenderer.read(VERTEX), "vertex");
            int n2 = PrimordialMenuRenderer.compile(35632, PrimordialMenuRenderer.read(FRAGMENT), "fragment");
            int n3 = GL20.glCreateProgram();
            GL20.glAttachShader(n3, n);
            GL20.glAttachShader(n3, n2);
            GL20.glLinkProgram(n3);
            GL20.glDeleteShader(n);
            GL20.glDeleteShader(n2);
            if (GL20.glGetProgrami(n3, 35714) == 0) {
                String string = GL20.glGetProgramInfoLog(n3, 4096);
                GL20.glDeleteProgram(n3);
                throw new IllegalStateException("menu shader link failed: " + string);
            }
            program = n3;
            timeUniform = GL20.glGetUniformLocation(program, "time");
            resolutionUniform = GL20.glGetUniformLocation(program, "resolution");
        }
        catch (Throwable throwable) {
            failed = true;
            program = 0;
            System.err.println("[PrimordialMenu] " + throwable.getMessage());
        }
    }

    private static int compile(int n, String string, String string2) {
        int n2 = GL20.glCreateShader(n);
        GL20.glShaderSource(n2, string);
        GL20.glCompileShader(n2);
        if (GL20.glGetShaderi(n2, 35713) == 0) {
            String string3 = GL20.glGetShaderInfoLog(n2, 4096);
            GL20.glDeleteShader(n2);
            throw new IllegalStateException(string2 + " shader compile failed: " + string3);
        }
        return n2;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static String read(ResourceLocation resourceLocation) throws Exception {
        InputStream inputStream = null;
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            int n;
            inputStream = Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation).getInputStream();
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] byArray = new byte[4096];
            while ((n = inputStream.read(byArray)) >= 0) {
                byteArrayOutputStream.write(byArray, 0, n);
            }
            String string = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
            return string;
        }
        finally {
            if (byteArrayOutputStream != null) {
                byteArrayOutputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    static {
        timeUniform = -1;
        resolutionUniform = -1;
    }
}
