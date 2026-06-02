package com.syntask.lancha;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class BoatListener implements Listener {

    private final Lancha plugin;
    private final BoatSpeedManager speeds;

    public BoatListener(Lancha plugin, BoatSpeedManager speeds) {
        this.plugin = plugin;
        this.speeds = speeds;
    }

    /**
     * PlayerInputEvent fires whenever the client changes which keys it is holding.
     * This lets us track W/S state accurately without polling.
     *
     * Added in Spigot 1.20.4 (API 1.20.4-R0.1).
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInput(PlayerInputEvent event) {
        Player player = event.getPlayer();

        // Only track input while in a boat
        if (!(player.getVehicle() instanceof Boat)) return;

        UUID uuid = player.getUniqueId();
        speeds.setForward(uuid,  event.getInput().isForward());
        speeds.setBackward(uuid, event.getInput().isBackward());
    }

    /**
     * Tag a boat entity with its horsepower when placed from a speed boat item.
     * Fires before the item is consumed, so the hand item is still present.
     * Clears the entity's custom name so the floating nametag doesn't appear.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Boat boat)) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack hand = player.getInventory().getItem(event.getHand());
        if (SpeedBoatItem.isSpeedBoat(hand)) {
            int hp = SpeedBoatItem.getHp(hand);
            boat.getPersistentDataContainer().set(
                SpeedBoatItem.getHpKey(), PersistentDataType.INTEGER, hp);
            boat.setCustomName(null);
        }
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        event.getPlayer().discoverRecipes(plugin.getRecipeKeys());
    }

    /**
     * VehicleMoveEvent fires every tick the vehicle moves.
     * We apply our boost on top of whatever vanilla velocity already exists.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (!(vehicle instanceof Boat boat)) return;

        // Must have exactly one passenger and they must be a player
        if (boat.getPassengers().isEmpty()) return;
        if (!(boat.getPassengers().get(0) instanceof Player player)) return;

        UUID uuid = player.getUniqueId();

        // Check if this boat is a speed boat (tagged via EntityPlaceEvent)
        int hp = boat.getPersistentDataContainer()
            .getOrDefault(SpeedBoatItem.getHpKey(), PersistentDataType.INTEGER, 0);
        if (hp <= 0) return;  // vanilla boat, no speed boost

        // Advance the speed simulation one tick using boat's HP
        double boost = speeds.tickBoost(uuid, hp);

        // Nothing to do if no boost is accumulated
        if (boost <= 0.0) return;

        // Boost along the direction the boat is currently facing.
        // We deliberately keep Y=0 so we never push the boat up or down.
        Vector facing = boat.getLocation().getDirection();
        facing.setY(0);

        // Guard against a zero-length vector (shouldn't happen but be safe)
        if (facing.lengthSquared() < 1e-6) return;
        facing.normalize();

        // Add our boost on top of the current velocity so vanilla physics
        // (turning, wall collisions, etc.) still work.
        Vector current = boat.getVelocity();
        Vector extra   = facing.multiply(boost);

        // Only add forward boost — don't amplify sideways drift or vertical movement
        boat.setVelocity(new Vector(
            current.getX() + extra.getX(),
            current.getY(),                 // leave Y alone (gravity, jumps, etc.)
            current.getZ() + extra.getZ()
        ));
    }

    /**
     * Clean up state when a player gets out of their boat.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Boat)) return;
        if (!(event.getExited() instanceof Player player)) return;
        speeds.clearPlayer(player.getUniqueId());
    }
}
