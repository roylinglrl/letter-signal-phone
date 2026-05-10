package net.royling.lsp.mail.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.royling.lsp.mail.MessageInBottleSavedData;
import net.royling.lsp.mail.ThrowableMessageInBottleData;
import net.royling.lsp.registry.ModEntityTypes;
import net.royling.lsp.registry.ModItems;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class ThrownMessageInBottle extends ThrowableItemProjectile {
    private static final int MIN_OCEAN_WATER_BLOCKS = 125;

    public ThrownMessageInBottle(EntityType<? extends ThrownMessageInBottle> type, Level level) {
        super(type, level);
    }

    public ThrownMessageInBottle(Level level, LivingEntity owner, ItemStack stack) {
        super(ModEntityTypes.THROWN_MESSAGE_IN_BOTTLE.get(), owner, level, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.THROWABLE_MESSAGE_IN_BOTTLE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && isInWater()) {
            finishThrow(blockPosition());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            finishThrow(BlockPos.containing(result.getLocation()));
        }
    }

    private void finishThrow(BlockPos pos) {
        if (!(level() instanceof ServerLevel serverLevel) || isRemoved()) {
            return;
        }
        Entity owner = getOwner();
        ItemStack stack = getItem().copyWithCount(1);
        if (isLargeEnoughWater(serverLevel, pos)) {
            MessageInBottleSavedData.get(serverLevel.getServer()).addPlayerLetter(
                    ThrowableMessageInBottleData.owner(stack),
                    ThrowableMessageInBottleData.encodedLetter(stack)
            );
            if (owner instanceof ServerPlayer player) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.letter_signal_phone.throwable_message_in_bottle.success"), true);
            }
            discard();
            return;
        }
        if (owner instanceof ServerPlayer player) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.letter_signal_phone.throwable_message_in_bottle.failed"), true);
        }
        spawnAtLocation(serverLevel, stack);
        discard();
    }

    private static boolean isLargeEnoughWater(ServerLevel level, BlockPos start) {
        BlockPos water = nearestWater(level, start);
        if (water == null) {
            return false;
        }
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(water);
        visited.add(water);
        while (!queue.isEmpty() && visited.size() <= MIN_OCEAN_WATER_BLOCKS) {
            BlockPos current = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!visited.contains(next) && isWater(level, next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return visited.size() > MIN_OCEAN_WATER_BLOCKS;
    }

    private static BlockPos nearestWater(ServerLevel level, BlockPos pos) {
        if (isWater(level, pos)) {
            return pos;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (isWater(level, next)) {
                return next;
            }
        }
        return null;
    }

    private static boolean isWater(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }
}
