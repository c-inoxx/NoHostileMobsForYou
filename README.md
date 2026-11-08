# No Hostile Mobs For You

A Fabric mod for Minecraft 26.3 that keeps hostile mobs away from you. It replaces the original
Paper plugin of the same name.

## Installing

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.3.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and this mod's jar into your `mods` folder.

To use it on a server, install it there too. For singleplayer, the client is enough.

## Where it works

| Setup | Works? |
|---|---|
| Singleplayer, mod in your client only | Yes |
| LAN world you host | Yes, for everyone on it who has the mod |
| Dedicated server **with** the mod | Yes, for players who have the mod |
| Server **without** the mod | No. The server decides where mobs are, so the client can't move or remove them. The mod tells you it's inactive. |

## Using it

- **K**: turn protection on/off
- **J**: open settings

Both keys can be changed under Options → Controls → No Hostile Mobs.

Settings:

- **Behavior**: *Repel* pushes mobs back out of the radius, *Remove* deletes them.
- **Radius**: 2–32 blocks. The area is a circle around you, reaching the same distance up and down.
- **Repel strength**: 0.2–3.0.
- **Block hostile spawns in radius**: stops natural hostile spawns near you (spawners still spawn).
- **Remove mob projectiles**: deletes arrows, fireballs and similar shot by mobs. Projectiles from players, including yours, are never touched.
- **Excluded mobs**: mobs the mod leaves alone, shown with their spawn eggs. "Exclude all", "Include all" and "Reset" buttons. Bosses (Ender Dragon, Wither, Warden, Elder Guardian) are excluded by default.
- **Ring / HUD**: a particle ring and a status line that only you can see.
- **Particle**: choose which particle draws the ring.

### Commands

Everything in the settings screen also works as a command (client-side, so tab-completion works on every server):

| Command | Does |
|---|---|
| `/nm` | show all current settings |
| `/nm on` / `off` / `toggle` | protection on/off |
| `/nm behavior repel\|remove` | set behavior |
| `/nm radius <2–32>` | set radius |
| `/nm strength <0.2–3>` | set repel strength |
| `/nm spawns on\|off` | block hostile spawns |
| `/nm projectiles on\|off` | remove mob projectiles |
| `/nm ring on\|off` / `/nm hud on\|off` | display options |
| `/nm particle <id>` | ring particle |
| `/nm exclude <mob>` / `include <mob>` / `excluded` | manage and list exclusions |
| `/nm settings` | open the settings screen |

Languages: English and German. Mob names come from the game, so they follow any game language.

Mobs with a name tag, and mobs riding something non-hostile (boats, minecarts), are always left alone.

Settings are stored in `config/nohostilemobs.json`.

## Building

Requires Java 25.

```
./gradlew build        # jar ends up in build/libs/
./gradlew runClient    # start a dev client with the mod
```

## Code layout

```
src/main/java/dev/cnxx/nohostilemobs/        runs on the (built-in or dedicated) server
  NoHostileMobs          entry point: networking, tick hook, cleanup
  ProtectionSettings     one player's settings + limits (server clamps everything)
  SettingsPayload        client -> server packet
  ProtectionManager      online players' settings, removed on logout
  ProtectionLogic        finds and handles hostile mobs; spawn check
  mixin/NaturalSpawnerMixin   blocks natural hostile spawns in the radius

src/client/java/dev/cnxx/nohostilemobs/client/   client only
  NoHostileMobsClient    keybinds, sync on join, particle ring
  ClientConfig           JSON config file
  ProtectionScreen       settings screen
  GridScreen / IconButton   paged button grid with item icons
  ExclusionScreen        mob exclusion grid (spawn eggs)
  ParticleScreen / RingParticles   ring particle picker + particle names
  NmCommands             /nm client commands
  StatusHud              status line
```

## License

CC0 (inherited from the Fabric example mod template).
