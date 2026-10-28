package com.example.emilia;

import com.mojang.brigadier.arguments.BoolArgumentType;
import java.util.Collection;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = EmiliaMod.MODID)
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Scheduler.tick();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SpiritManager.tick(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            SpiritManager.cleanup(sp);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SpiritManager.clearAll();
    }

    /** Remove orphaned Pucks left over from a previous session. */
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (!event.getLevel().isClientSide && e.getTags().contains(FrostUtil.PUCK_TAG)
                && !SpiritManager.isKnownPuck(e.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();

        // Emilia is immune to cold.
        if (victim instanceof ServerPlayer sp && SpiritManager.isAwakened(sp) && src.is(DamageTypes.FREEZE)) {
            event.setCanceled(true);
            return;
        }

        // Awakened wielders get more out of Emilia's ice weapons.
        if (src.getEntity() instanceof ServerPlayer holder && src.getDirectEntity() == holder
                && SpiritManager.isAwakened(holder) && holder.getMainHandItem().getItem() instanceof EmiliaWeapon
                && !FrostUtil.isAlly(victim, holder)) {
            event.setAmount(event.getAmount() + 2.0F);
        }

        // Ice Blade: melee strikes hit harder and freeze.
        if (src.getEntity() instanceof ServerPlayer att && src.getDirectEntity() == att
                && att.getData(ModAttachments.BLADE) > 0 && !FrostUtil.isAlly(victim, att)) {
            event.setAmount(event.getAmount() + 4.0F);
            FrostUtil.chill(victim, 100);
            if (victim.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
                        victim.getZ(), 20, 0.3, 0.4, 0.3, 0.1);
                sl.playSound(null, victim.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile proj = event.getProjectile();
        if (proj.level().isClientSide || !proj.getTags().contains(FrostUtil.SPEAR_TAG)) return;
        if (!(proj.level() instanceof ServerLevel level)) return;
        HitResult hit = event.getRayTraceResult();

        if (hit instanceof EntityHitResult ehr) {
            Entity target = ehr.getEntity();
            Entity owner = proj.getOwner();
            if (target.getTags().contains(FrostUtil.PUCK_TAG)
                    || (owner instanceof ServerPlayer sp && FrostUtil.isAlly(target, sp))) {
                event.setCanceled(true); // spears pass through allies
                return;
            }
            if (target instanceof LivingEntity le) FrostUtil.chill(le, 100);
        }

        Vec3 loc = hit.getLocation();
        BlockPos at = BlockPos.containing(loc);
        boolean glacial = proj.getTags().contains(FrostUtil.GLACIAL_TAG);
        int rad = glacial ? 3 : 2;
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (dx * dx + dz * dz <= rad * rad + 1) {
                    FrostUtil.freezeColumn(level, at.getX() + dx, at.getZ() + dz, at.getY(), false);
                }
            }
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, loc.x, loc.y, loc.z, 20, 0.3, 0.3, 0.3, 0.1);
        FrostUtil.overlayArea(level, at, rad, FrostUtil.permanent(), 0.6F);
        level.playSound(null, at, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6F, 1.8F);
        if (glacial && hit instanceof net.minecraft.world.phys.BlockHitResult bhr
                && hit.getType() == HitResult.Type.BLOCK && FrostUtil.permanent()) {
            BlockPos cp = bhr.getBlockPos().relative(bhr.getDirection());
            if (level.getBlockState(cp).canBeReplaced()) {
                level.setBlock(cp, ModBlocks.ICE_CRYSTAL.get().defaultBlockState(), 3);
            }
        }
        if (hit.getType() == HitResult.Type.BLOCK) {
            Scheduler.schedule(3, () -> {
                if (!proj.isRemoved()) proj.discard();
            });
        }
    }

    /** Frosty trail on ice spears, and they never linger long. */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e instanceof AbstractArrow arrow && arrow.level() instanceof ServerLevel level
                && arrow.getTags().contains(FrostUtil.SPEAR_TAG)) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, arrow.getX(), arrow.getY(), arrow.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
            if (arrow.tickCount > 80) arrow.discard();
        }
    }

    /** /emilia awaken <players> <true|false> */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("emilia")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("awaken")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            for (ServerPlayer p : players) {
                                                p.setData(ModAttachments.AWAKENED, enabled);
                                                if (enabled) p.setData(ModAttachments.MANA, SpiritManager.MAX_MANA);
                                                else {
                                                    p.setData(ModAttachments.AURA, false);
                                                    SpiritManager.cleanup(p);
                                                }
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.translatable(
                                                    "commands.emilia.awaken", players.size(), enabled), true);
                                            return players.size();
                                        })))));
    }
}
