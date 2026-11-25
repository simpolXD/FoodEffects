# AbyssalDominionFoodEffects

Configuration-driven potion effect additions, removals, and replacements for consumed items on Paper 26.2 / 1.21.11 servers.

## Compatibility

- Paper 26.2 (build 135); the local server log identifies Minecraft 26.2.
- The resolved build API is `io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT`; this is not a Paper 26.2 API artifact.
- Plugin bytecode target: Java 25.

## Installation

1. Build the plugin with `mvn clean package`.
2. Copy `target/abyssaldominion-foodeffects-1.1.0.jar` into your server's `plugins/` directory.
3. Restart the server.
4. Edit `plugins/AbyssalDominionFoodEffects/config.yml` to define effect rules.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/foodfx reload` | `abyssaldominion.foodeffects.reload` (op) | Reloads configuration from disk. |
| `/foodfx version` | `abyssaldominion.foodeffects.version` (everyone) | Shows plugin version and author. |

## Configuration

The configuration file is `plugins/AbyssalDominionFoodEffects/config.yml`.

### Schema

```yaml
settings:
  enabled: true
  max-duration-seconds: 7200   # hard cap on any effect duration
  max-amplifier: 10             # amplifier 0 = level I, 1 = level II, etc.
  custom-item-matching: true    # enable PDC-based custom item matching

items:
  ENCHANTED_GOLDEN_APPLE:
    material: ENCHANTED_GOLDEN_APPLE   # optional; auto-detected from key when key is a valid Material
    pdc: {}                             # optional PDC requirements
    remove-effects: []                  # effect types to suppress
    replace-effects: {}                 # effect type -> replacement definition
    add-effects: []                     # additional effects to apply after consumption
```

#### Effect definition

Each effect entry supports:

```yaml
SPEED:
  duration: 15       # seconds (must be > 0, <= max-duration-seconds)
  amplifier: 1       # 0 = level I (must be >= 0, <= max-amplifier)
  ambient: false     # whether the effect is ambient (no particle indicator)
  particles: true    # show particles
  icon: true         # show icon in the HUD
```

Removal rules are effect names only; they do not need duration or amplifier fields:

```yaml
remove-effects:
  - REGENERATION
  - ABSORPTION
```

The legacy map form is also accepted, and its keys are used as effect names.

### Custom item matching

When `custom-item-matching` is true, PDC rules take precedence over generic material rules. Put specific custom food definitions in a top-level `custom-items:` section; each entry must set a valid vanilla `material` and at least one string-valued `pdc` requirement.

```yaml
custom-items:
  super_apple:
    material: APPLE
    pdc:
      "myplugin:itemid": "super_apple"
    add-effects:
      STRENGTH:
        duration: 10
        amplifier: 2
        ambient: false
        particles: true
        icon: true
```

Every configured PDC key and string value must match. Multiple requirements are combined with AND. A custom-item rule that does not match falls back to the generic material rule, if one is configured.

## Supported items

- Vanilla foods (bread, cooked beef, golden apples, suspicious stew, etc.)
- Golden Apples and Enchanted Golden Apples
- Drinkable potions
- Honey Bottles
- Milk Buckets
- Suspicious Stew
- Custom consumable items identified by an exact material and PDC key/value match

Splash and lingering potions are **not** supported through the consumption event. They require explicit separate handling.

## Behavior

### ADD

Vanilla consumption completes normally. After all vanilla effects have been processed (next tick), the configured additional effects are applied exactly once. Unrelated vanilla behavior (hunger, saturation, container return, etc.) is preserved.

### REMOVE

When a configured item is consumed, the specified vanilla potion effects are suppressed at the `EntityPotionEffectEvent` level. Effects from other sources (beacons, commands, other plugins) are **not** affected.

### REPLACE

The specified vanilla effect is suppressed and replaced with the configured definition. The replacement uses Bukkit's default merging behavior (`force=false`): if the player already has a stronger or longer effect of the same type, the existing effect wins. The replacement never removes unrelated active effects.

### Conflict policy

- **add-effects**: applied with `force=false`. Existing stronger or longer effects take precedence.
- **replace-effects**: applied with `force=false` after cancelling the vanilla grant. Existing stronger or longer effects take precedence.
- **remove-effects**: only the consumption-sourced effect is cancelled. Existing effects from other sources are untouched.

## Anti-duplication guarantees

- One consumption context per player at a time. Rapid double-consumption replaces the prior context safely.
- Contexts are cleared on player disconnect and plugin shutdown.
- Delayed effect application runs on the next tick and validates the active context before applying.
- The listener does not modify the consume event's ItemStack or cancel successful consumptions.

## Reload safety

- Reload parses into a temporary configuration object.
- On validation failure, the previous valid configuration remains active and an error is returned.
- Pending consumption transactions use the snapshot that was active when the consumption occurred, preventing reload-time replay.

## Building

```bash
mvn clean package
```

The output JAR is `target/abyssaldominion-foodeffects-1.1.0.jar`.

## Limitations

- Custom consumable items from third-party plugins that grant effects via `EntityPotionEffectEvent.Cause.PLUGIN` cannot be generically intercepted. Such items require explicit integration adapters.
- Splash and lingering potions are not supported.
- The plugin assumes the server provides Adventure API 5.2.0 (bundled with Paper 26.2).
- `EntityPotionEffectEvent.setNewEffect()` is not available in the target API, so replacement is implemented by cancelling the vanilla event and applying the replacement manually.
