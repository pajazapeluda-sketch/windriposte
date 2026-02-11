package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void windriposte$onDamageReturn(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        // Only care about players for now
        if (!(self instanceof PlayerEntity player)) return;

        // Damage must apply
        if (!cir.getReturnValue()) return;

        // Only run if shield was disabled THIS tick
        WindRiposteFlag flag = (WindRiposteFlag) self;
        if (flag.windriposte$getShieldDisabledTick() != world.getTime()) return;

        // Clear so it can’t double-trigger
        flag.windriposte$clearShieldDisabled();

        @Nullable Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        // DEBUG: prove it triggered
        player.sendMessage(Text.literal("§b[WindRiposte] SHIELD DISABLED → PUSH!"), true);

        // HUGE push for testing
        Vec3d defenderPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d dir = attackerPos.subtract(defenderPos);

        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        double strength = 4.0; // absurd on purpose
        double lift = 0.8;     // absurd on purpose

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;
    }
}
