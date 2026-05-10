package net.royling.lsp.mail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.tooltip.StampTooltipComponent;

import java.util.Optional;
import java.util.function.Consumer;

public class StampItem extends Item {
    public StampItem(Properties properties) {
        super(properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new StampTooltipComponent(StampData.guiTexture(stack), StampData.foilEffect(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (StampData.RARITY_RARE.equals(StampData.rarity(stack))) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp.rarity.rare"));
        } else if (StampData.RARITY_HIGH_RARE.equals(StampData.rarity(stack))) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp.rarity.high_rare"));
        } else if (StampData.RARITY_UNIQUE_RARE.equals(StampData.rarity(stack))) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp.rarity.unique_rare"));
        } else if (StampData.RARITY_RGB_RARE.equals(StampData.rarity(stack))) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp.rarity.rgb_rare"));
        }
        String originPlayer = StampData.packOriginPlayer(stack);
        int originUse = StampData.packOriginUse(stack);
        if (!originPlayer.isBlank() && originUse > 0 && !StampData.RARITY_COMMON.equals(StampData.rarity(stack))) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp.pack_origin", originPlayer, originUse));
        }
    }
}
