package com.syntask.lancha;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SpeedBoatItem {

    private static NamespacedKey HP_KEY;

    public static void init(Lancha plugin) {
        HP_KEY = new NamespacedKey(plugin, "speed_boat_hp");
    }

    public static NamespacedKey getHpKey() {
        return HP_KEY;
    }

    public static boolean isSpeedBoat(ItemStack item) {
        if (item == null || !isBoatMaterial(item.getType())) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(HP_KEY, PersistentDataType.INTEGER);
    }

    public static int getHp(ItemStack item) {
        if (!isSpeedBoat(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(HP_KEY, PersistentDataType.INTEGER, 0);
    }

    public static ItemStack create(ItemStack boatItem, int hp) {
        Material material = boatItem.getType();
        ItemStack result = new ItemStack(material, 1);
        ItemMeta meta = result.getItemMeta();

        meta.displayName(Component.text(getWoodName(material) + " Speed Boat (" + hp + "hp)")
            .decoration(TextDecoration.ITALIC, false));

        meta.getPersistentDataContainer().set(HP_KEY, PersistentDataType.INTEGER, hp);

        result.setItemMeta(meta);
        return result;
    }

    static boolean isBoatMaterial(Material material) {
        return switch (material) {
            case OAK_BOAT, SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT,
                 ACACIA_BOAT, DARK_OAK_BOAT, MANGROVE_BOAT, CHERRY_BOAT,
                 PALE_OAK_BOAT, BAMBOO_RAFT -> true;
            default -> false;
        };
    }

    public static String getWoodName(Material material) {
        return switch (material) {
            case OAK_BOAT -> "Oak";
            case SPRUCE_BOAT -> "Spruce";
            case BIRCH_BOAT -> "Birch";
            case JUNGLE_BOAT -> "Jungle";
            case ACACIA_BOAT -> "Acacia";
            case DARK_OAK_BOAT -> "Dark Oak";
            case MANGROVE_BOAT -> "Mangrove";
            case CHERRY_BOAT -> "Cherry";
            case PALE_OAK_BOAT -> "Pale Oak";
            case BAMBOO_RAFT -> "Bamboo";
            default -> {
                String name = material.name().toLowerCase()
                    .replace("_boat", "").replace("_raft", "");
                String[] parts = name.split("_");
                StringBuilder sb = new StringBuilder();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(Character.toUpperCase(part.charAt(0)))
                          .append(part.substring(1));
                    }
                }
                yield sb.toString();
            }
        };
    }
}
