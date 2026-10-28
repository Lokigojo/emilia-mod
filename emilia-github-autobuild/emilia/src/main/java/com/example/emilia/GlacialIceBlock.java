package com.example.emilia;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Permanent, translucent, glowing ice. Left behind by Glacial Surge and Frozen Shatter. */
public class GlacialIceBlock extends Block {
    public GlacialIceBlock(Properties props) {
        super(props);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }
}
