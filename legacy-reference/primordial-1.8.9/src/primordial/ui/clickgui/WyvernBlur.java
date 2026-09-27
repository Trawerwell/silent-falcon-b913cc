/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class WyvernBlur {
    private static Framebuffer ping;
    private static Framebuffer pong;
    private static int kawaseProgram;
    private static int maskProgram;
    private static int kawaseSampler;
    private static int kawaseTexel;
    private static int kawaseOffset;
    private static int maskSampler;
    private static int maskRect;
    private static int maskRadius;
    private static int maskUvMin;
    private static int maskUvMax;
    private static boolean failed;
    private static int preparedWidth;
    private static int preparedHeight;

    private WyvernBlur() {
    }

    public static boolean prepare() {
        if (failed || !WyvernBlur.ensurePrograms()) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getFramebuffer() == null || minecraft.displayWidth <= 1 || minecraft.displayHeight <= 1) {
            return false;
        }
        int n = Math.max(1, minecraft.displayWidth / 2);
        int n2 = Math.max(1, minecraft.displayHeight / 2);
        WyvernBlur.ensureBuffers(n, n2);
        try {
            ping.framebufferClear();
            ping.bindFramebuffer(true);
            WyvernBlur.renderPass(minecraft.getFramebuffer().framebufferTexture, n, n2, 1.0f);
            pong.framebufferClear();
            pong.bindFramebuffer(true);
            WyvernBlur.renderPass(WyvernBlur.ping.framebufferTexture, n, n2, 2.0f);
            ping.framebufferClear();
            ping.bindFramebuffer(true);
            WyvernBlur.renderPass(WyvernBlur.pong.framebufferTexture, n, n2, 2.0f);
            minecraft.getFramebuffer().bindFramebuffer(true);
            preparedWidth = minecraft.displayWidth;
            preparedHeight = minecraft.displayHeight;
            return true;
        }
        catch (Throwable throwable) {
            failed = true;
            try {
                minecraft.getFramebuffer().bindFramebuffer(true);
            }
            catch (Throwable throwable2) {
                // empty catch block
            }
            System.err.println("[WyvernBlur] disabled: " + throwable.getMessage());
            return false;
        }
    }

    public static void draw(float f, float f2, float f3, float f4, float f5, int n, int n2, float f6) {
        if (failed || ping == null || maskProgram == 0 || preparedWidth <= 0 || preparedHeight <= 0) {
            return;
        }
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(WyvernBlur.ping.framebufferTexture);
        GL20.glUseProgram(maskProgram);
        GL20.glUniform1i(maskSampler, 0);
        GL20.glUniform2f(maskRect, f3, f4);
        GL20.glUniform1f(maskRadius, Math.min(f5, Math.min(f3, f4) * 0.5f));
        float f7 = f / Math.max(1.0f, (float)n);
        float f8 = (f + f3) / Math.max(1.0f, (float)n);
        float f9 = 1.0f - f2 / Math.max(1.0f, (float)n2);
        float f10 = 1.0f - (f2 + f4) / Math.max(1.0f, (float)n2);
        GL20.glUniform2f(maskUvMin, f7, f10);
        GL20.glUniform2f(maskUvMax, f8, f9);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, f6);
        GL11.glBegin(7);
        GL11.glTexCoord2f(f7, f9);
        GL11.glVertex2f(f, f2);
        GL11.glTexCoord2f(f7, f10);
        GL11.glVertex2f(f, f2 + f4);
        GL11.glTexCoord2f(f8, f10);
        GL11.glVertex2f(f + f3, f2 + f4);
        GL11.glTexCoord2f(f8, f9);
        GL11.glVertex2f(f + f3, f2);
        GL11.glEnd();
        GL20.glUseProgram(0);
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void renderPass(int n, int n2, int n3, float f) {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableBlend();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.bindTexture(n);
        GL20.glUseProgram(kawaseProgram);
        GL20.glUniform1i(kawaseSampler, 0);
        GL20.glUniform2f(kawaseTexel, 1.0f / (float)n2, 1.0f / (float)n3);
        GL20.glUniform1f(kawaseOffset, f);
        GlStateManager.matrixMode(5889);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0.0, n2, n3, 0.0, -1.0, 1.0);
        GlStateManager.matrixMode(5888);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glBegin(7);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(0.0f, 0.0f);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(0.0f, n3);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(n2, n3);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(n2, 0.0f);
        GL11.glEnd();
        GL20.glUseProgram(0);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5889);
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.depthMask(true);
    }

    private static void ensureBuffers(int n, int n2) {
        if (ping != null && WyvernBlur.ping.framebufferWidth == n && WyvernBlur.ping.framebufferHeight == n2) {
            return;
        }
        if (ping != null) {
            ping.deleteFramebuffer();
        }
        if (pong != null) {
            pong.deleteFramebuffer();
        }
        ping = new Framebuffer(n, n2, false);
        pong = new Framebuffer(n, n2, false);
        ping.setFramebufferFilter(9729);
        pong.setFramebufferFilter(9729);
    }

    private static boolean ensurePrograms() {
        if (kawaseProgram != 0 && maskProgram != 0) {
            return true;
        }
        if (failed) {
            return false;
        }
        try {
            String string = "#version 120\nvarying vec2 uv;void main(){gl_Position=gl_ModelViewProjectionMatrix*gl_Vertex;uv=gl_MultiTexCoord0.xy;gl_FrontColor=gl_Color;}";
            String string2 = "#version 120\nuniform sampler2D tex;uniform vec2 texel;uniform float offset;varying vec2 uv;void main(){vec2 d=texel*offset;vec4 s=texture2D(tex,uv)*4.0;s+=texture2D(tex,uv+vec2(d.x,d.y));s+=texture2D(tex,uv+vec2(-d.x,d.y));s+=texture2D(tex,uv+vec2(d.x,-d.y));s+=texture2D(tex,uv-vec2(d.x,d.y));gl_FragColor=s/8.0;}";
            String string3 = "#version 120\nuniform sampler2D tex;uniform vec2 rectSize;uniform vec2 uvMin;uniform vec2 uvMax;uniform float radius;varying vec2 uv;float sdf(vec2 p,vec2 b,float r){vec2 q=abs(p)-b+r;return min(max(q.x,q.y),0.0)+length(max(q,0.0))-r;}void main(){vec2 local=(uv-uvMin)/max(uvMax-uvMin,vec2(0.0001));vec2 p=local*rectSize-rectSize*0.5;float d=sdf(p,rectSize*0.5,radius);float a=1.0-smoothstep(-0.8,0.8,d);gl_FragColor=texture2D(tex,uv)*gl_Color*a;}";
            kawaseProgram = WyvernBlur.link(string, string2);
            maskProgram = WyvernBlur.link(string, string3);
            kawaseSampler = GL20.glGetUniformLocation(kawaseProgram, "tex");
            kawaseTexel = GL20.glGetUniformLocation(kawaseProgram, "texel");
            kawaseOffset = GL20.glGetUniformLocation(kawaseProgram, "offset");
            maskSampler = GL20.glGetUniformLocation(maskProgram, "tex");
            maskRect = GL20.glGetUniformLocation(maskProgram, "rectSize");
            maskRadius = GL20.glGetUniformLocation(maskProgram, "radius");
            maskUvMin = GL20.glGetUniformLocation(maskProgram, "uvMin");
            maskUvMax = GL20.glGetUniformLocation(maskProgram, "uvMax");
            return true;
        }
        catch (Throwable throwable) {
            failed = true;
            System.err.println("[WyvernBlur] shader compile failed: " + throwable.getMessage());
            return false;
        }
    }

    private static int link(String string, String string2) {
        int n = WyvernBlur.compile(35633, string);
        int n2 = WyvernBlur.compile(35632, string2);
        int n3 = GL20.glCreateProgram();
        GL20.glAttachShader(n3, n);
        GL20.glAttachShader(n3, n2);
        GL20.glLinkProgram(n3);
        GL20.glDeleteShader(n);
        GL20.glDeleteShader(n2);
        if (GL20.glGetProgrami(n3, 35714) == 0) {
            throw new IllegalStateException(GL20.glGetProgramInfoLog(n3, 4096));
        }
        return n3;
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
}
