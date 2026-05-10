package net.royling.lsp.owl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

public class OwlReturnToNestGoal extends Goal {
    private final OwlEntity owl;

    public OwlReturnToNestGoal(OwlEntity owl) {
        this.owl = owl;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        BlockPos nest = owl.nestPos();
        return nest != null && OwlEntity.isDaytime(owl.level()) && owl.distanceToSqr(nest.getX() + 0.5D, nest.getY() + 0.5D, nest.getZ() + 0.5D) > 2.0D;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        BlockPos nest = owl.nestPos();
        if (nest != null) {
            owl.getNavigation().moveTo(nest.getX() + 0.5D, nest.getY() + 0.5D, nest.getZ() + 0.5D, 0.68D);
            owl.getLookControl().setLookAt(nest.getX() + 0.5D, nest.getY() + 0.5D, nest.getZ() + 0.5D);
        }
    }
}
