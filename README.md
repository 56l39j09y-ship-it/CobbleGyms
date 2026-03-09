# CobbleGyms

A comprehensive Pokémon Gym League system for Cobblemon servers on Minecraft Fabric 1.21.1.

## Features

- **Gym Leaders** — Player gym leaders with type specializations
- **Elite Four** — Top 4 competitors with position ranking and statistics tracking
- **Champion** — Single champion title with full win/loss record
- **Battle Queue** — FIFO queue system per opponent with 5-minute timeout
- **Seasonal Competitions** — 30-day seasons with automatic resets
- **Reward System** — Weekly rewards with 7-day cooldown
- **Player Ban System** — Temporary and permanent bans from the gym system
- **Smogon Format Validation** — Configurable ban list and team validation
- **SQLite Persistence** — All data stored in a local database
- **Admin Commands** — Full admin control via `/gymsadmin`

## Commands

| Command | Description |
|---|---|
| `/gyms` | Open the gym system menu |
| `/gyms status` | Show gym system overview |
| `/gyms leaders` | List all gym leaders |
| `/gyms elite4` | List Elite Four members |
| `/gyms champion` | Show current champion |
| `/gyms season` | Show current season info |
| `/gyms rules` | Show battle rules and banned Pokémon |
| `/gyms queue` | Show your queue positions |
| `/challenge leader <player>` | Join a gym leader's queue |
| `/challenge elite4 <player>` | Join an Elite Four member's queue |
| `/challenge champion` | Join the champion's queue |
| `/challenge leave` | Leave all queues |
| `/gymsadmin setleader <player> <type>` | Assign a gym leader |
| `/gymsadmin removeleader <player>` | Remove a gym leader |
| `/gymsadmin sete4 <player> <1-4> <type>` | Set Elite Four member |
| `/gymsadmin removee4 <player>` | Remove Elite Four member |
| `/gymsadmin setchampion <player>` | Set the champion |
| `/gymsadmin removechampion` | Remove the champion |
| `/gymsadmin ban <player> <reason>` | Permanently ban a player |
| `/gymsadmin tempban <player> <days> <reason>` | Temporarily ban a player |
| `/gymsadmin unban <player>` | Unban a player |
| `/gymsadmin season new` | Start a new season |
| `/gymsadmin season end` | End the current season |
| `/gymsadmin clearqueue <player>` | Clear a player's queues |
| `/gymsadmin reload` | Reload configuration |

## Installation

1. Download `CobbleGyms-1.0.0.jar`
2. Place in your server's `mods/` folder
3. Ensure you have installed:
   - Minecraft 1.21.1
   - Fabric Loader
   - Fabric API
   - Cobblemon mod
   - Fabric Language Kotlin
4. Restart the server

## Configuration

Configuration is stored in `config/cobblegyms.properties` and is auto-generated on first run.

| Property | Default | Description |
|---|---|---|
| `season_duration_days` | `30` | Length of a competitive season |
| `max_gym_leaders` | `8` | Maximum number of gym leaders |
| `max_elite_four_members` | `4` | Maximum Elite Four slots |
| `gym_leader_weekly_reward` | `2000` | Weekly coins for gym leaders |
| `e4_weekly_reward` | `5000` | Weekly coins for E4 members |
| `champion_weekly_reward` | `10000` | Weekly coins for champion |
| `gym_leader_min_battles` | `25` | Min battles to claim gym leader reward |
| `gym_leader_min_winrate` | `50.0` | Min winrate (%) to claim gym leader reward |

## Requirements

- Minecraft 1.21.1
- Fabric Loader ≥ 0.14.0
- Fabric API
- Cobblemon
- Fabric Language Kotlin
- Java 21+

## License

MIT
