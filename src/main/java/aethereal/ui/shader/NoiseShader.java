package aethereal.ui.shader;

import aethereal.core.EventManager;
import aethereal.core.EventTarget;
import aethereal.core.Interface;
import aethereal.event.ResizeEvent;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NoiseShader extends Shader implements Interface {
    private static final Logger LOGGER = LogManager.getLogger(NoiseShader.class);
    private boolean viewportDiagnosed;
    private static final Identifier c = Identifier.of("primordial", "core/noise/noise_shader");
    private static final ShaderProgramKey MASK_SHADER = new ShaderProgramKey(
            Identifier.of("primordial", "core/noise/hand_mask"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final ShaderProgramKey BLUR_SHADER = new ShaderProgramKey(
            Identifier.of("primordial", "core/entity_outline_blur"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private final Matrix4f d;
    private SimpleFramebuffer e;
    private SimpleFramebuffer handScene, mask, blurX, blurY;
    private Uniform f;
    private Uniform g;
    private Uniform modeUniform, waveSpeedUniform, waveScaleUniform, outlineUniform,
            glowUniform, glowRadiusUniform, fillUniform, blurRadiusUniform, resolutionUniform;

    public NoiseShader() {
        super(c, VertexFormats.POSITION_COLOR);
        this.d = new Matrix4f();
        EventManager.a(this);
    }

    @EventTarget
    public void a(ResizeEvent event) {
        if (this.e != null) this.e.delete();
        if (this.handScene != null) this.handScene.delete();
        if (this.mask != null) this.mask.delete();
        if (this.blurX != null) this.blurX.delete();
        if (this.blurY != null) this.blurY.delete();
        this.e = new SimpleFramebuffer(mc.getWindow().getFramebufferWidth(), mc.getWindow().getFramebufferHeight(), true);
        this.handScene = this.mask = this.blurX = this.blurY = null;
        this.viewportDiagnosed = false;
    }

    public void e(boolean captureWorldColor) {
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (this.e == null || this.e.textureWidth != width || this.e.textureHeight != height) {
            if (this.e != null) this.e.delete();
            this.e = new SimpleFramebuffer(width, height, true);
        }
        if (this.e != null) {
            this.e.copyDepthFrom(mc.getFramebuffer());
            if (captureWorldColor) {
                int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
                int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                try {
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().fbo);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.e.fbo);
                    GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                            GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                } finally {
                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
                }
            }
        }
    }

    @Override
    protected void b() {
        this.f = a("TintColor");
        this.g = a("Time");
        this.modeUniform = a("EffectMode");
        this.waveSpeedUniform = a("WaveSpeed");
        this.waveScaleUniform = a("WaveScale");
        this.outlineUniform = a("OutlineWidth");
        this.glowUniform = a("GlowStrength");
        this.glowRadiusUniform = a("GlowRadius");
        this.fillUniform = a("FillAmount");
        this.blurRadiusUniform = a("BlurRadius");
        this.resolutionUniform = a("Resolution");
    }

    public void a(float[] color, int mode, float waveSpeed, float waveScale,
                  float outline, float glow, float glowRadius, float fill, float blurRadius) {
        if (this.e != null) {
            int width = mc.getWindow().getFramebufferWidth();
            int height = mc.getWindow().getFramebufferHeight();
            int halfWidth = Math.max(1, (width + 1) / 2);
            int halfHeight = Math.max(1, (height + 1) / 2);
            if (mask == null) mask = new SimpleFramebuffer(width, height, false);
            else if (mask.textureWidth != width || mask.textureHeight != height) mask.resize(width, height);
            if (handScene == null) handScene = new SimpleFramebuffer(width, height, false);
            else if (handScene.textureWidth != width || handScene.textureHeight != height) handScene.resize(width, height);
            int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
            try {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, mc.getFramebuffer().fbo);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, handScene.fbo);
                GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                        GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            } finally {
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
            }
            if (blurX == null) {
                blurX = new SimpleFramebuffer(halfWidth, halfHeight, false);
                blurY = new SimpleFramebuffer(halfWidth, halfHeight, false);
            } else if (blurX.textureWidth != halfWidth || blurX.textureHeight != halfHeight) {
                blurX.resize(halfWidth, halfHeight);
                blurY.resize(halfWidth, halfHeight);
            }
            mask.setTexFilter(GL11.GL_LINEAR);
            blurX.setTexFilter(GL11.GL_LINEAR);
            blurY.setTexFilter(GL11.GL_LINEAR);
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(this.d, ProjectionType.PERSPECTIVE);
            Matrix4fStack modelView = RenderSystem.getModelViewStack();
            modelView.pushMatrix().identity();
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            mask.setClearColor(0, 0, 0, 0);
            mask.clear();
            mask.beginWrite(true);
            RenderSystem.setShaderTexture(0, this.e.getColorAttachment());
            RenderSystem.setShaderTexture(1, handScene.getColorAttachment());
            RenderSystem.setShaderTexture(2, this.e.getDepthAttachment());
            RenderSystem.setShaderTexture(3, mc.getFramebuffer().getDepthAttachment());
            ShaderProgram maskProgram = RenderSystem.setShader(MASK_SHADER);
            Uniform maskResolution = maskProgram.getUniform("Resolution");
            if (maskResolution != null) maskResolution.set((float) width, (float) height);
            drawQuad();
            if (mode != 0 && glow > 0 && glowRadius > 0) {
                blur(blurX, mask.getColorAttachment(), width, height, glowRadius, true);
                blur(blurY, blurX.getColorAttachment(), width, height, glowRadius, false);
            } else {
                blurY.setClearColor(0, 0, 0, 0);
                blurY.clear();
            }
            // The blur passes use a half-size viewport. Binding the main FBO
            // without resetting it scales and displaces the entire hand effect.
            int[] previousViewport = viewportDiagnosed ? null : new int[4];
            if (previousViewport != null) GL11.glGetIntegerv(GL11.GL_VIEWPORT, previousViewport);
            mc.getFramebuffer().beginWrite(true);
            if (previousViewport != null) {
                int[] compositeViewport = new int[4];
                GL11.glGetIntegerv(GL11.GL_VIEWPORT, compositeViewport);
                LOGGER.info("Hands Shader viewport diagnostic: blur={}x{}, composite={}x{}, framebuffer={}x{}, mask={}x{}",
                        previousViewport[2], previousViewport[3], compositeViewport[2], compositeViewport[3],
                        width, height, mask.textureWidth, mask.textureHeight);
                viewportDiagnosed = true;
            }
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, handScene.getColorAttachment());
            RenderSystem.setShaderTexture(1, mc.getFramebuffer().getDepthAttachment());
            RenderSystem.setShaderTexture(2, this.e.getDepthAttachment());
            RenderSystem.setShaderTexture(3, this.e.getColorAttachment());
            RenderSystem.setShaderTexture(4, mask.getColorAttachment());
            RenderSystem.setShaderTexture(5, blurY.getColorAttachment());
            a();
            if (this.f != null) {
                this.f.set(color[0], color[1], color[2], color[3]);
            }
            if (this.g != null) {
                this.g.set((System.currentTimeMillis() % 100000) / 1000.0f);
            }
            if (modeUniform != null) modeUniform.set((float) mode);
            if (waveSpeedUniform != null) waveSpeedUniform.set(waveSpeed);
            if (waveScaleUniform != null) waveScaleUniform.set(waveScale);
            if (outlineUniform != null) outlineUniform.set(outline);
            if (glowUniform != null) glowUniform.set(glow);
            if (glowRadiusUniform != null) glowRadiusUniform.set(glowRadius);
            if (fillUniform != null) fillUniform.set(fill);
            if (blurRadiusUniform != null) blurRadiusUniform.set(blurRadius);
            if (resolutionUniform != null) resolutionUniform.set(
                    (float) mc.getWindow().getFramebufferWidth(), (float) mc.getWindow().getFramebufferHeight());
            drawQuad();
            for (int unit = 0; unit <= 5; unit++) RenderSystem.setShaderTexture(unit, 0);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private void blur(SimpleFramebuffer target, int source, int width, int height,
                      float radius, boolean horizontal) {
        target.setClearColor(0, 0, 0, 0);
        target.clear();
        target.beginWrite(true);
        RenderSystem.setShaderTexture(0, source);
        ShaderProgram program = RenderSystem.setShader(BLUR_SHADER);
        Uniform size = program.getUniform("TargetSize");
        if (size != null) size.set((float) target.textureWidth, (float) target.textureHeight);
        Uniform direction = program.getUniform("Direction");
        if (direction != null) direction.set(horizontal ? 1f / width : 0f,
                horizontal ? 0f : 1f / height);
        Uniform spread = program.getUniform("Radius");
        if (spread != null) spread.set(radius);
        drawQuad();
    }

    private static void drawQuad() {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(-1.0f, -1.0f, 0.0f).color(-1);
        builder.vertex(-1.0f, 1.0f, 0.0f).color(-1);
        builder.vertex(1.0f, 1.0f, 0.0f).color(-1);
        builder.vertex(1.0f, -1.0f, 0.0f).color(-1);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
}
