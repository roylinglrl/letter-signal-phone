package net.royling.lsp.owl;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;

public class OwlFollowMeatGoal extends Goal {
    private static final double STOP_DISTANCE_SQR = 4.0D;
    private final OwlEntity owl;
    private Player targetPlayer;

    public OwlFollowMeatGoal(OwlEntity owl) {
        this.owl = owl;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (owl.isSleepingOwl() || owl.isRecoveringFromHurt()) {
            return false;
        }
        targetPlayer = owl.level().getEntitiesOfClass(Player.class, owl.getBoundingBox().inflate(12.0D), OwlFollowMeatGoal::holdsMeat)
                .stream()
                .min(Comparator.comparingDouble(owl::distanceToSqr))
                .orElse(null);
        return targetPlayer != null;
    }

    @Override
    public boolean canContinueToUse() {
        return targetPlayer != null && targetPlayer.isAlive() && holdsMeat(targetPlayer) && owl.distanceToSqr(targetPlayer) < 225.0D && !owl.isSleepingOwl();
    }

    @Override
    public void stop() {
        targetPlayer = null;
    }

    @Override
    public void tick() {
        if (targetPlayer == null) {
            return;
        }
        Vec3 wanted = targetPlayer.position().add(0.0D, 1.25D, 0.0D);
        owl.getLookControl().setLookAt(targetPlayer);
        double distanceToPlayer = owl.distanceToSqr(targetPlayer);
        if (distanceToPlayer <= STOP_DISTANCE_SQR) {
            owl.getNavigation().stop();
            owl.setDeltaMovement(owl.getDeltaMovement().scale(0.72D).add(0.0D, 0.012D, 0.0D));
            return;
        }
        owl.getNavigation().moveTo(wanted.x, wanted.y, wanted.z, 0.68D);
        Vec3 toward = wanted.subtract(owl.position());
        if (toward.lengthSqr() > 0.001D) {
            owl.setDeltaMovement(owl.getDeltaMovement().add(toward.normalize().scale(0.02D)));
        }
    }

    private static boolean holdsMeat(Player player) {
        return isMeat(player.getMainHandItem()) || isMeat(player.getOffhandItem());
    }

    private static boolean isMeat(ItemStack stack) {
        return stack.is(ItemTags.MEAT);
    }
}
