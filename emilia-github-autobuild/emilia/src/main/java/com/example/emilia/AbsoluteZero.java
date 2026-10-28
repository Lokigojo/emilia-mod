package com.example.emilia;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Absolute Zero: the freeze spreads through the whole biome the caster stands in, in five stages.
 * The biome is mapped once at cast time (flood fill over 4x4 biome cells, loaded chunks only) and split into
 * five bands by distance from the caster. Stage N starts N intervals after the cast; columns are frozen a few
 * hundred per tick so the server never stalls.
 */
public final class AbsoluteZero {
    private static final int STAGES = 5;
    private static final int MAX_CELLS = 60000;

    private final ServerLevel level;
    private final ServerPlayer caster;
    private final LongArrayList[] stageCells = new LongArrayList[STAGES];
    private final Long2IntOpenHashMap cellStage = new Long2IntOpenHashMap();
    private final int[] cellCursor = new int[STAGES];
    private final int[] subCursor = new int[STAGES];
    private final Set<UUID> hit = new HashSet<>();
    private final long startTick;
    private final int interval;
    private final int budget;
    private int started = 0;

    private AbsoluteZero(ServerLevel level, ServerPlayer caster) {
        this.level = level;
        this.caster = caster;
        this.startTick = level.getGameTime();
        this.interval = EmiliaConfig.ABSOLUTE_ZERO_INTERVAL_TICKS.get();
        this.budget = EmiliaConfig.ABSOLUTE_ZERO_COLUMNS_PER_TICK.get();
        this.cellStage.defaultReturnValue(-1);
        for (int i = 0; i < STAGES; i++) stageCells[i] = new LongArrayList();
    }

    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    private static int cellX(long key) {
        return (int) (key >> 32);
    }

    private static int cellZ(long key) {
        return (int) key;
    }

    /** Maps the caster's biome and starts the five-stage freeze. Returns false if nothing could be mapped. */
    public static boolean cast(ServerLevel level, ServerPlayer p) {
        Holder<Biome> target = level.getBiome(p.blockPosition());
        int maxR = EmiliaConfig.ABSOLUTE_ZERO_MAX_RADIUS.get();
        int ox = p.getBlockX();
        int oz = p.getBlockZ();

        Long2IntOpenHashMap dist = new Long2IntOpenHashMap();
        dist.defaultReturnValue(-1);
        LongArrayList order = new LongArrayList();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        long start = cellKey(ox >> 2, oz >> 2);
        dist.put(start, 0);
        order.add(start);
        queue.enqueue(start);
        final int[] dx = {1, -1, 0, 0};
        final int[] dz = {0, 0, 1, -1};
        while (!queue.isEmpty() && order.size() < MAX_CELLS) {
            long cur = queue.dequeueLong();
            int d = dist.get(cur);
            for (int i = 0; i < 4; i++) {
                int nx = cellX(cur) + dx[i];
                int nz = cellZ(cur) + dz[i];
                long key = cellKey(nx, nz);
                if (dist.get(key) != -1) continue;
                int wx = nx * 4 + 2;
                int wz = nz * 4 + 2;
                long ddx = wx - ox;
                long ddz = wz - oz;
                if (ddx * ddx + ddz * ddz > (long) maxR * maxR) continue;
                if (!level.hasChunkAt(new BlockPos(wx, 0, wz))) continue;
                int sy = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz);
                Holder<Biome> b = level.getBiome(new BlockPos(wx, sy, wz));
                if (b.value() != target.value()) continue;
                dist.put(key, d + 1);
                order.add(key);
                queue.enqueue(key);
            }
        }

        AbsoluteZero az = new AbsoluteZero(level, p);
        int maxD = dist.get(order.getLong(order.size() - 1));
        for (int i = 0; i < order.size(); i++) {
            long cell = order.getLong(i);
            int stage = Math.min(STAGES - 1, (int) ((long) dist.get(cell) * STAGES / (maxD + 1)));
            az.stageCells[stage].add(cell);
            az.cellStage.put(cell, stage);
        }
        Scheduler.schedule(1, az::tick);
        return true;
    }

    private void tick() {
        long now = level.getGameTime();
        int shouldStart = (int) Math.min(STAGES, (now - startTick) / interval + 1);
        while (started < shouldStart) {
            startStage(started);
            started++;
        }
        int left = budget;
        for (int s = 0; s < started && left > 0; s++) {
            left = process(s, left);
        }
        if (now % 10 == 0) hitEntities();
        if (started >= STAGES && allDone()) {
            finish();
            return;
        }
        Scheduler.schedule(1, this::tick);
    }

    private boolean allDone() {
        for (int s = 0; s < STAGES; s++) {
            if (cellCursor[s] < stageCells[s].size()) return false;
        }
        return true;
    }

    private int process(int s, int left) {
        LongArrayList cells = stageCells[s];
        while (left > 0 && cellCursor[s] < cells.size()) {
            long cell = cells.getLong(cellCursor[s]);
            int sub = subCursor[s];
            freezeColumn(cellX(cell) * 4 + (sub & 3), cellZ(cell) * 4 + (sub >> 2));
            left--;
            if (++subCursor[s] >= 16) {
                subCursor[s] = 0;
                cellCursor[s]++;
            }
        }
        return left;
    }

    private void freezeColumn(int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        if (TerrainFx.enabled()) {
            TerrainFx.permafrost(level, x, z);
        } else {
            FrostUtil.freezeColumn(level, x, z, Integer.MIN_VALUE, true);
        }
        int gy = TerrainFx.groundY(level, x, z);
        FrostUtil.overlayColumn(level, x, z, gy - 2, gy + 10, TerrainFx.enabled(), 0.55F);
        RandomSource r = level.random;
        if (r.nextInt(150) == 0) FrostUtil.spire(level, x, z);
        if (r.nextInt(24) == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, x + 0.5, gy + 1.5, z + 0.5, 4, 0.5, 1.0, 0.5, 0.02);
        }
    }

    private void hitEntities() {
        List<? extends LivingEntity> list = level.getEntities(EntityTypeTest.forClass(LivingEntity.class),
                e -> e.isAlive() && !hit.contains(e.getUUID()) && !FrostUtil.isAlly(e, caster));
        for (LivingEntity e : list) {
            int st = cellStage.get(cellKey(e.getBlockX() >> 2, e.getBlockZ() >> 2));
            if (st < 0 || st >= started) continue;
            hit.add(e.getUUID());
            e.hurt(level.damageSources().playerAttack(caster), 10.0F);
            FrostUtil.chill(e, 400);
            if (e.getBbWidth() <= 2.5F && e.getBbHeight() <= 2.9F) {
                Spells.encase(level, e.blockPosition(), 2);
            }
        }
    }

    private void startStage(int s) {
        Component msg = Component.translatable("message.emilia.absolute_zero_stage", s + 1, STAGES);
        for (ServerPlayer pl : level.players()) {
            pl.displayClientMessage(msg, true);
            pl.playNotifySound(SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.AMBIENT, 1.5F, 0.4F + 0.1F * s);
        }
        if (s == 0 && level.dimension() == Level.OVERWORLD) {
            level.setWeatherParameters(0, 12000, true, false);
        }
    }

    private void finish() {
        Component msg = Component.translatable("message.emilia.absolute_zero_done");
        for (ServerPlayer pl : level.players()) {
            pl.displayClientMessage(msg, true);
            pl.playNotifySound(SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.5F, 0.5F);
        }
    }
}
