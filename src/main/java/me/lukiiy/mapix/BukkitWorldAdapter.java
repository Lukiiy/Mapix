package me.lukiiy.mapix;

import me.lukiiy.mapling.Position;
import me.lukiiy.mapling.WorldAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;

public final class BukkitWorldAdapter implements WorldAdapter<World> {
    @Override
    public World load(File folder) {
        String name = folder.getName();

        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;

        return new WorldCreator(name).createWorld();
    }

    @Override
    public void save(World world) {
        world.save();
    }

    @Override
    public boolean unload(World world) {
        world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation()));

        return Bukkit.unloadWorld(world, false);
    }

    public Location getLocation(World world, Position position) {
        return new Location(world, position.getX(), position.getY(), position.getZ(), position.getYaw(), position.getPitch());
    }
}
