package com.example.emilia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Helpers for freezing entities and the environment. */
public final class FrostUtil {
    public static final String SPEAR_TAG = "emilia_ice_spear";
    public static final String PUCK_TAG = "emilia_puck";
    public static final String GLACIAL_TAG = "emilia_glacial_spear";

    private FrostUtil() {}

    /** Do Emilia's spell-made ice blocks stay forever? (config: permanentIce) */
    public static boolean permanent() {
        return EmiliaConfig.PERMANENT_ICE.get();
    }

    /** The solid ice block that walls and cages are built from. */
    public static BlockState solidIce() {
        Block b = permanent() ? ModBlocks.GLACIAL_ICE.get() : ModBlocks.SPIRIT_ICE.get();
        return b.defaultBlockState();
    }

    /** The ice that spikes and lances erupt as: a solid Ice Crystal, or a melting Ice Spike. */
    public static BlockState spikeIce() {
        Block b = permanent() ? ModBlocks.ICE_CRYSTAL.get() : ModBlocks.ICE_SPIKE.get();
        return b.defaultBlockState();
    }

    /** Awakened players and Puck are immune to Emilia's cold. */
    public static boolean isEmilia(Entity e) {
        return (e instanceof Player p && p.getData(ModAttachments.AWAKENED)) || e.getTags().contains(PUCK_TAG);
    }

    public static boolean isAlly(Entity e, Player caster) {
        return e == caster || e.getTags().contains(PUCK_TAG)
                || (e instanceof TamableAnimal t && t.isOwnedBy(caster));
    }

    /** Frostbite plus the powder-snow shiver. */
    public static void chill(LivingEntity e, int ticks) {
        if (isEmilia(e)) return;
        e.addEffect(new MobEffectInstance(ModEffects.FROSTBITE, ticks, 0));
        e.setTicksFrozen(Math.max(e.getTicksFrozen(), e.getTicksRequiredToFreeze() + 20));
    }

    /**
     * Freezes the surface at one column: water becomes ice, plants and bare ground get a snow layer.
     * With harsh=true lava source blocks are turned to obsidian as well.
     * refY limits how far from a reference height the surface may be (Integer.MIN_VALUE disables the check).
     */
    public static void freezeColumn(ServerLevel level, int x, int z, int refY, boolean harsh) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        if (refY != Integer.MIN_VALUE && Math.abs(top.getY() - refY) > 5) return;
        BlockPos below = top.below();
        BlockState bs = level.getBlockState(below);
        if (bs.is(Blocks.WATER) && bs.getFluidState().isSource()) {
            level.setBlockAndUpdate(below, Blocks.ICE.defaultBlockState());
            return;
        }
        if (harsh && bs.is(Blocks.LAVA) && bs.getFluidState().isSource()) {
            level.setBlockAndUpdate(below, Blocks.OBSIDIAN.defaultBlockState());
            return;
        }
        BlockState ts = level.getBlockState(top);
        boolean free = ts.isAir() || (ts.canBeReplaced() && ts.getFluidState().isEmpty() && !ts.is(Blocks.SNOW));
        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (free && snow.canSurvive(level, top)) {
            level.setBlockAndUpdate(top, snow);
        }
    }

    /** A permanent jagged spire of packed and blue ice (Absolute Zero). */
    public static void spire(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        RandomSource rand = level.random;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        if (!level.getBlockState(top.below()).isSolid()) return;
        int h = 4 + rand.nextInt(7);
        for (int i = 0; i < h; i++) {
            BlockPos pos = top.above(i);
            if (!level.getBlockState(pos).canBeReplaced()) break;
            BlockState part = i == h - 1 ? ModBlocks.ICE_CRYSTAL.get().defaultBlockState()
                    : (i < 2 ? Blocks.PACKED_ICE.defaultBlockState() : ModBlocks.GLACIAL_ICE.get().defaultBlockState());
            level.setBlock(pos, part, 3);
        }
    }

    // ------------------------------------------------------------------ frost overlay

    /** Frosts the exposed faces of blocks in a cube around a center. */
    public static void overlayArea(ServerLevel level, BlockPos center, int radius, boolean permanent, float density) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!level.hasChunkAt(pos)) continue;
            overlayBlock(level, pos.immutable(), permanent, density);
        }
    }

    /** Frosts the exposed faces of blocks in a vertical slice of one column. */
    public static void overlayColumn(ServerLevel level, int x, int z, int yMin, int yMax, boolean permanent, float density) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        for (int y = Math.max(yMin, level.getMinBuildHeight()); y <= Math.min(yMax, level.getMaxBuildHeight() - 1); y++) {
            overlayBlock(level, new BlockPos(x, y, z), permanent, density);
        }
    }

    /** Lays a film of frost on each exposed (air-facing) side of one block, with the given chance per side. */
    public static void overlayBlock(ServerLevel level, BlockPos pos, boolean permanent, float density) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir() || !st.getFluidState().isEmpty() || st.getBlock() instanceof MultifaceBlock) return;
        RandomSource rand = level.random;
        Block film = (permanent ? ModBlocks.HOARFROST : ModBlocks.FROST_FILM).get();
        for (Direction d : Direction.values()) {
            if (rand.nextFloat() > density) continue;
            BlockPos n = pos.relative(d);
            BlockState ns = level.getBlockState(n);
            boolean existing = ns.getBlock() instanceof FrostFilmBlock;
            if (!ns.isAir() && !existing) continue;
            if (!st.isFaceSturdy(level, pos, d)) continue;
            BlockState next = film.defaultBlockState();
            if (existing) {
                if (ns.getBlock() != film && !permanent) continue; // never downgrade permanent frost
                for (Direction f : Direction.values()) {
                    if (MultifaceBlock.hasFace(ns, f)) {
                        next = next.setValue(MultifaceBlock.getFaceProperty(f), true);
                    }
                }
            }
            next = next.setValue(MultifaceBlock.getFaceProperty(d.getOpposite()), true);
            if (next.canSurvive(level, n)) {
                level.setBlock(n, next, 3);
            }
        }
    }
}
