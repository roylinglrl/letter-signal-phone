package net.royling.lsp.mail.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.royling.lsp.mail.StampData;
import net.royling.lsp.mail.StampPackData;
import net.royling.lsp.mail.StampPackDefinition;
import net.royling.lsp.mail.StampPackManager;
import net.royling.lsp.mail.StampPackSavedData;
import net.royling.lsp.mail.StampVariant;

import java.util.function.Consumer;

public class StampPackItem extends Item {
    public StampPackItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack pack = player.getItemInHand(hand);
        StampPackDefinition definition = StampPackManager.INSTANCE.byId(StampPackData.packId(pack));
        int openCount = 1;
        boolean guaranteeRare = false;
        if (player instanceof ServerPlayer serverPlayer) {
            StampPackSavedData savedData = StampPackSavedData.get(serverPlayer.level().getServer());
            openCount = savedData.recordOpen(serverPlayer.getUUID(), definition.id());
            guaranteeRare = openCount >= definition.guaranteedRareOpens();
        }
        RandomSource random = player.getRandom();
        boolean gotRare = false;
        for (int i = 0; i < definition.stampsPerPack(); i++) {
            boolean forceRare = guaranteeRare && !gotRare && i == definition.stampsPerPack() - 1;
            ItemStack stamp = randomStamp(definition, random, forceRare);
            if (!StampData.RARITY_COMMON.equals(StampData.rarity(stamp))) {
                gotRare = true;
                StampData.setPackOrigin(stamp, player.getScoreboardName(), openCount);
            }
            if (!player.addItem(stamp)) {
                player.drop(stamp, false);
            }
        }
        pack.shrink(1);
        if (player instanceof ServerPlayer serverPlayer) {
            if (gotRare) {
                StampPackSavedData.get(serverPlayer.level().getServer()).reset(serverPlayer.getUUID(), definition.id());
            }
            serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.stamp_pack.opened"), true);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        StampPackDefinition definition = StampPackManager.INSTANCE.byId(StampPackData.packId(stack));
        tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp_pack.contents", definition.stampsPerPack()));
        tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp_pack.rare_chance", chanceText(StampPackManager.INSTANCE.totalRarityChance(definition))));
        tooltip.accept(Component.translatable("tooltip.letter_signal_phone.stamp_pack.guarantee", definition.guaranteedRareOpens()));
    }

    private static ItemStack randomStamp(StampPackDefinition definition, RandomSource random, boolean forceRare) {
        StampVariant variant = StampPackManager.INSTANCE.randomStamp(definition, random);
        ItemStack stamp = StampData.stackFor(variant);
        StampPackDefinition.RarityEntry rarity = forceRare
                ? StampPackManager.INSTANCE.guaranteedRarity(definition, random)
                : StampPackManager.INSTANCE.randomRarity(definition, random);
        if (rarity != null) {
            StampData.setRarityAndFoil(stamp, rarity.rarity(), rarity.foilEffect());
        }
        return stamp;
    }

    private static String chanceText(float chance) {
        return String.format(java.util.Locale.ROOT, "%.3f%%", chance * 100.0F);
    }
}
