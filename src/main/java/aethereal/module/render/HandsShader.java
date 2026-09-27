package aethereal.module.render;

import aethereal.config.ThemeInfo;
import aethereal.core.*;
import aethereal.core.Module;
import aethereal.event.HandEvent;
import aethereal.render.ColorUtil;
import aethereal.setting.SliderSetting;
import aethereal.setting.ModeSetting;
import aethereal.ui.shader.NoiseShader;
import net.minecraft.client.option.Perspective;

@ModuleRegister(name = "Hands Shader", description = "Накладывает шейдер на руку от первого лица", category = Category.Render)
public class HandsShader extends Module {
    private final ModeSetting mode = new ModeSetting("Стиль рук", "Стандартный",
            "Стандартный", "Волны", "Заливка", "Блюр");
    private final SliderSetting b = new SliderSetting("Непрозрачность", 0.6f, 0.0f, 1.0f, 0.05f)
            .a(() -> !mode.l("Блюр"));
    private final SliderSetting waveSpeed = new SliderSetting("Скорость волн", 1.2f, 0.1f, 5.0f, 0.1f)
            .a(() -> mode.l("Волны"));
    private final SliderSetting waveScale = new SliderSetting("Частота волн", 1.0f, 1.0f, 3.0f, 0.1f)
            .a(() -> mode.l("Волны"));
    private final SliderSetting outline = new SliderSetting("Ширина обводки", 1.2f, 0.1f, 5.0f, 0.1f)
            .a(() -> !mode.l("Стандартный"));
    private final SliderSetting glow = new SliderSetting("Сила свечения", 1.0f, 0.0f, 5.0f, 0.1f)
            .a(() -> !mode.l("Стандартный"));
    private final SliderSetting fill = new SliderSetting("Заливка", 0.6f, 0.0f, 1.0f, 0.01f)
            .a(() -> mode.l("Волны") || mode.l("Заливка") || mode.l("Блюр"));
    private final SliderSetting blurRadius = new SliderSetting("Сила блюра", 10.0f, 0.0f, 48.0f, 0.5f)
            .a(() -> mode.l("Блюр"));

    public HandsShader() {
        a(mode, this.b, waveSpeed, waveScale, outline, glow, fill, blurRadius);
    }

    @EventTarget
    public void a(HandEvent event) {
        NoiseShader shader = Primordial.getInstance().getModuleProcessor().i().f();
        if (mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            if (event.isPreEvent()) {
                shader.e(true);
            }
            if (event.isPostEvent()) {
                float[] color = ColorUtil.a(Primordial.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor());
                color[3] = mode.l("Блюр") ? 1.0f : this.b.c().floatValue();
                int effect = mode.l("Волны") ? 1 : mode.l("Заливка") ? 3 : mode.l("Блюр") ? 4 : 0;
                shader.a(color, effect, waveSpeed.c(), waveScale.c(), outline.c(),
                        glow.c(), glow.c() * 18.0f, fill.c(), blurRadius.c());
            }
        }
    }
}
