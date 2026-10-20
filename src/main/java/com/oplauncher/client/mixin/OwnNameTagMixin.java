package com.oplauncher.client.mixin;

import com.oplauncher.client.OpClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Minecraft versions where shouldShowName takes only the entity (1.20 - 1.21.1). */
@Mixin(LivingEntityRenderer.class)
public abstract class OwnNameTagMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void op$showOwnName(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (OpClient.shouldShowOwnName(entity)) cir.setReturnValue(true);
    }
}
