package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteMod;
import com.nessie.windriposte.WindRiposteState;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // In 1.21.11 this method is static, so the injector MUST be static.
    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled(
            ServerWorld world,
            LivingEntity defender,
            float amount,
            ItemStack shield,
            CallbackInfo ci
    ) {
        doPush(world, defender, shield);
    }

    private static void doPush(ServerWorld world, LivingEntity defender, ItemStack shield) {
        if (!(defender instanceof PlayerEntity player)) return;

        LivingEntity attacker = ((WindRiposteState) defender).windriposte$getLastAttacker();
        ((WindRiposteState) defender).windriposte$clearLastAttacker();
        if (attacker == null) return;
        if (attacker.isRemoved()) return;

        // --- Require the enchantment on the actual shield that got disabled ---
        int level = getWindRiposteLevel(world, shield);
        if (level <= 0) return;

        // DEBUG (you can remove later)
        player.sendMessage(Text.literal("§b[WindRiposte] SHIELD DISABLED → RIPOSTE! (lvl " + level + ")"), true);

        // Direction: push attacker away from player
        Vec3d dir = new Vec3d(attacker.getX() - player.getX(), 0.0, attacker.getZ() - player.getZ());
        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        // Strong + obvious scaling while testing
        double strength = 1.8 + (level * 1.2); // lvl1=3.0, lvl2=4.2, lvl3=5.4
        double lift = 0.30 + (level * 0.15);   // lvl1=0.45, lvl2=0.60, lvl3=0.75

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;

        // --- Windy particles ---
        world.spawnParticles(
                ParticleTypes.GUST_EMITTER_SMALL,
                attacker.getX(),
                attacker.getBodyY(0.5),
                attacker.getZ(),
                1,
                0.0, 0.0, 0.0,
                0.0
        );

        world.spawnParticles(
                ParticleTypes.CLOUD,
                attacker.getX(),
                attacker.getBodyY(0.5),
                attacker.getZ(),
                18,
                0.35, 0.15, 0.35,
                0.02
        );

        // --- Wind charge sound (looked up by id so it won't fail compilation) ---
        playWindChargeBurst(world, player);
    }

    private static int getWindRiposteLevel(ServerWorld world, ItemStack stack) {
        Identifier id = Identifier.of(WindRiposteMod.MODID, "wind_riposte");

        Registry<Enchantment> enchReg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Optional<RegistryEntry.Reference<Enchantment>> entryOpt = enchReg.getEntry(id);
        if (entryOpt.isEmpty()) return 0;

        return EnchantmentHelper.getLevel(entryOpt.get(), stack);
    }

    private static void playWindChargeBurst(ServerWorld world, PlayerEntity player) {
        // Known sound event ids include:
        // entity.wind_charge.throw
        // entity.wind_charge.wind_burst :contentReference[oaicite:2]{index=2}
        Identifier soundId = Identifier.of("minecraft", "entity.wind_charge.wind_burst");

        Registry<SoundEvent> soundReg = world.getRegistryManager().getOrThrow(RegistryKeys.SOUND_EVENT);
        SoundEvent s = soundReg.get(soundId);
        if (s == null) return;

        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                s,
                SoundCategory.PLAYERS,
                1.0f,
                1.05f
        );
    }
}
