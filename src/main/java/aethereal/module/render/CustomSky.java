package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import aethereal.setting.ColorSetting;

@ModuleRegister(name = "Custom Sky", description = "Анимированное небо с несколькими шейдерными стилями", category = Category.Render)
public class CustomSky extends Module {
    private final ModeSetting style = new ModeSetting("Стиль неба", "Туманность",
            "Туманность", "Закат", "Сияние", "Каустика", "Плазма");
    private final ModeSetting nebulaCount = new ModeSetting("Цветов туманности", "3", "1", "2", "3")
            .a(() -> style.l("Туманность"));
    private final ColorSetting nebulaFirst = new ColorSetting("Туманность: основной цвет", 0xFF3A225A)
            .a(() -> style.l("Туманность"));
    private final ColorSetting nebulaSecond = new ColorSetting("Туманность: второй цвет", 0xFF287A9E)
            .a(() -> style.l("Туманность") && !nebulaCount.l("1"));
    private final ColorSetting nebulaThird = new ColorSetting("Туманность: третий цвет", 0xFF9D4D85)
            .a(() -> style.l("Туманность") && nebulaCount.l("3"));
    private final ColorSetting causticColor = new ColorSetting("Цвет каустики", 0xFF8B8DFF)
            .a(() -> style.l("Каустика"));
    private final SliderSetting speed = new SliderSetting("Скорость анимации", 1.0f, 0.0f, 3.0f, 0.1f);
    private final SliderSetting brightness = new SliderSetting("Яркость", 1.0f, 0.3f, 2.0f, 0.1f);
    private final SliderSetting glowStrength = new SliderSetting("Сила сияния", 1.0f, 0.0f, 2.5f, 0.1f)
            .a(() -> style.l("Сияние"));
    private final SliderSetting scale = new SliderSetting("Размер узора", 5.0f, 1.0f, 20.0f, 0.5f)
            .a(this::isSourceStyle);
    private final SliderSetting intensity = new SliderSetting("Интенсивность узора", 0.01f, 0.001f, 0.05f, 0.001f)
            .a(() -> style.l("Каустика"));
    private final SliderSetting opacity = new SliderSetting("Прозрачность узора", 1.0f, 0.3f, 1.0f, 0.05f)
            .a(this::isSourceStyle);

    public CustomSky() {
        a(style, nebulaCount, nebulaFirst, nebulaSecond, nebulaThird, causticColor, speed, brightness,
                glowStrength, scale, intensity, opacity);
    }

    public int getStyleIndex() {
        if (style.l("Туманность")) return 1;
        if (style.l("Закат")) return 2;
        if (style.l("Сияние")) return 4;
        if (style.l("Каустика")) return 5;
        if (style.l("Плазма")) return 6;
        return 1;
    }

    private boolean isSourceStyle() { return getStyleIndex() >= 5; }
    public float getScale() { return scale.c(); }
    public float getIntensity() { return intensity.c(); }
    public float getOpacity() { return opacity.c(); }
    public int getCausticColor() { return causticColor.c(); }
    public int[] getNebulaColors() {
        int first = nebulaFirst.c();
        int second = nebulaCount.l("1") ? first : nebulaSecond.c();
        int third = nebulaCount.l("3") ? nebulaThird.c() : first;
        return new int[]{first, second, third};
    }

    public float getSpeed() {
        return speed.c();
    }

    public float getBrightness() {
        return brightness.c();
    }

    public float getGlowStrength() {
        return glowStrength.c();
    }
}
