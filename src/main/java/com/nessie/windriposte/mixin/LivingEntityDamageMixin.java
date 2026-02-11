package com.nessie.windriposte.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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

    @Unique private boolean windriposte$wasUsingShield = false;
    @Unique private boolean windriposte$shieldCooldownBefore = false;

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
            windriposte$wasUsingShield = false;
            windriposte$shieldCooldownBefore = false;
            return;
        }

        // super reliable: "am I actively using a shield item right now?"
        ItemStack active = player.getActiveItem();
        windriposte$wasUsingShield = player.isUsingItem() && active.isOf(Items.SHIELD);

        // cooldown state before
        ItemStack heldShield = windriposte$getHeldShield(player);
        windriposte$shieldCooldownBefore =
                !heldShield.isEmpty() && player.getItemCooldownManager().isCoolingDown(heldShield);
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void windriposte$return(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        if (!(self instanceof PlayerEntity player)) return;

        // damage must apply
        if (!cir.getReturnValue()) return;

        // must have been using a shield
        if (!windriposte$wasUsingShield) return;

        ItemStack heldShield = windriposte$getHeldShield(player);
        if (heldShield.isEmpty()) return;

        // shield disable = cooldown flips false -> true on this hit
        boolean shieldCooldownAfter = player.getItemCooldownManager().isCoolingDown(heldShield);
        if (windriposte$shieldCooldownBefore) return;
        if (!shieldCooldownAfter) return;

        @Nullable Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        // CONFIRMATION: tell player it triggered (server-side)
        player.sendMessage(Text.literal("§b[WindRiposte] TRIGGERED"), true);

        // HUGE push for testing
        Vec3d defenderPos = new Vec3d(player.getX(), player.getY(), player.getZ());
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d dir = attackerPos.subtract(defenderPos);

        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        double strength = 3.5; // BIG
        double lift = 0.6;     // BIG

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;
    }
}
