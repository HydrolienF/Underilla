package com.jkantrell.mc.underilla.spigot.impl;

import com.jkantrell.mc.underilla.core.api.Biome;

public class BukkitBiome implements Biome {

    // FIELDS
    private String name;


    // // CONSTRUCTORS
    // public BukkitBiome(org.bukkit.block.Biome biome) { this.name = biome.name(); }
    public BukkitBiome(String name) {
        name = name.toLowerCase();
        if (!name.contains(":")) {
            name = "minecraft:" + name;
        }
        this.name = name;
    }


    // // // GETTERS
    // public org.bukkit.block.Biome getBiome() { return NMSBiomeUtils.getBukkitBiome(this.name); }


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
    public String toString() { return "BukkitBiome{" + name + '}'; }
}
