package net.royling.lsp.mail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.royling.lsp.mail.ThrowableMessageInBottleData;
import net.royling.lsp.mail.entity.ThrownMessageInBottle;

import java.util.function.Consumer;

public class ThrowableMessageInBottleItem extends Item {
    public ThrowableMessageInBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ThrowableMessageInBottleData.hasLetter(stack)) {
            return InteractionResult.PASS;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ThrownMessageInBottle bottle = new ThrownMessageInBottle(serverLevel, player, stack.copyWithCount(1));
            bottle.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            serverLevel.addFreshEntity(bottle);
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        String owner = ThrowableMessageInBottleData.ownerName(stack);
        if (!owner.isBlank()) {
            tooltip.accept(Component.translatable("tooltip.letter_signal_phone.throwable_message_in_bottle.owner", owner));
        }
    }
}
