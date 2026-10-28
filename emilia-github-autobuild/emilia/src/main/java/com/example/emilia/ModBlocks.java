package com.example.emilia;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EmiliaMod.MODID);

    public static final DeferredBlock<SpiritIceBlock> SPIRIT_ICE = BLOCKS.registerBlock("spirit_ice",
            SpiritIceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.5F)
                    .friction(0.98F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(s -> 4)
                    .noLootTable()
                    .isValidSpawn((s, l, p, t) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false));

    public static final DeferredBlock<IceSpikeBlock> ICE_SPIKE = BLOCKS.registerBlock("ice_spike",
            IceSpikeBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.3F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .noCollission()
                    .lightLevel(s -> 3)
                    .noLootTable()
                    .pushReaction(PushReaction.DESTROY));

    public static final DeferredBlock<GlacialIceBlock> GLACIAL_ICE = BLOCKS.registerBlock("glacial_ice",
            GlacialIceBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(1.5F)
                    .friction(0.98F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .lightLevel(s -> 5)
                    .isValidSpawn((s, l, p, t) -> false)
                    .isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false));

    public static final DeferredBlock<IceCrystalBlock> ICE_CRYSTAL = BLOCKS.registerBlock("ice_crystal",
            IceCrystalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(1.0F)
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .noOcclusion()
                    .lightLevel(s -> 8));

    public static final DeferredBlock<FrostFilmBlock> FROST_FILM = BLOCKS.registerBlock("frost_film",
            p -> new FrostFilmBlock(p, false), filmProperties());

    public static final DeferredBlock<FrostFilmBlock> HOARFROST = BLOCKS.registerBlock("hoarfrost",
            p -> new FrostFilmBlock(p, true), filmProperties());

    private static BlockBehaviour.Properties filmProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .replaceable()
                .noCollission()
                .noOcclusion()
                .strength(0.2F)
                .sound(SoundType.GLASS)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable();
    }

    private ModBlocks() {}
}
