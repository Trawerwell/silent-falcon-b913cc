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
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.MultiModeSetting;
import aethereal.setting.SliderSetting;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "Trails", description = "Настраиваемые следы игроков и снарядов", category = Category.Render)
public final class Trails extends Module {
    private final MultiModeSetting targets = new MultiModeSetting("Цели",
            new BooleanSetting("Локальный игрок", true),
            new BooleanSetting("Другие игроки", false),
            new BooleanSetting("Снаряды", true));
    private final ModeSetting style = new ModeSetting("Стиль", "Светящаяся линия",
            "Линия", "Лента", "Частицы", "Светящаяся линия");
    private final ModeSetting colorMode = new ModeSetting("Режим цвета", "Тема",
            "Тема", "Свой", "Радуга");
    private final ColorSetting customColor = new ColorSetting("Свой цвет", 0xFF8FB9FF)
            .a(() -> colorMode.l("Свой"));
    private final SliderSetting length = new SliderSetting("Длина следа", 4.0f, 0.5f, 12.0f, 0.5f);
    private final SliderSetting opacity = new SliderSetting("Прозрачность", 0.85f, 0.05f, 1.0f, 0.05f);
    private final SliderSetting thickness = new SliderSetting("Толщина", 2.0f, 0.5f, 8.0f, 0.25f);
    private final SliderSetting fadeSpeed = new SliderSetting("Скорость исчезновения", 1.0f, 0.25f, 3.0f, 0.25f);
    private final BooleanSetting firstPerson = new BooleanSetting("От первого лица", true);

    private final Map<Integer, Trail> trails = new HashMap<>();
    private Object lastWorld;

    public Trails() {
        a(targets, style, colorMode, customColor, length, opacity, thickness, fadeSpeed, firstPerson);
    }

    @Override
    public void b() {
        trails.clear();
        lastWorld = null;
        super.b();
    }

    @Override
    public void c() {
        trails.clear();
        lastWorld = null;
        super.c();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            trails.clear();
            lastWorld = null;
            return;
        }
        if (lastWorld != mc.world) {
            trails.clear();
            lastWorld = mc.world;
        }

        long now = System.currentTimeMillis();
        if (enabled("Локальный игрок")) sample(mc.player, Target.LOCAL_PLAYER, now);
        if (enabled("Другие игроки")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player && !player.isRemoved()) sample(player, Target.OTHER_PLAYER, now);
            }
        }
        if (enabled("Снаряды")) {
            Box search = mc.player.getBoundingBox().expand(128.0);
            for (ProjectileEntity projectile : mc.world.getEntitiesByClass(
                    ProjectileEntity.class, search, entity -> !entity.isRemoved())) {
                sample(projectile, Target.PROJECTILE, now);
            }
        }

        long lifetime = lifetimeMillis();
        Iterator<Trail> iterator = trails.values().iterator();
        while (iterator.hasNext()) {
            Trail trail = iterator.next();
            trail.points.removeIf(point -> now - point.created > lifetime);
            if (trail.points.isEmpty() && now - trail.lastSeen > lifetime) iterator.remove();
        }
    }

    private boolean enabled(String name) {
        BooleanSetting setting = targets.a(name);
        return setting != null && setting.c();
    }

    private long lifetimeMillis() {
        return Math.max(150L, Math.round(length.c() * 1000.0f / fadeSpeed.c()));
    }

    private void sample(Entity entity, Target target, long now) {
        Trail trail = trails.computeIfAbsent(entity.getId(), id -> new Trail(target));
        trail.target = target;
        trail.lastSeen = now;
        Vec3d position = entity.getPos().add(0.0, target == Target.PROJECTILE ? 0.0 : 0.08, 0.0);
        if (trail.points.isEmpty() || trail.points.get(trail.points.size() - 1).position.squaredDistanceTo(position) > 0.0025) {
            trail.points.add(new Point(position, now));
        }
        int maximum = Math.max(16, Math.round(length.c() * 40.0f));
        while (trail.points.size() > maximum) trail.points.remove(0);
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.player == null || mc.world == null || trails.isEmpty()) return;

        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPosition = camera.getPos();
        MatrixStack matrices = event.h();
        long now = System.currentTimeMillis();
        long lifetime = lifetimeMillis();
        int themeColor = Primordial.getInstance().getModuleProcessor().o()
                .a(ThemeInfo.PRIMARY).toIntColor();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        try {
            if (style.l("Лента")) {
                RenderSystem.defaultBlendFunc();
                drawRibbons(matrices, cameraPosition, now, lifetime, themeColor);
            } else if (style.l("Частицы")) {
                RenderSystem.defaultBlendFunc();
                drawParticles(matrices, camera, cameraPosition, now, lifetime, themeColor);
            } else if (style.l("Светящаяся линия")) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                drawLines(matrices, cameraPosition, now, lifetime, themeColor,
                        Math.max(1.0f, thickness.c() * 4.0f), 0.10f);
                drawLines(matrices, cameraPosition, now, lifetime, themeColor,
                        Math.max(1.0f, thickness.c() * 2.2f), 0.22f);
                RenderSystem.defaultBlendFunc();
                drawLines(matrices, cameraPosition, now, lifetime, themeColor,
                        Math.max(1.0f, thickness.c()), 1.0f);
            } else {
                RenderSystem.defaultBlendFunc();
                drawLines(matrices, cameraPosition, now, lifetime, themeColor,
                        Math.max(1.0f, thickness.c()), 1.0f);
            }
        } finally {
            RenderSystem.lineWidth(1.0f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void drawLines(MatrixStack matrices, Vec3d camera, long now, long lifetime,
                           int themeColor, float width, float strength) {
        if (trails.values().stream().noneMatch(trail -> visible(trail) && trail.points.size() >= 2)) return;
        RenderSystem.lineWidth(width);
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!visible(trail) || trail.points.size() < 2) continue;
            for (int i = 1; i < trail.points.size(); i++) {
                Point previous = trail.points.get(i - 1);
                Point current = trail.points.get(i);
                int first = color(previous, i - 1, now, lifetime, themeColor, strength);
                int second = color(current, i, now, lifetime, themeColor, strength);
                Vec3d a = previous.position.subtract(camera);
                Vec3d b = current.position.subtract(camera);
                buffer.vertex(matrix, (float) a.x, (float) a.y, (float) a.z).color(first);
                buffer.vertex(matrix, (float) b.x, (float) b.y, (float) b.z).color(second);
                vertices += 2;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawRibbons(MatrixStack matrices, Vec3d camera, long now, long lifetime, int themeColor) {
        if (trails.values().stream().noneMatch(trail -> visible(trail) && trail.points.size() >= 2)) return;
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = 0.035f * thickness.c();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!visible(trail) || trail.points.size() < 2) continue;
            for (int i = 1; i < trail.points.size(); i++) {
                Point previous = trail.points.get(i - 1);
                Point current = trail.points.get(i);
                Vec3d a = previous.position.subtract(camera);
                Vec3d b = current.position.subtract(camera);
                int ca = color(previous, i - 1, now, lifetime, themeColor, 1.0f);
                int cb = color(current, i, now, lifetime, themeColor, 1.0f);
                buffer.vertex(matrix, (float) a.x, (float) a.y - half, (float) a.z).color(ca);
                buffer.vertex(matrix, (float) b.x, (float) b.y - half, (float) b.z).color(cb);
                buffer.vertex(matrix, (float) b.x, (float) b.y + half, (float) b.z).color(cb);
                buffer.vertex(matrix, (float) a.x, (float) a.y + half, (float) a.z).color(ca);
                vertices += 4;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private void drawParticles(MatrixStack matrices, Camera camera, Vec3d cameraPosition,
                               long now, long lifetime, int themeColor) {
        if (trails.values().stream().noneMatch(trail -> visible(trail) && !trail.points.isEmpty())) return;
        BufferBuilder buffer = Tessellator.getInstance().begin(
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float half = 0.025f * thickness.c();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!visible(trail)) continue;
            for (int i = 0; i < trail.points.size(); i += 2) {
                Point point = trail.points.get(i);
                int color = color(point, i, now, lifetime, themeColor, 1.0f);
                if (((color >>> 24) & 255) < 2) continue;
                Vec3d position = point.position.subtract(cameraPosition);
                matrices.push();
                matrices.translate(position.x, position.y, position.z);
                matrices.multiply(camera.getRotation());
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                buffer.vertex(matrix, -half, -half, 0.0f).color(color);
                buffer.vertex(matrix, half, -half, 0.0f).color(color);
                buffer.vertex(matrix, half, half, 0.0f).color(color);
                buffer.vertex(matrix, -half, half, 0.0f).color(color);
                matrices.pop();
                vertices += 4;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private boolean visible(Trail trail) {
        if (trail.target != Target.LOCAL_PLAYER) return true;
        return firstPerson.c() || !mc.options.getPerspective().isFirstPerson();
    }

    private int color(Point point, int index, long now, long lifetime, int themeColor, float strength) {
        float age = Math.max(0.0f, Math.min(1.0f, (now - point.created) / (float) lifetime));
        float fade = (1.0f - age) * opacity.c() * strength;
        int rgb;
        if (colorMode.l("Свой")) {
            rgb = customColor.c();
        } else if (colorMode.l("Радуга")) {
            float hue = (now * 0.00012f + index * 0.018f) % 1.0f;
            rgb = hsv(hue, 0.72f, 1.0f);
        } else {
            rgb = themeColor;
        }
        int alpha = Math.max(0, Math.min(255, Math.round(fade * 255.0f)));
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private static int hsv(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0f;
        int sector = (int) h;
        float f = h - sector;
        float p = value * (1.0f - saturation);
        float q = value * (1.0f - saturation * f);
        float t = value * (1.0f - saturation * (1.0f - f));
        float r, g, b;
        switch (sector) {
            case 0 -> { r = value; g = t; b = p; }
            case 1 -> { r = q; g = value; b = p; }
            case 2 -> { r = p; g = value; b = t; }
            case 3 -> { r = p; g = q; b = value; }
            case 4 -> { r = t; g = p; b = value; }
            default -> { r = value; g = p; b = q; }
        }
        return 0xFF000000 | (Math.round(r * 255.0f) << 16)
                | (Math.round(g * 255.0f) << 8) | Math.round(b * 255.0f);
    }

    private enum Target { LOCAL_PLAYER, OTHER_PLAYER, PROJECTILE }

    private static final class Trail {
        final List<Point> points = new ArrayList<>();
        Target target;
        long lastSeen;

        Trail(Target target) {
            this.target = target;
        }
    }

    private record Point(Vec3d position, long created) { }
}
