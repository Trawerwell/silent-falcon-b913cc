package aethereal.ui.screen;

import aethereal.core.Primordial;
import aethereal.render.AnimationUtil;
import aethereal.render.Draw2DProcessor;
import aethereal.render.EasingList;
import aethereal.render.Fonts;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.joml.Vector4f;

/** Shared visuals for vanilla menu widgets. Widget input stays with Minecraft. */
public final class PrimordialMenuStyle {
    private static final Map<ClickableWidget, AnimationUtil> HOVER_ANIMATIONS = new WeakHashMap<>();
    private static final int BUTTON_COLOR = 0x780B0B0D;
    private static final int FIELD_COLOR = 0xD916171D;
    private static final int TEXT_COLOR = 0xFFB4B4B4;
    private static final int DISABLED_TEXT_COLOR = 0xFF6B6E78;

    private PrimordialMenuStyle() {
    }

    public static boolean isReady() {
        return Primordial.getInstance() != null && Primordial.getInstance().getModuleProcessor() != null;
    }

    public static void drawBackground(DrawContext context, int width, int height, int mouseX, int mouseY) {
        MainScreen.a(context, width, height, mouseX, mouseY, 1.0f);
    }

    public static void drawButton(DrawContext context, ClickableWidget widget, float delta) {
        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();
        float radius = Math.min(8.0f, height * 0.42f);
        boolean highlighted = widget.active && widget.isHovered();
        Draw2DProcessor draw = Primordial.getInstance().getModuleProcessor().i();
        MatrixStack matrices = context.getMatrices();
        pushHoverScale(matrices, widget, delta);

        draw.a(matrices, x, y, width, height, radius, BUTTON_COLOR);
        if (highlighted) {
            draw.a(matrices, x, y, width, height, radius, 0.5f, 0x20FFFFFF);
        }
        drawLabel(context, widget, height, 0.0f);
        matrices.pop();
    }

    public static void drawSlider(DrawContext context, ClickableWidget widget, double value, float delta) {
        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();
        float radius = Math.min(8.0f, height * 0.42f);
        float progress = (float) Math.max(0.0, Math.min(1.0, value));
        boolean hovered = widget.active && widget.isHovered();
        Draw2DProcessor draw = Primordial.getInstance().getModuleProcessor().i();
        MatrixStack matrices = context.getMatrices();
        pushHoverScale(matrices, widget, delta);

        draw.a(matrices, x, y, width, height, radius, BUTTON_COLOR);
        if (hovered) {
            draw.a(matrices, x, y, width, height, radius, 0.5f, 0x18FFFFFF);
        }
        int fillColor = hovered ? 0x668FA6F0 : 0x528A9BD8;
        draw.drawRoundedProgress(matrices, x, y, width, height,
                new Vector4f(radius), fillColor, progress);

        drawLabel(context, widget, height, 0.0f);
        matrices.pop();
    }

    public static void drawTextField(DrawContext context, int x, int y, int width, int height,
                                     boolean focused) {
        Draw2DProcessor draw = Primordial.getInstance().getModuleProcessor().i();
        float radius = Math.min(6.0f, height * 0.22f);
        draw.a(context.getMatrices(), x, y, width, height, radius, FIELD_COLOR);
        draw.a(context.getMatrices(), x, y, width, height, radius, 0.5f,
                focused ? 0x778A9BD8 : 0x24FFFFFF);
    }

    private static void drawLabel(DrawContext context, ClickableWidget widget,
                                  int height, float offsetY) {
        String label = widget.getMessage().getString();
        if (label.isEmpty()) {
            return;
        }
        float size = 8.0f;
        float availableWidth = Math.max(1.0f, widget.getWidth() - 12.0f);
        float textWidth = Fonts.e.a(label, size);
        if (textWidth > availableWidth) {
            size = Math.max(5.0f, size * availableWidth / textWidth);
            textWidth = Fonts.e.a(label, size);
        }
        int color = widget.active ? TEXT_COLOR : DISABLED_TEXT_COLOR;
        if (widget.active && widget.getMessage().getStyle().getColor() != null) {
            color = 0xFF000000 | widget.getMessage().getStyle().getColor().getRgb();
        } else if (widget.active && widget.isHovered()) {
            color = 0xFFE7E8EC;
        }
        int textColor = color;
        Text styled = Text.literal(label).styled(style -> style.withColor(textColor));
        float textX = widget.getX() + (widget.getWidth() - textWidth) / 2.0f;
        float textY = widget.getY() + (height - 9.0f) / 2.0f + offsetY;
        Fonts.e.a(context.getMatrices(), styled, textX, textY, size, 0.0f, 1.0f);
    }

    private static void pushHoverScale(MatrixStack matrices, ClickableWidget widget, float delta) {
        matrices.push();
        AnimationUtil animation = HOVER_ANIMATIONS.computeIfAbsent(widget, ignored -> new AnimationUtil());
        animation.a(widget.active && widget.isHovered());
        animation.a(0.0f, 1.0f, 0.35f, EasingList.i, delta);
        float hover = Math.max(0.0f, Math.min(1.0f, animation.c() / 0.9f));
        float scale = 1.0f + 0.03f * hover;
        float centerX = widget.getX() + widget.getWidth() / 2.0f;
        float centerY = widget.getY() + widget.getHeight() / 2.0f;
        matrices.translate(centerX, centerY, 0.0f);
        matrices.scale(scale, scale, 1.0f);
        matrices.translate(-centerX, -centerY, 0.0f);
    }
}
