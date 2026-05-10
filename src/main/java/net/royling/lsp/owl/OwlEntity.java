package net.royling.lsp.owl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.royling.lsp.registry.ModBlocks;
import net.royling.lsp.registry.ModEntityTypes;

import javax.annotation.Nullable;
import java.util.Comparator;

public class OwlEntity extends Animal {
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SLEEP_POSE = SynchedEntityData.defineId(OwlEntity.class, EntityDataSerializers.INT);
    private static final int DAY = 24000;
    private static final int NEST_SEARCH_TICKS = 200;
    private static final int LOST_NEST_LIMIT = DAY * 3;
    private static final int HURT_FLEE_TICKS = 20 * 20;

    @Nullable
    private BlockPos nestPos;
    @Nullable
    private BlockPos treeSleepPos;
    @Nullable
    private BlockPos groundSleepPos;
    @Nullable
    private Vec3 fleeFrom;
    private int ticksWithoutNest;
    private int ticksSinceHatched;
    private int nextNestSearch;
    private int hurtFleeTicks;
    private float sleepYaw;

    public OwlEntity(EntityType<? extends OwlEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Cat.class, 10.0F, 1.2D, 1.45D));
        goalSelector.addGoal(2, new OwlFollowMeatGoal(this));
        goalSelector.addGoal(3, new OwlReturnToNestGoal(this));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.15D, true));
        goalSelector.addGoal(5, new LeapAtTargetGoal(this, 0.35F));
        goalSelector.addGoal(6, new FollowParentGoal(this, 1.1D));
        goalSelector.addGoal(7, new WaterAvoidingRandomFlyingGoal(this, 0.42D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F, 1.0F, false) {
            @Override
            public boolean canUse() {
                return !OwlEntity.this.isSleepingOwl() && !OwlEntity.this.isRecoveringFromHurt() && super.canUse();
            }
        });
        goalSelector.addGoal(9, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !OwlEntity.this.isSleepingOwl() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Bat.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Rabbit.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLEEPING, false);
        builder.define(SLEEP_POSE, 0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setRequiredPathLength(48.0F);
        return navigation;
    }

    @Override
    public void travel(Vec3 input) {
        travelFlying(input, getSpeed());
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level();
        updateJuvenileAge();
        repelPhantoms();
        avoidFireBlocks();
        tickHurtFlee();
        preferLowFlight();

        boolean day = isDaytime(level());
        if (nestPos == null || !isValidNest(nestPos)) {
            ticksWithoutNest++;
            if (--nextNestSearch <= 0) {
                nextNestSearch = NEST_SEARCH_TICKS + random.nextInt(NEST_SEARCH_TICKS);
                findOrCreateNest(serverLevel);
            }
            handleUnboundDayRest(serverLevel, day);
        } else {
            ticksWithoutNest = 0;
            treeSleepPos = null;
            setSleeping(false);
            if (day && canEnterNest()) {
                enterNest(serverLevel);
            }
        }
        if (isSleepingOwl()) {
            getNavigation().stop();
            setTarget(null);
            setNoGravity(false);
            setDeltaMovement(Vec3.ZERO);
            setYRot(sleepYaw);
            setYBodyRot(sleepYaw);
            setYHeadRot(sleepYaw);
        }
    }

    private void tickHurtFlee() {
        if (hurtFleeTicks <= 0) {
            fleeFrom = null;
            return;
        }
        hurtFleeTicks--;
        setSleeping(false);
        if (fleeFrom == null) {
            return;
        }
        Vec3 away = position().subtract(fleeFrom);
        if (away.lengthSqr() < 0.001D) {
            away = Vec3.directionFromRotation(0.0F, getYRot()).scale(-1.0D);
        }
        Vec3 wanted = position().add(away.normalize().scale(8.0D));
        double wantedY = preferredFlightY((int) wanted.x, (int) wanted.z) + 0.8D;
        getNavigation().moveTo(wanted.x, wantedY, wanted.z, 0.75D);
        setDeltaMovement(getDeltaMovement().add(away.normalize().scale(0.03D)).add(0.0D, 0.014D, 0.0D));
    }

    private void preferLowFlight() {
        if (onGround() || isSleepingOwl()) {
            return;
        }
        double wantedY = preferredFlightY(blockPosition().getX(), blockPosition().getZ());
        double delta = wantedY - getY();
        if (delta < -0.6D) {
            setDeltaMovement(getDeltaMovement().add(0.0D, -0.035D, 0.0D));
        } else if (delta > 0.25D) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.025D, 0.0D));
        }
    }

    private double preferredFlightY(int x, int z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1.4D;
    }

    private void handleUnboundDayRest(ServerLevel level, boolean day) {
        if (!day || hurtFleeTicks > 0) {
            treeSleepPos = null;
            groundSleepPos = null;
            setSleeping(false);
            return;
        }

        BlockPos nearbyNest = findAvailableNest(level, 24, 12);
        if (nearbyNest != null) {
            treeSleepPos = null;
            groundSleepPos = null;
            bindNest(nearbyNest);
            setSleeping(false);
            return;
        }

        if (treeSleepPos == null || !isValidTreeSleepPos(level, treeSleepPos) || distanceToSqr(Vec3.atCenterOf(treeSleepPos)) > 1024.0D) {
            treeSleepPos = findTreeSleepPos(level);
        }

        if (treeSleepPos != null) {
            double x = treeSleepPos.getX() + 0.5D;
            double y = sleepY(level, treeSleepPos);
            double z = treeSleepPos.getZ() + 0.5D;
            if (distanceToSqr(x, y, z) <= 1.6D) {
                sleepAt(x, y, z);
            } else {
                setSleeping(false);
                moveSmoothlyToSleepTarget(x, y, z, 0.62D);
            }
            return;
        }

        if (groundSleepPos == null || !isValidGroundSleepPos(level, groundSleepPos) || distanceToSqr(Vec3.atCenterOf(groundSleepPos)) > 1024.0D) {
            groundSleepPos = findGroundSleepPos(level);
        }

        if (groundSleepPos != null) {
            double x = groundSleepPos.getX() + 0.5D;
            double y = sleepY(level, groundSleepPos);
            double z = groundSleepPos.getZ() + 0.5D;
            if (onGround() && distanceToSqr(x, y, z) <= 1.6D) {
                sleepAt(x, y, z);
            } else {
                setSleeping(false);
                moveSmoothlyToSleepTarget(x, y, z, 0.55D);
            }
            return;
        }

        setSleeping(onGround());
    }

    private void updateJuvenileAge() {
        if (!isBaby()) {
            return;
        }
        ticksSinceHatched++;
        if (ticksSinceHatched >= DAY * 6) {
            setBaby(false);
            nestPos = null;
            ticksWithoutNest = 0;
        }
    }

    private boolean canEnterNest() {
        return nestPos != null && distanceToSqr(Vec3.atCenterOf(nestPos)) < 9.0D;
    }

    private void enterNest(ServerLevel level) {
        if (nestPos == null || !(level.getBlockEntity(nestPos) instanceof OwlNestBlockEntity nest)) {
            return;
        }
        if (nest.storeOwl(this)) {
            discard();
        }
    }

    private void findOrCreateNest(ServerLevel level) {
        BlockPos nearest = findAvailableNest(level, 24, 12);
        if (nearest != null) {
            bindNest(nearest);
            if (level.getBlockEntity(nearest) instanceof OwlNestBlockEntity nest) {
                nest.bindNearbyOwl(this);
            }
            return;
        }

        if (ticksWithoutNest >= LOST_NEST_LIMIT) {
            createNestFromSpruce(level);
        }
    }

    private void createNestFromSpruce(ServerLevel level) {
        BlockPos.betweenClosedStream(blockPosition().offset(-12, -6, -12), blockPosition().offset(12, 8, 12))
                .filter(pos -> level.getBlockState(pos).is(Blocks.SPRUCE_LOG) || level.getBlockState(pos).is(Blocks.STRIPPED_SPRUCE_LOG))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(blockPosition())))
                .ifPresent(pos -> {
                    BlockState state = ModBlocks.OWL_NEST.get().defaultBlockState();
                    level.setBlock(pos, state, 3);
                    if (level.getBlockEntity(pos) instanceof OwlNestBlockEntity nest) {
                        nest.bindNearbyOwl(this);
                    }
                    bindNest(pos.immutable());
                    ticksWithoutNest = 0;
                });
    }

    @Nullable
    private BlockPos findAvailableNest(ServerLevel level, int horizontal, int vertical) {
        AABB area = getBoundingBox().inflate(horizontal, vertical, horizontal);
        return BlockPos.betweenClosedStream(
                        (int) area.minX, (int) area.minY, (int) area.minZ,
                        (int) area.maxX, (int) area.maxY, (int) area.maxZ)
                .filter(pos -> level.getBlockState(pos).is(ModBlocks.OWL_NEST.get()))
                .filter(pos -> level.getBlockEntity(pos) instanceof OwlNestBlockEntity nest && nest.canBindAdult())
                .min(Comparator.comparingDouble(pos -> pos.distSqr(blockPosition())))
                .map(BlockPos::immutable)
                .orElse(null);
    }

    @Nullable
    private BlockPos findTreeSleepPos(ServerLevel level) {
        return BlockPos.betweenClosedStream(blockPosition().offset(-18, -6, -18), blockPosition().offset(18, 12, 18))
                .filter(pos -> isTreeBlock(level, pos))
                .map(pos -> level.getBlockState(pos.above()).is(Blocks.SNOW) ? pos.above(2) : pos.above())
                .filter(pos -> isValidTreeSleepPos(level, pos))
                .filter(pos -> !isSleepPosOccupied(level, pos))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(blockPosition())))
                .map(BlockPos::immutable)
                .orElse(null);
    }

    private boolean isValidTreeSleepPos(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && isTreeBlock(level, supportingTreePos(pos));
    }

    @Nullable
    private BlockPos findGroundSleepPos(ServerLevel level) {
        return BlockPos.betweenClosedStream(blockPosition().offset(-12, -4, -12), blockPosition().offset(12, 4, 12))
                .map(pos -> new BlockPos(pos.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()), pos.getZ()))
                .filter(pos -> isValidGroundSleepPos(level, pos))
                .filter(pos -> !isSleepPosOccupied(level, pos))
                .min(Comparator.comparingDouble(pos -> pos.distSqr(blockPosition())))
                .map(BlockPos::immutable)
                .orElse(null);
    }

    private boolean isValidGroundSleepPos(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && !level.getBlockState(pos.below()).isAir()
                && !isTreeBlock(level, pos.below());
    }

    private boolean isSleepPosOccupied(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(0.75D, 1.1D, 0.75D);
        for (OwlEntity owl : level.getEntitiesOfClass(OwlEntity.class, area)) {
            if (owl != this && (pos.equals(owl.treeSleepPos) || pos.equals(owl.groundSleepPos) || owl.blockPosition().equals(pos))) {
                return true;
            }
        }
        return false;
    }

    private double sleepY(ServerLevel level, BlockPos sleepPos) {
        BlockPos support = sleepSupportPos(level, sleepPos);
        BlockState state = level.getBlockState(support);
        VoxelShape shape = state.getCollisionShape(level, support);
        double top = shape.isEmpty() ? 1.0D : shape.max(Direction.Axis.Y);
        return support.getY() + top;
    }

    private BlockPos sleepSupportPos(ServerLevel level, BlockPos sleepPos) {
        BlockPos below = sleepPos.below();
        return level.getBlockState(below).is(Blocks.SNOW) ? below : supportingTreePos(sleepPos);
    }

    private void sleepAt(double x, double y, double z) {
        setNoGravity(false);
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        setSleeping(true);
    }

    private void moveSmoothlyToSleepTarget(double x, double y, double z, double speed) {
        getNavigation().moveTo(x, y, z, speed);
        getLookControl().setLookAt(x, y, z);
        Vec3 toward = new Vec3(x, y, z).subtract(position());
        if (toward.lengthSqr() > 0.001D) {
            setDeltaMovement(getDeltaMovement().scale(0.82D).add(toward.normalize().scale(0.018D)));
        }
    }

    private BlockPos supportingTreePos(BlockPos sleepPos) {
        return level().getBlockState(sleepPos.below()).is(Blocks.SNOW) ? sleepPos.below(2) : sleepPos.below();
    }

    private boolean isTreeBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || state.is(Blocks.SNOW) && level.getBlockState(pos.below()).is(BlockTags.LEAVES);
    }

    private boolean isValidNest(BlockPos pos) {
        return level().getBlockEntity(pos) instanceof OwlNestBlockEntity;
    }

    private void repelPhantoms() {
        for (Phantom phantom : level().getEntitiesOfClass(Phantom.class, getBoundingBox().inflate(12.0D))) {
            Vec3 away = phantom.position().subtract(position());
            if (away.lengthSqr() > 0.001D) {
                phantom.setDeltaMovement(phantom.getDeltaMovement().add(away.normalize().scale(0.08D)));
                phantom.setTarget(null);
            }
        }
    }

    private void avoidFireBlocks() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        Vec3 push = Vec3.ZERO;
        for (int x = -5; x <= 5; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -5; z <= 5; z++) {
                    mutable.set(blockPosition().getX() + x, blockPosition().getY() + y, blockPosition().getZ() + z);
                    BlockState state = level().getBlockState(mutable);
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                        push = push.add(position().subtract(Vec3.atCenterOf(mutable)));
                    }
                }
            }
        }
        if (push.lengthSqr() > 0.001D) {
            setDeltaMovement(getDeltaMovement().add(push.normalize().scale(0.06D)));
        }
    }

    public void bindNest(BlockPos pos) {
        nestPos = pos.immutable();
    }

    @Nullable
    public BlockPos nestPos() {
        return nestPos;
    }

    public boolean isSleepingOwl() {
        return entityData.get(SLEEPING);
    }

    public int sleepPose() {
        return entityData.get(SLEEP_POSE);
    }

    public void setSleeping(boolean sleeping) {
        if (sleeping && !isSleepingOwl()) {
            entityData.set(SLEEP_POSE, random.nextInt(2));
            sleepYaw = getYRot();
        }
        entityData.set(SLEEPING, sleeping);
    }

    public boolean isRecoveringFromHurt() {
        return hurtFleeTicks > 0;
    }

    public static boolean isDaytime(Level level) {
        long time = level.getOverworldClockTime() % DAY;
        return time >= 0 && time < 13000L;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, spawnGroupData);
        if (spawnReason == EntitySpawnReason.NATURAL && random.nextInt(6) == 0) {
            setBaby(true);
        }
        return data;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        OwlEntity owl = ModEntityTypes.OWL.get().create(level, EntitySpawnReason.BREEDING);
        if (owl != null) {
            owl.setBaby(true);
        }
        return owl;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        Entity attacker = source.getEntity();
        fleeFrom = attacker == null ? position().subtract(getLookAngle()) : attacker.position();
        hurtFleeTicks = HURT_FLEE_TICKS;
        setSleeping(false);
        return super.hurtServer(level, source, amount);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (nestPos != null) {
            output.putInt("NestX", nestPos.getX());
            output.putInt("NestY", nestPos.getY());
            output.putInt("NestZ", nestPos.getZ());
        }
        output.putInt("TicksWithoutNest", ticksWithoutNest);
        output.putInt("TicksSinceHatched", ticksSinceHatched);
        output.putInt("HurtFleeTicks", hurtFleeTicks);
        output.putFloat("SleepYaw", sleepYaw);
        output.putBoolean("Sleeping", isSleepingOwl());
        output.putInt("SleepPose", sleepPose());
        if (fleeFrom != null) {
            output.putDouble("FleeFromX", fleeFrom.x);
            output.putDouble("FleeFromY", fleeFrom.y);
            output.putDouble("FleeFromZ", fleeFrom.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getInt("NestX").isPresent() && input.getInt("NestY").isPresent() && input.getInt("NestZ").isPresent()) {
            nestPos = new BlockPos(input.getIntOr("NestX", 0), input.getIntOr("NestY", 0), input.getIntOr("NestZ", 0));
        }
        ticksWithoutNest = input.getIntOr("TicksWithoutNest", 0);
        ticksSinceHatched = input.getIntOr("TicksSinceHatched", 0);
        hurtFleeTicks = input.getIntOr("HurtFleeTicks", 0);
        sleepYaw = input.getFloatOr("SleepYaw", getYRot());
        if (hurtFleeTicks > 0) {
            fleeFrom = new Vec3(input.getDoubleOr("FleeFromX", getX()), input.getDoubleOr("FleeFromY", getY()), input.getDoubleOr("FleeFromZ", getZ()));
        }
        entityData.set(SLEEPING, input.getBooleanOr("Sleeping", false));
        entityData.set(SLEEP_POSE, input.getIntOr("SleepPose", 0));
    }
}
