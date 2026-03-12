# 2.3.1
- Make the cave biome heigh be larger for the 3 cave biome & the same for the 3. This should fix deed_dark being much more common than the 2 others one.

# 2.3.0
- Support from 1.21.5 to 1.21.11.
- Underilla now use Paper biome API and won't be compatible with Spigot.
- Drop BiomeUtils dependency for Paper API to support a larger range of version (without NMS). It fix version 2.2.1 & 2.2.2 not working in 1.21.11.

# 2.2.3
- Support from 1.21.3 to 1.21.10. This is the last version to support 1.21.4 and previous version.

# 2.2.2
- Clean tasks are now merged into the main tasks.

# 2.2.1
- Support from 1.21.3 to 1.21.11

# 2.2.0
- Support & replace blocks before the chunk is generated as part of the BlockPopulator
- Support from 1.21.3 to 1.21.10

# 2.1.9
- Support from 1.21.3 to 1.21.9

# 2.1.8
- generationArea X and Z coordinates can be set to auto and will be auto computed.

# 2.1.7
- Support from 1.21.3 to 1.21.8

# 2.1.6
- Support 1.21.5 & 1.21.6. (From 1.21.3 to 1.21.6)

# 2.1.2
- Add customizable post step actions.

# 2.1.1
- Add functions to edit biomes, blocks or entities at the end of the world generation.
- Publish to maven central gradle config (Change the group for that & remove Underilla-Spigot directory).

# 2.0.13
- Performance improvement for NONE strategy.

# 2.0.11
- Remove a custom yaml dependency

# 2.0.10
- Read `biomesMerging.fromCavesGeneration.enabled` correctly from config.
- Fix cave biome that where over surface on eroded biome.

# 2.0.9
- Config: Aera values will be swap if min > max.