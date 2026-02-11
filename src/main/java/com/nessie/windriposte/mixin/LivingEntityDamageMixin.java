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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    // store attacker at the start of damage()
    @Unique private LivingEntity windriposte$attackerAtStart;

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void windriposte$captureAttacker(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        @Nullable Entity attackerEntity = source.getAttacker();
        windriposte$attackerAtStart = (attackerEntity instanceof LivingEntity le) ? le : null;
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/component/type/BlocksAttacksComponent;applyShieldCooldown(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;FLnet/minecraft/item/ItemStack;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void windriposte$afterShieldCooldown(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (!(self instanceof PlayerEntity player)) return;

        WindRiposteFlag flag = (WindRiposteFlag) self;

        // Should have been marked THIS tick by BlocksAttacksComponentMixin
        if (flag.windriposte$getShieldDisabledTick() != world.getTime()) return;

        // Clear so it won't repeat
        flag.windriposte$clearShieldDisabled();

        LivingEntity attacker = windriposte$attackerAtStart;
        if (attacker == null) return;

        // DEBUG message: if you see this, the push logic is running
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
