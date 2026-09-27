package platform.inject.mixin;

import aethereal.ui.screen.PrimordialMenuStyle;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void primordial$renderButton(DrawContext context, int mouseX, int mouseY,
                                         float delta, CallbackInfo ci) {
        if (!PrimordialMenuStyle.isReady()) {
            return;
        }
        PrimordialMenuStyle.drawButton(context, (ClickableWidget) (Object) this, delta);
        ci.cancel();
    }
}
