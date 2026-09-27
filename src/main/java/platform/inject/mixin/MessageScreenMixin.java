package platform.inject.mixin;

import aethereal.ui.screen.PrimordialMenuStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageScreen.class)
public abstract class MessageScreenMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void primordial$renderMessageBackground(DrawContext context, int mouseX,
                                                     int mouseY, float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null || !PrimordialMenuStyle.isReady()) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        PrimordialMenuStyle.drawBackground(context, screen.width, screen.height, mouseX, mouseY);
        ci.cancel();
    }
}
