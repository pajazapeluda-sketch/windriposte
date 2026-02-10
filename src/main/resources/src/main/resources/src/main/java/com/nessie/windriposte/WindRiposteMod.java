package com.nessie.windriposte;

import net.fabricmc.api.ModInitializer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class WindRiposteMod implements ModInitializer {

    public static final String MODID = "windriposte";

    public static final RegistryKey<Enchantment> WIND_RIPOSTE =
            RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(MODID, "wind_riposte"));

    @Override
    public void onInitialize() {
    }
}
