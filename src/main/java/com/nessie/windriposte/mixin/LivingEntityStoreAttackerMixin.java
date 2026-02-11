package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStoreAttackerMixin implements WindRiposteState {

    @Unique private LivingEntity windriposte$lastAttacker;

    @Override
    public void windriposte$setLastAttacker(LivingEntity attacker) {
        this.windriposte$lastAttacker = attacker;
    }

    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return this.windriposte$lastAttacker;
    }

    @Override
    public void windriposte$clearLastAttacker() {
        this.windriposte$lastAttacker = null;
    }

    @Inject(
            method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z",
            at = @At("HEAD")
    )
    private void windriposte$captureAttacker(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity)(Object)this;

        @Nullable Entity attackerEntity = source.getAttacker();
        if (attackerEntity instanceof LivingEntity attacker) {
            this.windriposte$setLastAttacker(attacker);
        } else {
            this.windriposte$clearLastAttacker();
        }
    }
}
