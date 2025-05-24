[download]: https://img.shields.io/github/downloads/HydrolienF/Underilla/total
[downloadLink]: https://hangar.papermc.io/Hydrolien/Underilla
[discord-shield]: https://img.shields.io/discord/728592434577014825?label=discord
[discord-invite]: https://discord.gg/RPNbtRSFqG


[ ![download][] ][downloadLink]
[ ![discord-shield][] ][discord-invite]

[**Discord**](https://discord.gg/RPNbtRSFqG) | [**Hangar**](https://hangar.papermc.io/Hydrolien/Underilla) | [**GitHub**](https://github.com/HydrolienF/Underilla) | [**Original author Github** (Outdated)](https://github.com/Jeshuakrc/Underilla)

# Underilla
Underilla is a minecraft plugin to 'merge' existing custom Minecraft word surfaces and vanilla undergrounds. It works by allowing the vanilla generation engine create chunks as normal, then intercepting the generator and forcing the surface of the original world, which works as a reference. In other worlds, Underilla generates a brand-new world with vanilla undergrounds, but cloning the surface of an already existing world.

It's original purpose is adding vanilla caves to custom [WorldPainter](https://www.worldpainter.net/) worlds, but it would perfectly work for any pre-generated world.

![Underilla](https://github.com/HydrolienF/Underilla/assets/71718798/5d4c0812-443e-42db-90cf-a138f11ec6c9)

## Main features
- Merge the original world surface and vanilla underground.
- Heightmap fixed. Underilla re-calculates heightmaps when merging chunks, getting rid of floating and buried structures. Vanilla villagers and other structures are placed at the right height.
- Biome overwrite. Biomes from the reference world will be transferred and overwrite biomes from de vanilla seed being used. Cave biomes underground will be preserved. Custom biomes are supported.
- Population. Trees, flowers, ores etc are placed back. Features can be customized with datapacks.
And many more options in the config to transform your custom world.

## Getting started
### Perquisites

- [Java 21](https://adoptium.net/temurin/releases/?version=21).
- A pre-generated world to use as a reference (Such as a [WorldPainter](https://www.worldpainter.net/) world).
- A [Paper](https://papermc.io/software/paper) (or forks) Minecraft Server. Supported Minecraft version are in the release name.

### Single player or non-Paper
Underilla is currently only implemented as a Paper plugin. It support datapacks and most plugins. It does not support mods and might be incompatible with some generation plugins that modify minecraft source code.
Once the map have been generated with a Paper server, you're free to use it in signle player or on modded server.

### Pregenerate
Underilla is significantly slower than the vanilla generator, as it doesn't rely only on noise generation but also on reading the reference world's region `nbt` files and analyzing its patterns to 'clone' its surface to a vanilla world.
So Underilla will pregenerate the world when you run it. You can disable auto generation in the config by disabling the generations steps.

## How to generate vanilla caves in a custom world with Underilla
This is a **step by step guide**, if you already did some steps you can move to the next ones.
If you are strugeling with world generation, you can ask for **help on the Discord**: https://discord.gg/RPNbtRSFqG

### Guide - Short version
This guide will help you to generate the 0, 0 to 512, 512 area as a first test of Underilla.

1.
    1. Download the latest paper version [here](https://papermc.io/downloads/paper).
    2. Create a new directory for your server and move the paper .jar file inside.
    3. Start the server in a terminal with `java -jar paper-1.21.4-222.jar -nogui` (Replace `1.21.4-222` by your paper version).
    4. Open `eula.txt`, set `eula=true` and restart the server.
2. 
    1. Download the latest Underilla version from the [releases](https://github.com/HydrolienF/Underilla/releases).
    2. Move the downloaded jar file to `plugins/` in your server directory.
    3. copy your custom world to `world_surface` in your server directory. (Only the `region/` sub directory of your world matters.)
3. 
   1. Restart the server again. Underilla will update some settings & download 2 needed plugins and stop the server.
   2. Restart the server again. Underilla is launched, you can join the server once it's running or after it have finished and check the result.

See the full guide below to generate other area than 0, 0 to 512, 512 & improve caves quality.

### Guide - Complete version

1. Setup a paper server
    1. Download the latest paper version [here](https://papermc.io/downloads/paper).
    2. Create a new directory for your server and move the paper .jar file inside.
    3. Create a `start.sh` on Linux or MacOS or a `start.bat` in Windows with `java -jar paper-1.21.4-222.jar -nogui` inside. (Replace `1.21.4-222` by your paper version.)
    4. On Linux & MacOS only, give exec perms to `start.sh` by running `chmod 700 start.sh`.
    5. Start the server in a terminal with `./start.sh` on Linux and MacOS or `./start.bat` on Windows.
    6. The server stops because of eula, open `eula.txt`, set `eula=true` and restart the server.
    7. If you want to have the same cave result for each of your generation try, you can edit `level-seed=` in `servers.properties` to a random number.
    8. You can edit the mob spawning settings or any other server config here if you want.
    9. You now have a vanilla ready to work server. Time to setup Underilla.
2. Setup Underilla
    1. Download the latest Underilla version from the [releases](https://github.com/HydrolienF/Underilla/releases).
    2. Move the downloaded jar file to `plugins/` in your server directory.
3. Setup your custom world
    1. copy your custom world to `world_surface` in your server directory. (Only the `region/` sub directory of your world matters.)
4. Configure Underilla
    1. Copy the config from [this file](https://github.com/HydrolienF/Underilla/blob/main/src/main/resources/config.yml) and save it as config.yml in `plugins/Underilla/`. The default config can also be initialized by running underilla, but copying it from the repo ensure that Underilla config is configured before Underilla starts.
    2. Edit `generationArea` inside `plugins/Underilla/config.yml` to match your surface world size. If you just want to test Underilla for a 1st generation, you can keep default values.
    3. You can read the other fields of the config and edit some of them. This steps can be done later after a 1st generation try, to customize your world generation.
    4. If you have already done a generation, you need to switch back some step (underillaGeneration, cleaningBlocks, cleaingEntities) from "done" to "todo".
5. Configure datapack
    1. If your custom world already have a datapack, you can move it to `world/datapacks/` to keep your custom biomes etc.
    2. If you don't have a datapack yet, you should create one from [vanilla biome files](https://github.com/misode/mcmeta/tree/data) where you have remove the features you don't want. For example if your custom surface world already have trees and most important, have caves high enought for your world.
    3. /!\ Make sure that cave will be generated under montains by having a high y level on your datapack. You can use [this basic datapack](https://github.com/HydrolienF/Underilla/tree/main/UnderillaBaseDataPack) to take care of the height or include it's files into your datapack. If you miss this step, there might be some empty space under montains in your world, but the generation will work.
6. Start **caves generation**
    1. Run the server again, this time the eula have been accepted, so the server will start. The 1st time you start the server Underilla will download it's dependencies to your `plugins/`, configure paper for faster world generation & set Underilla as world Generator in `bukkit.yml`. This steps can be disabled in the config.
    2. If you have set a start script, the server should restart automaticaly, if not you will have to restart it manually.
    3. The generation is now started, yopu just have to wait until it's done now. If you stop the server, the generation will restart the next time you start the server.
    You can explore the map while it's being generated to check how it's doing. Be aware that being on the server while the map is generated migth edit the world, even in spectator mod and might result in a sligtly different world generation because of water or falling sand being updated before the cleaning tasks. Best will be to stay out of the server for you last generation try.
    The generation process will be done in 3 steps:
        1. **Merging** your **surface world** to a new world with **vanilla caves**. This step merge blocks & biomes and generate new features, new structures & new mobs. It is by far the longest steps and it will takes hours for the biggest worlds.
        2. Cleaning the blocks of the final world.
        3. Cleaning the entities of the final world.
7. What's next
    1. Check that the world meet what you expected and redo the generation since step 4 if needed. (You should keep a save of `world` somewhere just in case.) Don't forget to switch back some step (underillaGeneration, cleaningBlocks, cleaingEntities) from "done" to "todo", else the generation won't be done again.
    2. You can now delete the `world_surface/` (You should keep a save somewhere just in case)
    3. You can now remove Underilla from `plugins/` & edit `bukkit.yml` to make `VoidWorldGenerator` your world generator. This will ensure that no chunk is generated by the vanilla generator outside of the final world area. If you wich to have a vanilla world merging with the generated world, you can remove the generator from `bukkit.yml`. Vanilla generator will try to merge it's custom world with the existing one. You can also add a datapack to have only ocean biome generated over the generated world.
    4. I hope Underilla will improve the cave experience of your players. If you find any bugs please report them in the [Github issues](https://github.com/HydrolienF/Underilla/issues).


## Known issues & workarounds

- Commands as `/locate` might timeout the server. This happens when Minecraft think that the structure should exist in the world, but you have disable that structure in Underilla config or if the generation area is to small and that structure haven't spawn.
- Olds map before 1.19 won't be load by Underilla. To use an old map, generate the full map without Underilla in the right version, then use the generated map. This will let minecraft update the map files and Underilla will be able to read them as expected.
- Chests content arent copied from custom world. This does not affect the structures chests generated by Underilla on the final world.
- Default Minecraft population will generate all vanilla features. Use a datapack in `world/datapacks/` to prevent that.
- There is a Paper issue on Windows with big world & many generation threads. To fix that switch `worker-threads: -1` in `config/paper-gobal.yml`.

## WorldPainter considerations
If you're going to plug your custom WorldPainter world into Underilla, consider before exporting:
- Disable caves, caverns, and chasms altogether, allow Underilla to take over that step. This is due to biome placement, every underground non-solid block in the surfaceWorld drags its biome over along with it, this interferes with proper underground vanilla biomes.
- Always disable the `Allow Minecraft to populate the entire terrain` option. Rather use the `vanillaPopulation` option in Underilla's `config.yml` file.
- Don't use the resource layer. Underilla will have the vanilla generator take care of that for you.
- The Populate layer has no effect. Weather all or none of the terrain will be populated based on the above point.

## Custom biome
Cave generation on custom biomes is working. Features (ores, flowers etc) & structures will be placed according to the custom surface world biome.

## Feature fiter
If you want to remove some of the game features, for example the `monster_room` you can create a datapack where you have customize witch feature can spawn in each biome. Underilla will generate feature according to your cusomized biome.
It can also be used to add feature to some biome. For example a quartz_ore feature if your nether is disabled you you still want your builders to have quartz.

## Performances
Huge map generation can takes hours or even days, here is some stats about performance to help you choose your configuration settings.
All tests have been done on paper 1.20.4 on a 1000*1000 map generation of the same world painter generated world with default settings except strategy. We can't garanty that your computer will be as fast as mine, but it should be enoth to imagine how much time your world will need.
- Minecraft Vanilla generator (No Underilla) 1:36
- None strategy 3:25 (2.13 times longer than Vanilla generation)
- Absolute stategy 4:34 (2.85 times longer than Vanilla generation)
- Surface srategy 4:32 (2.83 times longer than Vanilla generation)
- Relative strategy (have been removed since) 11:07 (6.94 times longer than Vanilla generation)

For a 50000 * 30000 world, it would take 40 hours to generate with Minecraft vanilla generator, 113 hours in surface strategie and 279 hours in relative.

## Build & test the plugin
Clone the [repo](https://github.com/HydrolienF/Underilla) `git clone git@github.com:HydrolienF/Underilla.git`

Create a working jar with `./gradlew assemble`.

Run a local paper server with the example map & datapack on Linux.
```sh
rm run/world_surface/ -fr; cp testMap/world/ run/world_surface/; rm -fr run/world/; mkdir -p run/world/datapacks; cp UnderillaBaseDataPack/ run/world/datapacks; ./gradlew runServer
```

Build for publication with `./gradlew clean zipStagingDeploy`

Feature requests or pull requests are welcome. Concider creating an issue first to talk about your new feature before sending a pull request.

## Thanks

Thanks a lot to [**Jeshuakrc**](https://github.com/Jeshuakrc) for creating this incredible project in 1.19.4 !
Since Minecraft 1.20, I'm maintaining the project. If you have any issues or have found a bug, please let me know [here](https://github.com/HydrolienF/Underilla/issues).
