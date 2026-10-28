package com.example.emilia;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/** A long ice lance: extra reach in melee, or right-click to hurl a piercing ice spear that leaves crystal behind. */
public class GlacialSpearItem extends Item implements EmiliaWeapon {
    private static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 7.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.7, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath(EmiliaMod.MODID, "spear_reach"),
                                1.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public GlacialSpearItem(Properties props) {
        super(props.durability(300).attributes(attributes()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        ServerLevel sl = (ServerLevel) level;
        Arrow a = EntityType.ARROW.create(sl);
        if (a == null) {
            return InteractionResultHolder.fail(stack);
        }
        a.setOwner(player);
        a.setPos(player.getEyePosition());
        a.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.5F, 0.0F);
        a.setBaseDamage(8.0);
        a.setPierceLevel((byte) 3);
        a.pickup = AbstractArrow.Pickup.DISALLOWED;
        a.setNoGravity(true);
        a.addTag(FrostUtil.SPEAR_TAG);
        a.addTag(FrostUtil.GLACIAL_TAG);
        sl.addFreshEntity(a);
        sl.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);
        player.getCooldowns().addCooldown(this, 20);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        Arms.decay(stack, level, entity);
    }
}
