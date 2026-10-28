package com.example.emilia;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Implementations of Emilia's spirit arts. Each returns true if the spell was cast. */
public final class Spells {
    private Spells() {}

    private static void msg(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key), true);
    }

    // ---------------------------------------------------------------- Icicle Line

    /** Spikes of ice erupt one after another along the ground in front of you. */
    static boolean icicleLine(ServerLevel level, ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0E-4) dir = Vec3.directionFromRotation(0, p.getYRot());
        dir = dir.normalize();
        final int by = p.getBlockY();
        for (int i = 1; i <= 16; i++) {
            final int bx = Mth_floor(p.getX() + dir.x * i);
            final int bz = Mth_floor(p.getZ() + dir.z * i);
            Scheduler.schedule(i, () -> erupt(level, p, bx, by, bz));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.8F);
        return true;
    }

    private static int Mth_floor(double v) {
        return net.minecraft.util.Mth.floor(v);
    }

    private static void erupt(ServerLevel level, ServerPlayer p, int x, int y, int z) {
        BlockPos ground = findGround(level, x, y, z);
        if (ground == null) return;
        level.setBlock(ground, FrostUtil.spikeIce(), 3);
        BlockPos above = ground.above();
        if (level.random.nextBoolean() && level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, FrostUtil.spikeIce(), 3);
        }
        AABB box = new AABB(ground).inflate(0.5, 1.0, 0.5);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> !FrostUtil.isAlly(e, p))) {
            e.hurt(level.damageSources().playerAttack(p), 6.0F);
            e.push(0, 0.7, 0);
            e.hurtMarked = true;
            FrostUtil.chill(e, 100);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, x + 0.5, ground.getY() + 0.8, z + 0.5, 14, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, ground, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6F, 1.4F);
        FrostUtil.overlayArea(level, ground, 1, FrostUtil.permanent(), 0.8F);
    }

    private static BlockPos findGround(ServerLevel level, int x, int y, int z) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos pos = new BlockPos(x, y + dy, z);
            if (!level.hasChunkAt(pos)) continue;
            BlockState st = level.getBlockState(pos);
            BlockPos below = pos.below();
            if (st.canBeReplaced() && st.getFluidState().isEmpty()
                    && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                return pos;
            }
        }
        return null;
    }

    // ---------------------------------------------------------- Ice Spear Barrage

    /** A fan of five flying ice spears. Each freezes what it hits and leaves frost where it lands. */
    static boolean iceSpear(ServerLevel level, ServerPlayer p) {
        for (int i = -2; i <= 2; i++) {
            Arrow a = EntityType.ARROW.create(level);
            if (a == null) continue;
            a.setOwner(p);
            a.setPos(p.getEyePosition());
            a.shootFromRotation(p, p.getXRot(), p.getYRot() + i * 6.0F, 0.0F, 3.0F, 0.3F);
            a.setBaseDamage(5.0);
            a.pickup = AbstractArrow.Pickup.DISALLOWED;
            a.setNoGravity(true);
            a.addTag(FrostUtil.SPEAR_TAG);
            level.addFreshEntity(a);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.PLAYERS, 1.2F, 0.7F);
        return true;
    }

    // ------------------------------------------------------------------ Ice Blade

    /** For 30 seconds your melee strikes carry a freezing edge. */
    static boolean iceBlade(ServerLevel level, ServerPlayer p) {
        p.setData(ModAttachments.BLADE, 600);
        msg(p, "message.emilia.blade");
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        return true;
    }

    // ------------------------------------------------------------------- Ice Wall

    /** A seven-wide, four-high wall of spirit ice rises where you look. */
    static boolean iceWall(ServerLevel level, ServerPlayer p) {
        HitResult hit = p.pick(24.0, 1.0F, false);
        BlockPos base;
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            base = bh.getBlockPos().relative(bh.getDirection());
        } else {
            Vec3 look = p.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z).normalize();
            base = BlockPos.containing(p.position().add(flat.scale(4)));
        }
        Vec3 look = p.getLookAngle();
        final boolean alongX = Math.abs(look.z) > Math.abs(look.x);
        final BlockPos origin = base;
        for (int h = 0; h < 4; h++) {
            final int hh = h;
            Scheduler.schedule(1 + h * 2, () -> {
                for (int w = -3; w <= 3; w++) {
                    BlockPos pos = alongX ? origin.offset(w, hh, 0) : origin.offset(0, hh, w);
                    placeIce(level, pos);
                }
                level.playSound(null, origin.above(hh), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 0.8F, 0.8F);
            });
        }
        Scheduler.schedule(9, () -> FrostUtil.overlayArea(level, origin.above(), 5, FrostUtil.permanent(), 0.35F));
        return true;
    }

    // ------------------------------------------------------------------- Ice Cage

    /** Encases the creature you are looking at in a shell of spirit ice. */
    static boolean iceCage(ServerLevel level, ServerPlayer p) {
        Entity target = findTarget(p, 32.0);
        if (target == null) {
            msg(p, "message.emilia.no_target");
            return false;
        }
        if (target instanceof LivingEntity le) FrostUtil.chill(le, 120);
        encase(level, target.blockPosition(), 2);
        final BlockPos cagePos = target.blockPosition();
        Scheduler.schedule(8, () -> FrostUtil.overlayArea(level, cagePos, 4, FrostUtil.permanent(), 0.4F));
        level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
        return true;
    }

    static void encase(ServerLevel level, BlockPos center, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            final int fdy = dy;
            Scheduler.schedule(dy + radius + 1, () -> {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(fdy), Math.abs(dz))) != radius) continue;
                        placeIce(level, center.offset(dx, fdy, dz));
                    }
                }
            });
        }
    }

    private static void placeIce(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState ice = FrostUtil.solidIce();
        if (level.getBlockState(pos).canBeReplaced() && level.isUnobstructed(ice, pos, CollisionContext.empty())) {
            level.setBlock(pos, ice, 3);
        }
    }

    static Entity findTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
        HitResult block = p.pick(range, 1.0F, false);
        double maxSq = block.getType() == HitResult.Type.MISS ? range * range : block.getLocation().distanceToSqr(eye);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !FrostUtil.isAlly(e, p), maxSq);
        return r == null ? null : r.getEntity();
    }

    // -------------------------------------------------------------- Absolute Zero

    /** Puck's ultimate art: the whole biome you stand in freezes over in five stages (see AbsoluteZero). */
    static boolean absoluteZero(ServerLevel level, ServerPlayer p) {
        msg(p, "message.emilia.absolute_zero");
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        level.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2.0F, 0.5F);
        return AbsoluteZero.cast(level, p);
    }
}
