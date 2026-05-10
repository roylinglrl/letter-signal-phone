package net.royling.lsp.phone.block;

import net.royling.lsp.phone.data.PhoneData;
import net.royling.lsp.phone.data.PhoneSavedData;
import net.royling.lsp.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CardWriterBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public CardWriterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        //跳过客户端
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        //跳过非玩家
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        //处理空电话卡，随机注册一个号码
        if (stack.is(ModItems.BLANK_PHONE_CARD.get())) {
            PhoneSavedData data = PhoneSavedData.get(serverPlayer.level().getServer());
            var registered = data.register(serverPlayer.getUUID(), serverPlayer.level().getServer().getTickCount());
            if (registered.isPresent()) {
                PhoneSavedData.Registration registration = registered.get();
                ItemStack card = new ItemStack(ModItems.PHONE_CARD.get());
                PhoneData.setCardRegistration(card, registration.number(), registration.token());
                stack.shrink(1);
                if (!player.addItem(card)) {
                    player.drop(card, false);
                }
                serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.card_writer.registered", registration.number()), true);
                return InteractionResult.SUCCESS_SERVER;
            }
            serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.card_writer.full"), true);
            return InteractionResult.FAIL;
        }
        //销号
        if (stack.is(ModItems.PHONE_CARD.get()) && PhoneData.hasCardNumber(stack)) {
            String number = PhoneData.getCardNumber(stack);
            PhoneSavedData.get(serverPlayer.level().getServer()).unregister(number);
            stack.shrink(1);
            ItemStack blank = new ItemStack(ModItems.BLANK_PHONE_CARD.get());
            if (!player.addItem(blank)) {
                player.drop(blank, false);
            }
            serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.card_writer.unregistered", number), true);
            return InteractionResult.SUCCESS_SERVER;
        }

        serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.card_writer.need_card"), true);
        return InteractionResult.SUCCESS_SERVER;
    }
}
