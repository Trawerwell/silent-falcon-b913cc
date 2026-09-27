/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.clickgui;

import java.awt.Color;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import primordial.util.render.RenderUtils;

public final class SmoothRender {
    private static final String VERTEX_SHADER = "#version 120\nvoid main(){\n  gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n  gl_TexCoord[0] = gl_MultiTexCoord0;\n}\n";
    private static final String FRAGMENT_SHADER = "#version 120\nuniform vec2 rectSize;\nuniform float radius;\nuniform vec4 drawColor;\nfloat roundedBoxSDF(vec2 p, vec2 halfSize, float r){\n  vec2 q = abs(p) - (halfSize - vec2(r));\n  return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;\n}\nvoid main(){\n  vec2 halfSize = rectSize * 0.5;\n  vec2 p = gl_TexCoord[0].st * rectSize - halfSize;\n  float d = roundedBoxSDF(p, halfSize, radius);\n  float aa = max(fwidth(d), 0.55);\n  float coverage = 1.0 - smoothstep(-aa, aa, d);\n  gl_FragColor = vec4(drawColor.rgb, drawColor.a * coverage);\n}\n";
    private static int program;
    private static int rectSizeUniform;
    private static int radiusUniform;
    private static int colorUniform;
    private static boolean failed;

    private SmoothRender() {
    }

    public static void roundedRect(float x, float y, float width, float height, float radius, Color color) {
        if (width <= 0.0f || height <= 0.0f || color == null) {
            return;
        }
        float safeRadius = Math.max(0.0f, Math.min(radius, Math.min(width, height) * 0.5f));
        if (!OpenGlHelper.shadersSupported || failed || !SmoothRender.ensureProgram()) {
            RenderUtils.drawRoundedRect(x, y, width, height, safeRadius, color);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL20.glUseProgram(program);
        try {
            GL20.glUniform2f(rectSizeUniform, width, height);
            GL20.glUniform1f(radiusUniform, safeRadius);
            GL20.glUniform4f(colorUniform, (float)color.getRed() / 255.0f, (float)color.getGreen() / 255.0f, (float)color.getBlue() / 255.0f, (float)color.getAlpha() / 255.0f);
            GL11.glBegin(7);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2f(x, y + height);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2f(x + width, y);
            GL11.glEnd();
        }
        finally {
            GL20.glUseProgram(0);
            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }

    public static void roundedOutline(float x, float y, float width, float height, float radius, float thickness, Color outline, Color fill) {
        float safeThickness = Math.max(0.5f, Math.min(thickness, Math.min(width, height) * 0.25f));
        SmoothRender.roundedRect(x, y, width, height, radius, outline);
        SmoothRender.roundedRect(x + safeThickness, y + safeThickness, width - safeThickness * 2.0f, height - safeThickness * 2.0f, Math.max(0.0f, radius - safeThickness), fill);
    }

    private static boolean ensureProgram() {
        block4: {
            if (program != 0) {
                return true;
            }
            try {
                int vertex = SmoothRender.compile(35633, VERTEX_SHADER);
                int fragment = SmoothRender.compile(35632, FRAGMENT_SHADER);
                program = GL20.glCreateProgram();
                GL20.glAttachShader(program, vertex);
                GL20.glAttachShader(program, fragment);
                GL20.glLinkProgram(program);
                GL20.glDeleteShader(vertex);
                GL20.glDeleteShader(fragment);
                if (GL20.glGetProgrami(program, 35714) != 0) break block4;
                GL20.glDeleteProgram(program);
                program = 0;
                failed = true;
                return false;
            }
            catch (Throwable throwable) {
                failed = true;
                program = 0;
                return false;
            }
        }
        rectSizeUniform = GL20.glGetUniformLocation(program, "rectSize");
        radiusUniform = GL20.glGetUniformLocation(program, "radius");
        colorUniform = GL20.glGetUniformLocation(program, "drawColor");
        return true;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, 35713) == 0) {
            GL20.glDeleteShader(shader);
            throw new IllegalStateException("ClickGUI corner shader compilation failed");
        }
        return shader;
    }
}
