package com.nessie.windriposte;

import net.minecraft.entity.LivingEntity;

public interface WindRiposteState {
    void windriposte$setLastAttacker(LivingEntity attacker);
    LivingEntity windriposte$getLastAttacker();
    void windriposte$clearLastAttacker();
}
private int windriposte$lastLevel = 0;

public void windriposte$setLastLevel(int level) {
    this.windriposte$lastLevel = level;
}

public int windriposte$getLastLevel() {
    return this.windriposte$lastLevel;
}
