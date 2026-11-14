# Ore compat

Prima Materia sifts, crushes and melts the materials other mods add, by item tag, so any mod that fills a convention tag is picked up with no code change. The material list is the union of the 26.2 and 26.3 lists of Forge and NeoForge in `Mod-Playbook/ore-matrix/PRIMA_COMPAT_LISTS.md` (`scripts/compat_materials.json`). `python scripts/make_compat.py` writes all of it.

## How it works
- **Recipes name tags.** A result written `#c:ingots/tin` stands for whichever item of that tag exists (lowest id, `minecraft` first). With no mod filling the tag it resolves to nothing and the recipe does nothing; its viewer slot stays empty. Inputs use tags the same way.
- **Sifting:** the raw material (or dust, or gem) of each ore material, from gravel, stone meal, pale sand or scoria by rarity and mesh tier (`recipe/sifting/<source>/compat_<material>.json`).
- **Crushing:** `#c:ores/<material>` to two of its raw material, dust or gem (`recipe/crushing/compat_<material>_ore.json`).
- **Melting and casting:** every metal has a molten fluid (`primamateria:molten_<metal>`, a real fluid with a bucket). Ingots and raw materials melt for 100 mB, dusts for 100 mB, nuggets for 11 mB; casting gives `#c:ingots/<metal>` (`recipe/melting/compat_*`, `recipe/casting/compat_*`).
- **Items without a convention tag** are listed in optional tag files (`data/c/tags/item/**`, `required: false`) so a missing mod leaves the tag as it was.
- All of it is datapack JSON: a pack edits or removes any entry, and adds a metal with a melting and a casting recipe naming a fluid.

## Materials
| Material | Kind | Forms | Mods |
|---|---|---|---|
| advanced_alloy | metal (molten fluid) | block, ingot | Energized Power |
| aero | gem | gem | AeroFlux |
| aerogold | metal (molten fluid) | ingot | AeroFlux |
| alexandrite | gem | block, gem, ore | Silent's Gems |
| allthemodium | metal (molten fluid) | block, dust, gem, ingot, nugget, ore, raw | AllTheModium |
| allthemodium_slate | ore or dust | ore | AllTheModium |
| aluminum | metal (molten fluid) | ore | GrowableOres, Skniro's GrowableOres |
| amber | gem | gem, ore | Biomes O' Plenty Legacy Reborn, Exotelcraft |
| ametrine | gem | gem, ingot, ore | Magic Vibe Decorations, Mythic Upgrades |
| ammolite | gem | block, gem, ore | Silent's Gems |
| apatite | ore or dust | ore | GrowableOres, Skniro's GrowableOres |
| aquamarine | gem | block, gem, ingot, ore | Magic Vibe Decorations, Mythic Upgrades, Silent's Gems |
| arsonist | metal (molten fluid) | ingot | Aerial Hell |
| azure_electrum | metal (molten fluid) | block, dust, ingot, nugget | Silent Gear |
| azurite | gem | gem, ore, raw | Aerial Hell, Crystals Overhauled |
| baronyte | ore or dust | ore | Occultism |
| blazing | gem | dust, gem | Powah!, Silent Gear |
| blazium | ore or dust | ore | Occultism |
| bort | gem | block, gem, ore | Silent Gear |
| bronze | metal (molten fluid) | block, ingot, nugget, raw | Ala Industrial, IngotCraft, Silent Gear |
| calorite | metal (molten fluid) | ore | GrowableOres, Skniro's GrowableOres |
| carnelian | gem | block, gem, ore | Silent's Gems |
| celestite | ore or dust | ore | Crystals Overhauled |
| chaos | ore or dust | block, ore | Silent's Gems |
| chimerite | gem | gem | Theurgy |
| cindrite | gem | block, gem, ore | Nerospace |
| cinnabar | ore or dust | ore | GrowableOres, Skniro's GrowableOres |
| citrine | gem | block, gem, ingot, ore | Crystals Overhauled, Magic Vibe Decorations, Mythic Upgrades, Silent's Gems |
| crystallite | ore or dust | ore | Occultism |
| crystallized_alloy | metal (molten fluid) | ingot | Energized Power |
| crystallized_lapis_lazuli | gem | gem | Energized Power |
| cupronickel | metal (molten fluid) | block, ingot | Ala Industrial |
| desh | metal (molten fluid) | ore | GrowableOres, Skniro's GrowableOres |
| dragonyst | ore or dust | dust | Occultism |
| electrum | metal (molten fluid) | block, ingot | Ala Industrial |
| enderite | metal (molten fluid) | block, ingot, ore | Enderite Mod, IngotCraft |
| enderium | metal (molten fluid) | ingot, nugget, ore, raw | Immersive Ores, ImmersiveOres |
| energized_alloy | metal (molten fluid) | ingot | Energized Power |
| fluix | gem | dust, gem | Applied Energistics 2, Occultism |
| fluorite | ore or dust | ore | Aerial Hell |
| fluxsteel | metal (molten fluid) | ingot | AeroFlux |
| garnet | gem | block, gem, ore | Silent's Gems |
| glacite | gem | block, gem, ore | Nerospace |
| heliodor | gem | block, gem, ore | Crystals Overhauled, Silent's Gems |
| iesnium | metal (molten fluid) | block, dust, ingot, nugget, ore, raw | Occultism |
| invar | metal (molten fluid) | block, ingot | Ala Industrial |
| iolite | gem | block, gem, ore | Silent's Gems |
| jade | gem | gem, ingot, ore | Mythic Upgrades, Occultism |
| kyanite | gem | block, gem, ore | Silent's Gems |
| lead | metal (molten fluid) | block, ingot, nugget, ore, raw | GrowableOres, IngotCraft, Skniro's GrowableOres |
| malachite | ore or dust | ore | Biomes O' Plenty Legacy Reborn, Crystals Overhauled |
| mercury | gem | gem | Theurgy |
| moldavite | gem | block, gem, ore | Silent's Gems |
| necoium | metal (molten fluid) | ingot, ore, raw | Mythic Upgrades |
| neptunium | metal (molten fluid) | block, ingot, nugget | Aquaculture 2 |
| nerosium | metal (molten fluid) | block, dust, ingot, ore, raw | Nerospace |
| nerosteel | metal (molten fluid) | block, ingot, ore, raw | Nerospace |
| netherite_alloy | metal (molten fluid) | block, ingot | Ala Industrial |
| nickel | metal (molten fluid) | block, dust, ingot, ore, raw | Ala Industrial, GrowableOres, Skniro's GrowableOres |
| niter | ore or dust | ore | GrowableOres, Skniro's GrowableOres |
| onyx | ore or dust | ore | Crystals Overhauled |
| opal | gem | block, gem, ore | Exotelcraft, Silent's Gems |
| ostrum | metal (molten fluid) | ore | GrowableOres, Skniro's GrowableOres |
| palladium | metal (molten fluid) | block, dust, ingot, ore, raw | Ala Industrial |
| peridot | gem | block, gem, ingot, ore | Biomes O' Plenty Legacy Reborn, Crystals Overhauled, Mythic Upgrades, Silent's Gems |
| redstone_alloy | metal (molten fluid) | ingot | Energized Power |
| rhodochrosite | ore or dust | ore | Crystals Overhauled |
| rubber | ore or dust | raw | Ala Industrial |
| rubellite | gem | gem | Magic Vibe Decorations |
| ruby | gem | block, gem, ingot, ore, raw | Aerial Hell, Biomes O' Plenty Legacy Reborn, Exotelcraft, GrowableOres, Mythic Upgrades, Mythic Upgrades +1, Silent's Gems |
| saltpeter | ore or dust | dust | Resource Cracker/Tools |
| sapphire | gem | block, gem, ingot, ore | Biomes O' Plenty Legacy Reborn, Crystals Overhauled, GrowableOres, Mythic Upgrades, Silent's Gems, Skniro's GrowableOres |
| shielding_alloy | metal (molten fluid) | block, ingot | Ala Industrial |
| silicon | ore or dust | block | Energized Power |
| silver | metal (molten fluid) | block, dust, ingot, nugget, ore, raw | Ala Industrial, GrowableOres, IngotCraft, Occultism, Silent Gear, Silent's Gems, Skniro's GrowableOres |
| soul_heart | gem | gem | Baubley Heart Canisters |
| starmetal | ore or dust | dust | Silent Gear |
| steel | metal (molten fluid) | block, dust, ingot, nugget, raw | Barely Powered, BarelyPowered, Energized Power, IngotCraft, Resource Cracker/Tools, Silent Gear |
| sulfur | ore or dust | dust, ore, raw | Ala Industrial, GrowableOres, Resource Cracker/Tools, Skniro's GrowableOres |
| tanzanite | gem | block, gem, ore | Biomes O' Plenty Legacy Reborn, Silent's Gems |
| tin | metal (molten fluid) | block, dust, ingot, nugget, ore, raw | Ala Industrial, Energized Power, GrowableOres, IngotCraft, Skniro's GrowableOres |
| titanium | metal (molten fluid) | block, ingot, nugget, ore, raw | Antarchy |
| topaz | gem | block, gem, ingot, ore | Biomes O' Plenty Legacy Reborn, Mythic Upgrades, Silent's Gems |
| turquoise | gem | block, gem, ore | Silent's Gems |
| unobtainium | metal (molten fluid) | block, dust, gem, ingot, nugget, ore, raw | AllTheModium |
| unobtainium_allthemodium_alloy | ore or dust | block, dust, ingot | AllTheModium |
| unobtainium_vibranium_alloy | ore or dust | block, dust, ingot | AllTheModium |
| uraninite | ore or dust | block, ingot, ore, raw | GrowableOres, Powah!, Skniro's GrowableOres, Theurgy |
| uranium | metal (molten fluid) | block, dust, ingot, nugget, ore, raw | Ala Industrial, Antarchy, GrowableOres, Skniro's GrowableOres |
| vibranium | metal (molten fluid) | block, dust, gem, ingot, nugget, ore, raw | AllTheModium, Immersive Ores, ImmersiveOres |
| vibranium_allthemodium_alloy | ore or dust | block, dust, ingot | AllTheModium |
| volucite | ore or dust | ore, raw | Aerial Hell |
| vulpus | metal (molten fluid) | ingot, nugget, ore, raw | Immersive Ores, ImmersiveOres |
| witherite | ore or dust | dust | Occultism |
| zurite | metal (molten fluid) | block, ingot | Kuro Materials |
