package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
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
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/component/type/BlocksAttacksComponent;applyShieldCooldown(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;FLnet/minecraft/item/ItemStack;)V",
            shift = At.Shift.AFTER
        )
    )
    private void windriposte$afterShieldCooldownApplied(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        @Nullable Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        ItemStack blocking = self.getBlockingItem();
        if (blocking == null || blocking.isEmpty()) return;

        // Look up enchantment entry
        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        @Nullable RegistryEntry<Enchantment> windRiposte = enchReg.getEntry(WindRiposteMod.WIND_RIPOSTE).orElse(null);
        if (windRiposte == null) return;

        int level = EnchantmentHelper.getLevel(windRiposte, blocking);
        if (level <= 0) return;

        Vec3d defenderPos = new Vec3d(self.getX(), self.getY(), self.getZ());
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d dir = attackerPos.subtract(defenderPos);

        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0E-6) return;

        dir = dir.normalize();

        double strength = 0.85 * level;
        double lift = 0.08 * level;

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;
    }
}
