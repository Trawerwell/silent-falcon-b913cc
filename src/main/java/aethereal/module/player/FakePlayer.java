package aethereal.module.player;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.event.TickEvent;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.util.Arm;

import java.util.UUID;
import java.util.EnumSet;
import java.util.Set;

@ModuleRegister(name = "Fake Player", description = "Создаёт локальную копию в вашей точке без брони и предметов", category = Category.Player)
public class FakePlayer extends Module {
    private FakePlayerEntity fakePlayer;
    private ClientWorld fakeWorld;

    @Override
    public void b() {
        super.b();
        syncFakePlayer();
    }

    @Override
    public void c() {
        removeFakePlayer();
        super.c();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        syncFakePlayer();
    }

    private void syncFakePlayer() {
        ClientPlayerEntity player = mc.player;
        ClientWorld world = mc.world;
        if (player == null || world == null) {
            removeFakePlayer();
            return;
        }

        if (fakePlayer == null || fakeWorld != world) {
            removeFakePlayer();
            fakeWorld = world;
            fakePlayer = new FakePlayerEntity(world, player);
            fakePlayer.copyPositionAndRotation(player);
            fakePlayer.setHeadYaw(player.getHeadYaw());
            fakePlayer.setBodyYaw(player.bodyYaw);
            fakePlayer.setOnGround(player.isOnGround());
            fakePlayer.setPose(player.getPose());
            world.addEntity(fakePlayer);
        }

    }

    private void removeFakePlayer() {
        if (fakePlayer != null && fakeWorld != null) {
            fakeWorld.removeEntity(fakePlayer.getId(), Entity.RemovalReason.DISCARDED);
        }
        fakePlayer = null;
        fakeWorld = null;
    }

    private static final class FakePlayerEntity extends OtherClientPlayerEntity {
        private final SkinTextures copiedSkin;
        private final Set<PlayerModelPart> visibleParts;
        private final Arm mainArm;

        private FakePlayerEntity(ClientWorld world, ClientPlayerEntity source) {
            super(world, new GameProfile(UUID.randomUUID(), source.getGameProfile().getName()));
            this.copiedSkin = source.getSkinTextures();
            this.visibleParts = EnumSet.noneOf(PlayerModelPart.class);
            for (PlayerModelPart part : PlayerModelPart.values()) {
                if (source.isPartVisible(part)) {
                    visibleParts.add(part);
                }
            }
            this.mainArm = source.getMainArm();
        }

        @Override
        public SkinTextures getSkinTextures() {
            return copiedSkin != null ? copiedSkin : super.getSkinTextures();
        }

        @Override
        public boolean isPartVisible(PlayerModelPart part) {
            return visibleParts != null ? visibleParts.contains(part) : super.isPartVisible(part);
        }

        @Override
        public Arm getMainArm() {
            return mainArm != null ? mainArm : super.getMainArm();
        }
    }
}
