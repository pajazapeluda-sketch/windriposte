package com.nessie.windriposte.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Unique private boolean windriposte$wasBlocking = false;
    @Unique private boolean windriposte$shieldCooldownBefore = false;
    @Unique private long windriposte$lastProcTick = 0L;

    @Unique
    private static ItemStack windriposte$getHeldShield(PlayerEntity player) {
        ItemStack off = player.getOffHandStack();
        if (off.isOf(Items.SHIELD)) return off;

        ItemStack main = player.getMainHandStack();
        if (main.isOf(Items.SHIELD)) return main;

        return ItemStack.EMPTY;
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void windriposte$head(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerEntity player)) {
            windriposte$wasBlocking = false;
            windriposte$shieldCooldownBefore = false;
            return;
        }

        windriposte$wasBlocking = player.isBlocking();

        ItemStack shield = windriposte$getHeldShield(player);
        windriposte$shieldCooldownBefore =
                !shield.isEmpty() && player.getItemCooldownManager().isCoolingDown(shield);
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void windriposte$return(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerEntity player)) return;

        if (!cir.getReturnValue()) return;        // damage must apply
        if (!windriposte$wasBlocking) return;     // must have been blocking before hit

        // 1 second internal cooldown so it can't spam
        long now = world.getTime();
        if (now - windriposte$lastProcTick < 20) return;

        ItemStack shield = windriposte$getHeldShield(player);
        if (shield.isEmpty()) return;

        boolean shieldCooldownAfter = player.getItemCooldownManager().isCoolingDown(shield);

        // Shield disable = cooldown turns on this hit
        if (windriposte$shieldCooldownBefore) return;
        if (!shieldCooldownAfter) return;

        @Nullable Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        windriposte$lastProcTick = now;

        // Push attacker away from player
        Vec3d defenderPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d dir = attackerPos.subtract(defenderPos);

        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        // TEST MODE values (like level 1)
        double strength = 0.85;
        double lift = 0.08;

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;
    }
}
