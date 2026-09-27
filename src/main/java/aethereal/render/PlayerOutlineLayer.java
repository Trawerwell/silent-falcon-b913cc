package aethereal.render;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/** Renders the posed player into an isolated mask, then expands only its screen-space silhouette. */
public final class PlayerOutlineLayer extends RenderLayer {
    private static final ShaderProgramKey MASK_SHADER = new ShaderProgramKey(Identifier.of("primordial", "core/entity_outline"),
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, net.minecraft.client.gl.Defines.EMPTY);
    private static final ShaderProgramKey EFFECT_SHADER = new ShaderProgramKey(Identifier.of("primordial", "core/entity_outline_effect"),
            VertexFormats.POSITION_COLOR, net.minecraft.client.gl.Defines.EMPTY);
    private static final Matrix4f SCREEN_PROJECTION = new Matrix4f();
    private static SimpleFramebuffer mask;
    private static SimpleFramebuffer blurX, blurY;
    private static final BufferAllocator ALLOCATOR = new BufferAllocator(262144);
    private static final ShaderProgramKey BLUR_SHADER = new ShaderProgramKey(Identifier.of("primordial", "core/entity_outline_blur"),
            VertexFormats.POSITION_COLOR, net.minecraft.client.gl.Defines.EMPTY);
    private static Identifier skin;

    private PlayerOutlineLayer() {
        super("primordial_player_mask", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 1536, false, true, PlayerOutlineLayer::begin, PlayerOutlineLayer::end);
    }

    private static final PlayerOutlineLayer LAYER = new PlayerOutlineLayer();

    private static void begin() {
        RenderSystem.disableBlend();
        // X-ray mask: ignore world depth so the full player silhouette is captured.
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(MASK_SHADER);
        RenderSystem.setShaderTexture(0, skin);
    }

    private static void end() {
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    public static <S extends LivingEntityRenderState> void draw(EntityModel<? super S> model, S state,
            MatrixStack matrices, Identifier texture, int light, int color, float width,
            float glowRadius, float glowStrength, float distance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getFramebuffer() == null) return;
        int w = mc.getFramebuffer().textureWidth;
        int h = mc.getFramebuffer().textureHeight;
        if (w <= 0 || h <= 0) return;
        if (mask == null) mask = new SimpleFramebuffer(w, h, true);
        else if (mask.textureWidth != w || mask.textureHeight != h) mask.resize(w, h);

        // Pixel radii scale gently with distance; distant silhouettes retain a readable line.
        float safeDistance = Math.max(0, distance);
        float distanceScale = (float) Math.sqrt(100.0 / (100.0 + safeDistance));
        width = Math.max(0.8f, width * distanceScale);
        glowRadius *= distanceScale;
        // Fade the glow smoothly over distance while keeping a readable outline.
        float fade = (float) Math.exp(-Math.max(0.0, safeDistance - 8.0) / 120.0);
        glowStrength *= fade;
        float[] bounds = bounds(matrices, w, h, Math.max(width, glowRadius) + 4);
        int bw = Math.max(1, (w + 1) / 2), bh = Math.max(1, (h + 1) / 2);
        if (blurX == null) {
            blurX = new SimpleFramebuffer(bw, bh, false);
            blurY = new SimpleFramebuffer(bw, bh, false);
        } else if (blurX.textureWidth != bw || blurX.textureHeight != bh) {
            blurX.resize(bw, bh);
            blurY.resize(bw, bh);
        }
        mask.setTexFilter(9729);
        blurX.setTexFilter(9729);
        blurY.setTexFilter(9729);
        skin = texture;
        mask.setClearColor(0, 0, 0, 0);
        mask.clear();
        mask.beginWrite(false);
        try {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            model.render(matrices, builder, light, OverlayTexture.DEFAULT_UV, -1);
            LAYER.draw(builder.end());
        } finally {
            ALLOCATOR.clear();
            mc.getFramebuffer().beginWrite(true);
        }

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(SCREEN_PROJECTION, ProjectionType.PERSPECTIVE);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix().identity();
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableBlend();
            RenderSystem.disableCull();
            if (glowRadius > 0 && glowStrength > 0) {
                blur(blurX, mask.getColorAttachment(), w, h, glowRadius, true, bounds);
                blur(blurY, blurX.getColorAttachment(), w, h, glowRadius, false, bounds);
            }
            mc.getFramebuffer().beginWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShaderTexture(0, mask.getColorAttachment());
            RenderSystem.setShaderTexture(1, blurY.getColorAttachment());
            net.minecraft.client.gl.ShaderProgram shader = RenderSystem.setShader(EFFECT_SHADER);
            uniform(shader, "ScreenSize", w, h);
            uniform(shader, "OutlineSize", width);
            uniform(shader, "GlowRadius", glowRadius);
            uniform(shader, "GlowStrength", glowStrength);
            
            Uniform tint = shader.getUniform("OutlineColor");
            if (tint != null) tint.set(((color >>> 16) & 255) / 255f, ((color >>> 8) & 255) / 255f,
                    (color & 255) / 255f, ((color >>> 24) & 255) / 255f);
            quad(bounds);
        } finally {
            mc.getFramebuffer().beginWrite(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.setShaderTexture(1, 0);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            modelView.popMatrix();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void blur(SimpleFramebuffer target, int source, int w, int h, float radius,
                             boolean horizontal, float[] bounds) {
        target.setClearColor(0, 0, 0, 0);
        target.clear();
        target.beginWrite(true);
        RenderSystem.setShaderTexture(0, source);
        net.minecraft.client.gl.ShaderProgram shader = RenderSystem.setShader(BLUR_SHADER);
        uniform(shader, "TargetSize", target.textureWidth, target.textureHeight);
        uniform(shader, "Direction", horizontal ? 1f / w : 0, horizontal ? 0 : 1f / h);
        uniform(shader, "Radius", radius);
        quad(bounds);
    }

    private static void quad(float[] b) {
        BufferBuilder quad = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quad.vertex(b[0], b[1], 0).color(-1);
        quad.vertex(b[0], b[3], 0).color(-1);
        quad.vertex(b[2], b[3], 0).color(-1);
        quad.vertex(b[2], b[1], 0).color(-1);
        BufferRenderer.drawWithGlobalProgram(quad.end());
    }

    // Conservative local bounds include animated arms and outer skin layers.
    // At the near plane fall back to the screen, avoiding clipped limbs.
    private static float[] bounds(MatrixStack matrices, int w, int h, float padding) {
        Matrix4f transform = new Matrix4f(RenderSystem.getProjectionMatrix())
                .mul(RenderSystem.getModelViewMatrix()).mul(matrices.peek().getPositionMatrix());
        float minX = 1, minY = 1, maxX = -1, maxY = -1;
        for (int i = 0; i < 8; i++) {
            org.joml.Vector4f p = new org.joml.Vector4f((i & 1) == 0 ? -2 : 2,
                    (i & 2) == 0 ? -1 : 3, (i & 4) == 0 ? -2 : 2, 1).mul(transform);
            if (p.w <= 0.01f) return new float[]{-1, -1, 1, 1};
            minX = Math.min(minX, p.x / p.w); maxX = Math.max(maxX, p.x / p.w);
            minY = Math.min(minY, p.y / p.w); maxY = Math.max(maxY, p.y / p.w);
        }
        return new float[]{Math.max(-1, minX - 2 * padding / w), Math.max(-1, minY - 2 * padding / h),
                Math.min(1, maxX + 2 * padding / w), Math.min(1, maxY + 2 * padding / h)};
    }

    private static void uniform(net.minecraft.client.gl.ShaderProgram shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void uniform(net.minecraft.client.gl.ShaderProgram shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }
}
