package me.lukiiy.mapix;

import me.lukiiy.mapling.WorldManager;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mapix extends JavaPlugin {
    private WorldManager<World> worldManager;

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    public static Mapix getInstance() {
        return JavaPlugin.getPlugin(Mapix.class);
    }

    public WorldManager<World> getWorldManager() {
        return worldManager;
    }
}
