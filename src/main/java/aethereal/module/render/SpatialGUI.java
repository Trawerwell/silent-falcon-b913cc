package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.DrawEvent;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@ModuleRegister(name = "Spatial GUI", description = "Интерактивный интерфейс-контейнер в пространстве мира", category = Category.Render)
public final class SpatialGUI extends Module {
    // The defaults intentionally match Spatial GUI 1.4.
    private final BooleanSetting firstPersonInventory = new BooleanSetting("От первого лица: инвентарь", false);
    private final BooleanSetting firstPersonContainers = new BooleanSetting("От первого лица: контейнеры", false);
    private final BooleanSetting autoScaleByFov = new BooleanSetting("Масштаб по FOV", true);
    private final BooleanSetting mirrorThirdPerson = new BooleanSetting("Зеркальный вид", false);
    private final BooleanSetting linearFiltering = new BooleanSetting("Линейная фильтрация", true);
    private final SliderSetting screenAlpha = new SliderSetting("Прозрачность экрана", 255.0f, 0.0f, 255.0f, 1.0f);

    private final SliderSetting screenDistance = new SliderSetting("Экран: расстояние", 2.5f, 0.5f, 6.0f, 0.1f);
    private final SliderSetting screenSideOffset = new SliderSetting("Экран: смещение вбок", -0.6f, -4.0f, 4.0f, 0.1f);
    private final SliderSetting screenHeightOffset = new SliderSetting("Экран: высота", -0.4f, -4.0f, 4.0f, 0.1f);
    private final SliderSetting screenYawOffset = new SliderSetting("Экран: поворот", 160.0f, -180.0f, 180.0f, 1.0f);
    private final SliderSetting screenPitchOffset = new SliderSetting("Экран: наклон", 0.0f, -90.0f, 90.0f, 1.0f);
    private final SliderSetting screenScale = new SliderSetting("Экран: масштаб", 3.5f, 0.4f, 8.0f, 0.1f);

    private final SliderSetting firstScreenDistance = new SliderSetting("1P экран: расстояние", 1.5f, 0.4f, 4.0f, 0.1f);
    private final SliderSetting firstScreenSideOffset = new SliderSetting("1P экран: смещение вбок", 0.0f, -3.0f, 3.0f, 0.1f);
    private final SliderSetting firstScreenHeightOffset = new SliderSetting("1P экран: высота", 0.0f, -3.0f, 3.0f, 0.1f);
    private final SliderSetting firstScreenYawOffset = new SliderSetting("1P экран: поворот", 180.0f, -180.0f, 180.0f, 1.0f);
    private final SliderSetting firstScreenPitchOffset = new SliderSetting("1P экран: наклон", 0.0f, -90.0f, 90.0f, 1.0f);
    private final SliderSetting firstScreenScale = new SliderSetting("1P экран: масштаб", 1.8f, 0.4f, 5.0f, 0.1f);

    private final SliderSetting cameraDistance = new SliderSetting("Камера: расстояние", 1.7f, 0.0f, 4.0f, 0.1f);
    private final SliderSetting cameraSideOffset = new SliderSetting("Камера: смещение вбок", -1.2f, -4.0f, 4.0f, 0.1f);
    private final SliderSetting cameraHeightOffset = new SliderSetting("Камера: высота", 1.5f, -2.0f, 5.0f, 0.1f);
    private final SliderSetting cameraTargetPitch = new SliderSetting("Камера: наклон", 10.0f, -70.0f, 70.0f, 1.0f);
    private final SliderSetting transitionDuration = new SliderSetting("Переход камеры, мс", 300.0f, 1.0f, 5000.0f, 10.0f);
    private final SliderSetting transitionSkip = new SliderSetting("Пропуск перехода, %", 15.0f, 0.0f, 100.0f, 1.0f);
    private final SliderSetting thirdYawSensitivity = new SliderSetting("3P параллакс X", 0.1f, 0.0f, 1.0f, 0.01f);
    private final SliderSetting thirdPitchSensitivity = new SliderSetting("3P параллакс Y", 0.03f, 0.0f, 0.5f, 0.01f);
    private final BooleanSetting disableThirdParallax = new BooleanSetting("Отключить 3P параллакс", false);
    private final SliderSetting firstYawSensitivity = new SliderSetting("1P параллакс X", 0.4f, 0.0f, 1.0f, 0.01f);
    private final SliderSetting firstPitchSensitivity = new SliderSetting("1P параллакс Y", 0.12f, 0.0f, 0.5f, 0.01f);
    private final BooleanSetting disableFirstParallax = new BooleanSetting("Отключить 1P параллакс", false);
    private final SliderSetting firstPitchClamp = new SliderSetting("1P предел наклона", 40.0f, 0.0f, 90.0f, 1.0f);

    private final BooleanSetting fadeAnimation = new BooleanSetting("Плавное появление", true);
    private final SliderSetting fadeDuration = new SliderSetting("Появление, мс", 150.0f, 0.0f, 1000.0f, 10.0f);
    private final BooleanSetting scaleAnimation = new BooleanSetting("Анимация масштаба", true);
    private final SliderSetting openDuration = new SliderSetting("Открытие, мс", 300.0f, 50.0f, 1000.0f, 10.0f);
    private final SliderSetting closeDuration = new SliderSetting("Закрытие, мс", 220.0f, 50.0f, 1000.0f, 10.0f);
    private final ModeSetting easing = new ModeSetting("Тип анимации", "Back", "Cubic", "Elastic", "Bounce", "Back", "Exponential", "Quadratic", "Quartic");
    private final SliderSetting animationStartScale = new SliderSetting("Начальный масштаб, %", 75.0f, 1.0f, 100.0f, 1.0f);

    private SimpleFramebuffer target;
    private Screen activeScreen;
    private long openTime;
    private long closeTime;
    private boolean closing;
    private boolean bypassClose;
    private boolean capturing;
    private boolean inventoryScreen;
    private Vec3d cameraStart;
    private float cameraStartYaw;
    private long cameraTransitionStart;
    private double rawMouseX;
    private double rawMouseY;
    private PlaneBasis planeBasis;

    public SpatialGUI() {
        a(firstPersonInventory, firstPersonContainers, autoScaleByFov, mirrorThirdPerson,
                linearFiltering, screenAlpha,
                screenDistance, screenSideOffset, screenHeightOffset, screenYawOffset, screenPitchOffset, screenScale,
                firstScreenDistance, firstScreenSideOffset, firstScreenHeightOffset, firstScreenYawOffset,
                firstScreenPitchOffset, firstScreenScale,
                cameraDistance, cameraSideOffset, cameraHeightOffset, cameraTargetPitch,
                transitionDuration, transitionSkip, thirdYawSensitivity, thirdPitchSensitivity,
                disableThirdParallax, firstYawSensitivity, firstPitchSensitivity, disableFirstParallax, firstPitchClamp,
                fadeAnimation, fadeDuration, scaleAnimation, openDuration, closeDuration, easing, animationStartScale);
    }

    @Override
    public void b() {
        resetState();
        super.b();
    }

    @Override
    public void c() {
        if (capturing && mc.getFramebuffer() != null) mc.getFramebuffer().beginWrite(true);
        resetState();
        super.c();
    }

    public boolean isActive() {
        return m() && mc.world != null && mc.player != null && mc.currentScreen instanceof HandledScreen<?>;
    }

    public boolean isThirdPersonMode() {
        return isActive() && !isFirstPerson();
    }

    private boolean isFirstPerson() {
        return inventoryScreen ? firstPersonInventory.c() : firstPersonContainers.c();
    }

    private void ensureScreen(Screen screen) {
        if (activeScreen == screen) return;
        activeScreen = screen;
        inventoryScreen = screen != null && (screen.getClass().getSimpleName().contains("InventoryScreen"));
        openTime = System.currentTimeMillis();
        closeTime = 0L;
        closing = false;
        cameraStart = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getPos() : null;
        cameraStartYaw = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null
                ? mc.gameRenderer.getCamera().getYaw() : 0.0f;
        cameraTransitionStart = System.currentTimeMillis();
        planeBasis = null;
    }

    /** Begins rendering the vanilla handled screen into a transparent off-screen texture. */
    public boolean begin(DrawContext context, Screen screen, int width, int height, int mouseX, int mouseY, float delta) {
        if (!m() || !(screen instanceof HandledScreen<?>) || mc.world == null) return false;
        ensureScreen(screen);
        int framebufferWidth = mc.getWindow().getFramebufferWidth();
        int framebufferHeight = mc.getWindow().getFramebufferHeight();
        if (framebufferWidth <= 0 || framebufferHeight <= 0) return false;
        if (target == null) target = new SimpleFramebuffer(framebufferWidth, framebufferHeight, true);
        else if (target.textureWidth != framebufferWidth || target.textureHeight != framebufferHeight)
            target.resize(framebufferWidth, framebufferHeight);
        target.setTexFilter(linearFiltering.c() ? 9729 : 9728);
        target.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        target.clear();
        target.beginWrite(true);
        capturing = true;
        return true;
    }

    /** Flushes the GUI batch while our framebuffer is bound, then restores the world framebuffer. */
    public void end(DrawContext context) {
        if (!capturing) return;
        context.draw();
        if (mc.getFramebuffer() != null) mc.getFramebuffer().beginWrite(true);
        capturing = false;
        if (closing && closeProgress() <= 0.0f) {
            bypassClose = true;
            mc.setScreen(null);
            bypassClose = false;
            resetState();
        }
    }

    public boolean isCapturing() {
        return capturing;
    }

    @EventTarget
    public void onDraw(DrawEvent event) {
        if (!event.c() || !isActive() || target == null || mc.player == null) return;
        ensureScreen(mc.currentScreen);
        float tickDelta = mc.getRenderTickCounter().getTickDelta(false);
        Vec3d player = new Vec3d(
                MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()),
                MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ()));
        boolean firstPerson = isFirstPerson();
        float playerYaw = mc.player.getYaw(tickDelta);
        float playerPitch = MathHelper.clamp(mc.player.getPitch(tickDelta), -firstPitchClamp.c(), firstPitchClamp.c());
        float yawRad = (float) Math.toRadians(playerYaw);
        float pitchRad = firstPerson ? (float) Math.toRadians(playerPitch) : 0.0f;
        float distance = firstPerson ? firstScreenDistance.c() : screenDistance.c();
        float side = firstPerson ? firstScreenSideOffset.c() : screenSideOffset.c();
        if (!firstPerson && mirrorThirdPerson.c()) side = -side;
        float height = firstPerson ? firstScreenHeightOffset.c() : screenHeightOffset.c();
        float yawOffset = firstPerson ? firstScreenYawOffset.c() : screenYawOffset.c();
        if (!firstPerson && mirrorThirdPerson.c()) yawOffset = -yawOffset;
        float pitchOffset = firstPerson ? firstScreenPitchOffset.c() : screenPitchOffset.c();
        float configuredScale = firstPerson ? firstScreenScale.c() : screenScale.c();
        float fovScale = autoScaleByFov.c() ? (float) Math.pow(currentFov() / 70.0f, 1.2f) : 1.0f;
        if (!firstPerson) fovScale = 1.0f + (fovScale - 1.0f) * 0.82f;
        float animatedScale = configuredScale * fovScale * animatedScale();

        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        Vec3d center = firstPerson
                ? player.add(lookX * distance + Math.cos(yawRad) * side,
                        mc.player.getEyeHeight(mc.player.getPose()) + lookY * distance + height,
                        lookZ * distance + Math.sin(yawRad) * side)
                : player.add(-Math.sin(yawRad) * distance + Math.cos(yawRad) * side,
                        height, Math.cos(yawRad) * distance + Math.sin(yawRad) * side);

        float rotationYaw = -playerYaw + yawOffset;
        float rotationPitch = (firstPerson ? -playerPitch : 0.0f) + pitchOffset;
        Matrix4f rotation = new Matrix4f().rotationY((float) Math.toRadians(rotationYaw))
                .rotateX((float) Math.toRadians(rotationPitch));
        Vector3f right = rotation.transformDirection(new Vector3f(1, 0, 0)).normalize();
        Vector3f up = rotation.transformDirection(new Vector3f(0, 1, 0)).normalize();
        Vector3f normal = new Vector3f(right).cross(up).normalize();
        float aspect = target.textureHeight > 0 ? target.textureWidth / (float) target.textureHeight : 1.0f;
        planeBasis = new PlaneBasis(center, right, up, normal, aspect * animatedScale * 0.5f, animatedScale * 0.5f);

        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        MatrixStack matrices = event.h();
        matrices.push();
        matrices.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotationPitch));
        matrices.scale(animatedScale, animatedScale, animatedScale);
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, target.getColorAttachment());
            float alpha = MathHelper.clamp(screenAlpha.c() / 255.0f, 0.0f, 1.0f) * fadeAlpha();
            int color = ((int) (alpha * 255.0f) << 24) | 0x00FFFFFF;
            float halfWidth = aspect * 0.5f;
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            buffer.vertex(matrix, -halfWidth, -0.5f, 0.0f).texture(0, 1).color(color);
            buffer.vertex(matrix, halfWidth, -0.5f, 0.0f).texture(1, 1).color(color);
            buffer.vertex(matrix, halfWidth, 0.5f, 0.0f).texture(1, 0).color(color);
            buffer.vertex(matrix, -halfWidth, 0.5f, 0.0f).texture(0, 0).color(color);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            matrices.pop();
        }
    }

    public CameraTransform getCameraTransform(Camera camera, Entity entity, float tickDelta) {
        if (!isActive() || entity == null) return null;
        ensureScreen(mc.currentScreen);
        boolean firstPerson = isFirstPerson();
        double mouseX = mc.mouse.getX();
        double mouseY = mc.mouse.getY();
        float normX = MathHelper.clamp((float) (mouseX / Math.max(1, mc.getWindow().getWidth()) * 2.0 - 1.0), -1.0f, 1.0f);
        float normY = MathHelper.clamp((float) (mouseY / Math.max(1, mc.getWindow().getHeight()) * 2.0 - 1.0), -1.0f, 1.0f);
        float yawOffset = firstPerson
                ? (disableFirstParallax.c() ? 0.0f : normX * 90.0f * firstYawSensitivity.c())
                : (disableThirdParallax.c() ? 0.0f : normX * 90.0f * thirdYawSensitivity.c());
        float pitchOffset = firstPerson
                ? (disableFirstParallax.c() ? 0.0f : normY * 180.0f * firstPitchSensitivity.c())
                : (disableThirdParallax.c() ? 0.0f : normY * 180.0f * thirdPitchSensitivity.c());
        float yaw = entity.getYaw(tickDelta) + yawOffset;
        float pitch = firstPerson
                ? MathHelper.clamp(entity.getPitch(tickDelta) + pitchOffset, -90.0f, 90.0f)
                : cameraTargetPitch.c() + pitchOffset;
        float positionYaw = entity.getYaw(tickDelta);
        float yawRad = (float) Math.toRadians(positionYaw);
        Vec3d entityPos = new Vec3d(MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        float distance = firstPerson ? 0.0f : cameraDistance.c();
        float side = firstPerson ? 0.0f : cameraSideOffset.c();
        if (!firstPerson && mirrorThirdPerson.c()) side = -side;
        float height = firstPerson ? entity.getEyeHeight(entity.getPose()) : cameraHeightOffset.c();
        Vec3d targetPos = entityPos.add(Math.sin(yawRad) * distance + Math.cos(yawRad) * side,
                height, -Math.cos(yawRad) * distance + Math.sin(yawRad) * side);
        if (firstPerson) return new CameraTransform(targetPos, yaw, pitch);

        float duration = Math.max(1.0f, transitionDuration.c());
        float elapsed = System.currentTimeMillis() - cameraTransitionStart + duration * transitionSkip.c() / 100.0f;
        float progress = MathHelper.clamp(elapsed / duration, 0.0f, 1.0f);
        float eased = 1.0f - (float) Math.pow(1.0f - progress, 3.0);
        Vec3d start = cameraStart == null ? camera.getPos() : cameraStart;
        Vec3d pos = new Vec3d(MathHelper.lerp(eased, start.x, targetPos.x),
                MathHelper.lerp(eased, start.y, targetPos.y), MathHelper.lerp(eased, start.z, targetPos.z));
        return new CameraTransform(pos, MathHelper.lerp(eased, cameraStartYaw, yaw), pitch);
    }

    public int mapMouseX(int mouseX) {
        rawMouseX = mouseX;
        return (int) Math.round(mapMouse(rawMouseX, rawMouseY).x);
    }

    public int mapMouseY(int mouseY) {
        rawMouseY = mouseY;
        return (int) Math.round(mapMouse(rawMouseX, rawMouseY).y);
    }

    public double mapMouseX(double mouseX) {
        rawMouseX = mouseX;
        return mapMouse(rawMouseX, rawMouseY).x;
    }

    public double mapMouseY(double mouseY) {
        rawMouseY = mouseY;
        return mapMouse(rawMouseX, rawMouseY).y;
    }

    private Vec2 mapMouse(double guiX, double guiY) {
        if (!isActive() || planeBasis == null) return new Vec2(guiX, guiY);
        Camera camera = mc.gameRenderer.getCamera();
        int guiWidth = mc.getWindow().getScaledWidth();
        int guiHeight = mc.getWindow().getScaledHeight();
        double ndcX = guiX / Math.max(1.0, guiWidth) * 2.0 - 1.0;
        double ndcY = 1.0 - guiY / Math.max(1.0, guiHeight) * 2.0;
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        Vec3d forward = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).normalize();
        Vec3d right = new Vec3d(-forward.z, 0.0, forward.x).normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        float tanHalf = (float) Math.tan(Math.toRadians(currentFov() * 0.5f));
        float aspect = mc.getWindow().getFramebufferWidth() / (float) Math.max(1, mc.getWindow().getFramebufferHeight());
        Vec3d direction = forward.add(right.multiply(ndcX * tanHalf * aspect)).add(up.multiply(ndcY * tanHalf)).normalize();
        Vec3d origin = camera.getPos();
        Vec3d normal = vec(planeBasis.normal);
        double denominator = direction.dotProduct(normal);
        if (Math.abs(denominator) < 1.0E-6) return new Vec2(guiX, guiY);
        double distance = planeBasis.center.subtract(origin).dotProduct(normal) / denominator;
        if (distance <= 0.0) return new Vec2(guiX, guiY);
        Vec3d hit = origin.add(direction.multiply(distance)).subtract(planeBasis.center);
        double localX = hit.dotProduct(vec(planeBasis.right));
        double localY = hit.dotProduct(vec(planeBasis.up));
        double u = MathHelper.clamp(localX / planeBasis.halfWidth * 0.5 + 0.5, 0.0, 1.0);
        double v = MathHelper.clamp(localY / planeBasis.halfHeight * 0.5 + 0.5, 0.0, 1.0);
        return new Vec2(u * guiWidth, (1.0 - v) * guiHeight);
    }

    public boolean requestClose(Screen screen) {
        if (!m() || bypassClose || screen == null || activeScreen != screen) return false;
        if (!closing) {
            closing = true;
            closeTime = System.currentTimeMillis();
        }
        return true;
    }

    private float animatedScale() {
        if (!scaleAnimation.c()) return 1.0f;
        float progress = closing ? closeProgress() : openProgress();
        float start = animationStartScale.c() / 100.0f;
        return start + (1.0f - start) * ease(progress);
    }

    private float fadeAlpha() {
        if (!fadeAnimation.c()) return closing ? closeProgress() : 1.0f;
        float open = fadeDuration.c() <= 0.0f ? 1.0f
                : MathHelper.clamp((System.currentTimeMillis() - openTime) / fadeDuration.c(), 0.0f, 1.0f);
        return closing ? Math.min(open, closeProgress()) : open;
    }

    private float openProgress() {
        return MathHelper.clamp((System.currentTimeMillis() - openTime) / Math.max(1.0f, openDuration.c()), 0.0f, 1.0f);
    }

    private float closeProgress() {
        return closing ? 1.0f - MathHelper.clamp((System.currentTimeMillis() - closeTime) / Math.max(1.0f, closeDuration.c()), 0.0f, 1.0f) : 1.0f;
    }

    private float ease(float value) {
        value = MathHelper.clamp(value, 0.0f, 1.0f);
        if (easing.l("Back")) {
            float p = value - 1.0f;
            return 1.0f + 2.70158f * p * p * p + 1.70158f * p * p;
        }
        if (easing.l("Elastic")) {
            if (value == 0.0f || value == 1.0f) return value;
            return (float) (Math.pow(2.0, -10.0 * value) * Math.sin((value * 10.0 - 0.75) * (2.0 * Math.PI / 3.0)) + 1.0);
        }
        if (easing.l("Bounce")) return bounce(value);
        if (easing.l("Exponential")) return value == 0.0f ? 0.0f : 1.0f - (float) Math.pow(2.0, -10.0 * value);
        if (easing.l("Quadratic")) return 1.0f - (1.0f - value) * (1.0f - value);
        if (easing.l("Quartic")) return 1.0f - (float) Math.pow(1.0f - value, 4.0);
        return 1.0f - (float) Math.pow(1.0f - value, 3.0);
    }

    private static float bounce(float x) {
        float n = 7.5625f, d = 2.75f;
        if (x < 1.0f / d) return n * x * x;
        if (x < 2.0f / d) { x -= 1.5f / d; return n * x * x + 0.75f; }
        if (x < 2.5f / d) { x -= 2.25f / d; return n * x * x + 0.9375f; }
        x -= 2.625f / d; return n * x * x + 0.984375f;
    }

    private float currentFov() {
        return ((Number) mc.options.getFov().getValue()).floatValue();
    }

    private static Vec3d vec(Vector3f value) {
        return new Vec3d(value.x, value.y, value.z);
    }

    private void resetState() {
        activeScreen = null;
        openTime = closeTime = 0L;
        closing = bypassClose = capturing = false;
        cameraStart = null;
        cameraTransitionStart = 0L;
        planeBasis = null;
        rawMouseX = rawMouseY = 0.0;
    }

    public record CameraTransform(Vec3d pos, float yaw, float pitch) { }
    private record PlaneBasis(Vec3d center, Vector3f right, Vector3f up, Vector3f normal, float halfWidth, float halfHeight) { }
    private record Vec2(double x, double y) { }
}
