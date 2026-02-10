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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {

    @Unique
    private int windriposte$preCooldown = 0;

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void windriposte$head(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;
        // "itemCooldownManager" exists on PlayerEntity only, so we can't use it here.
        // But LivingEntity has "getBlockingItem()" and "isBlocking()".
        // We store a simple flag: were we blocking at the start?
        // We'll re-check at RETURN whether the shield got disabled by cooldown.
        // (For non-players, this will just do nothing.)
        windriposte$preCooldown = self.isBlocking() ? 1 : 0;
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("RETURN")
    )
    private void windriposte$return(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        // If damage didn't apply, stop
        if (!cir.getReturnValue()) return;

        // Must have been blocking at start, and now not blocking (shield got broken/disabled)
        if (windriposte$preCooldown != 1) return;
        if (self.isBlocking()) return;

        @Nullable Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        ItemStack blocking = self.getBlockingItem();
        if (blocking == null || blocking.isEmpty()) return;

        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");
        RegistryEntry<Enchantment> entry = enchReg.getEntry(id).orElse(null);
        if (entry == null) return;

        int level = EnchantmentHelper.getLevel(entry, blocking);
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
