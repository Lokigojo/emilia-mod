package com.example.emilia;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Forging and upkeep of Emilia's ice weapons. */
final class Arms {
    private Arms() {}

    static boolean forgeBlade(ServerLevel level, ServerPlayer p) {
        return give(level, p, new ItemStack(ModItems.HOARFROST_BLADE.get()));
    }

    static boolean forgeSpear(ServerLevel level, ServerPlayer p) {
        return give(level, p, new ItemStack(ModItems.GLACIAL_SPEAR.get()));
    }

    static boolean forgeHammer(ServerLevel level, ServerPlayer p) {
        return give(level, p, new ItemStack(ModItems.FROSTBREAKER.get()));
    }

    private static boolean give(ServerLevel level, ServerPlayer p, ItemStack stack) {
        if (p.getMainHandItem().isEmpty()) {
            p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        } else if (!p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 0.4, 0.5, 0.05);
        p.displayClientMessage(Component.translatable("message.emilia.forged"), true);
        return true;
    }

    /**
     * Ice weapons are sustained by an awakened wielder. For anyone else they slowly melt:
     * not at all in cold biomes, steadily elsewhere, and fast in the Nether.
     */
    static void decay(ItemStack stack, Level level, Entity holder) {
        if (level.isClientSide || level.getGameTime() % 20 != 0 || !(holder instanceof Player player)) return;
        if (player.getData(ModAttachments.AWAKENED)) return;
        BlockPos pos = holder.blockPosition();
        int amount;
        if (level.dimensionType().ultraWarm()) {
            amount = 6;
        } else if (level.getBiome(pos).value().coldEnoughToSnow(pos)) {
            amount = 0;
        } else {
            amount = 1;
        }
        if (amount == 0) return;
        stack.setDamageValue(stack.getDamageValue() + amount);
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            stack.shrink(1);
            level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
            player.displayClientMessage(Component.translatable("message.emilia.weapon_shatters"), true);
        }
    }
}
