package com.example.emilia;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.SimpleTier;

public final class ModTiers {
    public static final Tier ICE = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 300, 7.0F, 3.0F, 18,
            () -> Ingredient.of(Blocks.PACKED_ICE));

    private ModTiers() {}
}
