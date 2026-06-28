package me.lukiiy.mapix;

import me.lukiiy.mapling.ManagedWorld;
import me.lukiiy.mapling.Position;
import me.lukiiy.mapling.WorldData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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

    // Groups TODO TODO TODO - Ensure compatibility with data lib!

    public Set<String> getGroups(ManagedWorld<World> managedWorld) { // lol
        WorldData section = managedWorld.getData().getSection("groups");

        return section == null ? Collections.emptySet() : new LinkedHashSet<>(section.keys());
    }

    public List<Position> getGroupPositions(ManagedWorld<World> managedWorld, String group) {
        var sec = managedWorld.getData().getSection("groups");
        if (sec == null) return new ArrayList<>();

        Object raw = sec.get(group);
        if (!(raw instanceof List<?> list)) return new ArrayList<>();

        return list.stream().filter(Position.class::isInstance).map(Position.class::cast).collect(Collectors.toCollection(ArrayList::new));
    }

    public boolean createGroup(ManagedWorld<World> managedWorld, String name) {
        if (getGroups(managedWorld).contains(name)) return false;

        managedWorld.getData().section("groups").set(name, new ArrayList<>());
        return true;
    }

    public boolean deleteGroup(ManagedWorld<World> managedWorld, String name) {
        WorldData section = managedWorld.getData().getSection("groups");
        if (section == null || !section.contains(name)) return false;

        section.remove(name);
        return true;
    }

    public String getSelectedGroup(Player player) {
        var managedWorld = mapFor(player);
        if (managedWorld == null) return null;

        PlayerEditState state = getState(player);
        if (state.selectedGroup == null) {
            var groups = getGroups(managedWorld);

            if (!groups.isEmpty()) state.selectedGroup = groups.iterator().next();
        }

        return state.selectedGroup;
    }

    public void selectGroup(Player player, String group) {
        getState(player).selectedGroup = group;
    }

    public String scrollGroup(Player player, int delta) {
        var managedWorld = mapFor(player);
        if (managedWorld == null) return null;

        List<String> keys = new ArrayList<>(getGroups(managedWorld));
        if (keys.isEmpty()) return null;

        int idx = Math.floorMod(keys.indexOf(getSelectedGroup(player)) + delta, keys.size());
        String group = keys.get(idx);

        getState(player).selectedGroup = group;
        return group;
    }

    public boolean addToGroup(ManagedWorld<World> managedWorld, String group, Location loc) {
        if (!getGroups(managedWorld).contains(group)) return false;

        List<Position> positions = getGroupPositions(managedWorld, group);

        positions.add(Utils.toPos(loc));
        managedWorld.getData().section("groups").set(group, positions);
        reloadPlips(managedWorld);

        return true;
    }

    public boolean removeFromGroup(ManagedWorld<World> managedWorld, String group, int index) {
        List<Position> positions = getGroupPositions(managedWorld, group);
        if (index < 0 || index >= positions.size()) return false;

        positions.remove(index);
        managedWorld.getData().section("groups").set(group, positions);
        reloadPlips(managedWorld);

        return true;
    }

    public void reloadPlips(ManagedWorld<World> managedWorld) {
        sessions.values().stream().filter(s -> s.world() == managedWorld)
                .findFirst().ifPresent(s -> {
                    removePlips(s);
                    spawnPlips(s);
                });
    }

    // the cool thing :3
    private void spawnPlips(EditSession session) {
        var world = session.world().getHandle();
        if (world == null) return;

        getGroups(session.world()).forEach(group -> {
            List<Position> positions = getGroupPositions(session.world(), group);

            IntStream.range(0, positions.size()).forEach(i -> {
                Location base = Utils.toLoc(world, positions.get(i));
                boolean blocked = !base.getBlock().isPassable();

                Component text = Component.text(group).color(NamedTextColor.WHITE).appendSpace().append(Component.text("[" + i + "]").color(NamedTextColor.YELLOW));
                if (blocked) text = text.append(Component.newline()).append(Component.text("↓").color(NamedTextColor.GRAY));

                final Component fText = text;
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
            });
        });
    }

    private void removePlips(EditSession session) {
        session.plips().forEach(Entity::remove);
        session.plips().clear();
    }

    private PlayerEditState getState(Player player) {
        return playerState.computeIfAbsent(player, a -> new PlayerEditState());
    }
}
