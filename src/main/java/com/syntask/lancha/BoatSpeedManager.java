package com.syntask.lancha;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the current speed multiplier for each player who is in a boat.
 * Speed ramps up smoothly when the player presses forward, and coasts down
 * (or brakes) when they release or press back.
 *
 * Speed values are interpolated from the boat's horsepower level, using the
 * base values at 100hp as the reference.
 */
public class BoatSpeedManager {

    // --- Base reference values at 100hp (blocks/tick) ---
    private static final double BASE_MAX_BOOST     = 1.4;
    private static final double BASE_ACCELERATION   = 0.048;
    private static final double BASE_DECELERATION   = 0.032;
    private static final double BASE_BRAKE_FORCE    = 0.1;

    // --- Configurable values ---

    /** Global multiplier applied to all speed values. Default 1.0. */
    private double globalMultiplier;

    // --- Runtime state ---

    /** Current boost speed for each player UUID currently in a boat */
    private final Map<UUID, Double> currentBoost = new HashMap<>();

    /** Whether each player is currently pressing the forward key */
    private final Map<UUID, Boolean> pressingForward = new HashMap<>();

    /** Whether each player is currently pressing the backward key */
    private final Map<UUID, Boolean> pressingBackward = new HashMap<>();

    public BoatSpeedManager(Lancha plugin) {
        reload(plugin.getConfig());
    }

    public void reload(FileConfiguration config) {
        globalMultiplier = config.getDouble("global-speed-multiplier", 1.0);
    }

    // --- Input state (updated by PlayerInputEvent) ---

    public void setForward(UUID uuid, boolean forward) {
        pressingForward.put(uuid, forward);
    }

    public void setBackward(UUID uuid, boolean backward) {
        pressingBackward.put(uuid, backward);
    }

    public boolean isPressingForward(UUID uuid) {
        return pressingForward.getOrDefault(uuid, false);
    }

    public boolean isPressingBackward(UUID uuid) {
        return pressingBackward.getOrDefault(uuid, false);
    }

    // --- Speed state (updated every VehicleMoveEvent) ---

    /**
     * Advance the speed simulation one tick for this player and return the
     * current boost value to apply this tick.
     *
     * @param hp the boat's horsepower level (used to interpolate speed values)
     */
    public double tickBoost(UUID uuid, int hp) {
        double maxBoost     = getMaxBoost(hp);
        double acceleration = getAcceleration(hp);
        double deceleration = getDeceleration(hp);
        double brakeForce   = getBrakeForce(hp);

        double boost = currentBoost.getOrDefault(uuid, 0.0);

        if (isPressingForward(uuid)) {
            boost = Math.min(boost + acceleration, maxBoost);
        } else if (isPressingBackward(uuid)) {
            boost = Math.max(boost - brakeForce, 0.0);
        } else {
            boost = Math.max(boost - deceleration, 0.0);
        }

        currentBoost.put(uuid, boost);
        return boost;
    }

    /** Call when a player leaves their boat so we don't leak map entries. */
    public void clearPlayer(UUID uuid) {
        currentBoost.remove(uuid);
        pressingForward.remove(uuid);
        pressingBackward.remove(uuid);
    }

    public void cleanup() {
        currentBoost.clear();
        pressingForward.clear();
        pressingBackward.clear();
    }

    // --- HP-interpolated value methods ---

    public double getMaxBoost(int hp) {
        return BASE_MAX_BOOST * (hp / 100.0) * globalMultiplier;
    }

    public double getAcceleration(int hp) {
        return BASE_ACCELERATION * (hp / 100.0) * globalMultiplier;
    }

    public double getDeceleration(int hp) {
        return BASE_DECELERATION * (hp / 100.0) * globalMultiplier;
    }

    public double getBrakeForce(int hp) {
        return BASE_BRAKE_FORCE * (hp / 100.0) * globalMultiplier;
    }

}
