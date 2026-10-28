package com.example.emilia;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Per-player state, mana, passives and Puck. */
public final class SpiritManager {
    public static final float MAX_MANA = 100.0F;

    private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();
    private static final Map<UUID, UUID> PUCKS = new HashMap<>();
    private static final Map<UUID, Long> PUCK_SHOTS = new HashMap<>();

    private SpiritManager() {}

    public static boolean isAwakened(Player p) {
        return p.getData(ModAttachments.AWAKENED);
    }

    public static Spell getSpell(Player p) {
        return Spell.byIndex(p.getData(ModAttachments.SPELL));
    }

    public static boolean isKnownPuck(UUID catId) {
        return PUCKS.containsValue(catId);
    }

    // ------------------------------------------------------------------ actions

    public static void handleAction(ServerPlayer p, int action) {
        if (!isAwakened(p)) return;
        switch (action) {
            case 0 -> cast(p);
            case 1 -> cycle(p, true);
            case 2 -> cycle(p, false);
            default -> { }
        }
    }

    public static void cycle(ServerPlayer p, boolean forward) {
        int i = p.getData(ModAttachments.SPELL) + (forward ? 1 : -1);
        p.setData(ModAttachments.SPELL, Math.floorMod(i, Spell.values().length));
        showStatus(p);
    }

    public static void cast(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        Spell spell = getSpell(p);
        long now = level.getGameTime();
        long[] cds = COOLDOWNS.computeIfAbsent(p.getUUID(), k -> new long[Spell.values().length]);
        if (now < cds[spell.ordinal()]) {
            p.displayClientMessage(Component.translatable("message.emilia.recharging"), true);
            return;
        }
        boolean creative = p.getAbilities().instabuild;
        boolean free = spell.cost == 0 || (spell == Spell.PUCK && puckAlive(level, p));
        float mana = p.getData(ModAttachments.MANA);
        if (!free && !creative && mana < spell.cost) {
            p.displayClientMessage(Component.translatable("message.emilia.no_mana"), true);
            return;
        }
        boolean ok = switch (spell) {
            case ICICLE_LINE -> Spells.icicleLine(level, p);
            case ICE_SPEAR -> Spells.iceSpear(level, p);
            case ICE_BLADE -> Spells.iceBlade(level, p);
            case ICE_WALL -> Spells.iceWall(level, p);
            case ICE_CAGE -> Spells.iceCage(level, p);
            case FROST_AURA -> toggleAura(p, mana);
            case PUCK -> togglePuck(level, p);
            case ABSOLUTE_ZERO -> Spells.absoluteZero(level, p);
            case HOARFROST_BLADE -> Arms.forgeBlade(level, p);
            case GLACIAL_SPEAR -> Arms.forgeSpear(level, p);
            case FROSTBREAKER -> Arms.forgeHammer(level, p);
            case GLACIAL_SURGE -> TerrainSpells.glacialSurge(level, p);
            case FROZEN_SHATTER -> TerrainSpells.frozenShatter(level, p);
        };
        if (ok) {
            if (!free && !creative) p.setData(ModAttachments.MANA, mana - spell.cost);
            cds[spell.ordinal()] = now + spell.cooldown;
            showStatus(p);
        }
    }

    private static boolean toggleAura(ServerPlayer p, float mana) {
        if (p.getData(ModAttachments.AURA)) {
            p.setData(ModAttachments.AURA, false);
            p.displayClientMessage(Component.translatable("message.emilia.aura_off"), true);
            return true;
        }
        if (mana < 10.0F && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("message.emilia.no_mana"), true);
            return false;
        }
        p.setData(ModAttachments.AURA, true);
        p.displayClientMessage(Component.translatable("message.emilia.aura_on"), true);
        return true;
    }

    public static void showStatus(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("hud.emilia.status",
                Component.translatable(getSpell(p).translationKey()),
                (int) p.getData(ModAttachments.MANA).floatValue(), (int) MAX_MANA), true);
    }

    // ------------------------------------------------------------------- Puck

    private static Cat getPuck(ServerLevel level, ServerPlayer p) {
        UUID id = PUCKS.get(p.getUUID());
        if (id == null) return null;
        Entity e = level.getEntity(id);
        return (e instanceof Cat c && c.isAlive()) ? c : null;
    }

    private static boolean puckAlive(ServerLevel level, ServerPlayer p) {
        return getPuck(level, p) != null;
    }

    private static boolean togglePuck(ServerLevel level, ServerPlayer p) {
        Cat existing = getPuck(level, p);
        if (existing != null) {
            existing.discard();
            PUCKS.remove(p.getUUID());
            p.displayClientMessage(Component.translatable("message.emilia.puck_off"), true);
            return true;
        }
        Cat cat = EntityType.CAT.create(level);
        if (cat == null) return false;
        cat.moveTo(p.getX() + 1.0, p.getY(), p.getZ(), p.getYRot(), 0.0F);
        cat.setCustomName(Component.literal("Puck"));
        cat.setCustomNameVisible(true);
        cat.setTame(true, true);
        cat.setOwnerUUID(p.getUUID());
        cat.setVariant(BuiltInRegistries.CAT_VARIANT.getHolderOrThrow(CatVariant.BRITISH_SHORTHAIR));
        cat.setCollarColor(DyeColor.LIGHT_BLUE);
        cat.setInvulnerable(true);
        cat.setPersistenceRequired();
        cat.addTag(FrostUtil.PUCK_TAG);
        PUCKS.put(p.getUUID(), cat.getUUID());
        level.addFreshEntity(cat);
        level.playSound(null, cat.blockPosition(), SoundEvents.CAT_AMBIENT, SoundSource.NEUTRAL, 1.0F, 1.5F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, cat.getX(), cat.getY() + 0.5, cat.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
        p.displayClientMessage(Component.translatable("message.emilia.puck_on"), true);
        return true;
    }

    private static void tickPuck(ServerPlayer p, ServerLevel level) {
        UUID id = PUCKS.get(p.getUUID());
        if (id == null) return;
        Entity e = level.getEntity(id);
        if (!(e instanceof Cat cat) || !cat.isAlive()) {
            // Either in another dimension or gone.
            if (e == null && p.getServer() != null) {
                for (ServerLevel other : p.getServer().getAllLevels()) {
                    Entity o = other.getEntity(id);
                    if (o != null) {
                        o.discard();
                        p.displayClientMessage(Component.translatable("message.emilia.puck_wrong_dim"), true);
                    }
                }
            }
            PUCKS.remove(p.getUUID());
            return;
        }
        if (cat.distanceToSqr(p) > 24 * 24) {
            cat.moveTo(p.getX(), p.getY(), p.getZ());
            cat.getNavigation().stop();
        }
        long now = level.getGameTime();
        Long last = PUCK_SHOTS.get(p.getUUID());
        if (last != null && now - last < 25) return;
        List<Monster> foes = level.getEntitiesOfClass(Monster.class, cat.getBoundingBox().inflate(16.0),
                m -> m.isAlive() && cat.hasLineOfSight(m));
        foes.sort(Comparator.comparingDouble(m -> m.distanceToSqr(cat)));
        if (foes.isEmpty()) return;
        LivingEntity target = foes.get(0);
        Arrow a = EntityType.ARROW.create(level);
        if (a == null) return;
        a.setOwner(cat);
        a.setPos(cat.getX(), cat.getEyeY(), cat.getZ());
        Vec3 d = target.getEyePosition().subtract(a.position());
        a.shoot(d.x, d.y, d.z, 2.5F, 1.0F);
        a.setBaseDamage(4.0);
        a.pickup = AbstractArrow.Pickup.DISALLOWED;
        a.setNoGravity(true);
        a.addTag(FrostUtil.SPEAR_TAG);
        level.addFreshEntity(a);
        level.playSound(null, cat.blockPosition(), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.4F);
        PUCK_SHOTS.put(p.getUUID(), now);
    }

    // ------------------------------------------------------------------- tick

    public static void tick(ServerPlayer p) {
        if (!isAwakened(p)) return;
        ServerLevel level = (ServerLevel) p.level();
        BlockPos bp = p.blockPosition();

        // Cold immunity.
        if (p.getTicksFrozen() > 0) p.setTicksFrozen(0);

        // Frost walking: water under your feet freezes. Sneak to swim instead.
        if (p.tickCount % 2 == 0 && !p.isShiftKeyDown()) frostWalk(p, level);

        // Mana.
        float mana = p.getData(ModAttachments.MANA);
        boolean aura = p.getData(ModAttachments.AURA);
        float regen = 0.1F;
        if (level.getBiome(bp).value().coldEnoughToSnow(bp)) regen += 0.1F;
        if (PUCKS.containsKey(p.getUUID())) regen += 0.05F;
        if (aura && !p.getAbilities().instabuild) regen -= 0.2F;
        mana = Mth.clamp(mana + regen, 0.0F, MAX_MANA);
        p.setData(ModAttachments.MANA, mana);
        if (aura && mana <= 0.0F) {
            p.setData(ModAttachments.AURA, false);
            p.displayClientMessage(Component.translatable("message.emilia.aura_off"), true);
            aura = false;
        }

        // Ice Blade.
        int blade = p.getData(ModAttachments.BLADE);
        if (blade > 0) {
            p.setData(ModAttachments.BLADE, blade - 1);
            if (p.tickCount % 2 == 0) {
                Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.4, 0);
                level.sendParticles(ParticleTypes.SNOWFLAKE, hand.x, hand.y, hand.z, 2, 0.15, 0.15, 0.15, 0.0);
            }
        }

        // Winter's Presence.
        if (aura) {
            if (p.tickCount % 5 == 0) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.0, p.getZ(), 10, 4.0, 1.0, 4.0, 0.02);
            }
            if (p.tickCount % 10 == 0) {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8.0),
                        e -> !FrostUtil.isAlly(e, p) && e.distanceToSqr(p) <= 64.0)) {
                    FrostUtil.chill(e, 60);
                }
                for (int i = 0; i < 8; i++) {
                    int dx = level.random.nextInt(17) - 8;
                    int dz = level.random.nextInt(17) - 8;
                    if (dx * dx + dz * dz > 64) continue;
                    FrostUtil.freezeColumn(level, bp.getX() + dx, bp.getZ() + dz, bp.getY(), false);
                    FrostUtil.overlayColumn(level, bp.getX() + dx, bp.getZ() + dz, bp.getY() - 3, bp.getY() + 3, FrostUtil.permanent(), 0.4F);
                }
                if (p.isOnFire()) p.clearFire();
            }
        }

        tickPuck(p, level);

        if (p.tickCount % 20 == 0 && (mana < MAX_MANA || aura)) showStatus(p);
    }

    private static void frostWalk(ServerPlayer p, ServerLevel level) {
        BlockPos base = BlockPos.containing(p.getX(), p.getY() - 0.5, p.getZ());
        // With permanentIce the path you walk across water stays solid ice; otherwise it is vanilla frosted ice that melts back.
        final boolean perm = FrostUtil.permanent();
        BlockState frosted = (perm ? Blocks.ICE : Blocks.FROSTED_ICE).defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, 0, -3), base.offset(3, 0, 3))) {
            if (!pos.closerToCenterThan(p.position(), 3.5)) continue;
            if (!level.getBlockState(pos.above()).isAir()) continue;
            BlockState st = level.getBlockState(pos);
            if (st.is(Blocks.WATER) && st.getFluidState().isSource()
                    && frosted.canSurvive(level, pos)
                    && level.isUnobstructed(frosted, pos, CollisionContext.empty())) {
                BlockPos fixed = pos.immutable();
                level.setBlockAndUpdate(fixed, frosted);
                if (!perm) level.scheduleTick(fixed, Blocks.FROSTED_ICE, Mth.nextInt(level.random, 60, 120));
            }
        }
    }

    // ---------------------------------------------------------------- cleanup

    public static void cleanup(ServerPlayer p) {
        COOLDOWNS.remove(p.getUUID());
        PUCK_SHOTS.remove(p.getUUID());
        UUID id = PUCKS.remove(p.getUUID());
        if (id != null && p.getServer() != null) {
            for (ServerLevel l : p.getServer().getAllLevels()) {
                Entity e = l.getEntity(id);
                if (e != null) e.discard();
            }
        }
    }

    public static void clearAll() {
        COOLDOWNS.clear();
        PUCKS.clear();
        PUCK_SHOTS.clear();
        Scheduler.clear();
    }
}
