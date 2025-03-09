package fr.formiko.mc.underilla.paper.impl;

import fr.formiko.mc.underilla.core.api.Block;
import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.SetMaterialKeys;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.EntityType;

public class BukkitBlock implements Block {
    public static final BukkitBlock AIR = new BukkitBlock(Material.AIR.createBlockData());

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
        return isSolid() && !Underilla.getUnderillaConfig().isMaterialInSet(SetMaterialKeys.IGNORED_BLOCK_FOR_SURFACE_CALCULATION,
                this.blockData_.getMaterial());
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
