package net.royling.lsp.mail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.royling.lsp.mail.LetterData;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.StampVariantManager;
import net.royling.lsp.mail.menu.LetterMenu;
import net.royling.lsp.mail.network.MailPayloads;

import java.util.function.Consumer;

public class LetterItem extends Item {
    public LetterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (LetterData.isSealed(stack) && player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                giveReturnedStamp(player, stack);
                LetterData.unseal(stack);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> new LetterMenu(id, inventory, serverPlayer, hand),
                    Component.translatable("screen.letter_signal_phone.letter")
            ));
            MailPayloads.sendOpenLetter(serverPlayer, stack);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(LetterData.isReadOnly(stack) ? "tooltip.letter_signal_phone.letter.sealed" : "tooltip.letter_signal_phone.letter.open"));
        String stamp = LetterData.getStamp(stack);
        if (!stamp.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.letter.stamp", stamp));
        }
        String signer = LetterData.getSigner(stack);
        if (LetterData.isSealed(stack) && !signer.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.letter.signer", signer));
        }
    }

    private static void giveReturnedStamp(Player player, ItemStack letter) {
        ItemStack stamp = returnedStamp(letter);
        if (!stamp.isEmpty() && !player.addItem(stamp)) {
            player.drop(stamp, false);
        }
    }

    private static ItemStack returnedStamp(ItemStack letter) {
        Identifier variant = returnedStampVariant(letter);
        ItemStack stamp = StampData.stackFor(StampVariantManager.INSTANCE.byId(variant));
        StampData.setRarityAndFoil(stamp, LetterData.getStampRarity(letter), LetterData.getStampFoilEffect(letter));
        return stamp;
    }

    private static Identifier returnedStampVariant(ItemStack letter) {
        String variant = LetterData.getStampVariant(letter);
        if (!variant.isEmpty()) {
            return parseVariantOrDefault(variant);
        }
        String legacyStamp = LetterData.getStamp(letter);
        if (!legacyStamp.isEmpty() && !legacyStamp.equals(StampData.DEFAULT_VARIANT.getNamespace() + ":stamp")) {
            return parseVariantOrDefault(legacyStamp);
        }
        return StampData.DEFAULT_VARIANT;
    }

    private static Identifier parseVariantOrDefault(String value) {
        try {
            return Identifier.parse(value);
        } catch (Exception ignored) {
            return StampData.DEFAULT_VARIANT;
        }
    }
}
