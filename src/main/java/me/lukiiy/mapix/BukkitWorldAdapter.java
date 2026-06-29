package me.lukiiy.mapix;

import me.lukiiy.mapling.WorldAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class BukkitWorldAdapter implements WorldAdapter<World> {
    private final Map<String, File> sourceDirectories = new HashMap<>();
    private final Map<String, File> activeDirectories = new HashMap<>();
    private final Set<String> markedForSave = new HashSet<>();

    @Override
    public World load(File folder) {
        if (!new File(folder, "level.dat").exists()) throw new IllegalArgumentException("No valid world at " + folder.getPath());

        String name = folder.getName() + "_" + Integer.toHexString(ThreadLocalRandom.current().nextInt());
        File copy = new File(Bukkit.getWorldContainer(), name);

        try {
            copyFolder(folder, copy);
        } catch (IOException e) {
            Mapix.getInstance().getLogger().severe("Failed to copy map " + folder.getName() + "! " + e.getMessage());
        }

        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;

        World world = new WorldCreator(name).createWorld();

        sourceDirectories.put(name, folder);
        activeDirectories.put(name, copy);
        return world;
    }

    @Override
    public void save(World world) {
        world.save();
        markedForSave.add(world.getName());
    }

    @Override
    public boolean unload(World world) {
        world.getPlayers().forEach(p -> p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation()));

        String name = world.getName();
        File copy = activeDirectories.remove(name);
        File original = sourceDirectories.remove(name);

        boolean saved = markedForSave.remove(name);
        boolean result = Bukkit.unloadWorld(world, false); // here due to locks

        if (copy != null && copy.exists()) {
            if (saved && original != null) {
                try {
                    copyFolder(copy, original);
                } catch (IOException e) {
                    Mapix.getInstance().getLogger().severe("Failed to write map " +original.getName() + " back!" + e.getMessage());
                }
            }

            try {
                deleteFolder(copy);
            } catch (IOException e) {
                Mapix.getInstance().getLogger().warning("Could not delete instanced world (in root) " + copy.getName() + "! " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Recursively copies a folder from the source directory to the target directory.
     * @param source Source folder
     * @param target Target destination folder
     */
    private static void copyFolder(File source, File target) throws IOException {
        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();

        Files.createDirectories(targetPath);

        try (Stream<Path> stream = Files.walk(sourcePath)) {
            Iterator<Path> it = stream.iterator();

            while (it.hasNext()) {
                Path path = it.next();
                if (path.getFileName().toString().equals("mapling.toml")) continue;

                Path resolved = targetPath.resolve(sourcePath.relativize(path));

                if (Files.isDirectory(path)) Files.createDirectories(resolved); else Files.copy(path, resolved, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Recursively deletes a folder and all of its contents.
     * @param target Folder to delete
     */
    private static void deleteFolder(File target) throws IOException {
        try (Stream<Path> stream = Files.walk(target.toPath())) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        }
    }
}