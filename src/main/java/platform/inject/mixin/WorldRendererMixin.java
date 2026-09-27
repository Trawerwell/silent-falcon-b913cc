package platform.inject.mixin;


import aethereal.core.Primordial;
import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.RemovalsEvent;
import aethereal.render.CustomSkyRenderer;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class WorldRendererMixin implements Interface {
    @Inject(method = "method_62215", at = @At("HEAD"), cancellable = true)
    private void primordial$renderCustomSky(Fog fog, DimensionEffects.SkyType skyType,
                                            float tickDelta, DimensionEffects effects, CallbackInfo ci) {
        if (skyType != DimensionEffects.SkyType.NORMAL || mc.gameRenderer == null) return;
        var module = Primordial.getInstance().getModuleProcessor().t().getCustomSky();
        if (module.m() && CustomSkyRenderer.render(mc.gameRenderer.getCamera(), module)) {
            ci.cancel();
        }
    }

    @Inject(method = {"renderWeather"}, at = {@At("HEAD")}, cancellable = true)
    private void onRenderWeather(FrameGraphBuilder frameGraphBuilder, Vec3d pos, float tickDelta, Fog fog, CallbackInfo ci) {
        RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.WEATHER);
        EventManager.a(event);
        if (event.a()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = {"setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"}, at = @At("HEAD"), argsOnly = true, index = 4)
    private boolean onSetupTerrain(boolean spectator) {
        return Primordial.getInstance().getModuleProcessor().t().h().m() || spectator;
    }
}
