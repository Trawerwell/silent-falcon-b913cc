package platform.inject.mixin;


import aethereal.core.*;
import aethereal.event.HotbarEvent;
import aethereal.module.player.OpenWalls;
import aethereal.ui.screen.GUIScreen;
import aethereal.util.WindowTitleBar;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Mixin({MinecraftClient.class})
public abstract class MinecraftClientMixin implements Interface {

    @Unique
    private static final String PRIMORDIAL_WINDOW_TITLE = "Primordial Client";

    @Unique
    private Set<String> resourcePacks;

    @Unique
    private int primordial$titleBarRefreshTicks;

    @Inject(method = {"getWindowTitle"}, at = {@At("HEAD")}, cancellable = true)
    private void getWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(PRIMORDIAL_WINDOW_TITLE);
    }

    @Inject(method = {"setScreen"}, at = {@At("HEAD")})
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen instanceof PackScreen) {
            this.resourcePacks = mc.getResourcePackManager().getEnabledProfiles().stream().map((v0) -> {
                return v0.getId();
            }).collect(Collectors.toCollection(HashSet::new));
        } else if (this.resourcePacks != null && !(mc.currentScreen instanceof PackScreen)) {
            this.resourcePacks = null;
        }
    }

    @Inject(method = {"reloadResources()Ljava/util/concurrent/CompletableFuture;"}, at = {@At("HEAD")}, cancellable = true)
    private void reloadResources(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        if (this.resourcePacks != null) {
            Set<String> current = mc.getResourcePackManager().getEnabledProfiles().stream().map((v0) -> {
                return v0.getId();
            }).collect(Collectors.toSet());
            if (this.resourcePacks.equals(current)) {
                cir.setReturnValue(CompletableFuture.completedFuture(null));
            }
            this.resourcePacks = null;
        }
    }

    @Inject(method = {"setScreen"}, at = {@At("HEAD")}, cancellable = true)
    private void setScreen(Screen screen, CallbackInfo ci) {
        // Never let a survival inventory screen flash while the active player is creative.
        // A few client-side flows may request InventoryScreen before their creative screen;
        // replace it synchronously before Minecraft can render that intermediate frame.
        if (screen instanceof InventoryScreen && mc.player != null && mc.interactionManager != null
                && mc.interactionManager.getCurrentGameMode() != null
                && mc.interactionManager.getCurrentGameMode().isCreative()
                && mc.player.networkHandler != null) {
            mc.setScreen(new CreativeInventoryScreen(mc.player,
                    mc.player.networkHandler.getEnabledFeatures(), mc.options.getOperatorItemsTab().getValue()));
            ci.cancel();
            return;
        }
        if (mc.currentScreen instanceof GUIScreen) {
            if (screen == null || (screen instanceof DownloadingTerrainScreen)) {
                for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                    if (element.getClassName().equals(Screen.class.getName()) || element.getClassName().equals(Keyboard.class.getName())) {
                        return;
                    }
                }
                ci.cancel();
            }
        }
    }

    @Inject(method = {"tick"}, at = {@At("HEAD")})
    private void onGlobalTick(CallbackInfo ci) {
        if (primordial$titleBarRefreshTicks++ == 10) {
            WindowTitleBar.refresh(mc.getWindow().getHandle());
        }
        EventManager.a(new GlobalEvent());
    }

    @Redirect(method = {"handleInputEvents"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;stopUsingItem(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    private void handleInputEvents(ClientPlayerInteractionManager manager, PlayerEntity player) {
        if (Primordial.getInstance().getModuleProcessor().v().getInteractHandler().hasTasks()) {
            return;
        }
        if (player.isUsingItem()) {
            ItemStack stack = player.getActiveItem();
            if ((stack.getItem() instanceof CrossbowItem) && stack.getItem().getMaxUseTime(stack, player) - player.getItemUseTimeLeft() <= CrossbowItem.getPullTime(stack, player)) {
                return;
            }
        }
        manager.stopUsingItem(player);
    }

    @Inject(method = {"handleInputEvents"}, at = {@At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = InterfaceC0020Opcode.cQ)}, cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void handleInputEvents(CallbackInfo ci, int i) {
        HotbarEvent event = new HotbarEvent(i);
        EventManager.a(event);
        if (event.a()) {
            ci.cancel();
        }
    }

    @Inject(method = {"doItemUse"}, at = {@At("HEAD")}, cancellable = true)
    private void doItemUse(CallbackInfo ci) {
        ItemStack stack = mc.player.getStackInHand(Hand.MAIN_HAND);
        if ((stack.getItem() instanceof PotionItem) && mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            ci.cancel();
        }
        if ((stack.getItem() instanceof CrossbowItem) && CrossbowItem.isCharged(stack) && mc.player.getItemCooldownManager().isCoolingDown(stack)) {
            ci.cancel();
        }
    }

    @Redirect(method = {"doItemUse"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;crosshairTarget:Lnet/minecraft/util/hit/HitResult;", opcode = InterfaceC0020Opcode.aK))
    private HitResult doItemUse(MinecraftClient instance) {
        OpenWalls openWalls = Primordial.getInstance().getModuleProcessor().t().a();
        if (!openWalls.m()) {
            return instance.crosshairTarget;
        }
        BlockHitResult hit = openWalls.a(Objects.requireNonNull(instance.player));
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return hit;
        }
        return instance.crosshairTarget;
    }
}
