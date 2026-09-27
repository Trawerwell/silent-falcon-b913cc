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
    private static final byte[][] CUBE_EDGES = {
            {-1,-1,-1, 1,-1,-1}, {1,-1,-1, 1,-1,1}, {1,-1,1, -1,-1,1}, {-1,-1,1, -1,-1,-1},
            {-1,1,-1, 1,1,-1}, {1,1,-1, 1,1,1}, {1,1,1, -1,1,1}, {-1,1,1, -1,1,-1},
            {-1,-1,-1, -1,1,-1}, {1,-1,-1, 1,1,-1}, {1,-1,1, 1,1,1}, {-1,-1,1, -1,1,1}
    };
    private static final byte[][] TRIANGLE_EDGES = {{0,1}, {0,2}, {0,3}, {0,4}, {1,2}, {2,3}, {3,4}, {4,1}};

    private final MultiModeSetting targets = new MultiModeSetting("Цели",
            new BooleanSetting("Локальный игрок", true),
            new BooleanSetting("Другие игроки", false),
            new BooleanSetting("Снаряды", true));

    private final ModeSetting playerStyle = new ModeSetting("Игроки: стиль", "Лента",
            "Лента", "Частицы");
    private final ColorSetting playerColor = new ColorSetting("Игроки: цвет", 0xFF8FB9FF);
    private final SliderSetting playerLength = new SliderSetting("Игроки: длина", 4.0f, 0.5f, 12.0f, 0.5f);
    private final SliderSetting playerOpacity = new SliderSetting("Игроки: прозрачность", 0.72f, 0.05f, 1.0f, 0.05f);
    private final SliderSetting playerFade = new SliderSetting("Игроки: скорость исчезновения", 1.0f, 0.25f, 3.0f, 0.25f);
    private final SliderSetting playerGlow = new SliderSetting("Игроки: сила свечения частиц", 1.2f, 0.0f, 5.0f, 0.1f)
            .a(() -> playerStyle.l("Частицы"));
    private final MultiModeSetting playerParticles = particleSetting("Игроки: частицы", playerStyle);
    private final BooleanSetting firstPerson = new BooleanSetting("Игроки: от первого лица", true);

    private final ModeSetting projectileStyle = new ModeSetting("Снаряды: стиль", "Линия",
            "Лента", "Частицы", "Линия");
    private final ColorSetting projectileColor = new ColorSetting("Снаряды: цвет", 0xFFFFB86C);
    private final SliderSetting projectileLength = new SliderSetting("Снаряды: длина", 2.5f, 0.25f, 8.0f, 0.25f);
    private final SliderSetting projectileOpacity = new SliderSetting("Снаряды: прозрачность", 0.9f, 0.05f, 1.0f, 0.05f);
    private final SliderSetting projectileFade = new SliderSetting("Снаряды: скорость исчезновения", 1.4f, 0.25f, 3.0f, 0.25f);
    private final SliderSetting projectileGlow = new SliderSetting("Снаряды: сила свечения", 1.5f, 0.0f, 5.0f, 0.1f)
            .a(() -> !projectileStyle.l("Лента"));
    private final MultiModeSetting projectileParticles = particleSetting("Снаряды: частицы", projectileStyle);

    private final Map<Integer, Trail> trails = new HashMap<>();
    private final List<TrailParticle> trailParticles = new ArrayList<>();
    private Object lastWorld;

    public Trails() {
        a(targets,
                playerStyle, playerColor, playerLength, playerOpacity, playerFade,
                playerGlow, playerParticles, firstPerson,
                projectileStyle, projectileColor, projectileLength, projectileOpacity,
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

    @Override public void b() { trails.clear(); trailParticles.clear(); lastWorld = null; super.b(); }
    @Override public void c() { trails.clear(); trailParticles.clear(); lastWorld = null; super.c(); }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) {
            trails.clear(); trailParticles.clear(); lastWorld = null; return;
        }
        if (lastWorld != mc.world) {
            trails.clear(); trailParticles.clear(); lastWorld = mc.world;
        }

        normalizeLegacyStyles();
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

        Iterator<TrailParticle> particleIterator = trailParticles.iterator();
        while (particleIterator.hasNext()) {
            TrailParticle particle = particleIterator.next();
            if (now - particle.created >= particle.lifetime) particleIterator.remove();
            else particle.tick();
        }
        while (trailParticles.size() > 5000) trailParticles.remove(0);
    }

    private boolean enabled(String name) {
        BooleanSetting value = targets.a(name);
        return value != null && value.c();
    }

    private void normalizeLegacyStyles() {
        if (!playerStyle.l("Лента") && !playerStyle.l("Частицы"))
            playerStyle.a("Лента");
        if (projectileStyle.l("Светящаяся линия"))
            projectileStyle.a("Линия");
        else if (!projectileStyle.l("Лента") && !projectileStyle.l("Частицы")
                && !projectileStyle.l("Линия"))
            projectileStyle.a("Линия");
    }

    private void sample(Entity entity, Target target, long now) {
        Trail trail = trails.computeIfAbsent(entity.getId(), id -> new Trail(target));
        trail.target = target;
        trail.lastSeen = now;
        Vec3d position = entity.getPos();
        if (trail.points.isEmpty()) {
            trail.points.add(new Point(position, now, ThreadLocalRandom.current().nextInt()));
        } else {
            Point previous = trail.points.get(trail.points.size() - 1);
            if (previous.position.squaredDistanceTo(position) > 0.0025) {
                Profile profile = profile(trail);
                if (profile.style.l("Частицы"))
                    emitUniformParticles(trail, previous.position, position, profile, now);
                trail.points.add(new Point(position, now, ThreadLocalRandom.current().nextInt()));
            }
        }
        int maximum = Math.max(16, Math.round(profile(trail).length.c() * 40.0f));
        while (trail.points.size() > maximum) trail.points.remove(0);
    }

    private void emitUniformParticles(Trail trail, Vec3d start, Vec3d end, Profile profile, long now) {
        Vec3d delta = end.subtract(start);
        double distance = delta.length();
        if (distance < 0.0001) return;
        if (distance > 12.0) {
            trail.emissionCarry = 0.0;
            return;
        }
        int[] forms = enabledForms(profile.particles);
        if (forms.length == 0) return;
        boolean projectile = trail.target == Target.PROJECTILE;
        double spacing = projectile ? 0.102 : 0.10;
        double next = spacing - trail.emissionCarry;
        int emitted = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        while (next <= distance && emitted < 128) {
            double progress = next / distance;
            Vec3d center = start.add(delta.multiply(progress));
            spawnParticle(center, trail.target, profile, forms, now, random);
            next += spacing;
            emitted++;
        }
        trail.emissionCarry = (trail.emissionCarry + distance) % spacing;
    }

    private void spawnParticle(Vec3d center, Target target, Profile profile, int[] forms,
                               long now, ThreadLocalRandom random) {
        boolean projectile = target == Target.PROJECTILE;
        int form = forms[random.nextInt(forms.length)];
        int seed = random.nextInt();
        int texture = textureIndex(form, seed);
        double spread = projectile ? random.nextDouble(0.05, 0.18) : random.nextDouble(0.03, 0.09);
        double angle = random.nextDouble(Math.PI * 2.0);
        double vertical = projectile ? random.nextDouble(-spread, spread)
                : random.nextDouble(0.18, 1.36);
        Vec3d position = center.add(Math.cos(angle) * spread, vertical, Math.sin(angle) * spread);
        Vec3d velocity = new Vec3d(random.nextDouble(-0.0025, 0.0025),
                random.nextDouble(0.0005, 0.0030), random.nextDouble(-0.0025, 0.0025));
        long lifetime = Math.max(250L, Math.round(profile.lifetime() * random.nextDouble(0.72, 1.18)));
        float size = (projectile ? 0.095f : 0.14f) * random.nextFloat(0.82f, 1.18f);
        trailParticles.add(new TrailParticle(position, velocity, target, form, texture, now, lifetime, size,
                random.nextFloat(0.0f, 360.0f), random.nextFloat(0.0f, 360.0f),
                random.nextFloat(0.0f, 360.0f), randomSpin(random, 1.0f, 3.2f),
                randomSpin(random, 1.0f, 3.2f), randomSpin(random, 1.5f, 4.5f)));
    }

    private static float randomSpin(ThreadLocalRandom random, float minimum, float maximum) {
        float speed = random.nextFloat(minimum, maximum);
        return random.nextBoolean() ? speed : -speed;
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.player == null || mc.world == null
                || (trails.isEmpty() && trailParticles.isEmpty())) return;
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
        float height = projectile ? 0.20f : 1.42f;
        float bottom = projectile ? -height * 0.5f : 0.12f;
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail)) continue;
            List<Point> points = smoothTrail(trail);
            if (points.size() < 2) continue;
            for (int i = 1; i < points.size(); i++) {
                Point previous = points.get(i - 1), current = points.get(i);
                Vec3d a = previous.position.subtract(camera), b = current.position.subtract(camera);
                int ca = color(previous, now, p, 0.90f), cb = color(current, now, p, 0.90f);
                Vec3d horizontal = new Vec3d(b.x - a.x, 0.0, b.z - a.z);
                if (horizontal.lengthSquared() < 1.0e-8) continue;
                Vec3d side = new Vec3d(-horizontal.z, 0.0, horizontal.x).normalize().multiply(0.25);
                Vec3d al = a.add(side).add(0, bottom, 0), ar = a.subtract(side).add(0, bottom, 0);
                Vec3d bl = b.add(side).add(0, bottom, 0), br = b.subtract(side).add(0, bottom, 0);
                Vec3d alt = al.add(0, height, 0), art = ar.add(0, height, 0);
                Vec3d blt = bl.add(0, height, 0), brt = br.add(0, height, 0);
                coloredQuad(ribbon, matrix, al, bl, blt, alt, ca, cb, cb, ca);
                coloredQuad(ribbon, matrix, br, ar, art, brt, cb, ca, ca, cb);
                coloredQuad(ribbon, matrix, alt, blt, brt, art, ca, cb, cb, ca);
                coloredQuad(ribbon, matrix, ar, br, bl, al, ca, cb, cb, ca);
                vertices += 16;
            }
        }
        if (vertices > 0) BufferRenderer.drawWithGlobalProgram(ribbon.end());

        float radius = 0.006f; // fixed thickness 0.5
        drawRails(matrices, camera, now, projectile, p, bottom, height, radius, 1.0f);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawRails(matrices, camera, now, projectile, p, bottom, height, 0.014f, 0.18f);
        drawRails(matrices, camera, now, projectile, p, bottom, height, 0.026f, 0.07f);
        RenderSystem.defaultBlendFunc();
    }

    private void drawRails(MatrixStack matrices, Vec3d camera, long now, boolean projectile,
                           Profile profile, float bottom, float height, float radius, float strength) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferBuilder tubes = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail)) continue;
            List<Point> points = smoothTrail(trail);
            if (points.size() < 2) continue;
            for (int i = 1; i < points.size(); i++) {
                Point previous = points.get(i - 1), current = points.get(i);
                int ca = color(previous, now, profile, strength), cb = color(current, now, profile, strength);
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

    private void drawLineProfile(MatrixStack matrices, Vec3d camera, long now, boolean projectile) {
        if (!projectile) return;
        Profile p = projectileProfile();
        if (!p.style.l("Линия") || !hasTrail(true, 2)) return;
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (p.glow.c() > 0.0f) {
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            float glow = p.glow.c();
            drawPathLines(matrices, camera, now, true, p,
                    Math.max(1.0f, 3.0f + glow * 2.0f), 0.06f * glow);
            drawPathLines(matrices, camera, now, true, p,
                    Math.max(1.0f, 1.8f + glow), 0.15f * glow);
        }
        RenderSystem.defaultBlendFunc();
        drawPathLines(matrices, camera, now, true, p, 1.0f, 1.0f);
    }

    private void drawPathLines(MatrixStack matrices, Vec3d camera, long now, boolean projectile,
                               Profile profile, float width, float strength) {
        RenderSystem.lineWidth(width);
        BufferBuilder lines = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int vertices = 0;
        for (Trail trail : trails.values()) {
            if (!matches(trail, projectile) || !visible(trail)) continue;
            List<Point> points = smoothTrail(trail);
            if (points.size() < 2) continue;
            float y = projectile ? 0.0f : 0.76f;
            for (int i = 1; i < points.size(); i++) {
                Point previous = points.get(i - 1), current = points.get(i);
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
        List<ParticleView> views = collectParticles(cameraPosition, now, projectile, p);
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
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(view.rotationZ));
                appendSprite(sprites, matrices.peek().getPositionMatrix(), view.size, view.color);
                matrices.pop();
            }
            BufferRenderer.drawWithGlobalProgram(sprites.end());
        }
        drawParticleShapes(matrices, views, CUBE);
        drawParticleShapes(matrices, views, TRIANGLE);
    }

    private List<ParticleView> collectParticles(Vec3d camera, long now, boolean projectile,
                                                Profile profile) {
        List<ParticleView> result = new ArrayList<>();
        for (TrailParticle particle : trailParticles) {
            if ((particle.target == Target.PROJECTILE) != projectile) continue;
            if (particle.target == Target.LOCAL_PLAYER && !firstPerson.c()
                    && mc.options.getPerspective().isFirstPerson()) continue;
            float age = Math.max(0.0f, (float) (now - particle.created));
            float progress = Math.min(1.0f, age / particle.lifetime);
            float appear = smooth(Math.min(1.0f, age / 120.0f));
            float disappear = 1.0f - smooth(progress);
            float alpha = appear * disappear * profile.opacity.c();
            if (alpha <= 0.002f) continue;
            int color = applyAlpha(profile.color.c(), alpha);
            result.add(new ParticleView(particle.position.subtract(camera), particle.form, particle.texture,
                    color, particle.size, particle.rotationX, particle.rotationY, particle.rotationZ));
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
            rotateShape(matrices, view);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            int faceColor = applyAlpha(view.color, 0.40f);
            if (form == CUBE) cube(faces, matrix, view.size * 0.55f, faceColor);
            else triangle(faces, matrix, view.size * 0.70f, faceColor);
            matrices.pop();
        }
        BufferRenderer.drawWithGlobalProgram(faces.end());

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder edges = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR);
        for (ParticleView view : views) {
            if (view.form != form) continue;
            matrices.push(); matrices.translate(view.position.x, view.position.y, view.position.z);
            rotateShape(matrices, view);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            if (form == CUBE) cubeEdges(edges, matrix, view.size * 0.55f, view.color);
            else triangleEdges(edges, matrix, view.size * 0.70f, view.color);
            matrices.pop();
        }
        BufferRenderer.drawWithGlobalProgram(edges.end());
        RenderSystem.defaultBlendFunc();
    }

    private static void rotateShape(MatrixStack matrices, ParticleView view) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(view.rotationX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(view.rotationY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(view.rotationZ));
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

    private List<Point> smoothTrail(Trail trail) {
        List<Point> source = trail.points;
        if (source.size() < 3) return source;
        List<Point> result = new ArrayList<>((source.size() - 1) * 3 + 1);
        for (int i = 0; i < source.size() - 1; i++) {
            Point p0 = source.get(Math.max(0, i - 1));
            Point p1 = source.get(i);
            Point p2 = source.get(i + 1);
            Point p3 = source.get(Math.min(source.size() - 1, i + 2));
            for (int step = 0; step < 3; step++) {
                float t = step / 3.0f;
                Vec3d position = catmullRom(p0.position, p1.position, p2.position, p3.position, t);
                long created = Math.round(p1.created + (p2.created - p1.created) * t);
                result.add(new Point(position, created, p1.seed));
            }
        }
        result.add(source.get(source.size() - 1));
        return result;
    }

    private static Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
        double t2 = t * t, t3 = t2 * t;
        return new Vec3d(
                0.5 * ((2*p1.x) + (-p0.x+p2.x)*t + (2*p0.x-5*p1.x+4*p2.x-p3.x)*t2 + (-p0.x+3*p1.x-3*p2.x+p3.x)*t3),
                0.5 * ((2*p1.y) + (-p0.y+p2.y)*t + (2*p0.y-5*p1.y+4*p2.y-p3.y)*t2 + (-p0.y+3*p1.y-3*p2.y+p3.y)*t3),
                0.5 * ((2*p1.z) + (-p0.z+p2.z)*t + (2*p0.z-5*p1.z+4*p2.z-p3.z)*t2 + (-p0.z+3*p1.z-3*p2.z+p3.z)*t3));
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
                playerFade, playerGlow, playerParticles);
    }

    private Profile projectileProfile() {
        return new Profile(projectileStyle, projectileColor, projectileLength, projectileOpacity,
                projectileFade, projectileGlow, projectileParticles);
    }

    private static int color(Point point, long now, Profile profile, float strength) {
        float ageMillis = Math.max(0.0f, (float) (now - point.created));
        float age = Math.min(1.0f, ageMillis / profile.lifetime());
        float appear = smooth(Math.min(1.0f, ageMillis / 180.0f));
        float fade = appear * (1.0f - smooth(age)) * profile.opacity.c()
                * Math.max(0.0f, Math.min(1.0f, strength));
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

    private static void coloredQuad(BufferBuilder buffer, Matrix4f matrix,
                                    Vec3d a, Vec3d b, Vec3d c, Vec3d d,
                                    int ca, int cb, int cc, int cd) {
        vertex(buffer, matrix, a, ca); vertex(buffer, matrix, b, cb);
        vertex(buffer, matrix, c, cc); vertex(buffer, matrix, d, cd);
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

    private static void cubeEdges(BufferBuilder b, Matrix4f m, float s, int c) {
        for (byte[] edge : CUBE_EDGES)
            dashed(b, m, edge[0]*s, edge[1]*s, edge[2]*s,
                    edge[3]*s, edge[4]*s, edge[5]*s, s, c);
    }

    private static void triangleEdges(BufferBuilder b, Matrix4f m, float s, int c) {
        float h = s * 0.866f;
        float[][] points = {{0,s,0},{-h,-s,h},{h,-s,h},{h,-s,-h},{-h,-s,-h}};
        for (byte[] edge : TRIANGLE_EDGES) {
            float[] a = points[edge[0]], z = points[edge[1]];
            dashed(b, m, a[0],a[1],a[2], z[0],z[1],z[2], s, c);
        }
    }

    private static void dashed(BufferBuilder b, Matrix4f m,
                               float x1,float y1,float z1,float x2,float y2,float z2,float size,int color) {
        float dx=x2-x1, dy=y2-y1, dz=z2-z1;
        float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (length < 0.001f) return;
        for (float start=0; start<length; start+=size*0.55f) {
            float end=Math.min(start+size*0.30f,length);
            b.vertex(m,x1+dx*start/length,y1+dy*start/length,z1+dz*start/length).color(color);
            b.vertex(m,x1+dx*end/length,y1+dy*end/length,z1+dz*end/length).color(color);
        }
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
        double emissionCarry;
        Trail(Target target) { this.target = target; }
    }

    private static final class TrailParticle {
        Vec3d position;
        Vec3d velocity;
        final Target target;
        final int form, texture;
        final long created, lifetime;
        final float size;
        float rotationX, rotationY, rotationZ;
        final float spinX, spinY, spinZ;

        TrailParticle(Vec3d position, Vec3d velocity, Target target, int form, int texture,
                      long created, long lifetime, float size,
                      float rotationX, float rotationY, float rotationZ,
                      float spinX, float spinY, float spinZ) {
            this.position = position;
            this.velocity = velocity;
            this.target = target;
            this.form = form;
            this.texture = texture;
            this.created = created;
            this.lifetime = lifetime;
            this.size = size;
            this.rotationX = rotationX;
            this.rotationY = rotationY;
            this.rotationZ = rotationZ;
            this.spinX = spinX;
            this.spinY = spinY;
            this.spinZ = spinZ;
        }

        void tick() {
            position = position.add(velocity);
            velocity = velocity.multiply(0.985).add(0.0, 0.00008, 0.0);
            rotationX += spinX;
            rotationY += spinY;
            rotationZ += spinZ;
        }
    }

    private record Point(Vec3d position, long created, int seed) { }
    private record ParticleView(Vec3d position, int form, int texture, int color, float size,
                                float rotationX, float rotationY, float rotationZ) { }
    private record Profile(ModeSetting style, ColorSetting color, SliderSetting length,
                           SliderSetting opacity, SliderSetting fade,
                           SliderSetting glow, MultiModeSetting particles) {
        long lifetime() {
            return Math.max(150L, Math.round(length.c() * 1000.0f / fade.c()));
        }
    }
}
