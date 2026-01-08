package fr.formiko.mc.underilla.paper.impl;

import fr.formiko.mc.underilla.core.api.Biome;
import fr.formiko.mc.underilla.paper.io.Tools;
import io.papermc.paper.registry.tag.TagKey;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.NamespacedKey;

public class BukkitBiome implements Biome {

    public static final BukkitBiome DEFAULT = new BukkitBiome("minecraft:plains");

    // FIELDS
    private String name;


    // CONSTRUCTORS
    public BukkitBiome(String name) { this.name = Tools.normalizeName(name); }
    public BukkitBiome(NamespacedKey key) { this(key.toString()); }


    // GETTERS
    public org.bukkit.block.Biome getBiome() { return getBiomeRegistryAccess().get(NamespacedKey.fromString(name)); }

    public static org.bukkit.Registry<org.bukkit.block.Biome> getBiomeRegistryAccess() {
        return io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.BIOME);
    }

    public static Stream<org.bukkit.block.Biome> getAllBiomesStream() { return getBiomeRegistryAccess().stream(); }
    public static Collection<org.bukkit.block.Biome> getAllBiomes() {
        return getBiomeRegistryAccess().stream().collect(Collectors.toSet());
    }
    public static List<org.bukkit.block.Biome> getAllBiomesList() { return getBiomeRegistryAccess().stream().toList(); }

    public static Collection<String> getAllBiomesNames() {
        return getAllBiomes().stream().map(biome -> biome.getKey().toString()).collect(Collectors.toSet());
    }
    public static Collection<org.bukkit.block.Biome> getAllBiomesOfTag(String tag) {
        return getBiomeRegistryAccess().getTagValues(TagKey.create(io.papermc.paper.registry.RegistryKey.BIOME, tag));
    }


    // IMPLEMENTATIONS
    @Override
    public String getName() { return name; }
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof BukkitBiome bukkitBiome)) {
            return false;
        }
        return this.name.equals(bukkitBiome.name);
    }
    @Override
    public int hashCode() { return name.hashCode(); }

    @Override
    public String toString() { return "BukkitBiome{" + name + '}'; }
}
