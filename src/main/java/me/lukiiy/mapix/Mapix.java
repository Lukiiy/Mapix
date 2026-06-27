package me.lukiiy.mapix;

import me.lukiiy.mapling.WorldManager;
import me.lukiiy.mapling.provided.TomlWorldDataStore;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mapix extends JavaPlugin {
    private final WorldManager<World> worldManager = new WorldManager<>(Bukkit.getWorldContainer(), new BukkitWorldAdapter(), new TomlWorldDataStore());
    private final SessionManager sessionManager = new SessionManager();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new Listen(), this);
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) sessionManager.saveAll();
    }

    public static Mapix getInstance() {
        return JavaPlugin.getPlugin(Mapix.class);
    }

    public WorldManager<World> getWorldManager() {
        return worldManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
