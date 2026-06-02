package com.syntask.lancha;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class Lancha extends JavaPlugin {

    private static final Material[] BOATS = {
        Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.BIRCH_BOAT,
        Material.JUNGLE_BOAT, Material.ACACIA_BOAT, Material.DARK_OAK_BOAT,
        Material.MANGROVE_BOAT, Material.CHERRY_BOAT, Material.PALE_OAK_BOAT,
        Material.BAMBOO_RAFT
    };

    private final List<NamespacedKey> recipeKeys = new ArrayList<>();
    private BoatSpeedManager speedManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        SpeedBoatItem.init(this);
        speedManager = new BoatSpeedManager(this);
        getServer().getPluginManager().registerEvents(new BoatListener(this, speedManager), this);
        registerRecipes();
        unlockRecipes();
        getLogger().info("Lancha enabled.");
    }

    @Override
    public void onDisable() {
        if (speedManager != null) speedManager.cleanup();
        getLogger().info("Lancha disabled.");
    }

    private void registerRecipes() {
        for (Material boat : BOATS) {
            register(boat, Material.IRON_INGOT, 50);
            register(boat, Material.GOLD_INGOT, 100);
            register(boat, Material.DIAMOND, 150);
        }
    }

    private void register(Material boat, Material ingredient, int hp) {
        NamespacedKey key = new NamespacedKey(this,
            boat.name().toLowerCase() + "_" + ingredient.name().toLowerCase());
        ItemStack result = SpeedBoatItem.create(new ItemStack(boat), hp);

        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(" I ", " B ", "   ");
        recipe.setIngredient('I', ingredient);
        recipe.setIngredient('B', boat);
        Bukkit.addRecipe(recipe);
        recipeKeys.add(key);
    }

    private void unlockRecipes() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.discoverRecipes(recipeKeys);
        }
    }

    public List<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    public BoatSpeedManager getSpeedManager() {
        return speedManager;
    }
}
