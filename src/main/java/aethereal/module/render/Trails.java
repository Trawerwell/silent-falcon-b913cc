package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "Trails", description = "Ленты, светящиеся линии и частицы за целями", category = Category.Render)
public final class Trails extends Module {
    private static final String[] PARTICLE_NAMES = {
            "Спарки", "Доллар", "Блум", "Снег", "Звезда", "Кубы", "Треугольники"
    };
    private static final Identifier[] PARTICLE_TEXTURES = {
            texture("spark_1"), texture("spark_2"), texture("spark_3"), texture("sparkle"),
            texture("dollar"), texture("bloom"), texture("snow"), texture("star")
    };
    private static final Identifier GLOW_TEXTURE = texture("glow");
    private static final int CUBE = 5;
    private static final int TRIANGLE = 6;

    private final MultiModeSetting targets = new MultiModeSetting("Цели",
            new BooleanSetting("Локальный игрок", true),
            new BooleanSetting("Другие игроки", false),
            new BooleanSetting("Снаряды", true));

    private final ModeSetting playerStyle = new ModeSetting("Игроки: стиль", "Лента",
            "Лента", "Частицы", "Светящаяся линия");
    private final ColorSetting playerColor = new ColorSetting("Игроки: цвет", 0xFF8FB9FF);
    private final SliderSetting playerLength = new SliderSetting("Игроки: длина", 4.0f, 0.5f, 12.0f, 0.5f);
    private final SliderSetting playerOpacity = new SliderSetting("Игроки: прозрачность", 0.72f, 0.05f, 1.0f, 0.05f);
    private final SliderSetting playerThickness = new SliderSetting("Игроки: толщина", 1.5f, 0.5f, 6.0f, 0.25f);
    private final SliderSetting playerFade = new SliderSetting("Игроки: скорость исчезновения", 1.0f, 0.25f, 3.0f, 0.25f);
    private final SliderSetting playerGlow = new SliderSetting("Игроки: сила свечения", 1.2f, 0.0f, 5.0f, 0.1f);
    private final MultiModeSetting playerParticles = particleSetting("Игроки: частицы", playerStyle);
    private final BooleanSetting firstPerson = new BooleanSetting("Игроки: от первого лица", true);

    private final ModeSetting projectileStyle = new ModeSetting("Снаряды: стиль", "Светящаяся линия",
            "Лента", "Частицы", "Светящаяся линия");
    private final ColorSetting projectileColor = new ColorSetting("Снаряды: цвет", 0xFFFFB86C);
    private final SliderSetting projectileLength = new SliderSetting("Снаряды: длина", 2.5f, 0.25f, 8.0f, 0.25f);
    private final SliderSetting projectileOpacity = new SliderSetting("Снаряды: прозрачность", 0.9f, 0.05f, 1.0f, 0.05f);
    private final SliderSetting projectileThickness = new SliderSetting("Снаряды: толщина", 1.0f, 0.5f, 6.0f, 0.25f);
    private final SliderSetting projectileFade = new SliderSetting("Снаряды: скорость исчезновения", 1.4f, 0.25f, 3.0f, 0.25f);
    private final SliderSetting projectileGlow = new SliderSetting("Снаряды: сила свечения", 1.5f, 0.0f, 5.0f, 0.1f);
    private final MultiModeSetting projectileParticles = particleSetting("Снаряды: частицы", projectileStyle);

    private final Map<Integer, Trail> trails = new HashMap<>();
    private Object lastWorld;

    public Trails() {
        a(targets,
                playerStyle, playerColor, playerLength, playerOpacity, playerThickness, playerFade,
                playerGlow, playerParticles, firstPerson,
                projectileStyle, projectileColor, projectileLength, projectileOpacity, projectileThickness,
                projectileFade, projectileGlow, projectileParticles);
    }

    private static MultiModeSetting particleSetting(String name, ModeSetting style) {
        return new MultiModeSetting(name,
                new BooleanSetting("Спарки", true), new BooleanSetting("Доллар", false),
                new BooleanSetting("Блум", false), new BooleanSetting("Снег", false),
                new BooleanSetting("Звезда", false), new BooleanSetting("Кубы", false),
                new BooleanSetting("Треугольники", false)).a(() -> style.l("Частицы"));
    }

    private static Identifier texture(String name) {
        return Identifier.of("primordial", "textures/particles/" + name + ".png");
    }

    @Override public void b() { trails.clear(); lastWorld = null; super.b(); }
    @Override public void c() { trails.clear(); lastWorld = null; super.c(); }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            trails.clear(); lastWorld = null; return;
        }
        if (lastWorld != mc.world) {
            trails.clear(); lastWorld = mc.world;
        }

        long now = System.currentTimeMillis();
        if (enabled("Локальный игрок")) sample(mc.player, Target.LOCAL_PLAYER, now);
        if (enabled("Другие игроки")) {
            for (PlayerEntity player : mc.world.getPlayers())
                if (player != mc.player && !player.isRemoved()) sample(player, Target.OTHER_PLAYER, now);
        }
        if (enabled("Снаряды")) {
            Box range = mc.player.getBoundingBox().expand(128.0);
            for (ProjectileEntity projectile : mc.world.getEntitiesByClass(
                    ProjectileEntity.class, range, entity -> !entity.isRemoved()))
                sample(projectile, Target.PROJECTILE, now);
        }

        Iterator<Trail> iterator = trails.values().iterator();
        while (iterator.hasNext()) {
            Trail trail = iterator.next();
            long lifetime = profile(trail).lifetime();
            trail.points.removeIf(point -> now - point.created > lifetime);
            if (trail.points.isEmpty() && now - trail.lastSeen > lifetime) iterator.remove();
        }
    }

    private boolean enabled(String name) {
        BooleanSetting value = targets.a(name);
        return value != null && value.c();
    }

    private void sample(Entity entity, Target target, long now) {
        Trail trail = trails.computeIfAbsent(entity.getId(), id -> new Trail(target));
        trail.target = target;
        trail.lastSeen = now;
        Vec3d position = entity.getPos();
        if (trail.points.isEmpty() || trail.points.get(trail.points.size() - 1).position.squaredDistanceTo(position) > 0.0025) {
            trail.points.add(new Point(position, now, ThreadLocalRandom.current().nextInt()));
        }
        int maximum = Math.max(16, Math.round(profile(trail).length.c() * 40.0f));
        while (trail.points.size() > maximum) trail.points.remove(0);
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.player == null || mc.world == null || trails.isEmpty()) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cameraPosition = camera.getPos();
        MatrixStack matrices = event.h();
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            drawRibbonProfile(matrices, cameraPosition, now, false);
            drawRibbonProfile(matrices, cameraPosition, now, true);
            drawLineProfile(matrices, cameraPosition, now, false);
            drawLineProfile(matrices, cameraPosition, now, true);
            drawParticleProfile(matrices, camera, cameraPosition, now, false);
            drawParticleProfile(matrices, camera, cameraPosition, now, true);
        } finally {
            RenderSystem.lineWidth(1.0f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private void drawRibbonProfile(MatrixStack matrices, Vec3d camera, long now, boolean projectile) {
        Profile p = projectile ? projectileProfile() : playerProfile();
        if (!p.style.l("Лента") || !hasTrail(projectile, 2)) return;

        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder ribbon = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float height = projectile ? 0.20f * p.thickness.c() : 1.42f;
        float bottom = projectile ? -height * 0.5f : 0.12f;
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail) || trail.points.size() < 2) continue;
            for (int i = 1; i < trail.points.size(); i++) {
                Point previous = trail.points.get(i - 1), current = trail.points.get(i);
                Vec3d a = previous.position.subtract(camera), b = current.position.subtract(camera);
                int ca = color(previous, now, p, 0.62f), cb = color(current, now, p, 0.62f);
                ribbon.vertex(matrix, (float) a.x, (float) a.y + bottom, (float) a.z).color(ca);
                ribbon.vertex(matrix, (float) b.x, (float) b.y + bottom, (float) b.z).color(cb);
                ribbon.vertex(matrix, (float) b.x, (float) b.y + bottom + height, (float) b.z).color(cb);
                ribbon.vertex(matrix, (float) a.x, (float) a.y + bottom + height, (float) a.z).color(ca);
                vertices += 4;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(ribbon.end());

        float radius = 0.012f * p.thickness.c();
        drawRails(matrices, camera, now, projectile, p, bottom, height, radius);
        if (p.glow.c() > 0.0f) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            float glow = p.glow.c();
            drawRailGlow(matrices, camera, now, projectile, p, bottom, height,
                    Math.max(1.0f, p.thickness.c() * (2.0f + glow * 1.7f)), 0.07f * glow);
            drawRailGlow(matrices, camera, now, projectile, p, bottom, height,
                    Math.max(1.0f, p.thickness.c() * (1.2f + glow * 0.7f)), 0.16f * glow);
            RenderSystem.defaultBlendFunc();
        }
    }

    private void drawRails(MatrixStack matrices, Vec3d camera, long now, boolean projectile,
                           Profile profile, float bottom, float height, float radius) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder tubes = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail) || trail.points.size() < 2) continue;
            for (int i = 1; i < trail.points.size(); i++) {
                Point previous = trail.points.get(i - 1), current = trail.points.get(i);
                int ca = color(previous, now, profile, 1.0f), cb = color(current, now, profile, 1.0f);
                Vec3d a = previous.position.subtract(camera), b = current.position.subtract(camera);
                vertices += tube(tubes, matrix, a.add(0, bottom, 0), b.add(0, bottom, 0), radius, ca, cb);
                vertices += tube(tubes, matrix, a.add(0, bottom + height, 0), b.add(0, bottom + height, 0), radius, ca, cb);
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(tubes.end());
    }

    private static int tube(BufferBuilder buffer, Matrix4f matrix, Vec3d a, Vec3d b,
                            float radius, int ca, int cb) {
        Vec3d direction = b.subtract(a);
        if (direction.lengthSquared() < 1.0e-7) return 0;
        direction = direction.normalize();
        Vec3d reference = Math.abs(direction.y) < 0.92 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d u = direction.crossProduct(reference).normalize().multiply(radius);
        Vec3d v = direction.crossProduct(u).normalize().multiply(radius);
        int sides = 8;
        for (int side = 0; side < sides; side++) {
            double angleA = Math.PI * 2.0 * side / sides;
            double angleB = Math.PI * 2.0 * (side + 1) / sides;
            Vec3d offsetA = u.multiply(Math.cos(angleA)).add(v.multiply(Math.sin(angleA)));
            Vec3d offsetB = u.multiply(Math.cos(angleB)).add(v.multiply(Math.sin(angleB)));
            vertex(buffer, matrix, a.add(offsetA), ca); vertex(buffer, matrix, b.add(offsetA), cb);
            vertex(buffer, matrix, b.add(offsetB), cb); vertex(buffer, matrix, a.add(offsetB), ca);
        }
        return sides * 4;
    }

    private void drawRailGlow(MatrixStack matrices, Vec3d camera, long now, boolean projectile,
                              Profile profile, float bottom, float height, float width, float strength) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.lineWidth(width);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail) || trail.points.size() < 2) continue;
            for (int i = 1; i < trail.points.size(); i++) {
                Point previous = trail.points.get(i - 1), current = trail.points.get(i);
                int ca = color(previous, now, profile, Math.min(1.0f, strength));
                int cb = color(current, now, profile, Math.min(1.0f, strength));
                Vec3d a = previous.position.subtract(camera), b = current.position.subtract(camera);
                line(lines, matrix, a.add(0, bottom, 0), b.add(0, bottom, 0), ca, cb);
                line(lines, matrix, a.add(0, bottom + height, 0), b.add(0, bottom + height, 0), ca, cb);
                vertices += 4;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void drawLineProfile(MatrixStack matrices, Vec3d camera, long now, boolean projectile) {
        Profile p = projectile ? projectileProfile() : playerProfile();
        if (!p.style.l("Светящаяся линия") || !hasTrail(projectile, 2)) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (p.glow.c() > 0.0f) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            float glow = p.glow.c();
            drawPathLines(matrices, camera, now, projectile, p,
                    Math.max(1.0f, p.thickness.c() * (3.0f + glow * 2.0f)), 0.06f * glow);
            drawPathLines(matrices, camera, now, projectile, p,
                    Math.max(1.0f, p.thickness.c() * (1.8f + glow)), 0.15f * glow);
        }
        RenderSystem.defaultBlendFunc();
        drawPathLines(matrices, camera, now, projectile, p, Math.max(1.0f, p.thickness.c()), 1.0f);
    }

    private void drawPathLines(MatrixStack matrices, Vec3d camera, long now, boolean projectile,
                               Profile profile, float width, float strength) {
        RenderSystem.lineWidth(width);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail) || trail.points.size() < 2) continue;
            float y = projectile ? 0.0f : 0.76f;
            for (int i = 1; i < trail.points.size(); i++) {
                Point previous = trail.points.get(i - 1), current = trail.points.get(i);
                Vec3d a = previous.position.subtract(camera).add(0, y, 0);
                Vec3d b = current.position.subtract(camera).add(0, y, 0);
                line(lines, matrix, a, b, color(previous, now, profile, strength),
                        color(current, now, profile, strength));
                vertices += 2;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(lines.end());
    }

    private void drawParticleProfile(MatrixStack matrices, Camera camera, Vec3d cameraPosition,
                                     long now, boolean projectile) {
        Profile p = projectile ? projectileProfile() : playerProfile();
        if (!p.style.l("Частицы")) return;
        int[] forms = enabledForms(p.particles);
        if (forms.length == 0) return;
        List<ParticleView> views = collectParticles(cameraPosition, now, projectile, p, forms);
        if (views.isEmpty()) return;

        if (p.glow.c() > 0.0f) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder glowBuffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR);
            for (ParticleView view : views) {
                matrices.push(); matrices.translate(view.position.x, view.position.y, view.position.z);
                matrices.multiply(camera.getRotation());
                float size = view.size * (2.4f + p.glow.c() * 1.35f);
                appendSprite(glowBuffer, matrices.peek().getPositionMatrix(), size,
                        applyAlpha(view.color, Math.min(0.38f, 0.07f * p.glow.c())));
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(glowBuffer.end());
        }

        RenderSystem.defaultBlendFunc();
        for (int textureIndex = 0; textureIndex < PARTICLE_TEXTURES.length; textureIndex++) {
            final int selectedTexture = textureIndex;
            if (views.stream().noneMatch(view -> view.texture == selectedTexture)) continue;
            RenderSystem.setShaderTexture(0, PARTICLE_TEXTURES[textureIndex]);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder sprites = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR);
            for (ParticleView view : views) {
                if (view.texture != textureIndex) continue;
                matrices.push(); matrices.translate(view.position.x, view.position.y, view.position.z);
                matrices.multiply(camera.getRotation());
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(view.rotation));
                appendSprite(sprites, matrices.peek().getPositionMatrix(), view.size, view.color);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(sprites.end());
        }
        drawParticleShapes(matrices, views, CUBE);
        drawParticleShapes(matrices, views, TRIANGLE);
    }

    private List<ParticleView> collectParticles(Vec3d camera, long now, boolean projectile,
                                                Profile profile, int[] forms) {
        List<ParticleView> result = new ArrayList<>();
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail)) continue;
            for (int i = 0; i < trail.points.size(); i += 2) {
                Point point = trail.points.get(i);
                int form = forms[Math.floorMod(point.seed, forms.length)];
                int texture = textureIndex(form, point.seed);
                float life = 1.0f - Math.min(1.0f, (now - point.created) / (float) profile.lifetime());
                if (life <= 0.01f) continue;
                double ox = signed(point.seed >>> 5) * (projectile ? 0.035 : 0.09);
                double oz = signed(point.seed >>> 13) * (projectile ? 0.035 : 0.09);
                double oy = projectile ? signed(point.seed >>> 21) * 0.035
                        : 0.18 + unit(point.seed >>> 21) * 1.18;
                double floatOffset = Math.sin(now * 0.003 + point.seed * 0.0001) * 0.025;
                Vec3d position = point.position.add(ox, oy + floatOffset, oz).subtract(camera);
                float pulse = 0.88f + 0.12f * (float) Math.sin(now * 0.006 + point.seed);
                float size = (projectile ? 0.095f : 0.14f) * profile.thickness.c() * pulse;
                int color = color(point, now, profile, Math.min(1.0f, life * 1.4f));
                result.add(new ParticleView(position, form, texture, color, size,
                        (point.seed & 359) + now * 0.018f));
            }
        }
        return result;
    }

    private void drawParticleShapes(MatrixStack matrices, List<ParticleView> views, int form) {
        if (views.stream().noneMatch(view -> view.form == form)) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder faces = Tessellator.getInstance().begin(
                form == CUBE ? VertexFormat.DrawMode.QUADS : VertexFormat.DrawMode.TRIANGLES,
                VertexFormats.POSITION_COLOR);
        for (ParticleView view : views) {
            if (view.form != form) continue;
            matrices.push(); matrices.translate(view.position.x, view.position.y, view.position.z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(view.rotation * 0.73f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(view.rotation));
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            if (form == CUBE) cube(faces, matrix, view.size * 0.55f, view.color);
            else triangle(faces, matrix, view.size * 0.70f, view.color);
            matrices.pop();
        }
        BufferRenderer.drawWithGlobalProgram(faces.end());
    }

    private int[] enabledForms(MultiModeSetting setting) {
        int count = 0;
        for (String name : PARTICLE_NAMES) if (setting.a(name).c()) count++;
        int[] enabled = new int[count];
        int index = 0;
        for (int i = 0; i < PARTICLE_NAMES.length; i++) if (setting.a(PARTICLE_NAMES[i]).c()) enabled[index++] = i;
        return enabled;
    }

    private static int textureIndex(int form, int seed) {
        if (form == 0) return Math.floorMod(seed >>> 8, 4);
        if (form >= 1 && form <= 4) return form + 3;
        return -1;
    }

    private boolean hasTrail(boolean projectile, int points) {
        return trails.values().stream().anyMatch(trail -> matches(trail, projectile)
                && visible(trail) && trail.points.size() >= points);
    }

    private static boolean matches(Trail trail, boolean projectile) {
        return (trail.target == Target.PROJECTILE) == projectile;
    }

    private boolean visible(Trail trail) {
        return trail.target != Target.LOCAL_PLAYER || firstPerson.c()
                || !mc.options.getPerspective().isFirstPerson();
    }

    private Profile profile(Trail trail) {
        return trail.target == Target.PROJECTILE ? projectileProfile() : playerProfile();
    }

    private Profile playerProfile() {
        return new Profile(playerStyle, playerColor, playerLength, playerOpacity,
                playerThickness, playerFade, playerGlow, playerParticles);
    }

    private Profile projectileProfile() {
        return new Profile(projectileStyle, projectileColor, projectileLength, projectileOpacity,
                projectileThickness, projectileFade, projectileGlow, projectileParticles);
    }

    private static int color(Point point, long now, Profile profile, float strength) {
        float age = Math.max(0.0f, Math.min(1.0f, (now - point.created) / (float) profile.lifetime()));
        float fade = (1.0f - smooth(age)) * profile.opacity.c() * Math.max(0.0f, Math.min(1.0f, strength));
        return applyAlpha(profile.color.c(), fade);
    }

    private static float smooth(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private static int applyAlpha(int color, float factor) {
        int alpha = Math.max(0, Math.min(255,
                Math.round(((color >>> 24) & 255) * Math.max(0.0f, Math.min(1.0f, factor)))));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static float unit(int value) { return (value & 1023) / 1023.0f; }
    private static float signed(int value) { return unit(value) * 2.0f - 1.0f; }

    private static void line(BufferBuilder buffer, Matrix4f matrix, Vec3d a, Vec3d b, int ca, int cb) {
        vertex(buffer, matrix, a, ca); vertex(buffer, matrix, b, cb);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vec3d point, int color) {
        buffer.vertex(matrix, (float) point.x, (float) point.y, (float) point.z).color(color);
    }

    private static void appendSprite(BufferBuilder buffer, Matrix4f matrix, float size, int color) {
        float half = size * 0.5f;
        buffer.vertex(matrix, -half, -half, 0).texture(0, 1).color(color);
        buffer.vertex(matrix, half, -half, 0).texture(1, 1).color(color);
        buffer.vertex(matrix, half, half, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, -half, half, 0).texture(0, 0).color(color);
    }

    private static void cube(BufferBuilder b, Matrix4f m, float s, int c) {
        quad(b,m,-s,-s,s, s,-s,s, s,s,s, -s,s,s,c);
        quad(b,m,s,-s,-s, -s,-s,-s, -s,s,-s, s,s,-s,c);
        quad(b,m,-s,s,s, s,s,s, s,s,-s, -s,s,-s,c);
        quad(b,m,-s,-s,-s, s,-s,-s, s,-s,s, -s,-s,s,c);
        quad(b,m,s,-s,s, s,-s,-s, s,s,-s, s,s,s,c);
        quad(b,m,-s,-s,-s, -s,-s,s, -s,s,s, -s,s,-s,c);
    }

    private static void triangle(BufferBuilder b, Matrix4f m, float s, int c) {
        float h = s * 0.866f;
        tri(b,m,0,s,0, -h,-s,h, h,-s,h,c); tri(b,m,0,s,0, h,-s,h, h,-s,-h,c);
        tri(b,m,0,s,0, h,-s,-h, -h,-s,-h,c); tri(b,m,0,s,0, -h,-s,-h, -h,-s,h,c);
        tri(b,m,-h,-s,h, h,-s,h, h,-s,-h,c); tri(b,m,-h,-s,h, h,-s,-h, -h,-s,-h,c);
    }

    private static void quad(BufferBuilder b, Matrix4f m, float ax,float ay,float az,
                             float bx,float by,float bz,float cx,float cy,float cz,
                             float dx,float dy,float dz,int c) {
        b.vertex(m,ax,ay,az).color(c); b.vertex(m,bx,by,bz).color(c);
        b.vertex(m,cx,cy,cz).color(c); b.vertex(m,dx,dy,dz).color(c);
    }

    private static void tri(BufferBuilder b, Matrix4f m, float ax,float ay,float az,
                            float bx,float by,float bz,float cx,float cy,float cz,int c) {
        b.vertex(m,ax,ay,az).color(c); b.vertex(m,bx,by,bz).color(c); b.vertex(m,cx,cy,cz).color(c);
    }

    private enum Target { LOCAL_PLAYER, OTHER_PLAYER, PROJECTILE }

    private static final class Trail {
        final List<Point> points = new ArrayList<>();
        Target target;
        long lastSeen;
        Trail(Target target) { this.target = target; }
    }

    private record Point(Vec3d position, long created, int seed) { }
    private record ParticleView(Vec3d position, int form, int texture, int color, float size, float rotation) { }
    private record Profile(ModeSetting style, ColorSetting color, SliderSetting length,
                           SliderSetting opacity, SliderSetting thickness, SliderSetting fade,
                           SliderSetting glow, MultiModeSetting particles) {
        long lifetime() {
            return Math.max(150L, Math.round(length.c() * 1000.0f / fade.c()));
        }
    }
}
