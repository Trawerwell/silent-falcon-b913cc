package platform.inject.mixin;


import aethereal.core.EventManager;
import aethereal.core.Interface;
import aethereal.event.BoundingBoxEvent;
import aethereal.event.RemovalsEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Entity.class})
public abstract class EntityMixin {

    @Shadow
    private Box boundingBox;

    @Inject(method = {"getBoundingBox"}, at = {@At("HEAD")}, cancellable = true)
    public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        BoundingBoxEvent event = new BoundingBoxEvent(this.boundingBox, (Entity) (Object) this);
        EventManager.a(event);
        cir.setReturnValue(event.getBox());
    }

    @Inject(method = {"isGlowing"}, at = {@At("RETURN")}, cancellable = true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue().booleanValue()) {
            RemovalsEvent event = new RemovalsEvent(RemovalsEvent.type.GLOW);
            EventManager.a(event);
            if (event.a()) {
                cir.setReturnValue(false);
            }
        }
    }

    @ModifyExpressionValue(method = {"move"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z")})
    private boolean move(boolean original) {
        if ((Object) this == Interface.mc.player) {
            return false;
        }
        return original;
    }
}
