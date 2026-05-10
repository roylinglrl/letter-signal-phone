package net.royling.lsp.mail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.royling.lsp.mail.menu.MailboxMenu;
import net.royling.lsp.registry.ModItems;
import org.jetbrains.annotations.Nullable;

public class MailboxBlock extends Block {
    public static final BooleanProperty HAS_MAIL = BooleanProperty.create("has_mail");
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 14.0D, 16.0D);

    public MailboxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HAS_MAIL, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_MAIL, FACING);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide() && placer instanceof ServerPlayer player) {
            MailSavedData data = MailSavedData.get(player.level().getServer());
            data.refreshMailboxBinding(player.level().getServer(), player.getUUID());
            if (!data.registerMailbox(player.getUUID(), player.getScoreboardName(), pos)) {
                player.sendSystemMessage(Component.translatable("message.letter_signal_phone.mailbox.only_one"), true);
                level.destroyBlock(pos, false, player);
                ItemStack mailbox = new ItemStack(ModItems.MAILBOX.get());
                if (!player.addItem(mailbox)) {
                    player.drop(mailbox, false);
                }
            } else {
                data.syncMailboxState(player.level().getServer(), player.getUUID());
                player.sendSystemMessage(Component.translatable("message.letter_signal_phone.mailbox.bound"), true);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        MailSavedData.get(level.getServer()).unregisterMailboxAt(level.getServer(), pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            MailSavedData data = MailSavedData.get(serverPlayer.level().getServer());
            if (!data.isMailboxOwner(serverPlayer.getUUID(), pos)) {
                serverPlayer.sendSystemMessage(Component.translatable("message.letter_signal_phone.mailbox.not_owner"), true);
                return InteractionResult.SUCCESS_SERVER;
            }
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, ignored) -> new MailboxMenu(id, inventory, serverPlayer),
                    Component.translatable("screen.letter_signal_phone.mailbox")
            ));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
