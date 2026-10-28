package com.example.emilia;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A jagged ice spike. Entities that wander into it are cut and chilled. */
public class IceSpikeBlock extends SpiritIceBlock {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5, 0, 5, 11, 6, 11),
            Block.box(6, 6, 6, 10, 12, 10),
            Block.box(7, 12, 7, 9, 16, 9));

    public IceSpikeBlock(Properties props) {
        super(props);
    }

    @Override
    protected int meltDelay(RandomSource random) {
        return 120 + random.nextInt(80);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(level instanceof ServerLevel) || !(entity instanceof LivingEntity le) || FrostUtil.isEmilia(le)) return;
        le.makeStuckInBlock(state, new Vec3(0.8, 0.75, 0.8));
        if (le.tickCount % 10 == 0) {
            le.hurt(level.damageSources().magic(), 2.0F);
            FrostUtil.chill(le, 60);
        }
    }
}
