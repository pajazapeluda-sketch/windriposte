package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    void windriposte$setLastAttacker(LivingEntity attacker);
    LivingEntity windriposte$getLastAttacker();
    void windriposte$clearLastAttacker();
}
