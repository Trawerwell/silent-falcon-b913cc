package platform.inject.mixin;


import aethereal.core.Primordial;
import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.TooltipEvent;
import aethereal.module.render.SpatialGUI;
import aethereal.ui.screen.PrimordialMenuStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.ReconfiguringScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin({Screen.class})
public class ScreenMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void primordial$renderMenuBackground(DrawContext context, int mouseX, int mouseY,
                                                  float delta, CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null || !PrimordialMenuStyle.isReady()) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        PrimordialMenuStyle.drawBackground(context, screen.width, screen.height, mouseX, mouseY);
        ci.cancel();
    }

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void primordial$renderPanoramaBackground(DrawContext context, float delta,
                                                     CallbackInfo ci) {
        if (MinecraftClient.getInstance().world != null || !PrimordialMenuStyle.isReady()) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        PrimordialMenuStyle.drawBackground(context, screen.width, screen.height, 0, 0);
        ci.cancel();
    }

    @Inject(method = "close", at = @At("HEAD"), cancellable = true)
    private void primordial$spatialClose(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (!(self instanceof HandledScreen<?>)) return;
        SpatialGUI spatialGUI = Primordial.getInstance().getModuleProcessor().t().getSpatialGUI();
        if (spatialGUI.requestClose(self)) ci.cancel();
    }

    @Inject(method = {"getTooltipFromItem"}, at = {@At("RETURN")})
    private static void getTooltipFromItem(MinecraftClient client, ItemStack stack, CallbackInfoReturnable<List<Text>> cir) {
        EventManager.a(new TooltipEvent(stack, cir.getReturnValue()));
    }

    @Inject(method = {"render"}, at = {@At("HEAD")}, cancellable = true)
    private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        if (((self instanceof ReconfiguringScreen) || (self instanceof DownloadingTerrainScreen)) && Primordial.getInstance().getModuleProcessor().t().aN().m()) {
            if (self instanceof DownloadingTerrainScreen) {
                Interface.mc.setScreen(null);
            }
            ci.cancel();
        }
    }
}
