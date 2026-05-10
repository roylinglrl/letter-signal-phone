package net.royling.lsp.phone.item;

import net.royling.lsp.phone.data.PhoneData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class PhoneCardItem extends Item {
    public PhoneCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        String number = PhoneData.getCardNumber(stack);
        if (!number.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.phone_number", number));
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.phone_text"));
        }
    }
}
