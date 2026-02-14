package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    LivingEntity windriposte$getLastAttacker();
    void windriposte$setLastAttacker(LivingEntity attacker);

    int windriposte$getLastLevel();
    void windriposte$setLastLevel(int level);

    void windriposte$clearLastAttacker();
}
