package fr.formiko.mc.underilla.paper;

import fr.formiko.mc.underilla.core.generation.Generator;
import fr.formiko.mc.underilla.paper.cleaning.CleanBlocksTask;
import fr.formiko.mc.underilla.paper.cleaning.CleanEntitiesTask;
import fr.formiko.mc.underilla.paper.cleaning.FollowableProgressTask;
import fr.formiko.mc.underilla.paper.generation.GeneratorAccessor;
import fr.formiko.mc.underilla.paper.generation.UnderillaChunkGenerator;
import fr.formiko.mc.underilla.paper.impl.BukkitWorldReader;
import fr.formiko.mc.underilla.paper.io.Tools;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.BooleanKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.IntegerKeys;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.StringKeys;
import fr.formiko.mc.underilla.paper.listener.StructureEventListener;
import fr.formiko.mc.underilla.paper.listener.WorldListener;
import fr.formiko.mc.underilla.paper.preparing.ServerSetup;
import fr.formiko.mc.underilla.paper.selector.Selector;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.popcraft.chunky.Chunky;
import org.popcraft.chunky.ChunkyProvider;
import org.popcraft.chunky.api.event.task.GenerationProgressEvent;

public final class Underilla extends JavaPlugin {

    private UnderillaConfig underillaConfig;
    private BukkitWorldReader worldSurfaceReader;
    private @Nullable BukkitWorldReader worldCavesReader;
    public static final int CHUNK_SIZE = 16;
    public static final int REGION_SIZE = 512;
    public static final int BIOME_AREA_SIZE = 4;
    public static final long MS_PER_SECOND = 1000;
    private static final String TODO = "todo";
    private static final String DOING = "doing";
    private static final String DONE = "done";
    private static final String FAILED = "failed";
    private CleanBlocksTask cleanBlocksTask;
    private CleanEntitiesTask cleanEntitiesTask;
    private StructureEventListener structureEventListener;

    private Function<org.bukkit.block.Biome, org.bukkit.block.Biome> endBiomeTransformer;
    private Consumer<Block> endBlockTransformer;
    private Consumer<Entity> endEntityTransformer;
    private Map<StringKeys, Runnable> endTaskActions = new EnumMap<>(StringKeys.class);

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        if (allStepsDone()) {
            info("Use the out of the surface world generator instead of Underilla because we have done all generation & cleaning steps.");
            return GeneratorAccessor.getOutOfTheSurfaceWorldGenerator(worldName, id);
        }
        if (this.worldSurfaceReader == null) {
            warning("No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found");
            return super.getDefaultWorldGenerator(worldName, id);
        }
        ChunkGenerator outOfTheSurfaceWorldGenerator = GeneratorAccessor.getOutOfTheSurfaceWorldGenerator(worldName, id);
        info("Using Underilla as main world generator (with " + outOfTheSurfaceWorldGenerator + " as outOfTheSurfaceWorldGenerator)!");
        return new UnderillaChunkGenerator(this.worldSurfaceReader, this.worldCavesReader, outOfTheSurfaceWorldGenerator);
    }

    @Override
    public void onEnable() {
        new Metrics(this, 24393);

        // save default config
        this.saveDefaultConfig();
        reloadConfig();

        runStepsOnEnabled();

        if (!allStepsDone()) {
            // Loading reference world
            try {
                this.worldSurfaceReader = new BukkitWorldReader(Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME));
                info("World '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found.");
            } catch (NoSuchFieldException e) {
                warning("No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.SURFACE_WORLD_NAME) + "' found");
                warning(() -> Tools.exceptionToString(e));
            }
            // Loading caves world if we should use it.
            if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.TRANSFER_BLOCKS_FROM_CAVES_WORLD)
                    || Underilla.getUnderillaConfig().getBoolean(BooleanKeys.TRANSFER_BIOMES_FROM_CAVES_WORLD)) {
                try {
                    info("Loading caves world");
                    this.worldCavesReader = new BukkitWorldReader(Underilla.getUnderillaConfig().getString(StringKeys.CAVES_WORLD_NAME));
                } catch (NoSuchFieldException e) {
                    warning("No world with name '" + Underilla.getUnderillaConfig().getString(StringKeys.CAVES_WORLD_NAME) + "' found");
                    warning(() -> Tools.exceptionToString(e));
                }
            }

            // Registering listeners
            if (Underilla.getUnderillaConfig().getBoolean(BooleanKeys.STRUCTURES_ENABLED)) {
                structureEventListener = new StructureEventListener();
                this.getServer().getPluginManager().registerEvents(structureEventListener, this);
            }
            this.getServer().getPluginManager().registerEvents(new WorldListener(), this);
        }
    }

    @Override
    public void onDisable() {
        try {
            stopTasks();
            if (Generator.times != null) {
                long totalTime = Generator.times.entrySet().stream().mapToLong(Map.Entry::getValue).sum();
                for (Map.Entry<String, Long> entry : Generator.times.entrySet()) {
                    info(entry.getKey() + " took " + entry.getValue() + "ms (" + (entry.getValue() * 100 / totalTime) + "%)");
                }
            }
            Map<String, Long> biomesPlaced = UnderillaChunkGenerator.getBiomesPlaced();
            if (biomesPlaced != null) {
                info("Map of biome placed: " + biomesPlaced.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                        .map(entry -> entry.getKey() + ": " + entry.getValue()).reduce((a, b) -> a + ", " + b).orElse(""));
            }
        } catch (Exception e) {
            info("Fail to print times or biomes placed.");
            Underilla.info(() -> Tools.exceptionToString(e));
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (underillaConfig == null) {
            underillaConfig = new UnderillaConfig(getConfig());
        } else {
            underillaConfig.reload(getConfig());
        }
        if (!allStepsDone()) {
            Underilla.info("Config reloaded with values: " + underillaConfig);
        }
    }

    public static Underilla getInstance() { return getPlugin(Underilla.class); }
    public static UnderillaConfig getUnderillaConfig() { return getInstance().underillaConfig; }


    public static void log(Level level, String message) { getInstance().getLogger().log(level, message); }
    public static void log(Level level, String message, Throwable e) { getInstance().getLogger().log(level, message, e); }
    public static void debug(String message) {
        if (getInstance().getConfig().getBoolean("debug", false)) {
            log(Level.INFO, message);
        }
    }
    public static void debug(Supplier<String> messageProvider) {
        if (getInstance().getConfig().getBoolean("debug", false)) {
            log(Level.INFO, messageProvider.get());
        }
    }
    public static void info(String message) { log(Level.INFO, message); }
    public static void info(Supplier<String> messageProvider) { log(Level.INFO, messageProvider.get()); }
    public static void info(String message, Throwable e) { log(Level.INFO, message, e); }
    public static void warning(String message) { log(Level.WARNING, message); }
    public static void warning(Supplier<String> messageProvider) { log(Level.WARNING, messageProvider.get()); }
    public static void warning(String message, Throwable e) { log(Level.WARNING, message, e); }
    public static void error(String message) { log(Level.SEVERE, message); }
    public static void error(Supplier<String> messageProvider) { log(Level.SEVERE, messageProvider.get()); }
    public static void error(String message, Throwable e) { log(Level.SEVERE, message, e); }


    private void runStepsOnEnabled() {
        boolean needARestart = false;

        if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_DOWNLOAD_DEPENDENCY_PLUGINS).equals(TODO)) {
            needARestart = ServerSetup.downloadNeededDependencies() || needARestart;
        }
        if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_SETUP_PAPER_FOR_QUICK_GENERATION).equals(TODO)) {
            needARestart = ServerSetup.setupPaperWorkerthreads() || needARestart;
        }
        if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_SET_UNDERILLA_AS_WORLD_GENERATOR).equals(TODO)) {
            needARestart = ServerSetup.setupBukkitWorldGenerator() || needARestart;
        }
        if (needARestart) {
            info("Underilla have done pre generation steps. Restarting server to apply changes.");
            Bukkit.shutdownMessage();
            // Bukkit.shutdown(); // It doesn't work before the world is loaded.
            Bukkit.getServer().restart();
            // System.exit(0);
        }
    }
    public void runNextStepsAfterWorldInit() {
        if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_UNDERILLA_GENERATION).equals(TODO)) {
            runChunky();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_UNDERILLA_GENERATION).equals(DOING)) {
            restartChunky();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals(TODO)) {
            runCleanBlocks();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals(DOING)) {
            restartCleanBlocks();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals(TODO)) {
            runCleanEntities();
        } else if (Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals(DOING)) {
            restartCleanEntities();
        }
    }
    public boolean allStepsDone() {
        return Underilla.getUnderillaConfig().getString(StringKeys.STEP_UNDERILLA_GENERATION).equals(DONE)
                && Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals(DONE)
                && Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals(DONE);
    }
    public void validateTask(StringKeys taskKey, boolean done) {
        getUnderillaConfig().saveNewValue(taskKey, done ? DONE : FAILED);
        if (done && endTaskActions.containsKey(taskKey)) {
            Underilla.info("Running post action for task " + taskKey);
            endTaskActions.get(taskKey).run();
        }
        runNextStepsAfterWorldInit();
    }
    public void validateInitServerTask(StringKeys taskKey, boolean done) {
        getUnderillaConfig().saveNewValue(taskKey, done ? DONE : FAILED);
    }
    public void validateTask(StringKeys taskKey) { validateTask(taskKey, true); }
    public void validateInitServerTask(StringKeys taskKey) { validateInitServerTask(taskKey, true); }
    public void setToDoingTask(StringKeys taskKey) { getUnderillaConfig().saveNewValue(taskKey, DOING); }

    // Custom actions -------------------------------------------------------------------------------------------------
    public Function<org.bukkit.block.Biome, org.bukkit.block.Biome> getEndBiomeTransformer() { return endBiomeTransformer; }
    public void setEndBiomeTransformer(Function<org.bukkit.block.Biome, org.bukkit.block.Biome> endBiomeTransformer) {
        this.endBiomeTransformer = endBiomeTransformer;
    }
    public boolean hasEndBiomeTransformer() { return endBiomeTransformer != null; }
    public Consumer<Block> getEndBlockTransformer() { return endBlockTransformer; }
    public void setEndBlockTransformer(Consumer<Block> endBlockTransformer) { this.endBlockTransformer = endBlockTransformer; }
    public boolean hasEndBlockTransformer() { return endBlockTransformer != null; }
    public Consumer<Entity> getEndEntityTransformer() { return endEntityTransformer; }
    public void setEndEntityTransformer(Consumer<Entity> endEntityTransformer) { this.endEntityTransformer = endEntityTransformer; }
    public boolean hasEndEntityTransformer() { return endEntityTransformer != null; }
    public void setPostTaskAction(Runnable action, StringKeys taskKey) { endTaskActions.put(taskKey, action); }

    // run tasks ------------------------------------------------------------------------------------------------------
    private void runChunky(boolean restart) {
        Chunky chunky = ChunkyProvider.get();
        // startTask(String world, String shape, double centerX, double centerZ, double radiusX, double radiusZ, String pattern)
        String worldName = Underilla.getUnderillaConfig().getString(StringKeys.FINAL_WORLD_NAME);
        int minX = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_X);
        int minZ = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MIN_Z);
        int maxX = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_X);
        int maxZ = Underilla.getUnderillaConfig().getInt(IntegerKeys.GENERATION_AREA_MAX_Z);
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int radiusX = (maxX - minX) / 2;
        int radiusZ = (maxZ - minZ) / 2;
        final long startTime = System.currentTimeMillis();
        // Set chunky silent
        chunky.getConfig().setSilent(true);

        chunky.getApi().onGenerationProgress(new Consumer<GenerationProgressEvent>() {
            long printTime = 0;
            long printTimeEachXMs = Underilla.MS_PER_SECOND
                    * Underilla.getUnderillaConfig().getInt(IntegerKeys.PRINT_PROGRESS_EVERY_X_SECONDS);
            @Override
            public void accept(GenerationProgressEvent generationProgressEvent) {
                if (printTime + printTimeEachXMs < System.currentTimeMillis()) {
                    printTime = System.currentTimeMillis();
                    FollowableProgressTask.printProgress(generationProgressEvent.chunks(), startTime,
                            generationProgressEvent.progress() / 100, 1, 3, "Rate: " + (int) (generationProgressEvent.rate())
                                    + ", Current: " + generationProgressEvent.x() + " " + generationProgressEvent.z());
                }
            }
        });

        chunky.getApi().onGenerationComplete(generationCompleteEvent -> {
            info("Chunky task for world " + worldName + " has finished");
            if (structureEventListener != null) {
                info("Structure generation: " + structureEventListener.getStructureCount());
            }
            validateTask(StringKeys.STEP_UNDERILLA_GENERATION);
        });

        boolean worked;
        if (restart) {
            worked = chunky.getApi().continueTask(worldName);
        } else {
            worked = chunky.getApi().startTask(worldName, "rectangle", centerX, centerZ, radiusX, radiusZ, "region");
            setToDoingTask(StringKeys.STEP_UNDERILLA_GENERATION);
        }
        if (worked) {
            info("Started Chunky task for world " + worldName);
        } else {
            warning("Failed to start Chunky task for world " + worldName);
            validateTask(StringKeys.STEP_UNDERILLA_GENERATION, false);
        }
    }
    private void runChunky() { runChunky(false); }
    private void runCleanBlocks(Selector selector) {
        setToDoingTask(StringKeys.STEP_CLEANING_BLOCKS);
        info("Starting clean blocks task");
        cleanBlocksTask = new CleanBlocksTask(2, 3, selector);
        cleanBlocksTask.run();
    }
    private void runCleanBlocks() { runCleanBlocks(Underilla.getUnderillaConfig().getSelector()); }
    private void runCleanEntities(Selector selector) {
        setToDoingTask(StringKeys.STEP_CLEANING_ENTITIES);
        info("Starting clean entities task");
        cleanEntitiesTask = new CleanEntitiesTask(3, 3);
        cleanEntitiesTask.run();
    }
    private void runCleanEntities() { runCleanEntities(Underilla.getUnderillaConfig().getSelector()); }

    // stop tasks -----------------------------------------------------------------------------------------------------
    private void stopTasks() {
        if (cleanBlocksTask != null && Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_BLOCKS).equals(DOING)) {
            Selector selector = cleanBlocksTask.stop();
            selector.saveIn("cleanBlocksTask");
        }
        if (cleanEntitiesTask != null && Underilla.getUnderillaConfig().getString(StringKeys.STEP_CLEANING_ENTITIES).equals(DOING)) {
            Selector selector = cleanEntitiesTask.stop();
            selector.saveIn("cleanEntitiesTask");
        }
    }

    // restart tasks --------------------------------------------------------------------------------------------------
    private void restartChunky() {
        info("Restarting Chunky task");
        runChunky(true);
    }
    private void restartCleanBlocks() {
        info("Restarting clean blocks task");
        try {
            runCleanBlocks(Selector.loadFrom("cleanBlocksTask"));
        } catch (Exception e) {
            Underilla.warning("Tasks can't be restarted from last state. Restarting from the beginning.");
            runCleanBlocks();
        }
    }
    private void restartCleanEntities() {
        info("Restarting clean entities task");
        try {
            runCleanEntities(Selector.loadFrom("cleanEntitiesTask"));
        } catch (Exception e) {
            Underilla.warning("Tasks can't be restarted from last state. Restarting from the beginning.");
            runCleanEntities();
        }
    }
}
