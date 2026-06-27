package me.lukiiy.mapix;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class Cmd { // TODO !!!
    public static final CommandSyntaxException NON_PLAYER = error("This command can only be used by in-game players.");
    public static final CommandSyntaxException NOT_IN_SESSION = error("Not in an editing session.");
    // TODO rework messages?

    public LiteralCommandNode<CommandSourceStack> build() {
        return Commands.literal("mapix")
                .requires(it -> it.getSender() instanceof Player player && player.hasPermission("mapeditor.edit"))
                .then(Commands.literal("tp")
                        .then(Commands.argument("id", new MapIdArgument())
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    Mapix.getInstance().getSessionManager().open(StringArgumentType.getString(it, "id"), player);

                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("save")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            handleSave(player, null);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((it, b) -> {
                                    Mapix.getInstance().getSessionManager().getActiveIds().forEach(b::suggest);

                                    return b.buildFuture();
                                })
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    handleSave(player, StringArgumentType.getString(it, "id"));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            Set<String> ids = Mapix.getInstance().getSessionManager().getActiveIds();
                            if (ids.isEmpty()) throw error("No active sessions.");

                            player.sendMessage(Component.text("Active: " + String.join(", ", ids)).color(NamedTextColor.YELLOW));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("pos1")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            handleSetPosition(player, true);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("pos2")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            handleSetPosition(player, false);
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("clearpos")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            Mapix.getInstance().getSessionManager().clearPosition(player);
                            player.sendMessage(Component.text("Positions cleared!").color(NamedTextColor.GREEN));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("plips")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            var managedWorld = Mapix.getInstance().getSessionManager().mapFor(player);
                            if (managedWorld == null) throw NOT_IN_SESSION;

                            Mapix.getInstance().getSessionManager().reloadPlips(managedWorld);
                            player.sendMessage(Component.text("Plips reloaded.").color(NamedTextColor.GREEN));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("info")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            SessionManager sessionManager = Mapix.getInstance().getSessionManager();
                            var managedWorld = sessionManager.mapFor(player);

                            player.sendMessage(Component.text("Session: " + (managedWorld != null ? managedWorld.getId() : "none")).color(NamedTextColor.YELLOW));

                            String group = sessionManager.getSelectedGroup(player);

                            if (managedWorld != null) {
                                player.sendMessage(Component.text("Groups: ").color(NamedTextColor.GREEN).append(Component.text(String.join(", ", sessionManager.getGroups(managedWorld)))));
                                if (group != null && !group.isBlank()) player.sendMessage(Component.text("Selected group: ").color(NamedTextColor.GREEN).append(Component.text(group))); // TODO arrow
                                player.sendMessage(Component.text("First position: ").color(NamedTextColor.BLUE).append(Component.text(formatLocation(sessionManager.getFirstPosition(player)))));
                                player.sendMessage(Component.text("Second position: ").color(NamedTextColor.BLUE).append(Component.text(formatLocation(sessionManager.getSecondPosition(player)))));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(buildGroup())
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(Component.text("Subcommands:").color(NamedTextColor.LIGHT_PURPLE));

                    Map.of(
                            "tp <id>", "Load a map for editing",
                            "save [id]", "Save & unload",
                            "list", "List active sessions",
                            "pos1", "Set first selection position",
                            "pos2", "Set second selection position",
                            "clearpos", "Clear both positions",
                            "group <sub>", "Manage positio groups",
                            "plips", "Refresh position markers",
                            "info", "Display session info"
                    ).forEach((sub, desc) -> ctx.getSource().getSender().sendMessage(Component.text(sub).append(Component.text(" - " + desc).color(NamedTextColor.GRAY))));

                    return Command.SINGLE_SUCCESS;
                }).build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildGroup() {
        return Commands.literal("group")
                .then(Commands.literal("list")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            var managedWorld = Mapix.getInstance().getSessionManager().mapFor(player);
                            if (managedWorld == null) throw NOT_IN_SESSION;

                            Set<String> groups = Mapix.getInstance().getSessionManager().getGroups(managedWorld);
                            if (groups.isEmpty()) throw error("No groups.");

                            player.sendMessage(Component.text("Groups: " + String.join(", ", groups)));

                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    SessionManager sessionManager = Mapix.getInstance().getSessionManager();
                                    String name = StringArgumentType.getString(it, "name");

                                    var managedWorld = sessionManager.mapFor(player);
                                    if (managedWorld == null) throw NOT_IN_SESSION;

                                    if (sessionManager.createGroup(managedWorld, name)) {
                                        sessionManager.selectGroup(player, name);

                                        player.sendMessage(Component.text("Created group '" + name + "'.").color(NamedTextColor.GREEN));
                                    } else throw error("Group '" + name + "' already exists.");

                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestGroups)
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    SessionManager sessionManager = Mapix.getInstance().getSessionManager();
                                    String name = StringArgumentType.getString(it, "name");

                                    var managedWorld = sessionManager.mapFor(player);
                                    if (managedWorld == null) throw NOT_IN_SESSION;

                                    if (sessionManager.deleteGroup(managedWorld, name)) {
                                        sessionManager.reloadPlips(managedWorld);

                                        player.sendMessage(Component.text("Deleted '" + name + "'.").color(NamedTextColor.RED));
                                    } else throw error("Group '" + name + "' not found.");

                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("select")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(this::suggestGroups)
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    SessionManager sessionManager = Mapix.getInstance().getSessionManager();
                                    String name = StringArgumentType.getString(it, "name");

                                    var managedWorld = sessionManager.mapFor(player);
                                    if (managedWorld == null) throw NOT_IN_SESSION;

                                    if (!sessionManager.getGroups(managedWorld).contains(name)) throw error("Group '" + name + "' not found.");

                                    sessionManager.selectGroup(player, name);
                                    player.sendMessage(Component.text("Selected: " + name).color(NamedTextColor.GREEN));

                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("add")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            SessionManager sessionManager = Mapix.getInstance().getSessionManager();

                            var managedWorld = sessionManager.mapFor(player);
                            if (managedWorld == null) throw NOT_IN_SESSION;

                            String group = sessionManager.getSelectedGroup(player);
                            if (group == null) throw error("No group selected.");

                            Location loc = sessionManager.getFirstPosition(player);

                            if (loc == null) loc = sessionManager.getSecondPosition(player);
                            if (loc == null) loc = player.getLocation();

                            player.sendMessage(sessionManager.addToGroup(managedWorld, group, loc) ? Component.text("Added to '" + group + "'.").color(NamedTextColor.GREEN) : Component.text("Failed to add location.").color(NamedTextColor.RED));
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("remove")
                        .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    SessionManager sessionManager = Mapix.getInstance().getSessionManager();
                                    int index = IntegerArgumentType.getInteger(it, "index");

                                    var managedWorld = sessionManager.mapFor(player);
                                    if (managedWorld == null) throw NOT_IN_SESSION;

                                    var group = sessionManager.getSelectedGroup(player);
                                    if (group == null) throw error("No group selected.");

                                    player.sendMessage(sessionManager.removeFromGroup(managedWorld, group, index) ? Component.text("Removed [" + index + "] from '" + group + "'.").color(NamedTextColor.GREEN) : Component.text("Index " + index + " out of range.").color(NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("scroll")
                        .executes(it -> {
                            if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                            groupScroll(player, 1);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("delta", IntegerArgumentType.integer())
                                .executes(it -> {
                                    if (!(it.getSource().getSender() instanceof Player player)) throw NON_PLAYER;

                                    groupScroll(player, IntegerArgumentType.getInteger(it, "delta"));
                                    return Command.SINGLE_SUCCESS;
                                })));
    }

    private static CommandSyntaxException error(String message) {
        return new SimpleCommandExceptionType(MessageComponentSerializer.message().serialize(Component.text(message).color(NamedTextColor.RED))).create();
    }

    private int handleSave(Player player, String id) throws CommandSyntaxException {
        SessionManager sessionManager = Mapix.getInstance().getSessionManager();

        if (id == null) {
            var managedWorld = sessionManager.mapFor(player);
            if (managedWorld == null) throw NOT_IN_SESSION;

            id = managedWorld.getId();
        }

        sessionManager.save(id);
        return Command.SINGLE_SUCCESS;
    }

    private int handleSetPosition(Player player, boolean first) throws CommandSyntaxException {
        SessionManager sm = Mapix.getInstance().getSessionManager();

        if (sm.mapFor(player) == null) throw NOT_IN_SESSION;

        sm.setPos(player, player.getLocation().toCenterLocation(), first);
        return Command.SINGLE_SUCCESS;
    }

    private int groupScroll(Player player, int delta) {
        String group = Mapix.getInstance().getSessionManager().scrollGroup(player, delta);

        player.sendActionBar(group != null ? Component.text("Group: ").color(NamedTextColor.GRAY).append(Component.text(group).color(NamedTextColor.GREEN)) : Component.text("No groups.").color(NamedTextColor.RED));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestGroups(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        if (ctx.getSource().getSender() instanceof Player player) {
            var managedWorld = Mapix.getInstance().getSessionManager().mapFor(player);

            if (managedWorld != null) Mapix.getInstance().getSessionManager().getGroups(managedWorld).forEach(builder::suggest);
        }

        return builder.buildFuture();
    }

    private String formatLocation(Location loc) {
        return loc == null ? "none" : (int) loc.getX() + " " + (int) loc.getY() + " " + (int) loc.getZ();
    }
}
