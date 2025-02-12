package com.jkantrell.mc.underilla.spigot.io;

import fr.formiko.mc.biomeutils.NMSBiomeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.Structure;
import com.jkantrell.mc.underilla.core.generation.MergeStrategy;
import com.jkantrell.mc.underilla.spigot.Underilla;
import com.jkantrell.mc.underilla.spigot.selector.Selector;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

public class UnderillaConfig {
    private final EnumMap<BooleanKeys, Boolean> booleanMap;
    private final EnumMap<IntegerKeys, Integer> integerMap;
    private final EnumMap<StringKeys, String> stringMap;
    private final EnumMap<SetStringKeys, Set<String>> listStringMap;
    private final EnumMap<SetBiomeStringKeys, Set<String>> listBiomeStringMap;
    private final EnumMap<SetMaterialKeys, Set<Material>> listMaterialMap;
    private final EnumMap<MapMaterialKeys, Map<Material, Material>> listMapMaterialMap;
    private final EnumMap<SetStructureKeys, Set<Structure>> listStructureMap;
    private final EnumMap<SetEntityTypeKeys, Set<EntityType>> listEntityTypeMap;
    private MergeStrategy mergeStrategy;


    public UnderillaConfig(FileConfiguration fileConfiguration) {
        booleanMap = new EnumMap<>(BooleanKeys.class);
        integerMap = new EnumMap<>(IntegerKeys.class);
        stringMap = new EnumMap<>(StringKeys.class);
        listStringMap = new EnumMap<>(SetStringKeys.class);
        listBiomeStringMap = new EnumMap<>(SetBiomeStringKeys.class);
        listMaterialMap = new EnumMap<>(SetMaterialKeys.class);
        listMapMaterialMap = new EnumMap<>(MapMaterialKeys.class);
        listStructureMap = new EnumMap<>(SetStructureKeys.class);
        listEntityTypeMap = new EnumMap<>(SetEntityTypeKeys.class);
        reload(fileConfiguration);
    }

    public boolean getBoolean(BooleanKeys key) { return booleanMap.get(key); }
    public int getInt(IntegerKeys key) { return integerMap.get(key); }
    public String getString(StringKeys key) { return stringMap.get(key); }
    public Set<String> getSetString(SetStringKeys key) { return listStringMap.get(key); }
    public Set<String> getSetBiomeString(SetBiomeStringKeys key) { return listBiomeStringMap.get(key); }
    public Set<Material> getSetMaterial(SetMaterialKeys key) { return listMaterialMap.get(key); }
    public Map<Material, Material> getMapMaterial(MapMaterialKeys key) { return listMapMaterialMap.get(key); }
    public Set<Structure> getSetStructure(SetStructureKeys key) { return listStructureMap.get(key); }
    public Set<EntityType> getSetEntityType(SetEntityTypeKeys key) { return listEntityTypeMap.get(key); }
    public boolean isStringInSet(SetStringKeys key, String value) { return getSetString(key).contains(value); }
    public boolean isBiomeInSet(SetBiomeStringKeys key, String biome) { return getSetBiomeString(key).contains(biome); }
    public boolean isMaterialInSet(SetMaterialKeys key, Material material) { return getSetMaterial(key).contains(material); }
    public @Nullable Material getMaterialFromMap(MapMaterialKeys key, Material material) {
        return getMapMaterial(key).getOrDefault(material, null);
    }
    public boolean isStructureInSet(SetStructureKeys key, Structure structure) { return getSetStructure(key).contains(structure); }
    public boolean isEntityTypeInSet(SetEntityTypeKeys key, EntityType entityType) { return getSetEntityType(key).contains(entityType); }

    public void saveNewValue(StringKeys key, String value) {
        stringMap.put(key, value);
        Underilla.getInstance().getConfig().set(key.path, value);
        Underilla.getInstance().saveConfig();
    }

    public MergeStrategy getMergeStrategy() { return mergeStrategy; }
    public Selector getSelector() {
        return new Selector(getInt(IntegerKeys.GENERATION_AREA_MIN_X), getInt(IntegerKeys.GENERATION_AREA_MIN_Y),
                getInt(IntegerKeys.GENERATION_AREA_MIN_Z), getInt(IntegerKeys.GENERATION_AREA_MAX_X),
                getInt(IntegerKeys.GENERATION_AREA_MAX_Y), getInt(IntegerKeys.GENERATION_AREA_MAX_Z),
                Bukkit.getWorld(getString(StringKeys.FINAL_WORLD_NAME)).getUID());
    }


    public void reload(FileConfiguration fileConfiguration) {
        booleanMap.clear();
        for (BooleanKeys key : BooleanKeys.values()) {
            if (!fileConfiguration.contains(key.path)) {
                warnKeyMissing(key);
            }
            booleanMap.put(key, fileConfiguration.getBoolean(key.path, key.defaultValue));
        }

        integerMap.clear();
        for (IntegerKeys key : IntegerKeys.values()) {
            if (!fileConfiguration.contains(key.path)) {
                warnKeyMissing(key);
            }
            int value = fileConfiguration.getInt(key.path, key.defaultValue);
            if (value > key.max) {
                Underilla.warning("Value " + value + " is greater than max " + key.max + " for key " + key);
                value = key.max;
            }
            if (value < key.min) {
                Underilla.warning("Value " + value + " is less than min " + key.min + " for key " + key);
                value = key.min;
            }
            integerMap.put(key, value);
        }
        swapAeraValueIfNeeded();

        stringMap.clear();
        for (StringKeys key : StringKeys.values()) {
            if (!fileConfiguration.contains(key.path)) {
                warnKeyMissing(key);
            }
            stringMap.put(key, fileConfiguration.getString(key.path, key.defaultValue));
        }
        mergeStrategy = MergeStrategy.valueOf(getString(StringKeys.STRATEGY));

        listStringMap.clear();
        for (SetStringKeys key : SetStringKeys.values()) {
            if (fileConfiguration.contains(key.path)) {
                listStringMap.put(key, new HashSet<>(fileConfiguration.getStringList(key.path)));
            } else {
                warnKeyMissing(key);
                listStringMap.put(key, key.defaultValue);
            }
        }

        listMaterialMap.clear();
        for (SetMaterialKeys key : SetMaterialKeys.values()) {
            List<String> regexList = new ArrayList<>();
            if (fileConfiguration.contains(key.path)) {
                regexList.addAll(fileConfiguration.getStringList(key.path));
            } else {
                warnKeyMissing(key);
                regexList.addAll(key.defaultValue);
            }
            Set<Material> materialSet = Arrays.stream(Material.values())
                    .filter(material -> regexList.stream().anyMatch(s -> material.toString().matches(s))).collect(Collectors.toSet());
            listMaterialMap.put(key, materialSet);
        }

        initMapMaterialMap(fileConfiguration);

        initSetStructures(fileConfiguration);

        initSetEntityType(fileConfiguration);

        initSetBiomeStringMap(fileConfiguration);

        if (mergeStrategy == MergeStrategy.NONE) {
            // If strategy is NONE, we need to set the max height of caves to the minimal possible height.
            integerMap.put(IntegerKeys.MAX_HEIGHT_OF_CAVES, getInt(IntegerKeys.GENERATION_AREA_MIN_Y));
        } else {
            // Check if the value is in the range of the final world.
            if (getInt(IntegerKeys.MAX_HEIGHT_OF_CAVES) < getInt(IntegerKeys.GENERATION_AREA_MIN_Y)) {
                integerMap.put(IntegerKeys.MAX_HEIGHT_OF_CAVES, getInt(IntegerKeys.GENERATION_AREA_MIN_Y));
            } else if (getInt(IntegerKeys.MAX_HEIGHT_OF_CAVES) > getInt(IntegerKeys.GENERATION_AREA_MAX_Y)) {
                integerMap.put(IntegerKeys.MAX_HEIGHT_OF_CAVES, getInt(IntegerKeys.GENERATION_AREA_MAX_Y));
            }
        }
        if (mergeStrategy != MergeStrategy.SURFACE) {
            integerMap.put(IntegerKeys.MERGE_DEPTH, 0);
            booleanMap.put(BooleanKeys.ADAPTATIVE_MERGE_DEPTH_ENABLED, false);
        }
        if (!getBoolean(BooleanKeys.ADAPTATIVE_MERGE_DEPTH_ENABLED)) {
            integerMap.put(IntegerKeys.ADAPTATIVE_MAX_MERGE_DEPTH, 0);
            integerMap.put(IntegerKeys.ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH, 0);
        }
        if (getInt(IntegerKeys.ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH) > getInt(IntegerKeys.ADAPTATIVE_MAX_MERGE_DEPTH)) {
            integerMap.put(IntegerKeys.ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH, getInt(IntegerKeys.ADAPTATIVE_MAX_MERGE_DEPTH));
        }
        if (getInt(IntegerKeys.ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH) > getInt(IntegerKeys.MERGE_DEPTH)) {
            integerMap.put(IntegerKeys.ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH, getInt(IntegerKeys.MERGE_DEPTH));
        }
    }

    private void swapAeraValueIfNeeded() {
        if (getInt(IntegerKeys.GENERATION_AREA_MIN_X) > getInt(IntegerKeys.GENERATION_AREA_MAX_X)) {
            Underilla.warning("Min X is greater than max X. Swapping values.");
            int temp = getInt(IntegerKeys.GENERATION_AREA_MIN_X);
            integerMap.put(IntegerKeys.GENERATION_AREA_MIN_X, getInt(IntegerKeys.GENERATION_AREA_MAX_X));
            integerMap.put(IntegerKeys.GENERATION_AREA_MAX_X, temp);
        }
        if (getInt(IntegerKeys.GENERATION_AREA_MIN_Y) > getInt(IntegerKeys.GENERATION_AREA_MAX_Y)) {
            Underilla.warning("Min Y is greater than max Y. Swapping values.");
            int temp = getInt(IntegerKeys.GENERATION_AREA_MIN_Y);
            integerMap.put(IntegerKeys.GENERATION_AREA_MIN_Y, getInt(IntegerKeys.GENERATION_AREA_MAX_Y));
            integerMap.put(IntegerKeys.GENERATION_AREA_MAX_Y, temp);
        }
        if (getInt(IntegerKeys.GENERATION_AREA_MIN_Z) > getInt(IntegerKeys.GENERATION_AREA_MAX_Z)) {
            Underilla.warning("Min Z is greater than max Z. Swapping values.");
            int temp = getInt(IntegerKeys.GENERATION_AREA_MIN_Z);
            integerMap.put(IntegerKeys.GENERATION_AREA_MIN_Z, getInt(IntegerKeys.GENERATION_AREA_MAX_Z));
            integerMap.put(IntegerKeys.GENERATION_AREA_MAX_Z, temp);
        }
    }

    public void initSetEntityType(FileConfiguration fileConfiguration) {
        for (SetEntityTypeKeys key : SetEntityTypeKeys.values()) {
            List<String> regexList = new ArrayList<>();
            if (fileConfiguration.contains(key.path)) {
                regexList.addAll(fileConfiguration.getStringList(key.path));
            } else {
                warnKeyMissing(key);
                regexList.addAll(key.defaultValue);
            }
            Set<EntityType> entityTypeSet = Arrays.stream(EntityType.values())
                    .filter(entityType -> regexList.stream().anyMatch(s -> entityType.toString().matches(s))).collect(Collectors.toSet());
            listEntityTypeMap.put(key, entityTypeSet);
        }
    }

    public void initMapMaterialMap(FileConfiguration fileConfiguration) {
        for (MapMaterialKeys key : MapMaterialKeys.values()) {
            Map<Material, Material> map = new EnumMap<>(Material.class);
            if (fileConfiguration.contains(key.path)) {
                fileConfiguration.getConfigurationSection(key.path).getKeys(false).forEach(materialKey -> {
                    Material material = Material.matchMaterial(materialKey);
                    if (material == null) {
                        Underilla.warning("Material " + materialKey + " not found in the material list of the server.");
                        return;
                    }
                    String value = fileConfiguration.getString(key.path + "." + materialKey);
                    Material valueMaterial = Material.matchMaterial(value);
                    if (valueMaterial == null) {
                        Underilla.warning("Material " + value + " not found in the material list of the server.");
                        return;
                    }
                    map.put(material, valueMaterial);
                });
            } else {
                warnKeyMissing(key);
                map.putAll(key.defaultValue);
            }
            listMapMaterialMap.put(key, map);
        }
    }

    private void initSetBiomeStringMap(FileConfiguration fileConfiguration) {
        Set<String> allBiomes = NMSBiomeUtils.getAllBiomes().keySet();
        listBiomeStringMap.clear();
        for (SetBiomeStringKeys key : SetBiomeStringKeys.values()) {
            List<String> biomesOrTags = new ArrayList<>();
            if (fileConfiguration.contains(key.path)) {
                biomesOrTags.addAll(fileConfiguration.getStringList(key.path));
            } else {
                warnKeyMissing(key);
                biomesOrTags.addAll(key.defaultValue);
            }
            biomesOrTags = NMSBiomeUtils.normalizeBiomeNameList(biomesOrTags);
            Set<String> existingBiomes = new HashSet<>();
            for (String biomeOrTag : biomesOrTags) {
                if (biomeOrTag.startsWith("#")) {
                    existingBiomes.addAll(NMSBiomeUtils.getAllBiomesKeyStringMatchingTag(biomeOrTag.substring(1)));
                } else if (allBiomes.contains(biomeOrTag)) {
                    existingBiomes.add(biomeOrTag);
                } else {
                    Underilla.warning("Biome or tag " + biomeOrTag + " not found in the biome list of the server.");
                }
            }
            listBiomeStringMap.put(key, existingBiomes);
        }

        // Special rules for ...OnlyOn & ...ExceptOn
        if (getBoolean(BooleanKeys.CARVERS_ENABLED)) {
            mergeOnlyOnAndExceptOn(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, SetBiomeStringKeys.APPLY_CARVERS_EXCEPT_ON_BIOMES,
                    allBiomes);
            // Do not apply carvers on the biomes that should have surface world only.
            listBiomeStringMap.get(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES)
                    .removeAll(listBiomeStringMap.get(SetBiomeStringKeys.SURFACE_WORLD_ONLY_ON_THIS_BIOMES));
        } else {
            listBiomeStringMap.put(SetBiomeStringKeys.APPLY_CARVERS_ONLY_ON_BIOMES, Set.of());
            listBiomeStringMap.remove(SetBiomeStringKeys.APPLY_CARVERS_EXCEPT_ON_BIOMES);
        }

        if (getBoolean(BooleanKeys.CARVERS_ENABLED) && getBoolean(BooleanKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS)) {
            mergeOnlyOnAndExceptOn(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES,
                    SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_EXCEPT_ON_BIOMES, allBiomes);
        } else {
            listBiomeStringMap.put(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES, Set.of());
            listBiomeStringMap.remove(SetBiomeStringKeys.PRESERVE_SURFACE_WORLD_FROM_CAVERS_EXCEPT_ON_BIOMES);
        }

        if (getBoolean(BooleanKeys.BIOME_MERGING_FROM_CAVES_GENERATION_ENABLED)) {
            mergeOnlyOnAndExceptOn(SetBiomeStringKeys.BIOME_MERGING_FROM_CAVES_GENERATION_ONLY_ON_BIOMES,
                    SetBiomeStringKeys.BIOME_MERGING_FROM_CAVES_GENERATION_EXCEPT_ON_BIOMES, allBiomes);
        } else {
            listBiomeStringMap.put(SetBiomeStringKeys.BIOME_MERGING_FROM_CAVES_GENERATION_ONLY_ON_BIOMES, Set.of());
            listBiomeStringMap.remove(SetBiomeStringKeys.BIOME_MERGING_FROM_CAVES_GENERATION_EXCEPT_ON_BIOMES);
        }
    }

    private void initSetStructures(FileConfiguration fileConfiguration) {
        Registry<Structure> structureRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        List<Structure> allStructures = structureRegistry.stream().toList();
        for (SetStructureKeys key : SetStructureKeys.values()) {
            List<String> regexList = new ArrayList<>();
            if (fileConfiguration.contains(key.path)) {
                regexList.addAll(fileConfiguration.getStringList(key.path));
            } else {
                warnKeyMissing(key);
                regexList.addAll(key.defaultValue);
            }

            Set<Structure> structureSet = allStructures.stream().filter(structure -> regexList.stream().anyMatch(
                    // Not biomes but normalizeBiomeName does exactly what we want.
                    s -> structureRegistry.getKeyOrThrow(structure).asString().matches(NMSBiomeUtils.normalizeBiomeName(s))))
                    .collect(Collectors.toSet());
            listStructureMap.put(key, structureSet);
        }

        if (getBoolean(BooleanKeys.STRUCTURES_ENABLED)) {
            // If no only or except is set, we keep all structures.
            if (listStructureMap.get(SetStructureKeys.SURUCTURE_ONLY).isEmpty()
                    && listStructureMap.get(SetStructureKeys.SURUCTURE_EXCEPT).isEmpty()) {
                listStructureMap.put(SetStructureKeys.SURUCTURE_ONLY, new HashSet<>(allStructures));
            } else if (listStructureMap.get(SetStructureKeys.SURUCTURE_ONLY).isEmpty()) {
                // if only is empty, we keep all structures except the ones in except.
                listStructureMap.put(SetStructureKeys.SURUCTURE_ONLY,
                        allStructures.stream()
                                .filter(structure -> !listStructureMap.get(SetStructureKeys.SURUCTURE_EXCEPT).contains(structure))
                                .collect(Collectors.toSet()));
            }
        } else {
            listStructureMap.put(SetStructureKeys.SURUCTURE_ONLY, Set.of());
        }
        listStructureMap.remove(SetStructureKeys.SURUCTURE_EXCEPT);
    }


    @Override
    public String toString() {
        return "UnderillaConfig{" + "booleanMap=" + booleanMap + "\nintegerMap=" + integerMap + "\nstringMap=" + stringMap
        // + "\nlistStringMap=" + toString(listStringMap)
                + "\nlistBiomeStringMap=" + toString(listBiomeStringMap) + "\nlistMaterialMap=" + toString(listMaterialMap)
                + "\nlistMapMaterialMap=" + listMapMaterialMap + "\nlistStructureMap=" + listStructureMapToString() + "\nmergeStrategy="
                + mergeStrategy + '}';
    }
    private String toString(Map<?, ? extends Collection<?>> map) {
        return map.entrySet().stream().map(e -> e.getKey() + " (" + e.getValue().size() + ") = " + e.getValue().stream().sorted().toList())
                .collect(Collectors.joining(",\n", "{", "}"));
    }
    private String listStructureMapToString() {
        Registry<Structure> structureRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.STRUCTURE);
        return listStructureMap.entrySet().stream()
                .map(e -> e.getKey() + " (" + e.getValue().size() + ") = "
                        + e.getValue().stream().map(s -> structureRegistry.getKeyOrThrow(s).asString()).sorted().toList())
                .collect(Collectors.joining(",\n", "{", "}"));
    }

    // private --------------------------------------------------------------------------------------------------------

    private void mergeOnlyOnAndExceptOn(SetBiomeStringKeys onlyOnKey, SetBiomeStringKeys exceptOnKey, Collection<String> allBiomes) {
        Set<String> onlyOn = listBiomeStringMap.get(onlyOnKey);
        Set<String> exceptOn = listBiomeStringMap.get(exceptOnKey);
        boolean onlyOnEmpty = onlyOn.isEmpty();
        boolean exceptOnEmpty = exceptOn.isEmpty();

        if (!onlyOnEmpty && !exceptOnEmpty) {
            Underilla.warning("Both " + onlyOnKey + " and " + exceptOnKey + " are set. Ignoring " + exceptOnKey + ".");
        } else if (onlyOnEmpty && exceptOnEmpty) {
            listBiomeStringMap.put(onlyOnKey, new HashSet<>(allBiomes));
        } else if (onlyOnEmpty) {
            listBiomeStringMap.put(onlyOnKey, allBiomes.stream().filter(biome -> !exceptOn.contains(biome)).collect(Collectors.toSet()));
        }

        listBiomeStringMap.remove(exceptOnKey);
    }

    private void warnKeyMissing(Object key) { Underilla.warning("Key " + key + " not found in config"); }


    // enum keys ------------------------------------------------------------------------------------------------------
    public enum BooleanKeys {
        // @formatter:off
        DEBUG("debug", false),
        SURFACE_WORLD_BIOME_USE_TOP_Y_VALUE_ONLY("surfaceWorld.useTopYBiomeOnly", true),
        TRANSFER_BLOCKS_FROM_CAVES_WORLD("transferBlocksFromCavesWorld", false),
        TRANSFER_BIOMES_FROM_CAVES_WORLD("transferBiomesFromCavesWorld", false),
        ADAPTATIVE_MERGE_DEPTH_ENABLED("surface.adaptativeDepth.enabled", true),
        VANILLA_POPULATION_ENABLED("vanillaPopulation.enabled", true),
        STRUCTURES_ENABLED("structures.enabled", true),
        CARVERS_ENABLED("carvers.enabled", true),
        PRESERVE_SURFACE_WORLD_FROM_CAVERS("carvers.preserveSurfaceWorldFromCavers", true),
        PRESERVE_LIQUID_FROM_CAVERS("carvers.preserveLiquidFromCavers", true),
        BIOME_MERGING_FROM_CAVES_GENERATION_ENABLED("biomesMerging.fromCavesGeneration.enabled", true),
        BIOME_MERGING_FROM_CAVES_GENERATION_ONLY_UNDER_SURFACE("biomesMerging.fromCavesGeneration.onlyUnderSurface", true),
        CLEAN_BLOCKS_ENABLED("clean.blocks.enabled", true),
        CLEAN_BLOCKS_REMOVE_UNSTABLE_BLOCKS("clean.blocks.removeUnstableBlocks", true),
        CLEAN_ENTITIES_ENABLED("clean.entities.enabled", true);
        // @formatter:on

        private final String path;
        private final boolean defaultValue;
        BooleanKeys(String path, boolean defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
    }
    public enum IntegerKeys {
        // @formatter:off
        PRINT_PROGRESS_EVERY_X_SECONDS("printProgressEveryXSeconds", 10),
        SURFACE_LAYER_THICKNESS("surface.depth", 6, 0, Integer.MAX_VALUE),
        GENERATION_AREA_MIN_X("generationArea.minX", 0),
        GENERATION_AREA_MIN_Z("generationArea.minZ", 0),
        GENERATION_AREA_MAX_X("generationArea.maxX", Underilla.REGION_SIZE),
        GENERATION_AREA_MAX_Z("generationArea.maxZ", Underilla.REGION_SIZE),
        GENERATION_AREA_MIN_Y("generationArea.minY", -64),
        GENERATION_AREA_MAX_Y("generationArea.maxY", 320),
        MERGE_DEPTH("surface.depth", 6),
        ADAPTATIVE_MAX_MERGE_DEPTH("surface.adaptativeDepth.maxDepth", 50),
        ADAPTATIVE_MIN_HIDDEN_BLOCKS_MERGE_DEPTH("surface.adaptativeDepth.minHiddenBlocksDepth", 2),
        MAX_HEIGHT_OF_CAVES("surfaceAndAbsolute.limit", Integer.MAX_VALUE),
        CACHE_SIZE("cache.size", 16, 1, Integer.MAX_VALUE);
        // @formatter:on

        private final String path;
        private final int defaultValue;
        private final int min;
        private final int max;
        IntegerKeys(String path, int defaultValue, int min, int max) {
            this.path = path;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
        }
        IntegerKeys(String path, int defaultValue) { this(path, defaultValue, Integer.MIN_VALUE, Integer.MAX_VALUE); }
        IntegerKeys(String path) { this(path, 0); }
    }
    public enum StringKeys {
        // @formatter:off
        STEP_SETUP_PAPER_FOR_QUICK_GENERATION("steps.setupPaperForQuickGeneration", "skip"),
        STEP_DOWNLOAD_DEPENDENCY_PLUGINS("steps.downloadDependencyPlugins", "skip"),
        STEP_SET_UNDERILLA_AS_WORLD_GENERATOR("steps.setUnderillaAsWorldGenerator", "skip"),
        STEP_UNDERILLA_GENERATION("steps.underillaGeneration", "skip"),
        STEP_CLEANING_BLOCKS("steps.cleaningBlocks", "skip"),
        STEP_CLEANING_ENTITIES("steps.cleaingEntities", "skip"),
        FINAL_WORLD_NAME("finalWorld.name", "world"),
        SURFACE_WORLD_NAME("surfaceWorld.name", "world_surface"),
        CAVES_WORLD_NAME("cavesWorld.name", "world_caves"),
        OUT_OF_THE_SURFACE_WORLD_GENERATOR("outOfTheSurfaceWorldGenerator", "VoidWorldGenerator"),
        STRATEGY("strategy", "SURFACE");
        // @formatter:on

        private final String path;
        private final String defaultValue;
        StringKeys(String path, String defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        StringKeys(String path) { this(path, ""); }
    }
    public enum SetStringKeys {
        // @formatter:off
        NULL("null");
        // @formatter:on

        private final String path;
        private final Set<String> defaultValue;
        SetStringKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetStringKeys(String path) { this(path, Set.of()); }
    }
    public enum SetBiomeStringKeys {
        // @formatter:off
        TRANSFERED_CAVES_WORLD_BIOMES("transferedCavesWorldBiomes", Set.of("minecraft:deep_dark", "minecraft:dripstone_caves", "minecraft:lush_caves")),
        SURFACE_WORLD_ONLY_ON_THIS_BIOMES("preserveBiomes"),
        APPLY_CARVERS_ONLY_ON_BIOMES("carvers.applyCarversOnBiomes.onlyOn"),
        APPLY_CARVERS_EXCEPT_ON_BIOMES("carvers.applyCarversOnBiomes.exceptOn"),
        PRESERVE_SURFACE_WORLD_FROM_CAVERS_ONLY_ON_BIOMES("carvers.preserveSurfaceWorldFromCaversOnBiomes.onlyOn"),
        PRESERVE_SURFACE_WORLD_FROM_CAVERS_EXCEPT_ON_BIOMES("carvers.preserveSurfaceWorldFromCaversOnBiomes.exceptOn"),
        BIOME_MERGING_FROM_CAVES_GENERATION_ONLY_ON_BIOMES("biomesMerging.fromCavesGeneration.onlyOn", Set.of("minecraft:deep_dark", "minecraft:dripstone_caves", "minecraft:lush_caves")),
        BIOME_MERGING_FROM_CAVES_GENERATION_EXCEPT_ON_BIOMES("biomesMerging.fromCavesGeneration.exceptOn");
        // @formatter:on

        private final String path;
        private final Set<String> defaultValue;
        SetBiomeStringKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetBiomeStringKeys(String path) { this(path, Set.of()); }
    }
    public enum SetMaterialKeys {
        // @formatter:off
        IGNORED_BLOCK_FOR_SURFACE_CALCULATION("ignoredBlockForSurfaceCalculation"),
        BLOCK_TO_KEEP_FROM_SURFACE_WORLD_IN_CAVES("keptReferenceWorldBlocks");
        // @formatter:on
        private final String path;
        private final Set<String> defaultValue;
        SetMaterialKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetMaterialKeys(String path) { this(path, Set.of()); }
    }
    public enum MapMaterialKeys {
        // @formatter:off
        CLEAN_BLOCK_TO_SUPPORT("clean.blocks.toSupport", Map.of(Material.SAND, Material.SANDSTONE, Material.RED_SAND, Material.RED_SANDSTONE, Material.GRAVEL, Material.ANDESITE)),
        CLEAN_BLOCK_TO_REPLACE("clean.blocks.toReplace"),
        SURFACE_WORLD_BLOCK_TO_REPLACE("surfaceWorld.blocks.toReplace");
        // @formatter:on

        private final String path;
        private final Map<Material, Material> defaultValue;
        MapMaterialKeys(String path, Map<Material, Material> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        MapMaterialKeys(String path) { this(path, Map.of()); }
    }

    public enum SetStructureKeys {
        // @formatter:off
        SURUCTURE_EXCEPT("structures.except"),
        SURUCTURE_ONLY("structures.only");
        // @formatter:on

        private final String path;
        private final Set<String> defaultValue;
        SetStructureKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetStructureKeys(String path) { this(path, Set.of()); }
    }

    public enum SetEntityTypeKeys {
        // @formatter:off
        CLEAN_ENTITY_TO_REMOVE("clean.entities.toRemove", Set.of("ITEM"));
        // @formatter:on

        private final String path;
        private final Set<String> defaultValue;
        SetEntityTypeKeys(String path, Set<String> defaultValue) {
            this.path = path;
            this.defaultValue = defaultValue;
        }
        SetEntityTypeKeys(String path) { this(path, Set.of()); }
    }
}
