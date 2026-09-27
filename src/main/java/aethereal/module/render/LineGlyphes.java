package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Primordial;
import aethereal.event.DrawEvent;
import aethereal.event.TickEvent;
import aethereal.setting.BooleanSetting;
import aethereal.setting.SliderSetting;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Line Glyphes", description = "Анимированные линии в мире", category = Category.Render)
public final class LineGlyphes extends Module {
    private final SliderSetting count = new SliderSetting("Количество", 50.0f, 10.0f, 200.0f, 1.0f);
    private final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final BooleanSetting glow = new BooleanSetting("Свечение", true);
    private final SliderSetting thickness = new SliderSetting("Толщина", 1.5f, 0.5f, 5.0f, 0.1f);
    private final List<Path> paths = new ArrayList<>();

    public LineGlyphes() { a(count, speed, glow, thickness); }
    @Override public void b() { paths.clear(); super.b(); }
    @Override public void c() { paths.clear(); super.c(); }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) { paths.clear(); return; }
        Iterator<Path> iterator = paths.iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next();
            path.tick(speed.c());
            if (path.dead()) iterator.remove();
        }
        while (paths.size() > Math.round(count.c())) paths.remove(paths.size() - 1);
        if (paths.size() < Math.round(count.c())) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            paths.add(new Path(mc.player.getPos().add(random.nextDouble(-30, 30),
                    random.nextDouble(-7.5, 7.5), random.nextDouble(-30, 30)), random.nextInt(12, 27)));
        }
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || paths.isEmpty() || mc.player == null) return;
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = event.h();
        int theme = Primordial.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
        RenderSystem.enableBlend(); RenderSystem.enableDepthTest(); RenderSystem.depthMask(false);
        RenderSystem.disableCull(); RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        try {
            if (glow.c()) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                drawPass(matrices, camera, theme, 5.0f, 0.08f, event.g());
                drawPass(matrices, camera, theme, 2.5f, 0.16f, event.g());
            }
            RenderSystem.defaultBlendFunc();
            drawPass(matrices, camera, theme, 1.0f, 0.85f, event.g());
        } finally {
            RenderSystem.lineWidth(1.0f); RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true); RenderSystem.enableCull(); RenderSystem.disableBlend();
        }
    }

    private void drawPass(MatrixStack matrices, Vec3d camera, int theme,
                          float width, float strength, float tickDelta) {
        RenderSystem.lineWidth(Math.max(1.0f, thickness.c() * width));
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f worldMatrix = matrices.peek().getPositionMatrix();
        for (Path path : paths) {
            float alpha = path.alpha() * strength;
            if (alpha <= 0.005f) continue;
            List<Vec3d> points = path.points;
            for (int i = 0; i < points.size() - 1; i++) {
                Vec3d a = points.get(i), b = points.get(i + 1);
                if (!path.removing && i == points.size() - 2) {
                    float progress = path.previousProgress + (path.progress - path.previousProgress)
                            * Math.max(0.0f, Math.min(1.0f, tickDelta));
                    b = a.add(b.subtract(a).multiply(progress));
                }
                Vec3d delta = b.subtract(a);
                double length = delta.length();
                if (length < 0.001) continue;
                int color = (Math.min(255, Math.round(alpha * (i + 1) / points.size() * 255)) << 24)
                        | (theme & 0x00FFFFFF);
                for (double start = 0; start < length; start += 0.92) {
                    Vec3d from = a.add(delta.multiply(start / length)).subtract(camera);
                    Vec3d to = a.add(delta.multiply(Math.min(start + 0.58, length) / length)).subtract(camera);
                    buffer.vertex(worldMatrix, (float) from.x, (float) from.y, (float) from.z).color(color);
                    buffer.vertex(worldMatrix, (float) to.x, (float) to.y, (float) to.z).color(color);
                }
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static final class Path {
        final List<Vec3d> points = new ArrayList<>();
        final int maxPoints;
        Vec3d lastDirection = Vec3d.ZERO;
        float progress, previousProgress, opacity;
        boolean removing;

        Path(Vec3d start, int maxPoints) { points.add(start); this.maxPoints = maxPoints; }

        void tick(float speed) {
            opacity += (removing ? -0.035f : 0.035f);
            opacity = Math.max(0.0f, Math.min(1.0f, opacity));
            if (removing) return;
            previousProgress = progress;
            progress += 0.025f * speed;
            if (progress < 1.0f) return;
            progress -= 1.0f;
            previousProgress = 0.0f;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Vec3d direction;
            do {
                int axis = random.nextInt(3), sign = random.nextBoolean() ? 1 : -1;
                direction = axis == 0 ? new Vec3d(sign, 0, 0)
                        : axis == 1 ? new Vec3d(0, sign, 0) : new Vec3d(0, 0, sign);
            } while (direction.dotProduct(lastDirection) < -0.5);
            lastDirection = direction;
            points.add(points.get(points.size() - 1).add(direction.multiply(2.5)));
            if (points.size() >= maxPoints) removing = true;
        }

        float alpha() { return opacity; }
        boolean dead() { return removing && opacity <= 0.0f; }
    }
}
