package platform.inject.mixin;


import aethereal.core.EventManager;
import aethereal.event.ResizeEvent;
import aethereal.util.WindowTitleBar;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.objectweb.asm.Opcodes;

@Mixin({Window.class})
public class WindowMixin {
    @Inject(method = "<init>", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/util/Window;handle:J",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void primordial$darkTitleBarOnCreate(CallbackInfo ci) {
        WindowTitleBar.apply(((Window) (Object) this).getHandle());
    }

    @Inject(method = {"setIcon"}, at = {@At("TAIL")})
    private void onSetIcon(CallbackInfo ci) {
        EventManager.a(new ResizeEvent());
        WindowTitleBar.apply(((Window) (Object) this).getHandle());
    }

    @Inject(method = {"onFramebufferSizeChanged"}, at = {@At("TAIL")})
    private void onFramebufferSizeChanged(long window, int width, int height, CallbackInfo ci) {
        EventManager.a(new ResizeEvent());
    }
}
