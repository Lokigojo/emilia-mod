package com.example.emilia;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, EmiliaMod.MODID);

    public static final DeferredHolder<MobEffect, FrostbiteEffect> FROSTBITE =
            EFFECTS.register("frostbite", FrostbiteEffect::new);

    private ModEffects() {}
}
