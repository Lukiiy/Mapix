package me.lukiiy.mapix;

import me.lukiiy.mapling.ManagedWorld;
import me.lukiiy.mapling.Position;
import me.lukiiy.mapling.WorldData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SessionManager {
    private static final TextColor FIRST_POSITION = TextColor.color(0x7874b7);
    private static final TextColor SECOND_POSITION = TextColor.color(0xaf8a6f);

    private final Map<String, EditSession> sessions = new LinkedHashMap<>();
    private final Map<Player, PlayerEditState> playerState = new WeakHashMap<>();

    public void edit(String id, Player player) {
        Mapix mapix = Mapix.getInstance();

        if (sessions.containsKey(id)) {
            EditSession session = sessions.get(id);
            if (session.world().getHandle() != null) enter(player, session);

            return;
        }

        try {
            var managedWorld = mapix.getWorldManager().load(id);
            BossBar bar = BossBar.bossBar(Component.text("Editing ").color(NamedTextColor.GRAY).append(Component.text(id).color(NamedTextColor.YELLOW)), 1f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
            EditSession session = new EditSession(managedWorld, new ArrayList<>(), bar);

            sessions.put(id, session);
            spawnPlips(session);
            enter(player, session);

            mapix.getLogger().info("Opened edit session: " + id);
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to initialize the session!").color(NamedTextColor.RED));
            mapix.getLogger().warning("Failed to open session '" + id + "': " + e.getMessage());
        }
    }

    public void save(String id) {
        var session = sessions.remove(id);
        if (session == null) return;

        var managedWorld = session.world();
        var world = managedWorld.getHandle();

        if (world != null) {
            Location fallback = Bukkit.getWorlds().getFirst().getSpawnLocation();

            List.copyOf(world.getPlayers()).forEach(p -> {
                p.hideBossBar(session.bar());
                p.sendMessage(Component.text("Session saved.").color(NamedTextColor.GREEN));
                p.teleport(fallback);

                Item.removeAll(p.getInventory());
                playerState.remove(p);
            });

            removePlips(session);
        }

        var manager = Mapix.getInstance().getWorldManager();

        manager.save(id);
        manager.unload(id, false);

        Mapix.getInstance().getLogger().info("Saved and closed session: " + id);
    }

    public void saveAll() {
        new ArrayList<>(sessions.keySet()).forEach(this::save);
    }

    public Set<String> getActiveIds() {
        return Collections.unmodifiableSet(sessions.keySet());
    }

    public EditSession sessionFor(Player player) {
        for (EditSession session : sessions.values()) {
            var world = session.world().getHandle();

            if (world != null && world.equals(player.getWorld())) return session;
        }

        return null;
    }

    public ManagedWorld<World> mapFor(Player player) {
        EditSession session = sessionFor(player);

        return session == null ? null : session.world();
    }

    private void enter(Player player, EditSession session) {
        var world = session.world().getHandle();
        if (world != null) player.teleport(world.getSpawnLocation());

        player.setGameMode(GameMode.CREATIVE);
        player.setFlying(true);
        player.showBossBar(session.bar());

        Item.applyAll(player.getInventory());
        player.sendMessage(Component.text("Teleporting to session " + session.world().getHandle().getName()).color(NamedTextColor.GREEN));
    }

    public void exit(Player player, boolean fullQuit) {
        sessions.values().stream()
                .filter(s -> s.world().getHandle() != null && s.world().getHandle() == player.getWorld())
                .findFirst().ifPresent(session -> player.hideBossBar(session.bar()));

        Item.removeAll(player.getInventory());

        if (fullQuit) playerState.remove(player);
    }

    public void setPos(Player player, Location loc, boolean first) {
        PlayerEditState state = getState(player);

        if (first) {
            if (loc.equals(state.first)) return;

            state.first = loc;
        } else {
            if (loc.equals(state.second)) return;

            state.second = loc;
        }

        player.sendMessage(Component.text("Position " + (first ? "1" : "2") + " set! (" + loc.blockX() + " " + loc.blockY() + " " + loc.blockZ() + ")").color(first ? FIRST_POSITION : SECOND_POSITION));
        if (state.selectionMode == SelectionMode.POINT) {
            player.sendMessage(Component.text("[ Add to " + state.selectedGroup + " ]").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.callback(aud -> {
                if (!(aud instanceof Player p)) return;

                var managedWorld = mapFor(player);
                if (managedWorld == null) return;

                addToGroup(managedWorld, state.selectedGroup, state.first);
            })));
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .25f, 1);
    }

    public Location getFirstPosition(Player player) {
        return getState(player).first;
    }

    public Location getSecondPosition(Player player) {
        return getState(player).second;
    }

    public void clearPosition(Player player) {
        PlayerEditState state = getState(player);

        state.first = null;
        state.second = null;
    }

    // Groups

    public Set<String> getGroups(ManagedWorld<World> managedWorld) {
        return managedWorld.getData().groupKeys();
    }

    public List<Position> getGroupPositions(ManagedWorld<World> managedWorld, String group) {
        WorldData data = managedWorld.getData();

        if (!data.hasGroup(group)) return new ArrayList<>();

        return new ArrayList<>(data.group(group)); // clone
    }

    public boolean createGroup(ManagedWorld<World> managedWorld, String name) {
        WorldData data = managedWorld.getData();

        if (data.hasGroup(name)) return false;
        data.group(name);

        return true;
    }

    public boolean deleteGroup(ManagedWorld<World> managedWorld, String name) {
        boolean removed = managedWorld.getData().removeGroup(name);
        if (removed) reloadPlips(managedWorld);

        return removed;
    }

    public String getSelectedGroup(Player player) {
        ManagedWorld<World> managedWorld = mapFor(player);
        if (managedWorld == null) return null;

        PlayerEditState state = getState(player);
        if (state.selectedGroup == null) {
            Set<String> groups = getGroups(managedWorld);

            if (!groups.isEmpty()) state.selectedGroup = groups.iterator().next();
        }

        return state.selectedGroup;
    }

    public void selectGroup(Player player, String group) {
        getState(player).selectedGroup = group;
    }

    public String scrollGroup(Player player, int delta) {
        ManagedWorld<World> managedWorld = mapFor(player);
        if (managedWorld == null) return null;

        List<String> keys = new ArrayList<>(getGroups(managedWorld));
        if (keys.isEmpty()) return null;

        int idx = Math.floorMod(keys.indexOf(getSelectedGroup(player)) + delta, keys.size());
        String group = keys.get(idx);

        getState(player).selectedGroup = group;
        return group;
    }

    public boolean addToGroup(ManagedWorld<World> managedWorld, String group, Location loc) {
        WorldData data = managedWorld.getData();
        if (!data.hasGroup(group)) return false;

        data.group(group).add(Utils.toPos(loc));
        reloadPlips(managedWorld);

        return true;
    }

    public boolean removeFromGroup(ManagedWorld<World> managedWorld, String group, int index) {
        WorldData data = managedWorld.getData();
        if (!data.hasGroup(group)) return false;

        List<Position> positions = data.group(group);
        if (index < 0 || index >= positions.size()) return false;

        positions.remove(index);
        reloadPlips(managedWorld);

        return true;
    }

    public int nearestIndex(Player player, List<Position> positions) {
        if (positions.isEmpty()) return -1;

        Location loc = player.getLocation();
        int best = 0;
        double min = Double.MAX_VALUE;

        for (int idx = 0; idx < positions.size(); idx++) {
            Position p = positions.get(idx);

            double distance = Math.pow(p.getX() - loc.getX(), 2) + Math.pow(p.getY() - loc.getY(), 2) + Math.pow(p.getZ() - loc.getZ(), 2);
            if (distance < min) {
                min = distance;
                best = idx;
            }
        }

        return best;
    }

    public void reloadPlips(ManagedWorld<World> managedWorld) {
        sessions.values().stream().filter(s -> s.world() == managedWorld)
                .findFirst().ifPresent(s -> {
                    removePlips(s);
                    spawnPlips(s);
                });
    }

    public void togglePlips(Player player) {
        PlayerEditState state = getState(player);

        state.plips = !state.plips;

        ManagedWorld<World> managedWorld = mapFor(player);
        if (managedWorld == null) return;

        sessions.values().stream().filter(s -> s.world() == managedWorld).findFirst().ifPresent(session -> session.plips().forEach(display -> {
            if (state.plips) player.showEntity(Mapix.getInstance(), display); else player.hideEntity(Mapix.getInstance(), display);
        }));
    }

    // the cool thing :3
    private void spawnPlips(EditSession session) {
        World world = session.world().getHandle();
        if (world == null) return;

        WorldData data = session.world().getData();

        for (String group : data.groupKeys()) {
            List<Position> positions = data.group(group);

            for (int i = 0; i < positions.size(); i++) {
                Location base = Utils.toLoc(world, positions.get(i));
                boolean blocked = !base.getBlock().isPassable();

                Component text = Component.text(group).color(NamedTextColor.WHITE).appendSpace().append(Component.text("[" + i + "]").color(NamedTextColor.YELLOW));
                if (blocked) text = text.append(Component.newline()).append(Component.text("↓").color(NamedTextColor.GRAY));

                Component fText = text;
                TextDisplay display = world.spawn(blocked ? base.clone().add(0, 1, 0) : base, TextDisplay.class, e -> {
                    e.text(fText);
                    e.setPersistent(false);
                    e.setDefaultBackground(false);
                    e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                    e.setSeeThrough(true);
                    e.setBillboard(Display.Billboard.VERTICAL);
                    e.setViewRange(.25f);
                });

                session.plips().add(display);

                playerState.forEach((p, state) -> {
                    if (!state.plips) p.hideEntity(Mapix.getInstance(), display);
                });
            }
        }
    }

    private void removePlips(EditSession session) {
        session.plips().forEach(Entity::remove);
        session.plips().clear();
    }

    public PlayerEditState getState(Player player) {
        return playerState.computeIfAbsent(player, a -> new PlayerEditState());
    }
}
