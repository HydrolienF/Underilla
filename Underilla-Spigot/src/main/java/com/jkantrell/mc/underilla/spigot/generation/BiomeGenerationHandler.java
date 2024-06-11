package com.jkantrell.mc.underilla.spigot.generation;

import java.util.Arrays;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class BiomeGenerationHandler implements Listener {
    @EventHandler(priority = EventPriority.LOW)
    public void onWorldInit(WorldInitEvent event) {
        ServerLevel serverLevel = ((CraftWorld) event.getWorld()).getHandle();
        System.out.println("World init event");
        System.out.println(Arrays.asList(ChunkGenerator.class.getDeclaredFields()));
    }
}
