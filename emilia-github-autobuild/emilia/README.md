# Emilia: Spirit Arts (NeoForge 1.21.1)

Unofficial Re:Zero fan mod. Only Emilia's abilities: ice magic, Puck, ice weaponry and a frozen, reshaped environment.

## Setup
Copy `src/` over the official NeoForge 1.21.1 MDK's `src/` (easiest), or use the included Gradle files plus a wrapper
(`gradle wrapper`), and set `neo_version` to the latest 21.1.x. Needs JDK 21. `./gradlew runClient` / `./gradlew build`.

## Getting the power
Craft **Puck's Crystal** (snowballs in the corners, diamonds on the edges, nether star in the center) and use it.
Admins: `/emilia awaken <players> <true|false>`. Creative players have unlimited mana.

## Controls (rebindable)
R casts the selected spirit art. G selects the next art, Shift+G the previous one.

## Passives
- Immune to freeze damage and to your own cold.
- Frost Walker: water under your feet freezes (sneak to swim instead).
- Mana regenerates faster in cold biomes and while Puck is out.
- Ice weapons never melt in your hands, and hit 2 harder.

## Spirit arts
| Art | Mana | Effect |
|---|---|---|
| Icicle Line | 12 | spikes erupt along the ground, damaging and chilling |
| Ice Spear Barrage | 15 | five flying ice spears that freeze targets and frost the ground |
| Ice Blade | 20 | 30 s: melee hits deal +4 damage and freeze |
| Ice Wall | 25 | 7x4 wall of Spirit Ice |
| Ice Cage | 30 | encases a creature in Spirit Ice |
| Winter's Presence | toggle | chills enemies, freezes water, lays snow; drains mana |
| Contract: Puck | 20 | summons Puck, who shoots ice at hostiles; cast again to dismiss |
| Absolute Zero | 100 | the whole biome you stand in freezes over in 5 stages (about 10 s apart): permafrost terrain, frozen forests, deep ice, spires of glacial ice and crystal, mobs encased as each stage arrives |
| Forge: Hoarfrost Blade | 20 | conjures the sword |
| Forge: Glacial Spear | 25 | conjures the spear |
| Forge: Frostbreaker | 35 | conjures the maul |
| Glacial Surge | 45 | a glacier erupts forward: shatters trees, freezes the ground, raises permanent ridges |
| Frozen Shatter | 60 | flash-freezes a sphere of terrain then shatters it into a permanent crater with crystals |

## Ice weapons (also craftable from blue ice and sticks)
- **Hoarfrost Blade**: fast sword; every cut chills the target.
- **Glacial Spear**: long melee reach; right-click hurls a piercing spear that leaves an Ice Crystal.
- **Frostbreaker**: heavy maul; right-click the ground to slam: shockwave, small crater, ring of spikes.
Weapons are sustained for awakened players. Anyone else sees them melt: not in cold biomes, slowly elsewhere, fast in the Nether.

## Blocks
Walls, cages, icicle spikes and the Frost Walker path are now built from solid, permanent blocks (**Glacial Ice**, **Ice Crystal**, vanilla Ice) that stay until you mine them (they drop themselves).
Set `permanentIce = false` in the config to go back to melting Spirit Ice / Ice Spike / frosted ice.
Packed and blue ice are used for the frozen ground. Frostbite is the status effect.

## Frost overlay
Ice spells now lay a translucent layer of frost over the faces of the blocks they touch (spear impacts, icicle spikes, walls,
cages, Winter's Presence, Glacial Surge, Frozen Shatter, the Frostbreaker slam and Absolute Zero). It clings like glow lichen.
With permanentIce on, spell frost is always **Hoarfrost** and stays; with it off, **Frost** melts away after 25-60 seconds. Neither is obtainable as an item.

## Config (`emilia-server.toml`)
`permanentIce` (default true; spell ice stays forever), `terrainDestruction` (default true; turn off on shared servers), `shatterRadius` (3-10), `absoluteZeroMaxRadius` (32-512, default 192),
`absoluteZeroIntervalTicks` (20-1200, default 200 = 10 s between stages), `absoluteZeroColumnsPerTick` (20-2000, default 250).
Absolute Zero only reaches chunks that are loaded when it is cast, so cast it near the middle of the biome or raise your view distance.
With terrainDestruction off, Surge/Shatter refuse to cast and Absolute Zero only freezes surfaces.
Terrain spells skip containers and anything harder than 10; they do not respect land-claim mods.
