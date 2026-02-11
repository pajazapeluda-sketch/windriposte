package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteFlag;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFlagMixin implements WindRiposteFlag {

    @Unique private long windriposte$shieldDisabledTick = -1;

    @Override
    public void windriposte$markShieldDisabled(long tick) {
        windriposte$shieldDisabledTick = tick;
    }

    @Override
    public long windriposte$getShieldDisabledTick() {
        return windriposte$shieldDisabledTick;
    }

    @Override
    public void windriposte$clearShieldDisabled() {
        windriposte$shieldDisabledTick = -1;
    }
}
