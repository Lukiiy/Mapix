package me.lukiiy.mapix;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
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

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, it -> it.registrar().register(new Command().build(), "Mapix's main command."));
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
