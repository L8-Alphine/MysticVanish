# MysticVanish

Layered stealth, silent moderation, and permission-based invisibility for Hytale servers.

A Hytale server mod built with Java. Requires Hytale Server `>=0.5.6 <0.6.0`.

## Optional Depends
This mod has optional depends but recommended integrations:

- [LuckPerms](https://www.curseforge.com/hytale/mods/luckperms)
- [PlaceholderAPI](https://www.curseforge.com/hytale/mods/placeholder-api)

## What it does

MysticVanish is a **layered** vanish system. Instead of a single on/off flag, every player has a *vanish level* (how concealed they are) and a *see level* (how much they can see). A viewer sees a vanished player only when `viewer.seeLevel >= target.vanishLevel`, so staff hierarchies fall out naturally — moderators can't see admins, admins can't see owners, and so on.

Highlights:

- **Layered, permission-driven levels** — vanish/see levels come from `mysticvanish.level.<N>` / `mysticvanish.see.<N>` nodes, resolved from LuckPerms or native permissions.
- **Stealth features**, each individually permission-gated: hide from tab list, map, and nameplate; suppress join/leave messages; stop NPCs/mobs from targeting you; skip trigger volumes; chat and command mistype protection.
- **Visual safety net** — a configurable glow and a per-viewer particle aura (mutual vs one-way colours) so staff don't collide while invisible. Both are visible only to players who can already see the vanished player.
- **On-screen HUD** reminding the vanished player of their state and level.
- **Integrations** — optional LuckPerms (live permission-change updates) and PlaceholderAPI (`%mysticvanish_*%`).
- **Developer API** — a stable `MysticVanishAPI` plus a full set of lifecycle events.

## Commands

| Command | Description |
| --- | --- |
| `/vanish` (`/v`, `/mvanish`, `/mysticvanish`) | Toggle your own vanish. |
| `/vanish off` | Force vanish off. |
| `/vanish status [player]` | Show vanish status. |
| `/vanish <player>` | Toggle vanish for another player. |
| `/vanish reload` | Reload config and messages. |
| `/vchat <message>` | Send intentional public chat while vanished. |

## Documentation

The wiki pages live in [MysticVanish Wiki](https://wiki.hytalemodding.dev/mod/mysticvanish):

- **[Getting Started](https://wiki.hytalemodding.dev/mod/mysticvanish/getting-started)** — install, configure, grant the first ranks, verify.
- **[User Guide](https://wiki.hytalemodding.dev/mod/mysticvanish/user-guide)** — for staff using vanish in-game.
- **[Features](https://wiki.hytalemodding.dev/mod/mysticvanish/features)** — every feature explained in detail.
- **[Administration Guide](https://wiki.hytalemodding.dev/mod/mysticvanish/administration-guide)** — every config value and feature setting.
- **[Developer Guide](https://wiki.hytalemodding.dev/mod/mysticvanish/developer-guide)** — the API, events, placeholders, and integration.

## Building

```bash
./gradlew shadowJar
```

The output JAR will be in `build/libs/`.

## Deploying

```bash
./gradlew deployMod
```

Builds the fat JAR and copies it to the Hytale server `mods/` folder.

## Running

Use the included run configurations in your IDE:

- **Run Hytale Server** — Builds, deploys, and starts the server
- **Debug Hytale Server** — Same as Run, with remote debugger on port 5005
- **Build Mod** — Compiles without deploying or starting the server
