package fr.formiko.mc.underilla.paper.impl;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import fr.formiko.mc.underilla.core.api.Biome;
import org.bukkit.NamespacedKey;

public class BukkitBiome implements Biome {

    public static final BukkitBiome DEFAULT = new BukkitBiome("minecraft:plains");

    // FIELDS
    private String name;


    // CONSTRUCTORS
    public BukkitBiome(String name) { this.name = NMSBiomeUtils.normalizeBiomeName(name); }
    public BukkitBiome(NamespacedKey key) { this(key.toString()); }


    // GETTERS
    public org.bukkit.block.Biome getBiome() {
        return io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.BIOME)
                .get(NamespacedKey.fromString(name));
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
