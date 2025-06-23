package fr.formiko.mc.underilla.paper.preparing;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.Tools;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.StringKeys;
import fr.formiko.utils.FLUFiles;
import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ServerSetup {
    private ServerSetup() {}

    public static boolean setupPaperWorkerthreads() {
        int cores = Runtime.getRuntime().availableProcessors();
        Underilla.info("Available cores: " + cores);
        // Underilla.info("config/paper-global.yml should be edit to: chunk-system.worker-threads: " + (cores -
        // 1));

        FileConfiguration paperGlobalConfig = getPaperGlobalFileConfiguration();
        String key = "chunk-system.worker-threads";
        if (paperGlobalConfig.getInt(key, -1) == cores - 1) {
            Underilla.info("The worker threads are already set to " + (cores - 1));
            return false;
        } else {
            paperGlobalConfig.set(key, cores - 1);
            Underilla.info("Set the worker threads to " + (cores - 1));
            try {
                paperGlobalConfig.save(getPaperGlobalFile());
                Underilla.getInstance().validateInitServerTask(StringKeys.STEP_SETUP_PAPER_FOR_QUICK_GENERATION);
            } catch (Exception e) {
                Underilla.error("Error saving paper-global.yml");
                Underilla.error(() -> Tools.exceptionToString(e));
                Underilla.getInstance().validateInitServerTask(StringKeys.STEP_SETUP_PAPER_FOR_QUICK_GENERATION, false);
                return false;
            }
            return true;
        }
    }

    public static boolean downloadNeededDependencies() {
        File plugins = new File(Underilla.getInstance().getServer().getWorldContainer(), "plugins");
        boolean shouldDownloadVoidWorldGenerator = true;
        boolean shouldDownloadChunky = true;
        boolean error = false;
        for (File file : plugins.listFiles()) {
            if (file.getName().matches("VoidWorldGenerator.*.jar")) {
                shouldDownloadVoidWorldGenerator = false;
            }
            if (file.getName().matches("Chunky.*.jar")) {
                shouldDownloadChunky = false;
            }
        }

        Underilla.info(String.format("Downloading needed dependencies to %s VoidWorldGenerator: %s, Chunky: %s", plugins.getAbsolutePath(),
                shouldDownloadVoidWorldGenerator, shouldDownloadChunky));

        if (shouldDownloadVoidWorldGenerator) {
            String voidWorldGeneratorVersion = Underilla.getInstance().getConfig().getString("voidWorldGeneratorVersion", "1.3.2");
            error = !FLUFiles.download(
                    String.format("https://github.com/HydrolienF/VoidWorldGenerator/releases/download/%s/VoidWorldGenerator-%s.jar",
                            voidWorldGeneratorVersion, voidWorldGeneratorVersion),
                    String.format("%s/VoidWorldGenerator-%s.jar", plugins.getAbsolutePath(), voidWorldGeneratorVersion)) || error;
            Underilla.info("Downloaded VoidWorldGenerator " + (error ? "with" : "without") + " errors");
        }
        if (shouldDownloadChunky) {
            String chunkyVersion = Underilla.getInstance().getConfig().getString("chunkyVersion", "1.4.28");
            error = !FLUFiles
                    .download(
                            String.format("https://hangarcdn.papermc.io/plugins/pop4959/Chunky/versions/%s/PAPER/Chunky-Bukkit-%s.jar",
                                    chunkyVersion, chunkyVersion),
                            String.format("%s/Chunky-%s.jar", plugins.getAbsolutePath(), chunkyVersion))
                    || error;
            Underilla.info("Downloaded Chunky " + (error ? "with" : "without") + " errors");
        }
        Underilla.getInstance().validateInitServerTask(StringKeys.STEP_DOWNLOAD_DEPENDENCY_PLUGINS, !error);
        return shouldDownloadVoidWorldGenerator || shouldDownloadChunky;
    }

    public static boolean setupBukkitWorldGenerator() {
        // Read the bukkit.yml file
        FileConfiguration bukkitConfig = getBukkitFileConfiguration();
        String key = "worlds." + Underilla.getUnderillaConfig().getString(StringKeys.FINAL_WORLD_NAME) + ".generator";
        String currentWorldGenerator = bukkitConfig.getString(key, "");
        if ("Underilla".equals(currentWorldGenerator)) {
            Underilla.info("The world generator is already set to Underilla");
            return false;
        } else {
            bukkitConfig.set(key, "Underilla");
            Underilla.info("Set the world generator to Underilla");
            try {
                bukkitConfig.save(getBukkitFile());
                Underilla.getInstance().validateInitServerTask(StringKeys.STEP_SET_UNDERILLA_AS_WORLD_GENERATOR);
            } catch (Exception e) {
                Underilla.error("Error saving bukkit.yml");
                Underilla.error(() -> Tools.exceptionToString(e));
                Underilla.getInstance().validateInitServerTask(StringKeys.STEP_SET_UNDERILLA_AS_WORLD_GENERATOR, false);
                return false;
            }
            return true;
        }
    }
    public static FileConfiguration getBukkitFileConfiguration() { return YamlConfiguration.loadConfiguration(getBukkitFile()); }
    public static File getBukkitFile() { return new File(Underilla.getInstance().getServer().getWorldContainer(), "bukkit.yml"); }
    public static FileConfiguration getPaperGlobalFileConfiguration() { return YamlConfiguration.loadConfiguration(getPaperGlobalFile()); }
    public static File getPaperGlobalFile() {
        return new File(Underilla.getInstance().getServer().getWorldContainer(), "config/paper-global.yml");
    }
}
