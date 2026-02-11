package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteFlag;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    // ✅ If applyShieldCooldown is STATIC in this version, this one will match.
    @Inject(
            method = "applyShieldCooldown",
            at = @At("HEAD")
    )
    private static void windriposte$markShieldDisabled_static(ServerWorld world, LivingEntity defender, float amount, ItemStack shield, CallbackInfo ci) {
        ((WindRiposteFlag) defender).windriposte$markShieldDisabled(world.getTime());

        if (defender instanceof PlayerEntity player) {
            player.sendMessage(Text.literal("§a[WindRiposte DEBUG] applyShieldCooldown() HIT (static)"), true);
        }
    }

    // ✅ If applyShieldCooldown is an INSTANCE method in this version, this one will match.
    @Inject(
            method = "applyShieldCooldown",
            at = @At("HEAD")
    )
    private void windriposte$markShieldDisabled_instance(ServerWorld world, LivingEntity defender, float amount, ItemStack shield, CallbackInfo ci) {
        ((WindRiposteFlag) defender).windriposte$markShieldDisabled(world.getTime());

        if (defender instanceof PlayerEntity player) {
            player.sendMessage(Text.literal("§a[WindRiposte DEBUG] applyShieldCooldown() HIT (instance)"), true);
        }
    }
}
