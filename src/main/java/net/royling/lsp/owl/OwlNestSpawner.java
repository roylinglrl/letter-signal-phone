package net.royling.lsp.owl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.royling.lsp.registry.ModBlocks;

public final class OwlNestSpawner {
    private static final int CHECK_INTERVAL = 1200;
    private static int ticks;

    private OwlNestSpawner() {
    }

    public static void tick(MinecraftServer server) {
        if (++ticks < CHECK_INTERVAL) {
            return;
        }
        ticks = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = (ServerLevel) player.level();
            if (level.getRandom().nextInt(160) != 0) {
                continue;
            }
            tryCreateNestNear(level, player.blockPosition());
        }
    }

    private static void tryCreateNestNear(ServerLevel level, BlockPos center) {
        for (int i = 0; i < 48; i++) {
            BlockPos pos = center.offset(level.getRandom().nextInt(65) - 32, level.getRandom().nextInt(24) - 8, level.getRandom().nextInt(65) - 32);
            if (!level.getBiome(pos).is(BiomeTags.IS_TAIGA) || !level.getBlockState(pos).is(Blocks.SPRUCE_LOG)) {
                continue;
            }
            if (!level.getBlockState(pos.north()).isAir() && !level.getBlockState(pos.south()).isAir() && !level.getBlockState(pos.east()).isAir() && !level.getBlockState(pos.west()).isAir()) {
                continue;
            }
            level.setBlock(pos, ModBlocks.OWL_NEST.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof OwlNestBlockEntity nest) {
                nest.addInitialOwls(1 + level.getRandom().nextInt(2));
            }
            return;
        }
    }
}
