# AGENTS.md

Instructions for AI coding agents working on the Lancha plugin.

## Quick Reference

- **Build**: `mvn package` → `target/lancha-1.0.0.jar`
- **Test server**: Paper 26.2 in `test/` (gitignored) — see [CONTRIBUTING.md](CONTRIBUTING.md) for setup
- **Hot reload**: `mvn package && cp target/lancha-*.jar test/plugins/` then `/reload confirm` in-server
- **No unit tests** — all testing is manual via the local Paper server
- **Full docs**: [README.md](README.md) (user-facing), [CONTRIBUTING.md](CONTRIBUTING.md) (dev setup)

## Architecture

Four source files in `com.syntask.lancha`:

| File | Role |
|------|------|
| `Lancha.java` | Entry point (`JavaPlugin`). Registers 30 shaped recipes (10 boat types × 3 tiers), initializes managers, registers listener. |
| `BoatListener.java` | Handles 6 events: `PlayerInputEvent` (W/S tracking), `EntityPlaceEvent` (HP tagging), `VehicleDestroyEvent` (correct drops), `VehicleMoveEvent` (velocity boost), `VehicleExitEvent` (cleanup), `PlayerJoinEvent` (recipe grant). |
| `BoatSpeedManager.java` | Per-player state machine. Tracks input + boost via `HashMap<UUID, ...>`. `tickBoost(uuid, hp)` returns the boost value each tick. |
| `SpeedBoatItem.java` | Static utility. Creates speed boat `ItemStack`s with `PersistentDataContainer` HP tag. Provides `isSpeedBoat()`, `getHp()`, `getWoodName()`, `isBoatMaterial()`. |

**Data flow per tick**: `PlayerInputEvent` → `BoatSpeedManager.setForward/setBackward()` → `VehicleMoveEvent` → `BoatSpeedManager.tickBoost(uuid, hp)` → velocity vector applied to boat (XZ only, Y preserved).

## Key Conventions

- **Java 17** — use switch expressions, pattern-matching `instanceof`, text blocks where they improve clarity
- **Flat package** — everything in `com.syntask.lancha`, no sub-packages
- **Paper-only APIs** — the plugin requires Paper, not Spigot. `PlayerInputEvent` does not exist on Spigot
- **Adventure text** — use `net.kyori.adventure.text.Component` for display names (Paper bundles Adventure natively)
- **PersistentDataContainer** — use `PersistentDataType.INTEGER` for entity metadata (not legacy `MetadataValue`)
- **Event priority**: `MONITOR` for read-only side effects (input/place/exit), `HIGH` for events that cancel (destroy), `NORMAL` for velocity modification (move)

## Pitfalls to Avoid

1. **`boat.getType()` returns `EntityType`, not `Material`** — use `boat.getBoatMaterial()` to get the wood material
2. **`Boat.Type` is deprecated** since 1.21.2 — do not use it for new code
3. **`SpeedBoatItem.init(plugin)` must be called before any PDC operations** — `HP_KEY` is static and will NPE if uninitialized
4. **Paper download URLs change** — the old `api.papermc.io/v2` is sunset. Current CDN: `fill-data.papermc.io/v1/objects/{sha256}/{filename}`
5. **`EntityPlaceEvent.getPlayer()` can be null** — always null-check before using the player
6. **`VehicleDestroyEvent` handler cancels the event** — the handler manually drops the correct item and removes the entity, so vanilla drop logic is skipped
7. **No `/lancha reload` command** — config changes require a server restart

## Handling Minecraft/Paper Updates

When a new Minecraft/Paper version is released, follow this checklist to keep the plugin compatible.

### Version Bump Checklist

1. **Update `pom.xml`** — change the Paper API dependency version. Check [Maven Central](https://central.sonatype.com/artifact/io.papermc.paper/paper-api) or the [Paper downloads page](https://papermc.io/downloads/paper) for the latest stable build. Paper's version format is `{MC_VERSION}.build.{BUILD_NUMBER}-stable`.

2. **Update `plugin.yml`** — set `api-version` to match the new Minecraft version (e.g., `'26.2'`).

3. **Update `CONTRIBUTING.md`** — change the download URL and jar filename to match the new Paper build.

4. **Check for deprecated API removals** — run `mvn package` and look for deprecation warnings. Paper removes deprecated APIs after ~2 versions. Key areas in this plugin:
   - `Boat` methods (`getWoodType()`, `Boat.Type`) — already migrated to `getBoatMaterial()`
   - `EntityPlaceEvent` — check if the event contract changes
   - `PlayerInputEvent` — Paper-only, verify it still exists in the new version

5. **Check for new boat types** — Minecraft occasionally adds new boat variants (e.g., bamboo rafts in 1.20). If a new `Material` is added (like `MANGROVE_BOAT` was), add it to the `BOATS` array and update `SpeedBoatItem.isBoatMaterial()` + `getWoodName()`.

6. **Test the full lifecycle** — on the test server, verify:
   - Crafting all 30 recipes works
   - Placing a speed boat tags it with HP
   - Speed boost applies when holding W
   - Breaking a speed boat drops the correct item with HP preserved
   - Riding and dismounting cleans up state

### Where to Track Changes

- **Paper changelog**: [https://github.com/PaperMC/Paper/blob/master/CHANGELOG.md](https://github.com/PaperMC/Paper/blob/master/CHANGELOG.md) (per-version)
- **Paper API diffs**: compare `paper-api` module between versions for removed/changed methods
- **Minecraft wiki**: [https://minecraft.wiki/w/Java_Edition_...] for new blocks/items/entities
- **Maven metadata**: `https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml` to find available versions

### Common Breaking Changes by Category

| Category | What breaks | How to fix |
|----------|-------------|------------|
| **Boat API** | `Boat.Type`, `getWoodType()` removed | Use `getBoatMaterial()` |
| **Events** | Event renamed, parameters changed, or removed | Check Paper changelog, find replacement event |
| **Recipe API** | `Bukkit.addRecipe()` deprecated | Switch to Paper's recipe manager when available |
| **Adventure text** | Component API changes | Paper bundles Adventure — check their bundled version |
| **PDC / NBT** | `PersistentDataType` changes | Rare, but verify `INTEGER` type still works |
| **Java version** | Paper requires newer Java | Update `java.version` in `pom.xml` and `maven-compiler-plugin` |

## Adding a New Boat Type

1. Add the `Material` to the `BOATS` array in `Lancha.java`
2. Add a `case` in `SpeedBoatItem.isBoatMaterial()` and `getWoodName()`
3. Recipes are auto-registered from the `BOATS` array — no recipe code changes needed

## Adding a New Tier

1. Add the ingredient to the `INGREDIENTS` array in `Lancha.java` with its HP and boost values
2. Recipes are auto-registered from the `INGREDIENTS` × `BOATS` cross-product
