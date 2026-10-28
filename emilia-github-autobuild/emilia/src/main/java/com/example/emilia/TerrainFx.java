package com.example.emilia;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Permanent terrain-altering helpers. All of them respect the terrainDestruction config. */
public final class TerrainFx {
    private TerrainFx() {}

    public static boolean enabled() {
        return EmiliaConfig.TERRAIN_DESTRUCTION.get();
    }

    public static boolean isIce(BlockState st) {
        return st.is(Blocks.ICE) || st.is(Blocks.PACKED_ICE) || st.is(Blocks.BLUE_ICE) || st.is(Blocks.FROSTED_ICE)
                || st.is(ModBlocks.GLACIAL_ICE.get()) || st.is(ModBlocks.ICE_CRYSTAL.get())
                || st.is(ModBlocks.SPIRIT_ICE.get()) || st.is(ModBlocks.ICE_SPIKE.get());
    }

    /** Can this block be frozen or shattered? Skips air, fluids, containers and very hard blocks. */
    public static boolean canAlter(ServerLevel level, BlockPos pos, BlockState st) {
        if (st.isAir() || !st.getFluidState().isEmpty()) return false;
        if (level.getBlockEntity(pos) != null) return false;
        float hardness = st.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= 10.0F;
    }

    public static BlockState hardIce(RandomSource rand) {
        return (rand.nextFloat() < 0.5F ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState();
    }

    /** Y of the real ground in a column, looking through canopy, trunks and plants. */
    public static int groundY(ServerLevel level, int x, int z) {
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        int y = top.getY() - 1;
        for (int i = 0; i < 48 && y > level.getMinBuildHeight(); i++) {
            BlockState st = level.getBlockState(new BlockPos(x, y, z));
            boolean passable = st.isAir() || st.is(BlockTags.LOGS) || st.is(BlockTags.LEAVES)
                    || (st.canBeReplaced() && st.getFluidState().isEmpty());
            if (!passable) break;
            y--;
        }
        return y;
    }

    /**
     * Deep permafrost for one column: canopy turns to ice or shatters, trunks become packed ice,
     * water freezes several blocks deep, lava turns to obsidian and the top three ground layers become hard ice.
     */
    public static void permafrost(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        RandomSource rand = level.random;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        int y = top.getY() - 1;
        for (int guard = 0; guard < 48 && y > level.getMinBuildHeight(); guard++) {
            BlockPos p = new BlockPos(x, y, z);
            BlockState st = level.getBlockState(p);
            if (st.is(BlockTags.LEAVES)) {
                level.setBlock(p, (rand.nextFloat() < 0.6F ? Blocks.ICE : Blocks.AIR).defaultBlockState(), 3);
            } else if (st.is(BlockTags.LOGS)) {
                level.setBlock(p, Blocks.PACKED_ICE.defaultBlockState(), 3);
            } else {
                break;
            }
            y--;
        }
        BlockPos ground = new BlockPos(x, y, z);
        BlockState gs = level.getBlockState(ground);
        if (gs.is(Blocks.WATER) && gs.getFluidState().isSource()) {
            for (int i = 0; i < 6; i++) {
                BlockPos p = ground.below(i);
                BlockState s = level.getBlockState(p);
                if (s.is(Blocks.WATER) && s.getFluidState().isSource()) {
                    level.setBlock(p, Blocks.ICE.defaultBlockState(), 3);
                } else {
                    break;
                }
            }
            return;
        }
        if (gs.is(Blocks.LAVA) && gs.getFluidState().isSource()) {
            level.setBlock(ground, Blocks.OBSIDIAN.defaultBlockState(), 3);
            return;
        }
        for (int k = 0; k < 3; k++) {
            BlockPos p = ground.below(k);
            BlockState s = level.getBlockState(p);
            if (isIce(s)) continue;
            if (!canAlter(level, p, s)) break;
            level.setBlock(p, k == 0 ? hardIce(rand) : Blocks.PACKED_ICE.defaultBlockState(), 3);
        }
        BlockPos above = ground.above();
        BlockState as = level.getBlockState(above);
        if (!as.isAir() && as.canBeReplaced() && as.getFluidState().isEmpty()) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Carves a bowl (air inside, hard ice lining below the rim) around a center block. */
    public static void crater(ServerLevel level, BlockPos c, int r) {
        RandomSource rand = level.random;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            double d = Math.sqrt(pos.distSqr(c));
            if (d > r + 0.3) continue;
            BlockState st = level.getBlockState(pos);
            if (!canAlter(level, pos, st) && !isIce(st)) continue;
            BlockPos ip = pos.immutable();
            if (d <= r - 0.7) {
                level.setBlock(ip, Blocks.AIR.defaultBlockState(), 3);
            } else if (ip.getY() <= c.getY()) {
                level.setBlock(ip, hardIce(rand), 3);
            }
        }
    }
}
