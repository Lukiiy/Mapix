package me.lukiiy.mapix;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class Dialogs {
    private static final ClickCallback.Options ONCE = ClickCallback.Options.builder().uses(1).build();

    public static void menu(Player player) {
        SessionManager sessions = Mapix.getInstance().getSessionManager();

        var world = sessions.mapFor(player);
        if (world == null) return;

        PlayerEditState state = sessions.getState(player);
        List<DialogBody> body = new ArrayList<>();

        // Selection

        body.add(category("Selection"));

        body.add(line("Mode", state.selectionMode.name()));
        body.add(line("Pos 1", "..."));
        body.add(line("Pos 2", "..."));

        body.add(actions(
                action("Clear", NamedTextColor.RED, sessions::clearPosition),
                action("Save to Data", NamedTextColor.GREEN, p -> sessions.addToGroup(world, state.selectedGroup, state.first))
        ));

        body.add(spacer());

        // Groups

        body.add(category("Groups"));
        body.add(description("The active group stores newly saved selections."));
        body.add(line("Active Group", Optional.ofNullable(sessions.getSelectedGroup(player)).orElse("None")));
        body.add(line("Selection Mode", state.selectionMode.name()));
        body.add(line("Show Plips", state.plips ? "ON" : "OFF"));

        body.add(spacer());

        // Session

        body.add(category("Session"));
        body.add(actions(action("Save", NamedTextColor.GREEN, p -> sessions.save(world.getId()))));

        player.showDialog(Dialog.create(dialog -> dialog.empty()
                .base(DialogBase.builder(Component.text("Editing ", NamedTextColor.GRAY).append(Component.text(world.getId(), NamedTextColor.YELLOW))).body(body).build())
                .type(DialogType.notice())
        ));
    }

    // help

    private static DialogBody category(String title) {
        return DialogBody.plainMessage(Component.text(title, NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
    }

    private static DialogBody description(String text) {
        return DialogBody.plainMessage(Component.text(text, NamedTextColor.GRAY));
    }

    private static DialogBody line(String key, String value) {
        return DialogBody.plainMessage(Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.WHITE)));
    }

    private static DialogBody actions(Component... actions) {
        return DialogBody.plainMessage(Component.join(JoinConfiguration.separator(Component.space().append(Component.space())), actions));
    }

    private static DialogBody spacer() {
        return DialogBody.plainMessage(Component.empty());
    }

    private static Component action(String title, NamedTextColor color, Consumer<Player> callback) {
        return Component.text("[ ").color(color).decorate(TextDecoration.BOLD).append(Component.text(title)).append(Component.text(" ]")).clickEvent(ClickEvent.callback(aud -> { if (aud instanceof Player player) callback.accept(player); }, ONCE)).hoverEvent(Component.text(title));
    }

    private static ActionButton actionButton(String text, NamedTextColor color, DialogActionCallback callback) {
        return ActionButton.builder(Component.text(text, color)).action(DialogAction.customClick(callback, ONCE)).build();
    }

    public static void nameInput(Player player, Component title, Component label, Consumer<String> callback) {
        DialogBase base = DialogBase.builder(title)
                .inputs(List.of(DialogInput.text("value", label).build()))
                .build();

        DialogType type = DialogType.confirmation(
                actionButton("Confirm", NamedTextColor.GREEN, (view, audience) -> {
                    String value = view.getText("value");

                    if (value != null && !value.isBlank()) callback.accept(value.trim());
                }),

                actionButton("Cancel", NamedTextColor.RED, null)
        );

        player.showDialog(Dialog.create(dialog -> dialog.empty().base(base).type(type)));
    }
}