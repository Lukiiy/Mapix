package me.lukiiy.mapix;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

public class Item {
    public static final NamespacedKey KEY = new NamespacedKey(Mapix.getInstance(), "item");

    public static final ItemStack POSITION_SELECTOR = create(Material.BLAZE_ROD, i -> {
        i.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        i.setData(DataComponentTypes.ITEM_NAME, Component.text("Position Selector").color(NamedTextColor.YELLOW));
        i.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        i.addUnsafeEnchantment(Enchantment.EFFICIENCY, 1);
    });

    public static final ItemStack GROUP_TOOL = create(Material.REDSTONE, i -> {
        i.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        i.setData(DataComponentTypes.ITEM_NAME, Component.text("Group Tool").color(NamedTextColor.RED));
        i.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        i.addUnsafeEnchantment(Enchantment.EFFICIENCY, 1);
    });

    public static final ItemStack MENU = create(Material.NETHER_STAR, i -> {
        i.setData(DataComponentTypes.MAX_STACK_SIZE, 1);
        i.setData(DataComponentTypes.ITEM_NAME, Component.text("Menu").color(NamedTextColor.RED));
    });

    private static ItemStack create(Material material, Consumer<ItemStack> builder) {
        ItemStack item = ItemStack.of(material);

        builder.accept(item);
        item.editPersistentDataContainer(c -> c.set(KEY, PersistentDataType.BOOLEAN, true));

        return item;
    }

    public static boolean isEditorItem(ItemStack itemStack) {
        return itemStack.getPersistentDataContainer().has(KEY);
    }

    public static void applyAll(PlayerInventory inventory) {
        inventory.setItem(0, POSITION_SELECTOR);
    }

    public static void removeAll(PlayerInventory inventory) {
        Arrays.stream(inventory.getContents()).filter(Objects::nonNull).filter(Item::isEditorItem).forEach(inventory::remove);
    }
}
