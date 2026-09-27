package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Primordial;
import aethereal.event.DrawEvent;
import aethereal.event.TickEvent;
import aethereal.setting.SliderSetting;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleRegister(name = "Jump Circle", description = "Светящийся круг при прыжке", category = Category.Render)
public final class JumpCircle extends Module {
    private static final Identifier TEXTURE = Identifier.of("primordial", "textures/particles/circle.png");
    private final SliderSetting radius = new SliderSetting("Радиус", 1.85f, 0.5f, 4.0f, 0.1f);
    private final SliderSetting speed = new SliderSetting("Скорость", 1.2f, 1.0f, 5.0f, 0.1f);
    private final SliderSetting fadeSpeed = new SliderSetting("Скорость исчезновения", 1.5f, 1.0f, 5.0f, 0.5f);
    private final List<Circle> circles = new ArrayList<>();
    private boolean wasOnGround;

    public JumpCircle() { a(radius, speed, fadeSpeed); }
    @Override public void b() { circles.clear(); wasOnGround = mc.player != null && mc.player.isOnGround(); super.b(); }
    @Override public void c() { circles.clear(); super.c(); }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) { circles.clear(); wasOnGround = false; return; }
        boolean onGround = mc.player.isOnGround();
        if (wasOnGround && !onGround && mc.player.getVelocity().y > 0.0) {
            circles.add(new Circle(new Vec3d(mc.player.getX(), Math.floor(mc.player.getY()) + 0.01,
                    mc.player.getZ()), System.currentTimeMillis()));
            while (circles.size() > 8) circles.remove(0);
        }
        wasOnGround = onGround;
        long now = System.currentTimeMillis();
        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) if (now - iterator.next().started > 1850.0f / speed.c()) iterator.remove();
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || circles.isEmpty() || mc.player == null) return;
        long now = System.currentTimeMillis();
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = event.h();
        int theme = Primordial.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, TEXTURE);
        try {
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR);
            for (Circle circle : circles) {
                float progress = (now - circle.started) * speed.c() / 1850.0f;
                if (progress >= 1.0f) continue;
                float fade = Math.max(0.0f, 1.0f - progress * fadeSpeed.c());
                if (fade <= 0.01f) continue;
                float eased = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
                float elapsed = (now - circle.started) / 1000.0f;
                float pulse = 1.0f + (float) Math.sin(elapsed * 7.0f * speed.c()) * 0.06f;
                float half = radius.c() * eased * pulse * 0.5f;
                int color = (Math.min(255, Math.round(fade * 255.0f)) << 24) | (theme & 0x00FFFFFF);
                int outer = (Math.min(255, Math.round(fade * 160.0f)) << 24) | (theme & 0x00FFFFFF);
                matrices.push();
                matrices.translate(circle.pos.x - camera.x, circle.pos.y - camera.y, circle.pos.z - camera.z);
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(elapsed * 120.0f * speed.c()));
                quad(buffer, matrices.peek().getPositionMatrix(), half, color);
                quad(buffer, matrices.peek().getPositionMatrix(), half * 1.08f, outer);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, float h, int color) {
        buffer.vertex(matrix, -h, -h, 0).texture(0, 1).color(color);
        buffer.vertex(matrix, -h, h, 0).texture(0, 0).color(color);
        buffer.vertex(matrix, h, h, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, h, -h, 0).texture(1, 1).color(color);
    }

    private record Circle(Vec3d pos, long started) { }
}
