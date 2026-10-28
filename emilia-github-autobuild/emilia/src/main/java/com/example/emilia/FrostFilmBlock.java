package com.example.emilia;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A thin film of frost that clings to the faces of neighboring blocks, like glow lichen.
 * Temporary frost melts away after a while; permanent hoarfrost stays.
 */
public class FrostFilmBlock extends MultifaceBlock {
    public static final MapCodec<FrostFilmBlock> CODEC = simpleCodec(p -> new FrostFilmBlock(p, false));

    private final boolean permanent;
    private final MultifaceSpreader spreader = new MultifaceSpreader(this);

    public FrostFilmBlock(Properties props, boolean permanent) {
        super(props);
        this.permanent = permanent;
    }

    @Override
    public MapCodec<FrostFilmBlock> codec() {
        return CODEC;
    }

    public MultifaceSpreader getSpreader() {
        return spreader;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide && !permanent) {
            level.scheduleTick(pos, this, 500 + level.random.nextInt(700));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (permanent) return;
        level.removeBlock(pos, false);
        level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                3, 0.3, 0.3, 0.3, 0.01);
    }
}
