package net.royling.lsp.mail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.royling.lsp.mail.MailItemCodec;
import net.royling.lsp.mail.PackageData;

import java.util.function.Consumer;

public class PackageItem extends Item {
    public PackageItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer && PackageData.isPacked(stack)) {
            for (MailItemCodec.PackedStack packed : MailItemCodec.decodePackedStacks(PackageData.getItems(stack))) {
                ItemStack unpacked = packed.toStack();
                if (!unpacked.isEmpty() && !player.addItem(unpacked)) {
                    player.drop(unpacked, false);
                }
            }
            stack.shrink(1);
            serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.package.unpacked"), true);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        var packedStacks = MailItemCodec.decodePackedStacks(PackageData.getItems(stack));
        tooltip.accept(Component.translatable("tooltip.letter_signal_phone.package.items", packedStacks.size()));
        for (MailItemCodec.PackedStack packed : packedStacks) {
            ItemStack packedStack = packed.toStack();
            if (!packedStack.isEmpty()) {
                tooltip.accept(Component.literal("- ")
                        .append(packedStack.getHoverName())
                        .append(Component.literal(" x" + packedStack.getCount())));
            }
        }
    }
}
