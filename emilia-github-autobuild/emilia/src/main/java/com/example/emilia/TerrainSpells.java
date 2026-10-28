package com.example.emilia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Spells and weapon abilities that permanently reshape terrain. */
public final class TerrainSpells {
    private TerrainSpells() {}

    private static void msg(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
    }

    // -------------------------------------------------------------- Glacial Surge

    /** A glacier erupts forward in a wave: trees and plants are shattered, the ground turns to ice and ridges of glacial ice rise. */
    static boolean glacialSurge(ServerLevel level, ServerPlayer p) {
        if (!TerrainFx.enabled()) {
            msg(p, "message.emilia.terrain_disabled");
            return false;
        }
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0E-4) dir = Vec3.directionFromRotation(0, p.getYRot());
        final Vec3 d = dir.normalize();
        final Vec3 side = new Vec3(-d.z, 0, d.x);
        final Vec3 start = p.position();
        final int baseY = p.getBlockY();
        final Set<UUID> hit = new HashSet<>();
        for (int i = 2; i <= 28; i++) {
            final int step = i;
            Scheduler.schedule(i / 2, () -> surgeSlice(level, p, start, d, side, step, baseY, hit));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
        return true;
    }

    private static void surgeSlice(ServerLevel level, ServerPlayer p, Vec3 start, Vec3 dir, Vec3 side,
                                   int step, int baseY, Set<UUID> hit) {
        RandomSource rand = level.random;
        double cx = start.x + dir.x * step;
        double cz = start.z + dir.z * step;
        int half = step < 22 ? 3 : 2;
        for (int off = -half; off <= half; off++) {
            int x = Mth.floor(cx + side.x * off);
            int z = Mth.floor(cz + side.z * off);
            if (!level.hasChunkAt(new BlockPos(x, baseY, z))) continue;
            int gy = TerrainFx.groundY(level, x, z);
            if (Math.abs(gy - baseY) > 6) continue;

            // Shatter everything standing in the way (trees, plants, loose blocks).
            for (int dy = 1; dy <= 12; dy++) {
                BlockPos q = new BlockPos(x, gy + dy, z);
                BlockState s = level.getBlockState(q);
                if (!s.isAir() && !TerrainFx.isIce(s) && TerrainFx.canAlter(level, q, s)) {
                    level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            // Freeze the ground itself.
            for (int k = 0; k < 3; k++) {
                BlockPos g = new BlockPos(x, gy - k, z);
                BlockState gs = level.getBlockState(g);
                if (k == 0 && gs.is(Blocks.WATER) && gs.getFluidState().isSource()) {
                    level.setBlock(g, Blocks.ICE.defaultBlockState(), 3);
                    break;
                }
                if (TerrainFx.isIce(gs)) continue;
                if (!TerrainFx.canAlter(level, g, gs)) break;
                level.setBlock(g, k == 0 ? TerrainFx.hardIce(rand) : Blocks.PACKED_ICE.defaultBlockState(), 3);
            }
            // Raise the ridge.
            int a = Math.abs(off);
            int h = a <= 1 ? 2 + rand.nextInt(3) : (a == 2 ? rand.nextInt(3) : (rand.nextFloat() < 0.3F ? 1 : 0));
            for (int i = 1; i <= h; i++) {
                BlockPos q = new BlockPos(x, gy + i, z);
                if (!level.getBlockState(q).canBeReplaced()) break;
                Block b = (i == h && rand.nextFloat() < 0.35F) ? ModBlocks.ICE_CRYSTAL.get() : ModBlocks.GLACIAL_ICE.get();
                level.setBlock(q, b.defaultBlockState(), 3);
            }
            FrostUtil.overlayColumn(level, x, z, gy - 1, gy + h + 1, true, 0.6F);
        }
        AABB box = new AABB(cx - half - 1, baseY - 4, cz - half - 1, cx + half + 2, baseY + 8, cz + half + 2);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> !FrostUtil.isAlly(e, p) && !hit.contains(e.getUUID()))) {
            hit.add(e.getUUID());
            e.hurt(level.damageSources().playerAttack(p), 7.0F);
            e.push(dir.x * 0.8, 0.9, dir.z * 0.8);
            e.hurtMarked = true;
            FrostUtil.chill(e, 160);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, cx, baseY + 1.5, cz, 30, 2.0, 1.2, 2.0, 0.05);
        if (step % 3 == 0) {
            level.playSound(null, BlockPos.containing(cx, baseY, cz), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F,
                    0.5F + rand.nextFloat() * 0.4F);
        }
    }

    // ------------------------------------------------------------- Frozen Shatter

    /** Flash-freezes a sphere of terrain, then shatters it, leaving a permanent crater lined with ice and crystals. */
    static boolean frozenShatter(ServerLevel level, ServerPlayer p) {
        if (!TerrainFx.enabled()) {
            msg(p, "message.emilia.terrain_disabled");
            return false;
        }
        HitResult hit = p.pick(40.0, 1.0F, false);
        if (!(hit instanceof BlockHitResult bh) || hit.getType() != HitResult.Type.BLOCK) {
            msg(p, "message.emilia.no_target");
            return false;
        }
        final BlockPos c = bh.getBlockPos();
        final int radius = EmiliaConfig.SHATTER_RADIUS.get();
        final RandomSource rand = level.random;
        final List<BlockPos> inner = new ArrayList<>();

        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-radius, -radius, -radius), c.offset(radius, radius, radius))) {
            double d = Math.sqrt(pos.distSqr(c));
            if (d > radius) continue;
            BlockState st = level.getBlockState(pos);
            if (!TerrainFx.canAlter(level, pos, st) || TerrainFx.isIce(st)) continue;
            BlockPos ip = pos.immutable();
            level.setBlock(ip, TerrainFx.hardIce(rand), 3);
            if (d <= radius - 1.0) inner.add(ip);
        }

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c).inflate(radius + 1),
                e -> !FrostUtil.isAlly(e, p))) {
            e.hurt(level.damageSources().playerAttack(p), 10.0F);
            FrostUtil.chill(e, 200);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5,
                200, radius * 0.5, radius * 0.4, radius * 0.5, 0.05);
        level.playSound(null, c, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);

        Scheduler.schedule(25, () -> {
            for (BlockPos ip : inner) {
                if (TerrainFx.isIce(level.getBlockState(ip))) {
                    level.setBlock(ip, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            // Crystals sprout on the new crater floor.
            for (int i = 0; i < 16 && !inner.isEmpty(); i++) {
                BlockPos pos = inner.get(rand.nextInt(inner.size()));
                if (level.getBlockState(pos).isAir() && !level.getBlockState(pos.below()).isAir()
                        && level.getBlockState(pos.below()).getFluidState().isEmpty()) {
                    level.setBlock(pos, ModBlocks.ICE_CRYSTAL.get().defaultBlockState(), 3);
                }
            }
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c).inflate(radius),
                    e -> !FrostUtil.isAlly(e, p))) {
                e.push(0, 1.1, 0);
                e.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.SNOWFLAKE, c.getX() + 0.5, c.getY() + 1.0, c.getZ() + 0.5,
                    300, radius * 0.6, radius * 0.5, radius * 0.6, 0.15);
            level.playSound(null, c, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2.0F, 0.4F);
            FrostUtil.overlayArea(level, c, radius + 1, true, 0.45F);
        });
        return true;
    }

    // -------------------------------------------------------- Frostbreaker slam

    /** Ground slam used by the Frostbreaker: shockwave, small permanent crater and a ring of spikes. */
    static boolean groundSlam(ServerLevel level, ServerPlayer p) {
        HitResult hit = p.pick(5.0, 1.0F, false);
        final BlockPos c;
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            c = bh.getBlockPos();
        } else if (p.onGround()) {
            c = p.blockPosition().below();
        } else {
            msg(p, "message.emilia.no_target");
            return false;
        }
        Vec3 center = Vec3.atCenterOf(c);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c).inflate(6, 3, 6),
                e -> !FrostUtil.isAlly(e, p))) {
            Vec3 away = e.position().subtract(center).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-3 ? Vec3.ZERO : away.normalize();
            e.hurt(level.damageSources().playerAttack(p), 9.0F);
            e.push(away.x * 1.2, 0.7, away.z * 1.2);
            e.hurtMarked = true;
            FrostUtil.chill(e, 120);
        }
        if (TerrainFx.enabled()) {
            TerrainFx.crater(level, c, 2);
        }
        for (int i = 0; i < 8; i++) {
            double ang = i * Math.PI / 4.0;
            final int x = c.getX() + (int) Math.round(Math.cos(ang) * 3.5);
            final int z = c.getZ() + (int) Math.round(Math.sin(ang) * 3.5);
            Scheduler.schedule(1 + i % 3, () -> {
                if (!level.hasChunkAt(new BlockPos(x, c.getY(), z))) return;
                BlockPos spot = new BlockPos(x, TerrainFx.groundY(level, x, z) + 1, z);
                if (Math.abs(spot.getY() - c.getY()) <= 4 && level.getBlockState(spot).canBeReplaced()) {
                    level.setBlock(spot, FrostUtil.spikeIce(), 3);
                }
            });
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y + 1.0, center.z, 80, 2.5, 0.5, 2.5, 0.1);
        FrostUtil.overlayArea(level, c, 4, TerrainFx.enabled(), 0.5F);
        level.playSound(null, c, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null, c, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.6F);
        return true;
    }
}
