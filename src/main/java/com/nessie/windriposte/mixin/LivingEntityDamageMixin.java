package com.nessie.windriposte.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void windriposte$head(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        // Only players have a real "shield disable" behavior we care about.
        if (!(self instanceof PlayerEntity)) {
            windriposte$wasBlocking = false;
            return;
        }
        windriposte$wasBlocking = self.isBlocking();
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void windriposte$return(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerEntity)) return;

        // If damage didn't apply, nothing happened.
        if (!cir.getReturnValue()) return;

        // If we weren't blocking before, not a shield situation.
        if (!windriposte$wasBlocking) return;

        // Still blocking after damage -> shield did NOT get disabled.
        if (self.isBlocking()) return;

        @Nullable Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        // Must actually have had a shield up
        ItemStack blockingItem = self.getBlockingItem();
        if (blockingItem == null || blockingItem.isEmpty()) return;

        // TEST MODE: always push back (no enchantment yet)
        Vec3d defenderPos = new Vec3d(self.getX(), self.getY(), self.getZ());
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d dir = attackerPos.subtract(defenderPos);

        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0E-6) return;

        dir = dir.normalize();

        double strength = 0.85; // like level 1
        double lift = 0.08;

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;
    }
}
