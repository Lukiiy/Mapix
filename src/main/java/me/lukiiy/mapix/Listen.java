package me.lukiiy.mapix;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class Listen implements Listener {
    @EventHandler
    public void interact(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;

        SessionManager sessionManager = Mapix.getInstance().getSessionManager();
        Player player = e.getPlayer();

        var managedWorld = sessionManager.mapFor(player);
        if (managedWorld == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!Item.isEditorItem(item)) return;

        e.setCancelled(true);

        if (item.isSimilar(Item.POSITION_SELECTOR)) {
            if (e.getClickedBlock() == null) return;

            Location loc = e.getClickedBlock().getLocation().toCenterLocation();

            sessionManager.setPos(player, loc, e.getAction() == Action.LEFT_CLICK_BLOCK);
        }
    }

    @EventHandler
    public void worldChange(PlayerChangedWorldEvent e) {
        Player player = e.getPlayer();
        SessionManager sessionManager = Mapix.getInstance().getSessionManager();

        // Left an edit world
        sessionManager.getActiveIds().stream()
                .map(id -> Mapix.getInstance().getWorldManager().get(id))
                .filter(managedWorld -> managedWorld != null && managedWorld.getHandle() != null && managedWorld.getHandle().equals(e.getFrom())).findFirst()
                .ifPresent(mw -> sessionManager.exit(player, false));

        EditSession entered = sessionManager.sessionFor(player);

        // Entered an edit world
        if (entered != null) player.showBossBar(entered.bar());
    }

    @EventHandler
    public void drop(PlayerDropItemEvent e) {
        if (Item.isEditorItem(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void quit(PlayerQuitEvent e) {
        Mapix.getInstance().getSessionManager().exit(e.getPlayer(), true);
    }
}
