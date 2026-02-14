package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStateMixin implements WindRiposteState {

    @Unique private LivingEntity windriposte$lastAttacker = null;
    @Unique private int windriposte$lastLevel = 0;

    @Override
    public LivingEntity windriposte$getLastAttacker() {
        return windriposte$lastAttacker;
    }

    @Override
    public void windriposte$setLastAttacker(LivingEntity attacker) {
        this.windriposte$lastAttacker = attacker;
    }

    @Override
    public int windriposte$getLastLevel() {
        return windriposte$lastLevel;
    }

    @Override
    public void windriposte$setLastLevel(int level) {
        this.windriposte$lastLevel = level;
    }

    @Override
    public void windriposte$clearLastAttacker() {
        this.windriposte$lastAttacker = null;
        this.windriposte$lastLevel = 0;
    }
}
