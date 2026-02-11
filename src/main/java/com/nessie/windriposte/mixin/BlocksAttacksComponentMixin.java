package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        doPush(world, defender);
    }

    private static void doPush(ServerWorld world, LivingEntity defender) {
        if (!(defender instanceof PlayerEntity player)) return;

        LivingEntity attacker = ((WindRiposteState) defender).windriposte$getLastAttacker();
        ((WindRiposteState) defender).windriposte$clearLastAttacker();
        if (attacker == null) return;
        if (attacker.isRemoved()) return;

        // DEBUG: if you see this, we are firing at the correct moment
        player.sendMessage(Text.literal("§b[WindRiposte] SHIELD DISABLED → PUSH!"), true);

        // --- WIND VFX + SFX (server-side so everyone sees/hears) ---
        double cx = player.getX();
        double cy = player.getBodyY(0.5);
        double cz = player.getZ();

        // If ParticleTypes.GUST doesn't exist in your mappings, swap it for CLOUD/POOF only.
        world.spawnParticles(ParticleTypes.GUST, cx, cy, cz, 18, 0.35, 0.20, 0.35, 0.02);
        world.spawnParticles(ParticleTypes.POOF, cx, cy, cz, 14, 0.30, 0.15, 0.30, 0.02);
        world.spawnParticles(ParticleTypes.CLOUD, cx, cy, cz, 10, 0.25, 0.10, 0.25, 0.01);

        world.playSound(null, cx, cy, cz, SoundEvents.ENTITY_WIND_CHARGE_WIND_BURST, SoundCategory.PLAYERS, 1.0f, 1.0f);
        world.playSound(null, cx, cy, cz, SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.7f, 1.2f);

        // Ridiculous push so it's obvious
        Vec3d attackerPos = new Vec3d(attacker.getX(), attacker.getY(), attacker.getZ());
        Vec3d playerPos = new Vec3d(player.getX(), player.getY(), player.getZ());

        Vec3d dir = attackerPos.subtract(playerPos);
        dir = new Vec3d(dir.x, 0, dir.z);

        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        double strength = 4.0;
        double lift = 0.8;

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;

        // Extra burst at the attacker so it looks like they got blasted
        world.spawnParticles(ParticleTypes.GUST, attacker.getX(), attacker.getBodyY(0.5), attacker.getZ(), 12, 0.25, 0.15, 0.25, 0.02);
    }
}
