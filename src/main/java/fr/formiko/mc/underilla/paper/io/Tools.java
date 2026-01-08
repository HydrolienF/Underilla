package fr.formiko.mc.underilla.paper.io;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class Tools {
    private Tools() {}
    public static String exceptionToString(Exception e) {
        return Arrays.stream(e.getStackTrace()).map(s -> s.toString()).collect(Collectors.joining("\n", e.getMessage() + ": ", ""));
    }

    /**
     * Normalize the name to the format minecraft:name
     * Any bukkit name will be converted to minecraft name.
     * e.g. "minecraft:plains" or "PLAINS" will be returned as "minecraft:plains"
     * 
     * @param name the name
     * @return a normalized name
     */
    public static @Nonnull String normalizeName(@Nonnull String name) {
        name = name.toLowerCase();
        if (!name.contains(":")) {
            name = "minecraft:" + name;
        }
        return name;
    }
    /**
     * Normalize the name to the format minecraft:name
     * Any bukkit name will be converted to minecraft name.
     * e.g. "minecraft:plains" or "PLAINS" will be returned as "minecraft:plains"
     * 
     * @param nameList a list of names
     * @return a list of normalized names
     */
    public static @Nonnull List<String> normalizeNameList(@Nonnull List<String> nameList) {
        return nameList.stream().map(Tools::normalizeName).toList();
    }
}
