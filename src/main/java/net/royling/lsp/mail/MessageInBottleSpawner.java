package net.royling.lsp.mail;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.royling.lsp.registry.ModBlocks;

public final class MessageInBottleSpawner {
    private static final int ATTEMPTS_PER_PLAYER = 12;
    private static final int HORIZONTAL_RANGE = 96;

    private MessageInBottleSpawner() {
    }

    public static void tick(MinecraftServer server) {
        long day = server.overworld().getGameTime() / 24000L;
        MessageInBottleSavedData data = MessageInBottleSavedData.get(server);
        if (!data.shouldAttempt(day)) {
            return;
        }
        data.markAttempted(day);
        for (ServerLevel level : server.getAllLevels()) {
            trySpawnInLevel(level);
        }
    }

    private static void trySpawnInLevel(ServerLevel level) {
        if (level.players().isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Player player : level.players()) {
            BlockPos center = player.blockPosition();
            for (int i = 0; i < ATTEMPTS_PER_PLAYER; i++) {
                int x = center.getX() + random.nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE;
                int z = center.getZ() + random.nextInt(HORIZONTAL_RANGE * 2 + 1) - HORIZONTAL_RANGE;
                BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
                if (trySpawnAt(level, surface, random)) {
                    break;
                }
            }
        }
    }

    private static boolean trySpawnAt(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasChunkAt(pos) || !level.getBiome(pos).is(BiomeTags.IS_BEACH)) {
            return false;
        }
        BlockPos ground = pos.below();
        if (!level.getBlockState(pos).isAir() || !isBeachGround(level.getBlockState(ground).getBlock())) {
            return false;
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        level.setBlock(pos, ModBlocks.MESSAGE_IN_BOTTLE.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, facing), 3);
        return true;
    }

    private static boolean isBeachGround(net.minecraft.world.level.block.Block block) {
        return block == Blocks.SAND || block == Blocks.RED_SAND || block.defaultBlockState().is(BlockTags.DIRT);
    }
}
