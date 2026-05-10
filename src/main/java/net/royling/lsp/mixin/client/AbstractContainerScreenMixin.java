package net.royling.lsp.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.royling.lsp.mail.client.StampFoilRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "renderSlotContents", at = @At("TAIL"))
    private void lsp$renderStampFoilInSlot(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, String itemCount, CallbackInfo ci) {
        StampFoilRenderer.drawIfPresent(graphics, itemStack, slot.x, slot.y, 16);
    }

    @Inject(method = "extractFloatingItem", at = @At("TAIL"))
    private void lsp$renderStampFoilOnFloatingItem(GuiGraphicsExtractor graphics, ItemStack carried, int x, int y, String itemCount, CallbackInfo ci) {
        StampFoilRenderer.drawIfPresent(graphics, carried, x, y, 16);
    }
}
