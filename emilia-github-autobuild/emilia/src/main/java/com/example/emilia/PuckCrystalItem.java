package com.example.emilia;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Puck's crystal. Use it to take the contract and gain Emilia's spirit arts. */
public class PuckCrystalItem extends Item {
    public PuckCrystalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player.getData(ModAttachments.AWAKENED)) {
            player.displayClientMessage(Component.translatable("message.emilia.already"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.setData(ModAttachments.AWAKENED, true);
        player.setData(ModAttachments.SPELL, 0);
        player.setData(ModAttachments.MANA, SpiritManager.MAX_MANA);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ServerLevel sl = (ServerLevel) level;
        sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        sl.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 1.0, player.getZ(),
                100, 0.7, 1.0, 0.7, 0.1);
        player.displayClientMessage(Component.translatable("message.emilia.awakened"), false);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
