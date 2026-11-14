**Crush it, sieve it, soak it, melt it. A skyblock resource mod where every material comes from breaking matter down and sorting it.**

Prima Materia is a skyblock resource mod for Forge and NeoForge on Minecraft 26.2 and 26.3. You start on a small island in the void with one tree. Hammer leaves into fiber, shake gravel through a sieve, soak stone meal in a barrel, grow crystals in brine, melt dust in a crucible and cast it into ingots. Drives shake the sieves for you. Every recipe and every drop table is a datapack file, so packs can change all of it. Wiki: https://itmecasey.com/wiki/primamateria/

## Features

- **The Sieve.** Holds one mesh and a load of one to four blocks. Shake it by hand or let a drive do it, and watch the fines fall through and the finds appear on the mesh. Five mesh tiers (cloth, copper, iron, diamond, netherite) decide what you can find.
- **Hammers.** Six tiers, wooden to netherite. Crush leaves, cobblestone, gravel and stone into fiber, gravel, sand and stone meal. Higher tiers give bonus results.
- **The Barrel.** Wooden and clay. Collects dew and rain, composts plants into dirt, soaks stone meal into clay, steeps salt into brine and grows quartz, amethyst, lapis, diamond and emerald crystals from seeds. The clay barrel also holds lava.
- **The Crucible and the Casting Tray.** Fire a crucible, melt stone into lava and dust into molten copper, iron and gold, pour it into a casting tray and take the ingot.
- **Drives.** Windup, furnace and powered (FE) drives go under a sieve. The Auto Hammer sits on a drive and crushes blocks fed to it. Hoppers feed and empty the machines.
- **Nether and End materials.** Crush netherrack and end stone, sift them, soak scoria in brine for soul sand, and reach netherite.
- **Void Island world type** with a start island.
- **Everything is data.** Recipes, drop tables, mesh tiers, heat sources, melts and compost values are datapack JSON. Add, change or remove any of them with a datapack, KubeJS or ModJS.
- **Settings file** for sieve speed, drop multiplier, drive speeds, barrel timings and crucible rates.

## Compatibility

| Mod | NeoForge 26.2 | Forge 26.2 | NeoForge 26.3 | Forge 26.3 |
|---|---|---|---|---|
| BEI (Barely Enough Items) | yes | yes | yes | yes |
| JEI | yes | when JEI ports | yes | when JEI ports |
| REI | yes | when REI ports | yes | when REI ports |
| EMI | when EMI ports | when EMI ports | when EMI ports | when EMI ports |
| Show Me What This Is | yes | yes | yes | yes |
| Jade | yes | when Jade ports | yes | when Jade ports |
| ModJS | yes | yes | yes | yes |
| KubeJS | datapack JSON and recipes; event helper when KubeJS ports | same | same | same |

- **Recipe viewers:** sifting, crushing, composting, barrel, melting and casting each have their own category with the catalyst, the mesh tier or hammer tier it needs, and the chance and count of every drop. Every siftable block has a drop table page with one column per mesh tier. Click any slot to look it up.
- **In-world info:** Show Me What This Is and Jade show the state of the sieve, barrel, crucible, casting tray, drives and Auto Hammer.
- **Scripting:** ModJS gets a `PrimaMateria` global with events (sifted, crushed, barrel finished, melted, cast, drive wound), read access to machine state and calls to add, change or remove recipes and data. KubeJS edits every recipe type and data file directly.

## FAQ

**Does it need any other mod?** No. Every integration above is optional.

**Where do the numbers live?** In datapack files and in `config/primamateria.properties`. Packs can change every chance, count, time and tier.

**Which versions?** Minecraft 26.2 and 26.3, Forge and NeoForge, one jar each. No Fabric, no older versions.

**Can I use it in a modpack?** Yes. Source and license: https://github.com/ItMeCasey/PrimaMateria (MIT)
