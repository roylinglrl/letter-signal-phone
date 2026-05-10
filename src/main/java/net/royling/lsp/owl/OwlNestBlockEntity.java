package net.royling.lsp.owl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.royling.lsp.registry.ModBlockEntities;
import net.royling.lsp.registry.ModEntityTypes;
import net.royling.lsp.registry.ModItems;

public class OwlNestBlockEntity extends BlockEntity {
    private static final int DAY = 24000;
    private static final int EGG_INTERVAL = DAY * 4;
    private static final int HATCH_INTERVAL = DAY * 3;
    private static final int BABY_STAY = DAY;

    private int adultOwls;
    private int incubatingAdults;
    private int babyOwls;
    private int eggs;
    private int eggTimer;
    private int hatchTimer;
    private int babyTimer;

    public OwlNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.OWL_NEST.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, OwlNestBlockEntity nest) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (nest.adultOwls >= 2 && nest.eggs == 0) {
            nest.eggTimer++;
            if (nest.eggTimer >= EGG_INTERVAL) {
                nest.eggs = 2 + serverLevel.getRandom().nextInt(3);
                nest.eggTimer = 0;
                nest.hatchTimer = 0;
                nest.incubatingAdults = Math.max(1, Math.min(1, nest.adultOwls));
                nest.setChanged();
            }
        } else if (nest.eggs == 0) {
            nest.eggTimer = 0;
        }

        if (nest.eggs > 0) {
            nest.hatchTimer++;
            nest.incubatingAdults = Math.max(1, Math.min(nest.adultOwls, nest.incubatingAdults == 0 ? 1 : nest.incubatingAdults));
            if (nest.hatchTimer >= HATCH_INTERVAL) {
                nest.babyOwls += nest.eggs;
                nest.eggs = 0;
                nest.hatchTimer = 0;
                nest.babyTimer = 0;
                nest.incubatingAdults = 0;
                nest.setChanged();
            }
        }

        if (nest.babyOwls > 0) {
            nest.babyTimer++;
        }

        if (!OwlEntity.isDaytime(serverLevel)) {
            nest.releaseNightOwls(serverLevel);
        }
    }

    public boolean storeOwl(OwlEntity owl) {
        if (owl.isBaby()) {
            babyOwls++;
        } else if (adultOwls < 2) {
            adultOwls++;
        } else {
            return false;
        }
        setChanged();
        return true;
    }

    public boolean canBindAdult() {
        return adultOwls < 2;
    }

    public void bindNearbyOwl(OwlEntity owl) {
        owl.bindNest(worldPosition);
    }

    public int stealEggs() {
        int stolen = eggs;
        eggs = 0;
        hatchTimer = 0;
        incubatingAdults = 0;
        setChanged();
        return stolen;
    }

    public ItemStack eggsStack() {
        return new ItemStack(ModItems.OWL_EGG.get(), eggs);
    }

    public int eggs() {
        return eggs;
    }

    public void addInitialOwls(int count) {
        adultOwls = Math.min(2, adultOwls + count);
        setChanged();
    }

    private void releaseNightOwls(ServerLevel level) {
        int adultsToRelease = eggs > 0 ? Math.max(0, adultOwls - Math.max(1, incubatingAdults)) : adultOwls;
        for (int i = 0; i < adultsToRelease; i++) {
            spawnOwl(level, false);
        }
        adultOwls -= adultsToRelease;

        if (babyOwls > 0 && babyTimer >= BABY_STAY) {
            int babies = babyOwls;
            babyOwls = 0;
            babyTimer = 0;
            for (int i = 0; i < babies; i++) {
                spawnOwl(level, true);
            }
        }
        if (adultsToRelease > 0) {
            setChanged();
        }
    }

    private void spawnOwl(ServerLevel level, boolean baby) {
        OwlEntity owl = ModEntityTypes.OWL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (owl == null) {
            return;
        }
        owl.setPos(worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D);
        owl.setYRot(level.getRandom().nextFloat() * 360.0F);
        owl.bindNest(worldPosition);
        owl.setBaby(baby);
        level.addFreshEntity(owl);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        adultOwls = input.getIntOr("AdultOwls", 0);
        incubatingAdults = input.getIntOr("IncubatingAdults", 0);
        babyOwls = input.getIntOr("BabyOwls", 0);
        eggs = input.getIntOr("Eggs", 0);
        eggTimer = input.getIntOr("EggTimer", 0);
        hatchTimer = input.getIntOr("HatchTimer", 0);
        babyTimer = input.getIntOr("BabyTimer", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("AdultOwls", adultOwls);
        output.putInt("IncubatingAdults", incubatingAdults);
        output.putInt("BabyOwls", babyOwls);
        output.putInt("Eggs", eggs);
        output.putInt("EggTimer", eggTimer);
        output.putInt("HatchTimer", hatchTimer);
        output.putInt("BabyTimer", babyTimer);
    }
}
