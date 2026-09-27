package platform.inject.mixin;

import aethereal.ui.screen.PrimordialMenuStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {
    @Redirect(method = "renderWidget", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"))
    private void primordial$renderTextFieldBackground(DrawContext context,
                                                       Function<Identifier, RenderLayer> renderLayer,
                                                       Identifier texture, int x, int y,
                                                       int width, int height) {
        if (PrimordialMenuStyle.isReady()) {
            PrimordialMenuStyle.drawTextField(context, x, y, width, height,
                    ((TextFieldWidget) (Object) this).isFocused());
        } else {
            context.drawGuiTexture(renderLayer, texture, x, y, width, height);
        }
    }
}
