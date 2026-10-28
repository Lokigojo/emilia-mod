package com.example.emilia;

/** Emilia's spirit arts. */
public enum Spell {
    ICICLE_LINE("icicle_line", 12, 20),
    ICE_SPEAR("ice_spear", 15, 15),
    ICE_BLADE("ice_blade", 20, 40),
    ICE_WALL("ice_wall", 25, 40),
    ICE_CAGE("ice_cage", 30, 60),
    FROST_AURA("frost_aura", 0, 20),
    PUCK("puck", 20, 40),
    ABSOLUTE_ZERO("absolute_zero", 100, 1200),
    HOARFROST_BLADE("hoarfrost_blade", 20, 100),
    GLACIAL_SPEAR("glacial_spear", 25, 100),
    FROSTBREAKER("frostbreaker", 35, 100),
    GLACIAL_SURGE("glacial_surge", 45, 200),
    FROZEN_SHATTER("frozen_shatter", 60, 300);

    public final String id;
    public final int cost;
    public final int cooldown;

    Spell(String id, int cost, int cooldown) {
        this.id = id;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public String translationKey() {
        return "spell.emilia." + id;
    }

    public static Spell byIndex(int i) {
        Spell[] all = values();
        return all[Math.floorMod(i, all.length)];
    }
}
