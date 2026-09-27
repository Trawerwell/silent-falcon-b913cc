package platform.inject.mixin;

import aethereal.ui.screen.PrimordialMenuStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SliderWidget.class)
public abstract class SliderWidgetMixin {
    @Shadow protected double value;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void primordial$renderSlider(DrawContext context, int mouseX, int mouseY,
                                         float delta, CallbackInfo ci) {
        if (!PrimordialMenuStyle.isReady()) {
            return;
        }
        PrimordialMenuStyle.drawSlider(context, (ClickableWidget) (Object) this, value, delta);
        ci.cancel();
    }
}
