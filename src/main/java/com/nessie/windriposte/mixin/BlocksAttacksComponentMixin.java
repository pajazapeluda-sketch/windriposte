package com.nessie.windriposte.mixin;

import com.nessie.windriposte.WindRiposteFlag;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlocksAttacksComponent.class)
public abstract class BlocksAttacksComponentMixin {

    @Inject(
            method = "applyShieldCooldown(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/LivingEntity;FLnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void windriposte$markShieldDisabled(ServerWorld world, LivingEntity defender, float amount, ItemStack shield, CallbackInfo ci) {
        // This is THE moment vanilla disables a shield.
        ((WindRiposteFlag) defender).windriposte$markShieldDisabled(world.getTime());
    }
}
