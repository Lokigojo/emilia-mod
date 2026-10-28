package com.example.emilia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A permanent cluster of glowing ice crystals. */
public class IceCrystalBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(6, 0, 6, 10, 14, 10),
            Block.box(2, 0, 3, 6, 9, 7),
            Block.box(9, 0, 9, 13, 7, 13));

    public IceCrystalBlock(Properties props) {
        super(props);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
