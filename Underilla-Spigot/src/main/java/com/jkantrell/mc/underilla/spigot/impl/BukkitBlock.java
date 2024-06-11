package com.jkantrell.mc.underilla.spigot.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;
import com.jkantrell.mc.underilla.core.api.Block;
import com.jkantrell.mc.underilla.spigot.Underilla;

public class BukkitBlock implements Block {
    private static Collection<Material> ignoredBlockForSurfaceCalculation;

    // FIELDS
    private BlockData blockData_;
    private Optional<EntityType> spawnedType;


    // CONSTRUCTORS
    public BukkitBlock(BlockData blockData) {
        this.blockData_ = blockData;
        this.spawnedType = Optional.empty();
    }


    // GETTERS
    public BlockData getBlockData() { return this.blockData_; }
    private static Collection<Material> getIgnoredBlocksForSurfaceCalculation() {
        if (ignoredBlockForSurfaceCalculation == null) {
            List<String> regexListFromConfig = Underilla.CONFIG.ignoredBlockForSurfaceCalculation;
            ignoredBlockForSurfaceCalculation = Arrays.stream(Material.values())
                    .filter(material -> regexListFromConfig.stream().anyMatch(s -> material.toString().matches(s)))
                    .collect(Collectors.toSet());
        }
        return ignoredBlockForSurfaceCalculation;
    }

    public Optional<EntityType> getSpawnedType() { return this.spawnedType; }
    public void setSpawnedType(String spawnedType) {
        this.spawnedType = Optional.ofNullable(EntityType.valueOf(spawnedType.replace("minecraft:", "").toUpperCase()));
    }

    // IMPLEMENTATIONS
    @Override
    public boolean isAir() { return this.blockData_.getMaterial().isAir(); }

    @Override
    public boolean isSolid() { return this.blockData_.getMaterial().isSolid(); }

    @Override
    public boolean isSolidAndSurfaceBlock() {
        return isSolid() && !getIgnoredBlocksForSurfaceCalculation().contains(this.blockData_.getMaterial());
    }

    @Override
    public boolean isLiquid() {
        Material m = this.blockData_.getMaterial();
        return m.equals(Material.WATER) || m.equals(Material.LAVA) || isWaterLogged() || m.equals(Material.BUBBLE_COLUMN)
                || m.equals(Material.SEAGRASS) || m.equals(Material.TALL_SEAGRASS) || m.equals(Material.KELP)
                || m.equals(Material.KELP_PLANT);
    }

    @Override
    public boolean isWaterloggable() { return this.blockData_ instanceof Waterlogged; }

    public boolean isWaterLogged() { return this.blockData_ instanceof Waterlogged waterlogged && waterlogged.isWaterlogged(); }

    @Override
    public void waterlog() {
        if (this.isAir()) {
            this.blockData_ = Material.WATER.createBlockData();
            return;
        }
        if (!(this.blockData_ instanceof Waterlogged waterlogged)) {
            return;
        }
        waterlogged.setWaterlogged(true);
        this.blockData_ = waterlogged;
    }

    @Override
    public String getName() { return this.blockData_.getMaterial().toString().toLowerCase(); }
    public Material getMaterial() { return this.blockData_.getMaterial(); }

    @Override
    public String getNameSpace() { return null; }
}
