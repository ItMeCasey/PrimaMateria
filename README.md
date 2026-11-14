# Prima Materia

A skyblock resource mod for Minecraft 26.2 and 26.3 on Forge and NeoForge. You start on a small island in the void and make everything from first matter: crush stone with a hammer, shake it through a sieve, soak and grow it in a barrel, melt and cast it in a crucible, and let wound-up and fired drives do the shaking for you.

By ItMeCasey Studios. Mod id `primamateria`. First release planned as `2026.11.1`.

**Status: scaffold.** The four modules build and every block and item exists as a placeholder; no gameplay yet. See [PROJECT_STATUS.md](PROJECT_STATUS.md).

## Design documents

- [docs/DESIGN.md](docs/DESIGN.md): the game. Loop, progression, every block, item, recipe family and drop table.
- [docs/SIFTING.md](docs/SIFTING.md): the sifting animation, rendering, sound and performance budget.
- [docs/ART.md](docs/ART.md): the 16x art direction, palette, models and textures to make, the icon.
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): modules, data formats, ModJS and BEI, tests, the work split.

## Build

Each Forge or NeoForge folder is its own Gradle build (Java 25): `forge-26.2`, `neoforge-26.2`, `forge-26.3`, `neoforge-26.3`. Shared pure logic lives in `common/`; everything that touches the game lives in `version-26.2`, and `version-26.3` is generated from it by `scripts/sync_versions.py`. Developer notes are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Clean-room rule

Prima Materia is an original design. Do not open, read, search or copy the code, textures, models, language files or data of any other sifting or skyblock resource mod while working on it, and do not download their jars. Ideas are free; their files are not ours. See "Originality" in the design document.

## License

MIT, see [LICENSE](LICENSE).
