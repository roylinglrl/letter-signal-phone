package net.royling.lsp.mail;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.royling.lsp.registry.ModItems;

public final class MessageInBottleCrafting {
    private MessageInBottleCrafting() {
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack bottle = player.getMainHandItem();
        ItemStack letter = player.getOffhandItem();
        if (!bottle.is(Items.GLASS_BOTTLE) || !letter.is(ModItems.LETTER.get())) {
            return;
        }
        ItemStack throwableBottle = ThrowableMessageInBottleData.pack(
                new ItemStack(ModItems.THROWABLE_MESSAGE_IN_BOTTLE.get()),
                letter,
                player.getUUID().toString(),
                player.getScoreboardName()
        );
        if (!player.getAbilities().instabuild) {
            bottle.shrink(1);
            letter.shrink(1);
        }
        if (!player.addItem(throwableBottle)) {
            player.drop(throwableBottle, false);
        }
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
        event.setCanceled(true);
    }
}
