package com.example.emilia;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EmiliaMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EmiliaMod.MODID);

    public static final DeferredItem<Item> PUCK_CRYSTAL = ITEMS.register("puck_crystal",
            () -> new PuckCrystalItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<Item> HOARFROST_BLADE = ITEMS.register("hoarfrost_blade",
            () -> new HoarfrostBladeItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> GLACIAL_SPEAR = ITEMS.register("glacial_spear",
            () -> new GlacialSpearItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<Item> FROSTBREAKER = ITEMS.register("frostbreaker",
            () -> new FrostbreakerItem(new Item.Properties().rarity(Rarity.EPIC)));

    static {
        ITEMS.registerSimpleBlockItem(ModBlocks.SPIRIT_ICE);
        ITEMS.registerSimpleBlockItem(ModBlocks.ICE_SPIKE);
        ITEMS.registerSimpleBlockItem(ModBlocks.GLACIAL_ICE);
        ITEMS.registerSimpleBlockItem(ModBlocks.ICE_CRYSTAL);
    }

    public static final Supplier<CreativeModeTab> TAB = TABS.register("emilia",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.emilia"))
                    .icon(() -> new ItemStack(PUCK_CRYSTAL.get()))
                    .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
                    .build());

    private ModItems() {}
}
