package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

@ModuleRegister(name = "Spatial GUI", description = "Показывает инвентари и контейнеры как плавную пространственную плоскость", category = Category.Render)
public final class SpatialGUI extends Module {
    private final ModeSetting perspectiveMode = new ModeSetting("Режим перспективы", "Объёмный",
            "Объёмный", "Мягкий", "Плоский");
    private final SliderSetting scale = new SliderSetting("Масштаб", 0.94f, 0.65f, 1.20f, 0.01f);
    private final SliderSetting yaw = new SliderSetting("Горизонтальный угол", 12.0f, 0.0f, 45.0f, 1.0f)
            .a(() -> !perspectiveMode.l("Плоский"));
    private final SliderSetting pitch = new SliderSetting("Вертикальный угол", 8.0f, 0.0f, 35.0f, 1.0f)
            .a(() -> !perspectiveMode.l("Плоский"));
    private final SliderSetting horizontalOffset = new SliderSetting("Смещение по X", 0.0f, -120.0f, 120.0f, 1.0f);
    private final SliderSetting verticalOffset = new SliderSetting("Смещение по Y", 0.0f, -90.0f, 90.0f, 1.0f);
    private final SliderSetting parallax = new SliderSetting("Параллакс мыши", 12.0f, 0.0f, 40.0f, 1.0f);
    private final SliderSetting smoothness = new SliderSetting("Плавность", 0.18f, 0.03f, 0.60f, 0.01f);
    private final SliderSetting opacity = new SliderSetting("Прозрачность GUI", 1.0f, 0.25f, 1.0f, 0.05f);
    private final SliderSetting backgroundDarkness = new SliderSetting("Затемнение фона", 0.35f, 0.0f, 0.85f, 0.05f);
    private final SliderSetting openDuration = new SliderSetting("Открытие, мс", 300.0f, 50.0f, 1000.0f, 10.0f);
    private final SliderSetting closeDuration = new SliderSetting("Закрытие, мс", 220.0f, 50.0f, 1000.0f, 10.0f);
    private final SliderSetting startScale = new SliderSetting("Начальный масштаб", 0.76f, 0.40f, 1.0f, 0.01f);
    private final ModeSetting easing = new ModeSetting("Анимация", "Back",
            "Cubic", "Back", "Exponential", "Quadratic");
    private final BooleanSetting mouseParallax = new BooleanSetting("Реакция на мышь", true);

    private Screen activeScreen;
    private long openTime;
    private long closeTime;
    private boolean closing;
    private boolean bypassClose;
    private boolean pushed;
    private float currentOffsetX;
    private float currentOffsetY;
    private float transformScaleX = 1.0f;
    private float transformScaleY = 1.0f;
    private float transformOffsetX;
    private float transformOffsetY;
    private int transformWidth;
    private int transformHeight;

    public SpatialGUI() {
        a(perspectiveMode, scale, yaw, pitch, horizontalOffset, verticalOffset,
                mouseParallax, parallax, smoothness, opacity, backgroundDarkness,
                openDuration, closeDuration, startScale, easing);
    }

    @Override
    public void b() {
        reset();
        super.b();
    }

    @Override
    public void c() {
        reset();
        super.c();
    }

    public boolean begin(DrawContext context, Screen screen, int width, int height,
                         int mouseX, int mouseY, float delta) {
        if (!m() || screen == null) return false;
        if (activeScreen != screen) {
            activeScreen = screen;
            openTime = System.currentTimeMillis();
            closeTime = 0L;
            closing = false;
            currentOffsetX = 0.0f;
            currentOffsetY = 0.0f;
        }

        long now = System.currentTimeMillis();
        float animation = closing
                ? 1.0f - clamp((now - closeTime) / closeDuration.c())
                : clamp((now - openTime) / openDuration.c());
        float eased = ease(animation);

        float normalizedX = width > 0 ? mouseX / (float) width * 2.0f - 1.0f : 0.0f;
        float normalizedY = height > 0 ? mouseY / (float) height * 2.0f - 1.0f : 0.0f;
        float targetX = horizontalOffset.c() + (mouseParallax.c() ? normalizedX * parallax.c() : 0.0f);
        float targetY = verticalOffset.c() + (mouseParallax.c() ? normalizedY * parallax.c() * 0.65f : 0.0f);
        float smoothing = Math.max(0.01f, Math.min(1.0f, smoothness.c()));
        currentOffsetX += (targetX - currentOffsetX) * smoothing;
        currentOffsetY += (targetY - currentOffsetY) * smoothing;

        float baseScale = startScale.c() + (1.0f - startScale.c()) * eased;
        float angleFactor = perspectiveMode.l("Плоский") ? 0.0f : perspectiveMode.l("Мягкий") ? 0.55f : 1.0f;
        float xPerspective = (float) Math.cos(Math.toRadians(yaw.c() * angleFactor));
        float yPerspective = (float) Math.cos(Math.toRadians(pitch.c() * angleFactor));
        transformScaleX = Math.max(0.25f, scale.c() * baseScale * xPerspective);
        transformScaleY = Math.max(0.25f, scale.c() * baseScale * yPerspective);
        transformOffsetX = currentOffsetX * eased;
        transformOffsetY = currentOffsetY * eased;
        transformWidth = width;
        transformHeight = height;

        int alpha = Math.max(0, Math.min(255, Math.round(backgroundDarkness.c() * 255.0f * eased)));
        if (alpha > 0) context.fill(0, 0, width, height, alpha << 24);

        context.getMatrices().push();
        context.getMatrices().translate(width * 0.5f + transformOffsetX,
                height * 0.5f + transformOffsetY, 0.0f);
        context.getMatrices().scale(transformScaleX, transformScaleY, 1.0f);
        context.getMatrices().translate(-width * 0.5f, -height * 0.5f, 0.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f,
                Math.max(0.0f, Math.min(1.0f, opacity.c() * eased)));
        pushed = true;
        return true;
    }

    public void end(DrawContext context) {
        if (!pushed) return;
        context.getMatrices().pop();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        pushed = false;
        if (closing && System.currentTimeMillis() - closeTime >= closeDuration.c()) {
            bypassClose = true;
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(null);
            bypassClose = false;
            reset();
        }
    }

    public int mapMouseX(int mouseX) { return Math.round((float) mapMouseX((double) mouseX)); }
    public int mapMouseY(int mouseY) { return Math.round((float) mapMouseY((double) mouseY)); }

    public double mapMouseX(double mouseX) {
        if (!m() || transformWidth <= 0) return mouseX;
        float center = transformWidth * 0.5f;
        return center + (mouseX - center - transformOffsetX) / Math.max(0.001f, transformScaleX);
    }

    public double mapMouseY(double mouseY) {
        if (!m() || transformHeight <= 0) return mouseY;
        float center = transformHeight * 0.5f;
        return center + (mouseY - center - transformOffsetY) / Math.max(0.001f, transformScaleY);
    }

    public boolean requestClose(Screen screen) {
        if (!m() || bypassClose || screen == null || activeScreen != screen) return false;
        if (!closing) {
            closing = true;
            closeTime = System.currentTimeMillis();
        }
        return true;
    }

    private float ease(float value) {
        value = clamp(value);
        if (easing.l("Back")) {
            float c1 = 1.70158f, c3 = c1 + 1.0f;
            float p = value - 1.0f;
            return 1.0f + c3 * p * p * p + c1 * p * p;
        }
        if (easing.l("Exponential")) return value == 0.0f ? 0.0f : (float) Math.pow(2.0, 10.0 * value - 10.0);
        if (easing.l("Quadratic")) return 1.0f - (1.0f - value) * (1.0f - value);
        return 1.0f - (float) Math.pow(1.0f - value, 3.0);
    }

    private static float clamp(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private void reset() {
        activeScreen = null;
        openingReset();
        currentOffsetX = currentOffsetY = 0.0f;
        transformScaleX = transformScaleY = 1.0f;
        transformOffsetX = transformOffsetY = 0.0f;
        transformWidth = transformHeight = 0;
        pushed = false;
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void openingReset() {
        openTime = closeTime = 0L;
        closing = false;
        bypassClose = false;
    }
}
