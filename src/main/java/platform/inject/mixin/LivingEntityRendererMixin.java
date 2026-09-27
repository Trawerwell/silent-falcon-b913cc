package platform.inject.mixin;


import aethereal.core.Primordial;
import aethereal.core.Interface;
import aethereal.render.ColorUtil;
import aethereal.render.PlayerOutlineLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Shadow
    protected M model;

    @Shadow
    public abstract net.minecraft.util.Identifier getTexture(S state);

    @Unique
    private T currentEntity;

    @Unique
    private float pitch;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;", shift = At.Shift.BEFORE, ordinal = 0))
    private void primordial$renderModelOutline(S state, MatrixStack matrices, VertexConsumerProvider consumers,
                                               int light, CallbackInfo ci) {
        if (!(this.currentEntity instanceof LivingEntity living)) return;
        if (living == Interface.mc.player && Interface.mc.options.getPerspective().isFirstPerson()) return;

        var outline = Primordial.getInstance().getModuleProcessor().t().getEntityESP();
        if (!outline.m()) return;
        if (!outline.shouldRender(living)) return;

        PlayerOutlineLayer.draw(this.model, state, matrices, this.getTexture(state), light,
                outline.getOutlineColor(), outline.getOutlineWidth(), outline.getGlowRadius(),
                outline.getGlowStrength(), (float) Interface.mc.gameRenderer.getCamera().getPos().distanceTo(living.getPos()));
    }

    @Inject(method = {"updateRenderState*"}, at = {@At("HEAD")})
    private void onUpdateRenderState(T entity, S state, float f, CallbackInfo ci) {
        this.currentEntity = entity;
    }

    @Inject(method = {"updateRenderState*"}, at = {@At("TAIL")})
    private void updateRenderState(T entity, S state, float f, CallbackInfo ci) {
        if (entity == Interface.mc.player) {
            this.pitch = Primordial.getInstance().getModuleProcessor().k().getCurrentLook().a() ? MathHelper.lerp(0.5f, this.pitch, state.pitch) : state.pitch;
            state.pitch = this.pitch;
        }
    }

    @Inject(method = {"isVisible"}, at = {@At("HEAD")}, cancellable = true)
    private void onIsVisible(S state, CallbackInfoReturnable<Boolean> cir) {
        if (Primordial.getInstance().getModuleProcessor().t().T().m() && state.invisible && (this.currentEntity instanceof PlayerEntity)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyReturnValue(method = {"getMixColor"}, at = {@At("RETURN")})
    private int modifyMixColor(int original, S state) {
        if (Primordial.getInstance().getModuleProcessor().t().T().m() && state.invisible && (this.currentEntity instanceof PlayerEntity)) {
            return ColorUtil.applyAlphaToColor(original, Primordial.getInstance().getModuleProcessor().t().T().q());
        }
        return original;
    }
}
