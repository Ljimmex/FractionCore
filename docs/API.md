# FractionCore — Public API

This document outlines the public API surface of FractionCore. It is currently a skeleton and will be expanded as gameplay modules are implemented.

## Module API

Modules are the primary extension point. External plugins or future internal features can interact with modules through `ModuleManager`.

### Accessing Modules

```java
FractionCore plugin = ...;
ModuleManager moduleManager = plugin.getModuleManager();
GuildModule guildModule = (GuildModule) moduleManager.getModule("guild");
```

> ⚠️ Always check `module.getState() == ModuleState.RUNNING` before using a module.

### Creating Custom Modules

Extend `BaseModule` and register the module at runtime:

```java
public class MyModule extends BaseModule {
    public MyModule(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "my_module";
    }

    @Override
    public void onEnable() {
        // Initialization
    }

    @Override
    public void onDisable() {
        // Cleanup
    }
}
```

## Events

Planned events for cross-module communication:

| Event | Description |
|-------|-------------|
| `GuildCreateEvent` | Fired when a guild is created |
| `GuildDisbandEvent` | Fired when a guild is disbanded |
| `GuildInviteEvent` | Fired when a guild invitation is sent |
| `EggDestroyEvent` | Fired when a guild egg is destroyed |
| `CuboidUpgradeEvent` | Fired when a cuboid level changes |

> These events are not yet implemented. They will be added during Sprint 1 and later.

## Placeholders

### Internal Placeholders

Used in language files (`{placeholder}`):

| Placeholder | Context | Example Value |
|-------------|---------|---------------|
| `{player}` | Command sender | `Notch` |
| `{guild}` | Player's guild | `Silver Lions` |
| `{tag}` | Guild tag | `[SL]` |
| `{points}` | Guild points | `15420` |
| `{level}` | Cuboid/guild level | `5` |
| `{members}` | Member count | `8` |

### PlaceholderAPI Integration

Planned PlaceholderAPI identifiers:

| Identifier | Description |
|------------|-------------|
| `%fgc_guild%` | Full guild name |
| `%fgc_tag%` | Guild tag |
| `%fgc_rank%` | Player rank in guild |
| `%fgc_points%` | Guild points |
| `%fgc_level%` | Guild/cuboid level |

> PlaceholderAPI integration is optional and will be registered automatically when the plugin is detected.

## Database Access

Direct database access is discouraged for external plugins. Use DAOs exposed through modules when available.

```java
DatabaseModule databaseModule = (DatabaseModule) moduleManager.getModule("database");
GuildDao guildDao = databaseModule.getGuildDao();
```

## Language API

Send a translated message to a player:

```java
LangModule langModule = (LangModule) moduleManager.getModule("lang");
Component message = langModule.getLangManager().getMessage("guild.create.success", MessageType.SUCCESS, context);
player.sendMessage(message);
```

## Future API Additions

- FGCAPI interface for external plugins.
- Economy API for multi-currency operations.
- Cuboid API for region checks and flag manipulation.
