package me.lukiiy.mapix;

import me.lukiiy.mapling.WorldAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;

public final class BukkitWorldAdapter implements WorldAdapter<World> {
    @Override
    public World load(File folder) {
        String name = folder.getName();
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;

        if (!new File(folder, "level.dat").exists()) throw new IllegalArgumentException("No valid world at: " + folder.getPath());

        return new WorldCreator(name).createWorld();
    }

    @Override
    public void save(World world) {
        world.save();
    }

    @Override
    public boolean unload(World world) {
        world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation()));

        return Bukkit.unloadWorld(world, true);
    }
}