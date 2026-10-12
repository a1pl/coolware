package com.github.cooldood.mixins.net.minecraft.client.renderer.entity.layers;

import com.github.cooldood.modules.impl.render.NoRender;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerArmorBase.class)
public abstract class LayerArmorBaseMixin {

    @Redirect(
            method = "renderLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;isItemEnchanted()Z"
            )
    )
    private boolean redirectIsItemEnchanted(ItemStack itemStack) {
        if (NoRender.noEnchantGlint()) {
            return false;
        }
        return itemStack.isItemEnchanted();
    }
}