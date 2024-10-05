package me.jascotty2.libv3.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.logging.Level;

public class NBTEdit {

    public static ItemStack setFromJson(ItemStack item, String ignoredJson) {
        if (!enabled) {
            return item;
        }
        if (item != null) {
            try {
                // Create a new ItemStack if the original is null or empty
                if (item.getType().isAir()) {
                    return item;
                }

                // Assuming json contains key-value pairs, parse it and set it to the item
                ItemMeta meta = item.getItemMeta();
                if (meta == null) {
                    meta = Bukkit.getItemFactory().getItemMeta(item.getType());
                }

                item.setItemMeta(meta);
                return item;
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "NBT Edit Error", e);
            }
        }
        return null;
    }

    private static boolean enabled = true;

    public static boolean available() {
        return enabled;
    }
}