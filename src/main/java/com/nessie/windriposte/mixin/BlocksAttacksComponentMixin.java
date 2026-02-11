package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteState;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private void windriposte$onShieldDisabled_instance(ServerWorld world, LivingEntity defender, float amount, ItemStack shield, CallbackInfo ci) {
        doPush(world, defender);
    }

    @Inject(method = "applyShieldCooldown", at = @At("HEAD"))
    private static void windriposte$onShieldDisabled_static(ServerWorld world, LivingEntity defender, float amount, ItemStack shield, CallbackInfo ci) {
        doPush(world, defender);
    }

    private static void doPush(ServerWorld world, LivingEntity defender) {
        if (!(defender instanceof PlayerEntity player)) return;

        LivingEntity attacker = ((WindRiposteState) defender).windriposte$getLastAttacker();
        ((WindRiposteState) defender).windriposte$clearLastAttacker();
        if (attacker == null) return;

        // DEBUG: if you see this, we are firing at the correct moment
        player.sendMessage(Text.literal("§b[WindRiposte] SHIELD DISABLED → PUSH!"), true);

        // Ridiculous push so it's obvious
        Vec3d dir = attacker.getPos().subtract(player.getPos());
        dir = new Vec3d(dir.x, 0, dir.z);
        if (dir.lengthSquared() < 1.0E-6) return;
        dir = dir.normalize();

        double strength = 4.0;
        double lift = 0.8;

        attacker.addVelocity(dir.x * strength, lift, dir.z * strength);
        attacker.velocityDirty = true;
    }
}
