package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.render.ColorUtil;
import aethereal.setting.ColorSetting;
import aethereal.setting.SliderSetting;
import aethereal.setting.BooleanSetting;
import aethereal.setting.MultiModeSetting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;

@ModuleRegister(name = "Entity ESP", description = "Обводит видимую модель игрока шейдером", category = Category.Render)
public class EntityESP extends Module {
    private final ColorSetting color = new ColorSetting("Цвет обводки", ColorUtil.convertToARGB(130, 165, 255, 255));
    private final SliderSetting width = new SliderSetting("Толщина обводки", 2.0f, 1.0f, 5.0f, 0.5f);
    private final SliderSetting glowRadius = new SliderSetting("Радиус подсветки", 12.0f, 0.0f, 96.0f, 1.0f);
    private final SliderSetting glowStrength = new SliderSetting("Яркость подсветки", 0.55f, 0.0f, 1.0f, 0.05f);
    private final MultiModeSetting targets = new MultiModeSetting("Цели",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Животные", false),
            new BooleanSetting("Мобы", false));

    public EntityESP() {
        a(targets, color, width, glowRadius, glowStrength);
    }

    public boolean shouldRender(LivingEntity entity) {
        if (entity instanceof PlayerEntity) return targets.a("Игроки").c();
        if (entity instanceof AnimalEntity || entity instanceof VillagerEntity || entity instanceof ShulkerEntity) {
            return targets.a("Животные").c();
        }
        if (entity instanceof HostileEntity) return targets.a("Мобы").c();
        return false;
    }

    public int getOutlineColor() {
        return color.c();
    }

    public float getOutlineWidth() {
        return width.c();
    }

    public float getGlowRadius() { return glowRadius.c(); }
    public float getGlowStrength() { return glowStrength.c(); }
}
