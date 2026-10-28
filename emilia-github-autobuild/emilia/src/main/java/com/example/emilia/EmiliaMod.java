package com.example.emilia;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(EmiliaMod.MODID)
public class EmiliaMod {
    public static final String MODID = "emilia";

    public EmiliaMod(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, EmiliaConfig.SPEC);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
    }
}
