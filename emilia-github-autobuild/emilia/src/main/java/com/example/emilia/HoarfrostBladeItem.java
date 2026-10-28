package com.example.emilia;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;

/** Emilia's ice sword: fast, and every cut chills the target. */
public class HoarfrostBladeItem extends SwordItem implements EmiliaWeapon {
    public HoarfrostBladeItem(Properties props) {
        super(ModTiers.ICE, props.attributes(SwordItem.createAttributes(ModTiers.ICE, 3, -2.2F)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel sl) {
            FrostUtil.chill(target, 120);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(), 12, 0.3, 0.4, 0.3, 0.08);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        Arms.decay(stack, level, entity);
    }
}
