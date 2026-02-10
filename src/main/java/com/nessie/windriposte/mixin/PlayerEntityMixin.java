package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Unique private LivingEntity windriposte$lastAttacker;
    @Unique private long windriposte$nextAllowedTick = 0;

    @Inject(method = "takeShieldHit", at = @At("HEAD"))
    private void windriposte$capture(LivingEntity attacker, CallbackInfo ci) {
        windriposte$lastAttacker = attacker;
    }

    @Inject(method = "disableShield", at = @At("TAIL"))
    private void windriposte$onDisable(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (self.getEntityWorld().isClient()) return;

        if (windriposte$lastAttacker == null) return;

        long now = self.getEntityWorld().getTime();
        if (now < windriposte$nextAllowedTick) return;

        ItemStack shield = ItemStack.EMPTY;
        Hand hand = null;

        if (self.getOffHandStack().getItem() instanceof ShieldItem) {
            shield = self.getOffHandStack();
            hand = Hand.OFF_HAND;
        } else if (self.getMainHandStack().getItem() instanceof ShieldItem) {
            shield = self.getMainHandStack();
            hand = Hand.MAIN_HAND;
        }

        if (shield.isEmpty()) return;

        var entryOpt = self.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(WindRiposteMod.WIND_RIPOSTE);

        if (entryOpt.isEmpty()) return;

        int level = EnchantmentHelper.getLevel(entryOpt.get(), shield);
        if (level <= 0) return;

        LivingEntity attacker = windriposte$lastAttacker;

        Vec3d dir = attacker.getPos().subtract(self.getPos());
        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0e-5) return;

        dir = dir.normalize();

        double strength = level; // L1=1x, L2=2x, L3=3x

        attacker.addVelocity(
                dir.x * 0.60 * strength,
                0.12 * strength,
                dir.z * 0.60 * strength
        );
        attacker.velocityDirty = true;

        if (self instanceof ServerPlayerEntity sp && hand != null) {
            shield.damage(level * 4, sp, hand);
        }

        // 5s shield disable (100 ticks) + 1s per level re-arm
        windriposte$nextAllowedTick = now + 100 + (20L * level);
    }
}
