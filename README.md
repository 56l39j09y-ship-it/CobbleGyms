# CobbleGyms 🏟️

A comprehensive Pokémon Gym system mod for Minecraft 1.21.1 (Fabric), integrating with Cobblemon and Pokémon Mega Showdown to bring a professional Gym, Elite Four, and Champion system to your server.

## 📋 Features

### 🏟️ Gym System
- **18 Pokémon Types** — Fire, Water, Grass, Electric, Ice, Fighting, Poison, Ground, Flying, Psychic, Bug, Rock, Ghost, Dragon, Dark, Steel, Fairy, Normal
- Each Gym Leader can only use Pokémon of their type (Monotype)
- Singles or Doubles format per leader (configurable individually)
- Virtual badge system with per-season tracking
- 24-hour challenge cooldown after losing
- Leaders can re-challenge after winning

### 🏆 Elite Four & Champion
- Multiple Elite Four members (no limit) each with 2 dual types
- Only 1 Champion at a time — defeat them to take the title!
- Individual battle format per E4/Champion
- All gym badges required before challenging E4
- All E4 victories required before challenging Champion

### ✅ Team Validation (Smogon Rules)
- Automatic detection of banned Pokémon (Ubers tier)
- Banned moves, abilities, items enforcement
- Battle clauses: Sleep, Evasion, OHKO, Species, Endless Battle
- Per-gym extra Pokémon ban (1 per season)
- Team validation before joining queue

### 📅 Season System
- Configurable season duration
- Auto-reset of badges and E4/Champion victories
- Season history preserved for viewing
- Manual season end by moderators

### 🎁 Reward System
- **Gym Leaders**: Minimum 25 battles/week + 50% winrate required
- **Elite Four & Champion**: Automatic weekly rewards
- Configurable reward commands via config file

### ⚔️ Battle Management
- Queue system for challengers
- Automatic teleportation to battle arenas
- Battle redo system (manual, first turn only)
- Active battle tracking
- Battle records with team snapshots

### 🔨 Moderation Tools
- Assign/remove Gym Leaders, Elite Four, Champion
- Import teams from Pokémon Showdown pokepaste URLs
- Per-leader team slots (up to 3 teams per leader)
- Multi-team selection (leader chooses team before seeing challenger's)
- Global Pokémon/move/ability ban management

### 🔗 Discord Integration
- Battle record logging to Discord channels
- Admin action logging
- New Champion announcements
- Private battle logs (accessible to leader/mods only)

## 🎮 Commands

### `/gyms` — Challengers
| Subcommand | Description |
|---|---|
| `/gyms` | Open main menu |
| `/gyms badges` | View your badge collection |
| `/gyms challenge gym <type>` | Join a gym's challenge queue |
| `/gyms challenge e4 <player>` | Challenge an Elite Four member |
| `/gyms challenge champion` | Challenge the Champion |
| `/gyms validate` | Validate your team |
| `/gyms rules` | View current ban rules |
| `/gyms season` | View season info |
| `/gyms leaderboard` | View weekly rankings |
| `/gyms queue <target>` | View a leader's queue |

### `/challenge` — Gym Leaders, E4, Champion
| Subcommand | Description |
|---|---|
| `/challenge` | Open challenge panel |
| `/challenge open` / `/challenge close` | Open/close your gym |
| `/challenge equip [slot]` | Equip your gym team |
| `/challenge unequip` | Restore personal team |
| `/challenge start <player>` | Start battle with queued challenger |
| `/challenge cancel all` | Cancel all battles and queue |
| `/challenge cancel current` | Cancel current battle |
| `/challenge ban <player> <hours>` | Ban player from your gym |
| `/challenge unban <player>` | Unban player |
| `/challenge records` | View your battle records |
| `/challenge stats` | View your weekly stats |
| `/challenge queue` | View your challenger queue |

### `/gymban` — Gym Leaders (Extra Pokemon Ban)
| Subcommand | Description |
|---|---|
| `/gymban set <pokemon>` | Set your extra banned Pokémon (once per season!) |
| `/gymban clear` | Clear ban (only if from previous season) |
| `/gymban info` | View current ban info |

### `/gymsadmin` — Moderators (OP Level 2+)
| Subcommand | Description |
|---|---|
| `/gymsadmin gym assign <player> <type>` | Assign Gym Leader |
| `/gymsadmin gym remove <player>` | Remove Gym Leader |
| `/gymsadmin gym setteam <player> <slot> <pokepaste\|url>` | Set team |
| `/gymsadmin gym setformat <player> <singles\|doubles>` | Set battle format |
| `/gymsadmin gym setlocation <player>` | Set arena at current position |
| `/gymsadmin gym multiteam <player> <true\|false>` | Enable multi-team |
| `/gymsadmin gym list` | List all Gym Leaders |
| `/gymsadmin e4 assign <player> <type1> <type2>` | Assign Elite Four |
| `/gymsadmin e4 remove <player>` | Remove Elite Four |
| `/gymsadmin e4 setteam <player> <slot> <pokepaste\|url>` | Set E4 team |
| `/gymsadmin champion assign <player>` | Assign Champion |
| `/gymsadmin champion remove` | Remove Champion |
| `/gymsadmin champion setteam <slot> <pokepaste\|url>` | Set Champion team |
| `/gymsadmin season end` | End current season |
| `/gymsadmin rules banpokemon <name>` | Globally ban a Pokémon |
| `/gymsadmin rules banmove <name>` | Globally ban a move |
| `/gymsadmin redo <leader>` | Redo a battle (first turn only) |
| `/gymsadmin leaderboard` | View full rankings |
| `/gymsadmin records gym <player>` | View battle records |

## ⚙️ Configuration

Config file: `config/cobblegyms.json`

```json
{
  "season": {
    "durationDays": 30,
    "autoReset": true
  },
  "rewards": {
    "leaderMinWeeklyBattles": 25,
    "leaderMinWinrate": 0.5,
    "leaderRewardCommand": "give {player} minecraft:diamond 5",
    "eliteFourRewardCommand": "give {player} minecraft:diamond_block 1",
    "championRewardCommand": "give {player} minecraft:netherite_ingot 1"
  },
  "discord": {
    "enabled": false,
    "token": "",
    "guildId": "",
    "battleLogChannelId": "",
    "adminChannelId": ""
  },
  "battle": {
    "challengeCooldownHours": 24,
    "maxQueueSize": 10,
    "battleTimeoutMinutes": 60,
    "showTeamBeforeBattle": true
  },
  "smogon": {
    "format": "nationaldex",
    "bannedPokemon": ["..."],
    "bannedMoves": ["Double Team", "Minimize", "Horn Drill", "Guillotine", "Sheer Cold", "Fissure"],
    "bannedAbilities": ["Arena Trap", "Shadow Tag", "Moody"],
    "bannedItems": [],
    "clauses": ["Sleep Clause", "Evasion Clause", "OHKO Clause", "Species Clause", "Endless Battle Clause"]
  }
}
```

## 🛠️ Setup

### Requirements
- Minecraft 1.21.1
- [Fabric Loader](https://fabricmc.net/) ≥ 0.16.0
- [Fabric API](https://modrinth.com/mod/fabric-api) ≥ 0.105.3+1.21.1
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) ≥ 1.12.3+kotlin.2.0.21
- [Cobblemon](https://modrinth.com/mod/cobblemon) ≥ 1.5.2+1.21.1

### Installation
1. Download the latest CobbleGyms `.jar` from Releases
2. Place it in your server's `mods/` folder
3. Start the server — config will be auto-generated at `config/cobblegyms.json`
4. Edit the config to your preferences
5. Use `/gymsadmin` to set up your gyms!

### Quick Start
```
# Assign a Gym Leader
/gymsadmin gym assign Steve fire

# Set their team (from pokepaste URL)
/gymsadmin gym setteam Steve 1 https://pokepast.es/XXXXXXXXXXXX

# Set battle format
/gymsadmin gym setformat Steve singles

# Set arena location (stand at the spot first)
/gymsadmin gym setlocation Steve

# Assign Elite Four
/gymsadmin e4 assign Alex fire steel

# Assign Champion
/gymsadmin champion assign NotchChampion

# Steve opens his gym
/challenge open

# Player challenges Steve
/gyms challenge gym fire
```

## 📊 Database

CobbleGyms uses SQLite stored at `{server_root}/cobblegyms.db`.

Tables:
- `seasons` — Season history
- `gym_leaders` — Leader data and teams
- `elite_four` — E4 data and teams
- `champion` — Champion data
- `player_badges` — Per-player, per-season badges
- `player_e4_victories` — Per-player, per-season E4 victories
- `battle_records` — Full battle history
- `challenge_cooldowns` — 24h cooldown tracking
- `gym_bans` — Per-gym player bans
- `weekly_stats` — Weekly performance data
- `active_battles` — Currently active battles
- `challenge_queue` — Challenger queues

## 🏗️ Architecture

```
src/main/kotlin/com/cobblegyms/
├── CobbleGyms.kt              # Mod entry point
├── config/
│   └── GymConfig.kt           # JSON configuration
├── data/
│   ├── DatabaseManager.kt     # SQLite setup & tables
│   ├── GymRepository.kt       # All DB operations
│   └── models/
│       ├── GymLeaderData.kt   # Data models
│       └── PlayerData.kt      # Player data models
├── gym/
│   └── GymManager.kt          # Leader/E4/Champion management
├── battle/
│   ├── BattleManager.kt       # Battle lifecycle & queue
│   ├── TeamValidator.kt       # Smogon rule validation
│   └── CobblemonIntegration.kt # Cobblemon API bridge
├── season/
│   └── SeasonManager.kt       # Season lifecycle
├── rewards/
│   └── RewardManager.kt       # Weekly reward distribution
├── discord/
│   └── DiscordBotManager.kt   # JDA Discord integration
├── pokepaste/
│   └── PokepasteImporter.kt   # Pokepaste URL importer
├── commands/
│   ├── GymsCommand.kt         # /gyms
│   ├── ChallengeCommand.kt    # /challenge
│   ├── GymsAdminCommand.kt    # /gymsadmin
│   └── ExtraBanCommand.kt     # /gymban
├── gui/
│   ├── GymsMenu.kt            # Player menus
│   ├── ChallengeMenu.kt       # Leader menus
│   └── AdminMenu.kt           # Admin menus
└── util/
    ├── MessageUtil.kt         # Chat formatting utilities
    ├── TimeUtil.kt            # Time formatting utilities
    └── PokemonTypeUtil.kt     # Type utilities
```

## 🔧 Building

```bash
./gradlew build
```

Output: `build/libs/cobblegyms-1.0.0.jar`

## 📝 License

MIT License — See [LICENSE](LICENSE) for details.

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request
