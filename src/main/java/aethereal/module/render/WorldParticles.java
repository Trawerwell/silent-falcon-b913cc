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
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Box;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "World Particles", description = "2D частицы, кубы и треугольники", category = Category.Render)
public class WorldParticles extends Module {
    private static final String[] FORMS = {"Спарк 1", "Спарк 2", "Спарк 3", "Сияние", "Доллар",
            "Блум", "Свечение", "Снег", "Звезда", "Кубы", "Треугольники"};
    private static final Identifier[] TEXTURES = new Identifier[9];
    private static final Identifier GLOW_TEXTURE = Identifier.of("primordial", "textures/particles/glow.png");
    private static final float[] GLOW_SCALES = {10.0f, 6.0f, 3.5f};
    private static final float[] GLOW_ALPHA = {0.06f, 0.14f, 0.25f};
    private static final byte[][] CUBE_EDGES = {
            {-1,-1,-1, 1,-1,-1}, {1,-1,-1, 1,-1,1}, {1,-1,1, -1,-1,1}, {-1,-1,1, -1,-1,-1},
            {-1,1,-1, 1,1,-1}, {1,1,-1, 1,1,1}, {1,1,1, -1,1,1}, {-1,1,1, -1,1,-1},
            {-1,-1,-1, -1,1,-1}, {1,-1,-1, 1,1,-1}, {1,-1,1, 1,1,1}, {-1,-1,1, -1,1,1}
    };
    private static final byte[][] TRIANGLE_EDGES = {{0,1}, {0,2}, {0,3}, {0,4}, {1,2}, {2,3}, {3,4}, {4,1}};
    static {
        String[] names = {"spark_1", "spark_2", "spark_3", "sparkle", "dollar", "bloom", "glow", "snow", "star"};
        for (int i = 0; i < names.length; i++)
            TEXTURES[i] = Identifier.of("primordial", "textures/particles/" + names[i] + ".png");
    }

    private final MultiModeSetting forms = new MultiModeSetting("Формы частиц",
            new BooleanSetting("Спарк 1", false), new BooleanSetting("Спарк 2", false),
            new BooleanSetting("Спарк 3", false), new BooleanSetting("Сияние", false),
            new BooleanSetting("Доллар", false), new BooleanSetting("Блум", false),
            new BooleanSetting("Свечение", false), new BooleanSetting("Снег", false),
            new BooleanSetting("Звезда", false), new BooleanSetting("Кубы", true),
            new BooleanSetting("Треугольники", false));
    private final ModeSetting animation = new ModeSetting("Анимация", "Падение", "Падение", "Разлёт");
    private final ColorSetting color = new ColorSetting("Цвет частиц", 0xFFD2E6FF);
    private final SliderSetting count = new SliderSetting("Количество", 30.0f, 5.0f, 100.0f, 1.0f);
    private final SliderSetting size = new SliderSetting("Размер", 1.0f, 0.1f, 3.0f, 0.1f);
    private final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.1f, 5.0f, 0.1f);
    private final SliderSetting glow = new SliderSetting("Свечение", 1.5f, 0.0f, 1.5f, 0.05f);
    private final BooleanSetting bounce = new BooleanSetting("Отскок от блоков", false)
            .a(() -> animation.l("Разлёт"));
    private final List<Particle> particles = new ArrayList<>();
    private Object lastWorld;

    public WorldParticles() {
        a(forms, animation, color, count, size, speed, glow, bounce);
    }

    @Override public void b() { particles.clear(); lastWorld = null; super.b(); }
    @Override public void c() { particles.clear(); lastWorld = null; super.c(); }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) { particles.clear(); lastWorld = null; return; }
        if (lastWorld != mc.world) { particles.clear(); lastWorld = mc.world; }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int[] enabledForms = enabledForms();
        if (enabledForms.length == 0) { particles.clear(); return; }
        int target = Math.round(count.c());
        boolean falling = animation.l("Падение");
        float motion = 0.25f * speed.c();
        Vec3d player = mc.player.getPos();
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            if (!forms.a(p.form).c()) p.form = enabledForms[random.nextInt(enabledForms.length)];
            if (!hasRoom(p.x, p.y, p.z)) { iterator.remove(); continue; }
            p.px = p.x; p.py = p.y; p.pz = p.z;
            if (falling) {
                p.phase += 0.06f * motion;
                p.x += p.vx * motion + Math.sin(p.phase + p.offset) * 0.0024f * motion;
                p.y += p.vy * motion;
                p.z += p.vz * motion + Math.cos(p.phase * 0.8f + p.offset) * 0.0020f * motion;
                p.vy = Math.max(p.vy - 0.00008f * motion, -0.032f);
            } else {
                p.x += p.vx * motion; p.y += p.vy * motion; p.z += p.vz * motion;
                p.vx *= 0.995f; p.vy *= 0.995f; p.vz *= 0.995f;
            }
            boolean bouncing = !falling && bounce.c();
            if (!resolveBlockApproach(p, bouncing, motion)) {
                iterator.remove();
                continue;
            }
            p.rx += p.sx * motion; p.ry += p.sy * motion; p.rz += p.sz * motion;
            p.life--;
            double dx = p.x - player.x, dy = p.y - player.y, dz = p.z - player.z;
            if (p.life <= 0 || dx * dx + dy * dy + dz * dz > 900.0 || (falling && p.y < player.y - 2.5))
                iterator.remove();
        }
        while (particles.size() > target) particles.remove(particles.size() - 1);
        for (int attempt = 0; particles.size() < target && attempt < Math.max(16, target * 2); attempt++) {
            Particle spawned = spawn(random, falling, enabledForms);
            if (spawned != null) particles.add(spawned);
        }
    }

    private int[] enabledForms() {
        int count = 0;
        for (String name : FORMS) if (forms.a(name).c()) count++;
        int[] enabled = new int[count];
        int index = 0;
        for (int i = 0; i < FORMS.length; i++) if (forms.a(FORMS[i]).c()) enabled[index++] = i;
        return enabled;
    }

    private Particle spawn(ThreadLocalRandom random, boolean falling, int[] enabledForms) {
        Particle p = new Particle();
        p.form = enabledForms[random.nextInt(enabledForms.length)];
        boolean found = false;
        for (int attempt = 0; attempt < 12; attempt++) {
            p.x = mc.player.getX() + random.nextDouble(-12.0, 12.0);
            p.y = mc.player.getY() + (falling ? random.nextDouble(4.0, 10.6) : random.nextDouble(2.0, 11.6));
            p.z = mc.player.getZ() + random.nextDouble(-12.0, 12.0);
            if (hasRoom(p.x, p.y, p.z)) { found = true; break; }
        }
        if (!found) return null;
        p.px = p.x; p.py = p.y; p.pz = p.z;
        float mult = speed.c();
        if (falling) {
            p.vx = random.nextFloat(-0.004f, 0.004f) * mult;
            p.vy = random.nextFloat(-0.024f, -0.012f) * mult;
            p.vz = random.nextFloat(-0.004f, 0.004f) * mult;
        } else {
            float angle = random.nextFloat(0.0f, 6.2831855f);
            float velocity = random.nextFloat(0.010f, 0.030f) * mult;
            p.vx = (float) Math.cos(angle) * velocity;
            p.vy = random.nextFloat(-0.005f, 0.005f) * mult;
            p.vz = (float) Math.sin(angle) * velocity;
        }
        p.rx = random.nextFloat(0.0f, 360.0f); p.ry = random.nextFloat(0.0f, 360.0f);
        p.rz = random.nextFloat(0.0f, 360.0f);
        p.sx = random.nextFloat(-0.75f, 0.75f); p.sy = random.nextFloat(-0.75f, 0.75f);
        p.sz = random.nextFloat(-0.75f, 0.75f);
        p.phase = random.nextFloat(0.0f, 6.2831855f); p.offset = random.nextFloat(0.0f, 10.0f);
        p.maxLife = p.life = falling ? random.nextInt(260, 480) : random.nextInt(420, 840);
        return p;
    }

    private boolean hasRoom(double x, double y, double z) {
        double radius = 0.18 * size.c() * Math.sqrt(3.0) + 0.08;
        Box volume = new Box(x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius);
        return !mc.world.getBlockCollisions(null, volume).iterator().hasNext();
    }

    private boolean resolveBlockApproach(Particle p, boolean bouncing, float motion) {
        double mx = p.x - p.px, my = p.y - p.py, mz = p.z - p.pz;
        double step = Math.sqrt(mx * mx + my * my + mz * mz);
        if (step < 0.000001) {
            p.previousFade = p.fade;
            p.fade = 1.0f;
            return true;
        }
        // The rendered cube rotates, so its corner can hit a block while its center ray misses.
        // Trace the center and a ring around its bounding sphere along the actual flight path.
        Vec3d direction = new Vec3d(mx / step, my / step, mz / step);
        Vec3d from = new Vec3d(p.px, p.py, p.pz);
        double radius = 0.18 * size.c() * Math.sqrt(3.0) + 0.08;
        Approach approach = nearestApproach(from, direction, radius);
        p.previousFade = p.fade;
        if (approach == null) {
            p.fade = 1.0f;
            return hasRoom(p.x, p.y, p.z);
        }
        double distance = approach.distance;
        if (bouncing) {
            p.fade = 1.0f;
            if (distance <= step + 0.10 || !hasRoom(p.x, p.y, p.z)) {
                // Keep the center on the free side, then reflect the velocity at the face.
                p.x = p.px; p.y = p.py; p.z = p.pz;
                Direction side = approach.hit.getSide();
                Direction.Axis axis = approach.hit.getSide().getAxis();
                if (axis == Direction.Axis.X) p.vx = -p.vx * 0.85f;
                else if (axis == Direction.Axis.Y) p.vy = -p.vy * 0.85f;
                else p.vz = -p.vz * 0.85f;
                // A small tangential deflection avoids a particle repeating the same line.
                ThreadLocalRandom random = ThreadLocalRandom.current();
                if (axis != Direction.Axis.X) p.vx += random.nextFloat(-0.003f, 0.003f) * motion;
                if (axis != Direction.Axis.Z) p.vz += random.nextFloat(-0.003f, 0.003f) * motion;
                double separation = Math.min(0.08, Math.max(0.025, step));
                double separatedX = p.x + side.getOffsetX() * separation;
                double separatedY = p.y + side.getOffsetY() * separation;
                double separatedZ = p.z + side.getOffsetZ() * separation;
                if (hasRoom(separatedX, separatedY, separatedZ)) {
                    p.x = separatedX; p.y = separatedY; p.z = separatedZ;
                }
            }
            return hasRoom(p.x, p.y, p.z);
        }
        // Fade over the last block of flight. Remove before the visible model meets the face.
        float remaining = (float) distance;
        p.fade = Math.max(0.0f, Math.min(1.0f, remaining));
        return distance > 0.0;
    }

    private Approach nearestApproach(Vec3d center, Vec3d direction, double radius) {
        Vec3d reference = Math.abs(direction.y) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d u = direction.crossProduct(reference).normalize();
        Vec3d v = direction.crossProduct(u).normalize();
        Approach nearest = null;
        for (int i = 0; i <= 8; i++) {
            double angle = (i - 1) * Math.PI / 4.0;
            Vec3d offset = i == 0 ? Vec3d.ZERO : u.multiply(Math.cos(angle) * radius * 0.72)
                    .add(v.multiply(Math.sin(angle) * radius * 0.72));
            Vec3d start = center.add(offset);
            Vec3d end = start.add(direction.multiply(radius + 1.35));
            BlockHitResult hit = mc.world.raycast(new RaycastContext(start, end,
                    RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            Direction side = hit.getSide();
            double facing = direction.x * side.getOffsetX()
                    + direction.y * side.getOffsetY() + direction.z * side.getOffsetZ();
            if (facing >= -0.001) continue;
            double contactDistance = start.distanceTo(hit.getPos()) - radius * (i == 0 ? 1.0 : 0.70);
            if (nearest == null || contactDistance < nearest.distance)
                nearest = new Approach(hit, contactDistance);
        }
        return nearest;
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || mc.player == null || mc.world == null || particles.isEmpty()) return;
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d cam = camera.getPos();
        MatrixStack matrices = event.h();
        float delta = Math.max(0.0f, Math.min(1.0f, event.g()));
        float particleSize = 0.18f * size.c();
        int baseColor = color.c();
        List<Visible> visible = new ArrayList<>(particles.size());
        for (Particle p : particles) {
            double x = p.px + (p.x - p.px) * delta - cam.x;
            double y = p.py + (p.y - p.py) * delta - cam.y;
            double z = p.pz + (p.z - p.pz) * delta - cam.z;
            if (x * x + y * y + z * z > 900.0) continue;
            float approachFade = p.previousFade + (p.fade - p.previousFade) * delta;
            float fade = Math.min(1.0f, (p.maxLife - p.life) / 20.0f)
                    * p.life / p.maxLife * approachFade;
            int alpha = Math.min(255, Math.round(((baseColor >>> 24) & 255) * fade));
            if (alpha > 2) visible.add(new Visible(p, x, y, z, alpha));
        }
        if (visible.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            if (glow.c() > 0.0f) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Visible v : visible) {
                    matrices.push();
                    matrices.translate(v.x, v.y, v.z);
                    matrices.multiply(camera.getRotation());
                    appendGlow(buffer, matrices.peek().getPositionMatrix(), particleSize, v.alpha, baseColor, glow.c());
                    matrices.pop();
                }
                BufferRenderer.drawWithGlobalProgram(buffer.end());
            }
            RenderSystem.defaultBlendFunc();
            for (int shape = 0; shape < 9; shape++) {
                if (!containsForm(visible, shape)) continue;
                RenderSystem.setShaderTexture(0, TEXTURES[shape]);
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                BufferBuilder sprites = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                for (Visible v : visible) {
                    if (v.p.form != shape) continue;
                    matrices.push(); matrices.translate(v.x, v.y, v.z);
                    matrices.multiply(camera.getRotation());
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(v.p.rz));
                    appendSprite(sprites, matrices.peek().getPositionMatrix(), particleSize * 1.65f,
                            (v.alpha << 24) | (baseColor & 0x00FFFFFF));
                    matrices.pop();
                }
                BufferRenderer.drawWithGlobalProgram(sprites.end());
            }
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            for (int shape = 9; shape <= 10; shape++) {
                if (!containsForm(visible, shape)) continue;
                BufferBuilder faces = Tessellator.getInstance().begin(
                        shape == 9 ? VertexFormat.DrawMode.QUADS : VertexFormat.DrawMode.TRIANGLES,
                        VertexFormats.POSITION_COLOR);
                for (Visible v : visible) {
                    if (v.p.form != shape) continue;
                    matrices.push(); matrices.translate(v.x, v.y, v.z); rotate(matrices, v.p);
                    int face = (Math.round(v.alpha * 0.40f) << 24) | (baseColor & 0x00FFFFFF);
                    if (shape == 9) cubeFaces(faces, matrices.peek().getPositionMatrix(), particleSize, face);
                    else triangleFaces(faces, matrices.peek().getPositionMatrix(), particleSize, face);
                    matrices.pop();
                }
                BufferRenderer.drawWithGlobalProgram(faces.end());
            }
            if (containsForm(visible, 9) || containsForm(visible, 10)) {
                RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
                BufferBuilder edges = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                for (Visible v : visible) {
                    if (v.p.form < 9) continue;
                    matrices.push(); matrices.translate(v.x, v.y, v.z); rotate(matrices, v.p);
                    int edgeColor = (v.alpha << 24) | (baseColor & 0x00FFFFFF);
                    if (v.p.form == 9) cubeEdges(edges, matrices.peek().getPositionMatrix(), particleSize, edgeColor);
                    else triangleEdges(edges, matrices.peek().getPositionMatrix(), particleSize, edgeColor);
                    matrices.pop();
                }
                BufferRenderer.drawWithGlobalProgram(edges.end());
            }
        } finally {
            RenderSystem.defaultBlendFunc(); RenderSystem.depthMask(true);
            RenderSystem.enableCull(); RenderSystem.disableBlend();
        }
    }

    private static boolean containsForm(List<Visible> visible, int form) {
        for (Visible v : visible) if (v.p.form == form) return true;
        return false;
    }

    private static void rotate(MatrixStack matrices, Particle p) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(p.rx));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p.ry));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(p.rz));
    }

    private static void appendSprite(BufferBuilder b, Matrix4f m, float size, int color) {
        float h = size * 0.5f;
        b.vertex(m, -h, -h, 0).texture(0, 1).color(color);
        b.vertex(m, -h, h, 0).texture(0, 0).color(color);
        b.vertex(m, h, h, 0).texture(1, 0).color(color);
        b.vertex(m, h, -h, 0).texture(1, 1).color(color);
    }

    private static void appendGlow(BufferBuilder b, Matrix4f m, float size, int alpha, int rgb, float strength) {
        for (int i = 0; i < 3; i++) {
            float h = size * GLOW_SCALES[i] * strength * 0.5f;
            int a = Math.min(255, Math.round(alpha * GLOW_ALPHA[i] * strength));
            int c = (a << 24) | (rgb & 0x00FFFFFF);
            b.vertex(m, -h, h, 0).texture(0, 1).color(c);
            b.vertex(m, h, h, 0).texture(1, 1).color(c);
            b.vertex(m, h, -h, 0).texture(1, 0).color(c);
            b.vertex(m, -h, -h, 0).texture(0, 0).color(c);
        }
    }

    private static void cubeFaces(BufferBuilder b, Matrix4f m, float s, int c) {
        quad(b,m,-s,-s,s, s,-s,s, s,s,s, -s,s,s,c);
        quad(b,m,s,-s,-s, -s,-s,-s, -s,s,-s, s,s,-s,c);
        quad(b,m,-s,s,s, s,s,s, s,s,-s, -s,s,-s,c);
        quad(b,m,-s,-s,-s, s,-s,-s, s,-s,s, -s,-s,s,c);
        quad(b,m,s,-s,s, s,-s,-s, s,s,-s, s,s,s,c);
        quad(b,m,-s,-s,-s, -s,-s,s, -s,s,s, -s,s,-s,c);
    }

    private static void triangleFaces(BufferBuilder b, Matrix4f m, float s, int c) {
        float h = s * 0.866f;
        tri(b,m,0,s,0, -h,-s,h, h,-s,h,c);
        tri(b,m,0,s,0, h,-s,h, h,-s,-h,c);
        tri(b,m,0,s,0, h,-s,-h, -h,-s,-h,c);
        tri(b,m,0,s,0, -h,-s,-h, -h,-s,h,c);
        tri(b,m,-h,-s,h, h,-s,h, h,-s,-h,c);
        tri(b,m,-h,-s,h, h,-s,-h, -h,-s,-h,c);
    }

    private static void cubeEdges(BufferBuilder b, Matrix4f m, float s, int c) {
        for (byte[] e : CUBE_EDGES) dashed(b,m,e[0]*s,e[1]*s,e[2]*s,e[3]*s,e[4]*s,e[5]*s,s,c);
    }

    private static void triangleEdges(BufferBuilder b, Matrix4f m, float s, int c) {
        float h = s * 0.866f;
        float[][] p = {{0,s,0},{-h,-s,h},{h,-s,h},{h,-s,-h},{-h,-s,-h}};
        for (byte[] edge : TRIANGLE_EDGES) {
            float[] a = p[edge[0]], z = p[edge[1]];
            dashed(b,m,a[0],a[1],a[2],z[0],z[1],z[2],s,c);
        }
    }

    private static void dashed(BufferBuilder b, Matrix4f m, float x1,float y1,float z1,
                               float x2,float y2,float z2,float s,int c) {
        float dx=x2-x1, dy=y2-y1, dz=z2-z1;
        float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if (length < 0.001f) return;
        for (float t=0; t<length; t+=s*0.55f) {
            float end=Math.min(t+s*0.30f,length);
            b.vertex(m,x1+dx*t/length,y1+dy*t/length,z1+dz*t/length).color(c);
            b.vertex(m,x1+dx*end/length,y1+dy*end/length,z1+dz*end/length).color(c);
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

    private record Visible(Particle p, double x, double y, double z, int alpha) { }
    private record Approach(BlockHitResult hit, double distance) { }
    private static final class Particle {
        double x,y,z,px,py,pz;
        float vx,vy,vz,rx,ry,rz,sx,sy,sz,phase,offset;
        float fade = 1.0f, previousFade = 1.0f;
        int life,maxLife,form;
    }
}
