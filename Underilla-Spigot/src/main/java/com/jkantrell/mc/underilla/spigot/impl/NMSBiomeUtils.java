package com.jkantrell.mc.underilla.spigot.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBiome;
import com.jkantrell.mc.underilla.spigot.Underilla;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

public class NMSBiomeUtils {
    public static Registry<Biome> getBiomeRegistry() {
        return ((CraftServer) Bukkit.getServer()).getServer().registryAccess().registryOrThrow(Registries.BIOME);
    }

    public static Biome getBiome(String key) { return getBiomeRegistry().get(resourceLocation(key)); }
    public static Biome getBiome(Location location) {
        return getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld());
    }
    public static Biome getBiome(int x, int y, int z, World bukkitWorld) {
        if (bukkitWorld == null)
            return null;
        ServerLevel nmsWorld = ((CraftWorld) bukkitWorld).getHandle();
        return nmsWorld.getNoiseBiome(x >> 2, y >> 2, z >> 2).value();
    }

    public static ResourceLocation getBiomeKey(Location location) { return getBiomeRegistry().getKey(getBiome(location)); }
    public static ResourceLocation getBiomeKey(int x, int y, int z, World bukkitWorld) {
        return getBiomeRegistry().getKey(getBiome(x, y, z, bukkitWorld));
    }

    public static ResourceLocation resourceLocation(String name) {
        String[] t = name.split(":");
        return ResourceLocation.fromNamespaceAndPath(t[0], t[1]);
    }

    // Convert between Minecraft and Bukkit biomes
    // minecraft to bukkit don't work with custom biomes.
    public static org.bukkit.block.Biome minecraftToBukkit(Biome minecraft) { return CraftBiome.minecraftToBukkit(minecraft); }
    public static Biome bukkitToMinecraft(org.bukkit.block.Biome bukkit) { return CraftBiome.bukkitToMinecraft(bukkit); }
    public static org.bukkit.block.Biome minecraftHolderToBukkit(Holder<Biome> minecraft) {
        return CraftBiome.minecraftToBukkit(minecraft.value());
    }
    public static Holder<Biome> bukkitToMinecraftHolder(org.bukkit.block.Biome bukkit) {
        return CraftBiome.bukkitToMinecraftHolder(bukkit);
    }

    public static org.bukkit.block.Biome getBukkitBiome(String name) {
        try {
            return org.bukkit.block.Biome.valueOf(name.split(":")[1].toUpperCase());
        }catch (IllegalArgumentException e) {
            Underilla.getInstance().getLogger().warning("Failed to get Bukkit biome from " + name);
            return org.bukkit.block.Biome.PLAINS;
        }
    }
}
