package com.nessie.windriposte;

public interface WindRiposteFlag {
    void windriposte$markShieldDisabled(long tick);
    long windriposte$getShieldDisabledTick();
    void windriposte$clearShieldDisabled();
}
