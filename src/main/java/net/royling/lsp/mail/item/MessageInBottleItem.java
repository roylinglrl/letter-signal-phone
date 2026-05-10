package net.royling.lsp.mail.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.royling.lsp.mail.MessageInBottleLetterPool;

public class MessageInBottleItem extends BlockItem {
    private static final float TREASURE_MAP_CHANCE = 0.08F;
    private static final int TREASURE_SEARCH_RADIUS = 100;
    private static final byte TREASURE_MAP_SCALE = 2;

    public MessageInBottleItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        return openBottleStack(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return super.useOn(context);
        }
        return openBottleStack(context.getLevel(), player, context.getHand());
    }

    public static InteractionResult openPlacedBottle(Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            openBottle(serverPlayer, () -> level.destroyBlock(pos, false, serverPlayer));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult openBottleStack(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack bottle = player.getItemInHand(hand);
            openBottle(serverPlayer, () -> bottle.shrink(1));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void openBottle(ServerPlayer player, Runnable consumeBottle) {
        ItemStack letter = MessageInBottleLetterPool.INSTANCE.randomLetter(player);
        if (letter.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.letter_signal_phone.message_in_bottle.empty"), true);
            return;
        }
        consumeBottle.run();
        if (!player.addItem(letter)) {
            player.drop(letter, false);
        }
        giveTreasureMapIfLucky(player);
        player.sendSystemMessage(Component.translatable("message.letter_signal_phone.message_in_bottle.opened"), true);
    }

    private static void giveTreasureMapIfLucky(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || player.getRandom().nextFloat() >= TREASURE_MAP_CHANCE) {
            return;
        }
        BlockPos treasure = level.findNearestMapStructure(StructureTags.ON_TREASURE_MAPS, player.blockPosition(), TREASURE_SEARCH_RADIUS, true);
        if (treasure == null) {
            return;
        }
        ItemStack map = MapItem.create(level, treasure.getX(), treasure.getZ(), TREASURE_MAP_SCALE, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, treasure, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.CUSTOM_NAME, Component.translatable("filled_map.buried_treasure").withStyle(style -> style.withItalic(false)));
        if (!player.addItem(map)) {
            player.drop(map, false);
        }
    }
}
