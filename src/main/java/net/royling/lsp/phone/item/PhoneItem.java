package net.royling.lsp.phone.item;

import net.royling.lsp.phone.call.CallManager;
import net.royling.lsp.phone.data.PhoneData;
import net.royling.lsp.phone.data.PhoneSavedData;
import net.royling.lsp.registry.ModItems;
import net.royling.lsp.phone.network.PhonePayloads;
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
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

public class PhoneItem extends Item {
    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack phone = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            ItemStack offhand = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (!PhoneData.hasInstalledCard(phone) && offhand.is(ModItems.PHONE_CARD.get()) && PhoneData.hasCardNumber(offhand)) {
                String number = PhoneData.getCardNumber(offhand);
                String token = PhoneData.getCardToken(offhand);
                if (!PhoneSavedData.get(serverPlayer.level().getServer()).matches(number, token)) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.phone.invalid_card"), true);
                    return InteractionResult.FAIL;
                }
                PhoneData.setInstalledRegistration(phone, number, token);
                offhand.shrink(1);
                serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.phone.installed", number), true);
                return InteractionResult.SUCCESS_SERVER;
            }

            if (player.isShiftKeyDown() && PhoneData.hasInstalledCard(phone)) {
                String number = PhoneData.getInstalledNumber(phone);
                String token = PhoneData.getInstalledToken(phone);
                ItemStack card = new ItemStack(ModItems.PHONE_CARD.get());
                PhoneData.setCardRegistration(card, number, token);
                if (!player.addItem(card)) {
                    player.drop(card, false);
                }
                PhoneData.clearInstalledNumber(phone);
                serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.phone.ejected", number), true);
                return InteractionResult.SUCCESS_SERVER;
            }

            String number = PhoneData.getInstalledNumber(phone);
            PacketDistributor.sendToPlayer(serverPlayer, new PhonePayloads.OpenPhoneScreenPayload(number, CallManager.statusFor(serverPlayer.getUUID()), CallManager.peerNameFor(serverPlayer)));
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        String number = PhoneData.getInstalledNumber(stack);
        if (number.isEmpty()) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.phone.empty"));
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.phone.insert_card"));
        } else {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.phone_number", number));
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.phone.remove_card"));
        }
    }
}
