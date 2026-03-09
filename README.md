# CobbleGyms 🎮

> A comprehensive competitive Pokémon Gym League system for [Cobblemon](https://cobblemon.com) on Fabric.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)](https://minecraft.net)
[![Cobblemon](https://img.shields.io/badge/Cobblemon-compatible-blue)](https://cobblemon.com)

---

## ✨ Features

### 🏅 Gym League
- Register up to **unlimited** Gym Leaders, each with a custom type and badge name
- FIFO battle queue per leader with configurable timeout (default 5 minutes)
- Win/loss tracking persisted to SQLite

### ★ Elite Four
- Assign players to **positions 1–4** in the Elite Four
- Per-member statistics (wins, losses)
- Ordered challenge progression

### 👑 Champion
- Single Champion title — dethrone and crown automatically on battle end
- Defense count tracked for each championship reign
- Full champion history stored in database

### 📊 Seasonal Competitions
- Start/end seasons manually via admin commands
- Per-season player statistics: badges earned, wins/losses at every tier
- Leaderboard sorted by badges then total wins

### ⚔ Smogon Format Validation
- Configurable banned Pokémon, moves, and abilities
- Team-size and level-cap enforcement
- Species Clause (no duplicate Pokémon)

### 🎁 Reward System
- Configurable currency rewards for gym badge, E4, and champion victories
- Per-player cooldown to prevent reward farming

### 🤖 Discord Integration (Optional)
- Rich embed notifications for gym wins, E4 victories, new champions, and season events
- Requires a Discord bot token and text channel ID

### 🖥 Chest GUI Menus
- In-game menus for the main overview, gym leaders, Elite Four, and admin panel

---

## 📋 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/gyms` | Show all gym league info | Everyone |
| `/gyms leaders` | List active gym leaders | Everyone |
| `/gyms e4` | List Elite Four members | Everyone |
| `/gyms champion` | Show current champion | Everyone |
| `/gyms leaderboard` | Season leaderboard | Everyone |
| `/gyms season` | Current season info | Everyone |
| `/gyms rules` | Battle rules / ban list | Everyone |
| `/challenge leader <name>` | Join a gym leader's queue | Everyone |
| `/challenge e4 <1-4>` | Join an Elite Four queue | Everyone |
| `/challenge champion` | Join the champion queue | Everyone |
| `/challenge leave` | Leave all queues | Everyone |
| `/challenge status` | Check your queue positions | Everyone |
| `/gymsadmin setleader <player> <type> <badge>` | Register a gym leader | OP (level 2) |
| `/gymsadmin removeleader <player>` | Remove a gym leader | OP (level 2) |
| `/gymsadmin sete4 <player> <1-4>` | Assign Elite Four position | OP (level 2) |
| `/gymsadmin removee4 <player>` | Remove Elite Four member | OP (level 2) |
| `/gymsadmin setchampion <player>` | Crown the champion | OP (level 2) |
| `/gymsadmin startseason` | Start a new season | OP (level 2) |
| `/gymsadmin endseason` | End current season | OP (level 2) |
| `/gymsadmin leaderboard [limit]` | Print season leaderboard | OP (level 2) |
| `/gymsadmin reload` | Reload configuration | OP (level 2) |

---

## 🔧 Installation

1. Download `CobbleGyms-1.0.0.jar` from the [Releases](https://github.com/56l39j09y-ship-it/CobbleGyms/releases) page.
2. Place the JAR in your server's `mods/` folder.
3. Ensure the following are installed:
   - [Fabric Loader](https://fabricmc.net/use/installer/)
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)
   - [Cobblemon](https://cobblemon.com)
4. Start the server. A default config will be created at `config/cobblegyms/config.json`.

### Requirements

| Dependency | Version |
|------------|---------|
| Minecraft | 1.21.1 |
| Fabric Loader | ≥ 0.15.11 |
| Fabric API | ≥ 0.100.1 |
| Fabric Language Kotlin | ≥ 1.11.0 |
| Cobblemon | ≥ 1.5.2 |
| Java | ≥ 17 |

---

## ⚙ Configuration

On first run, `config/cobblegyms/config.json` is generated with defaults.
A human-readable reference is provided in [`cobblegyms.yml`](cobblegyms.yml).

Key settings:

```json
{
  "battle": {
    "queueTimeoutSeconds": 300,
    "maxQueueSize": 50
  },
  "rewards": {
    "gymBadgeReward": 1000,
    "e4WinReward": 5000,
    "championReward": 10000,
    "cooldownHours": 24
  },
  "smogon": {
    "format": "gen9ou",
    "banListEnabled": true,
    "teamSize": 6,
    "maxLevel": 100
  },
  "discord": {
    "enabled": false,
    "botToken": "",
    "channelId": ""
  }
}
```

### Discord Setup

1. Create a bot at <https://discord.com/developers/applications>
2. Copy the bot token into `discord.botToken`
3. Copy the text channel ID into `discord.channelId`
4. Set `discord.enabled` to `true`
5. Reload with `/gymsadmin reload`

---

## 🗄 Database Schema

CobbleGyms uses SQLite stored at `config/cobblegyms/cobblegyms.db`.

| Table | Description |
|-------|-------------|
| `gym_leaders` | Registered gym leaders |
| `elite_four` | Elite Four members (positions 1-4) |
| `champion` | Champion history |
| `season` | Season records |
| `player_stats` | Per-season player statistics |
| `battle_history` | Individual battle log |

---

## 🏗 Building from Source

```bash
git clone https://github.com/56l39j09y-ship-it/CobbleGyms.git
cd CobbleGyms
./gradlew build
```

Output: `build/libs/CobbleGyms-1.0.0.jar`

---

## 🔌 Developer Integration

### Battle Outcome Hook

Wire Cobblemon battle results to the gym system via `BattleEventListener.onBattleEnd()`:

```kotlin
// In your Cobblemon battle listener:
eventListener.onBattleEnd(
    challengerUuid = challengerPlayer.uuid,
    challengerName = challengerPlayer.name.string,
    defenderUuid   = defenderPlayer.uuid,
    challengerWon  = battleResult.winner == challenger,
    server         = minecraftServer
)
```

The listener automatically determines whether the defender is a Gym Leader, Elite Four member, or Champion, updates statistics, grants rewards, and broadcasts the result.

### Economy Integration

Edit `RewardSystem.grantCurrency()` to connect to your economy plugin:

```kotlin
private fun grantCurrency(player: ServerPlayerEntity, amount: Int) {
    // Example: EconomyAPI.addBalance(player.uuid, amount.toDouble())
}
```

---

## 📝 License

MIT — see [LICENSE](LICENSE).

---

## 🙏 Credits

Built with ❤ for the [Cobblemon](https://cobblemon.com) community.
