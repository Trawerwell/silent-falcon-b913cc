package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.DrawEvent;
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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@ModuleRegister(name = "Spatial GUI", description = "Интерактивный интерфейс-контейнер в пространстве мира", category = Category.Render)
public final class SpatialGUI extends Module {
    // Fixed values loaded from the supplied config/spatial-gui.json.
    // Spatial GUI intentionally exposes no per-module settings: only its on/off toggle.
    private static final boolean firstPersonInventory = false;
    private static final boolean firstPersonContainers = true;
    private static final boolean autoScaleByFov = true;
    private static final boolean mirrorThirdPerson = false;
    private static final boolean linearFiltering = true;
    private static final float screenAlpha = 255.0f;

    private static final float screenDistance = 2.5f;
    private static final float screenSideOffset = -0.6f;
    private static final float screenHeightOffset = -0.4f;
    private static final float screenYawOffset = 160.0f;
    private static final float screenPitchOffset = 0.0f;
    private static final float screenScale = 3.5f;

    private static final float firstScreenDistance = 1.5f;
    private static final float firstScreenSideOffset = 0.0f;
    private static final float firstScreenHeightOffset = 0.0f;
    private static final float firstScreenYawOffset = 180.0f;
    private static final float firstScreenPitchOffset = 0.0f;
    private static final float firstScreenScale = 1.8f;

    private static final float cameraDistance = 1.7f;
    private static final float cameraSideOffset = -1.2f;
    private static final float cameraHeightOffset = 1.5f;
    private static final float cameraTargetPitch = 10.0f;
    private static final float transitionDuration = 300.0f;
    private static final float transitionSkip = 15.0f;
    private static final float thirdYawSensitivity = 0.1f;
    private static final float thirdPitchSensitivity = 0.03f;
    private static final boolean disableThirdParallax = false;
    private static final float firstYawSensitivity = 0.4f;
    private static final float firstPitchSensitivity = 0.12f;
    private static final boolean disableFirstParallax = false;
    private static final float firstPitchClamp = 40.0f;

    private static final boolean fadeAnimation = true;
    private static final float fadeDuration = 150.0f;
    private static final boolean scaleAnimation = true;
    private static final float openDuration = 300.0f;
    private static final float closeDuration = 220.0f;
    private static final String animationEasing = "Back";
    private static final float animationStartScale = 75.0f;

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

    public boolean shouldHideFirstPersonItem(ItemStack stack) {
        // Supplied config: hideHandsInFirstPerson=false, hideShieldInFirstPerson=true.
        return isActive() && isFirstPerson() && stack != null && stack.isOf(Items.SHIELD);
    }

    private boolean isFirstPerson() {
        return inventoryScreen ? firstPersonInventory : firstPersonContainers;
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
        target.setTexFilter(linearFiltering ? 9729 : 9728);
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
        float playerPitch = MathHelper.clamp(mc.player.getPitch(tickDelta), -firstPitchClamp, firstPitchClamp);
        float yawRad = (float) Math.toRadians(playerYaw);
        float pitchRad = firstPerson ? (float) Math.toRadians(playerPitch) : 0.0f;
        float distance = firstPerson ? firstScreenDistance : screenDistance;
        float side = firstPerson ? firstScreenSideOffset : screenSideOffset;
        if (!firstPerson && mirrorThirdPerson) side = -side;
        float height = firstPerson ? firstScreenHeightOffset : screenHeightOffset;
        float yawOffset = firstPerson ? firstScreenYawOffset : screenYawOffset;
        if (!firstPerson && mirrorThirdPerson) yawOffset = -yawOffset;
        float pitchOffset = firstPerson ? firstScreenPitchOffset : screenPitchOffset;
        float configuredScale = firstPerson ? firstScreenScale : screenScale;
        float fovScale = autoScaleByFov ? (float) Math.pow(currentFov() / 70.0f, 1.2f) : 1.0f;
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
            float alpha = MathHelper.clamp(screenAlpha / 255.0f, 0.0f, 1.0f) * fadeAlpha();
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
                ? (disableFirstParallax ? 0.0f : normX * 90.0f * firstYawSensitivity)
                : (disableThirdParallax ? 0.0f : normX * 90.0f * thirdYawSensitivity);
        float pitchOffset = firstPerson
                ? (disableFirstParallax ? 0.0f : normY * 180.0f * firstPitchSensitivity)
                : (disableThirdParallax ? 0.0f : normY * 180.0f * thirdPitchSensitivity);
        float yaw = entity.getYaw(tickDelta) + yawOffset;
        float pitch = firstPerson
                ? MathHelper.clamp(entity.getPitch(tickDelta) + pitchOffset, -90.0f, 90.0f)
                : cameraTargetPitch + pitchOffset;
        float positionYaw = entity.getYaw(tickDelta);
        float yawRad = (float) Math.toRadians(positionYaw);
        Vec3d entityPos = new Vec3d(MathHelper.lerp(tickDelta, entity.prevX, entity.getX()),
                MathHelper.lerp(tickDelta, entity.prevY, entity.getY()),
                MathHelper.lerp(tickDelta, entity.prevZ, entity.getZ()));
        float distance = firstPerson ? 0.0f : cameraDistance;
        float side = firstPerson ? 0.0f : cameraSideOffset;
        if (!firstPerson && mirrorThirdPerson) side = -side;
        float height = firstPerson ? entity.getEyeHeight(entity.getPose()) : cameraHeightOffset;
        Vec3d targetPos = entityPos.add(Math.sin(yawRad) * distance + Math.cos(yawRad) * side,
                height, -Math.cos(yawRad) * distance + Math.sin(yawRad) * side);
        if (firstPerson) return new CameraTransform(targetPos, yaw, pitch);

        float duration = Math.max(1.0f, transitionDuration);
        float elapsed = System.currentTimeMillis() - cameraTransitionStart + duration * transitionSkip / 100.0f;
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
        // The supplied config uses the screen crosshair in first-person mode.
        double rayX = isFirstPerson() ? guiWidth * 0.5 : guiX;
        double rayY = isFirstPerson() ? guiHeight * 0.5 : guiY;
        double ndcX = rayX / Math.max(1.0, guiWidth) * 2.0 - 1.0;
        double ndcY = 1.0 - rayY / Math.max(1.0, guiHeight) * 2.0;
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
        if (!scaleAnimation) return 1.0f;
        float progress = closing ? closeProgress() : openProgress();
        float start = animationStartScale / 100.0f;
        return start + (1.0f - start) * ease(progress);
    }

    private float fadeAlpha() {
        if (!fadeAnimation) return closing ? closeProgress() : 1.0f;
        float open = fadeDuration <= 0.0f ? 1.0f
                : MathHelper.clamp((System.currentTimeMillis() - openTime) / fadeDuration, 0.0f, 1.0f);
        return closing ? Math.min(open, closeProgress()) : open;
    }

    private float openProgress() {
        return MathHelper.clamp((System.currentTimeMillis() - openTime) / Math.max(1.0f, openDuration), 0.0f, 1.0f);
    }

    private float closeProgress() {
        return closing ? 1.0f - MathHelper.clamp((System.currentTimeMillis() - closeTime) / Math.max(1.0f, closeDuration), 0.0f, 1.0f) : 1.0f;
    }

    private float ease(float value) {
        value = MathHelper.clamp(value, 0.0f, 1.0f);
        if (animationEasing.equals("Back")) {
            float p = value - 1.0f;
            return 1.0f + 2.70158f * p * p * p + 1.70158f * p * p;
        }
        if (animationEasing.equals("Elastic")) {
            if (value == 0.0f || value == 1.0f) return value;
            return (float) (Math.pow(2.0, -10.0 * value) * Math.sin((value * 10.0 - 0.75) * (2.0 * Math.PI / 3.0)) + 1.0);
        }
        if (animationEasing.equals("Bounce")) return bounce(value);
        if (animationEasing.equals("Exponential")) return value == 0.0f ? 0.0f : 1.0f - (float) Math.pow(2.0, -10.0 * value);
        if (animationEasing.equals("Quadratic")) return 1.0f - (1.0f - value) * (1.0f - value);
        if (animationEasing.equals("Quartic")) return 1.0f - (float) Math.pow(1.0f - value, 4.0);
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
