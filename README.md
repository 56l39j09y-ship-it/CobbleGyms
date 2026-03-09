# 🏆 CobbleGyms

Sistema completo de Gyms, E4 y Campeón Pokémon para servidores **Cobblemon** en **Minecraft 1.21.1 (Fabric)**.

---

## 📋 Características

### 🎮 Sistema de Gyms
- **18 gimnasios** — uno por cada tipo Pokémon (Fuego, Agua, Planta, etc.)
- Líderes de gym son **jugadores reales** asignados por moderadores
- Modalidad **Singles o Dobles** configurable individualmente por líder
- Validación de tipo: el líder sólo puede usar Pokémon de su tipo asignado
- Los líderes tienen **medalla automática** de su propio tipo (no pueden retarse a sí mismos)
- Hasta **3 equipos diferentes** por líder (el líder elige antes de ver el equipo del retador)

### 🏅 Sistema E4 y Campeón
- Múltiples miembros del **Alto Mando (E4)** con **doble tipo** a elección
- Un único **Campeón** que puede perder su título ante cualquier retador que lo derrote
- Modalidad Singles/Dobles individual por E4/Campeón
- El campeón usa un equipo libre siguiendo las reglas Smogon

### ✅ Validación Smogon
- Ban automático de Pokémon, movimientos, habilidades e ítems de tier Uber
- Detección de estrategias prohibidas (Baton Pass chain, Evasion+Moody, etc.)
- **Ban extra por gym**: cada líder puede añadir 1 Pokémon extra baneado (una vez por temporada)
- Los moderadores pueden editar las listas de ban en tiempo real

### 🗓️ Sistema de Temporadas
- Duración configurable (por defecto 30 días)
- Al finalizar la temporada, **los badges y victorias se reinician** (los líderes mantienen su puesto)
- Historial de medallero visible por temporada

### 🎁 Sistema de Recompensas
- Líderes de gym: mínimo 25 combates semanales + 50% winrate → recompensa semanal
- E4 y Campeón: recompensa semanal automática sin restricciones
- Recompensas de fin de temporada configurables

### ⚔️ Sistema de Combates
- Cola de retadores por líder (respeta si el líder está ocupado)
- Detección de desconexiones y opción de **rehacimiento manual** (solo si ≤1 turno)
- Registro privado de combates por líder (visible también para moderadores)
- Integración con Discord para notificaciones de registros

### 👥 Sistema de Equipamiento
- Equipo de gym "prestado": se equipa automáticamente al abrir el gym y se restaura al cerrar
- Sistema de backup del equipo personal del líder

### 📤 Importación desde Pokémon Showdown
- Admins importan equipos vía **link de pokepaste.me**
- Parseo automático del formato Showdown (especies, movimientos, habilidades, EVs, IVs, naturaleza, objeto)

### 🚫 Baneo Temporal por Gym
- Líderes, E4 y Campeón pueden banear temporalmente a usuarios irrespetuosos
- El baneo es local al gym (solo impide retar a ese líder)
- Duración configurable, se puede deshacer manualmente

---

## 🖥️ Comandos

### `/gyms` — Para todos los jugadores
| Sección | Descripción |
|---------|-------------|
| Medallero | Ver medallas ganadas por temporada + victorias E4/Campeón |
| Retar Líder | Elegir líder a retar (verifica equipo, mete en cola) |
| Retar E4 | Requiere los 18 badges de la temporada actual |
| Retar Campeón | Requiere haber ganado al E4 |
| Info de Temporada | Número de temporada y tiempo restante |
| Validar Equipo | Comprobar equipo contra reglas Smogon |
| Reglas | Lista de baneados con búsqueda estilo yunque |
| Estado de Cola | Posición actual en la cola de algún gym |

### `/challenge` — Para líderes / E4 / Campeón
| Sección | Descripción |
|---------|-------------|
| Abrir/Cerrar Gym | Activa o desactiva la recepción de retadores |
| Equipar/Quitar Equipo | Equipa el team de gym y restaura el personal |
| Iniciar Combate | Empieza el combate con el siguiente en cola |
| Cancelar Combate | Cancela el combate activo |
| Cancelar Todos | Cancela toda la cola pendiente |
| Ver Registros | Historial privado de combates |
| Estadísticas | Rendimiento semanal (combates, victorias, winrate) |
| Rankings | Top semanal de líderes y E4+Campeón |
| Banear Jugador | Baneo temporal del propio gym |

### `/gymsadmin` — Para moderadores (permiso nivel 3)
| Sección | Descripción |
|---------|-------------|
| Gestionar Líderes | Asignar/eliminar líderes, cambiar modalidad |
| Gestionar E4 | Asignar/eliminar E4 con sus tipos dobles |
| Gestionar Campeón | Asignar/eliminar campeón |
| Importar Equipo | Pegar link de pokepaste.me para dar equipo a un líder |
| Listas de Baneos | Añadir/quitar baneados de Pokémon, movs, habilidades, ítems |
| Gestión de Temporada | Iniciar nueva temporada forzosamente |
| Registros | Ver registros de combate de todos los líderes |
| Rankings | Ver clasificación global |
| Rehacer Combate | Aprobar rehacimiento manual de combate |

---

## ⚙️ Configuración

Los archivos de configuración se generan en `config/cobblegyms/` al iniciar el servidor por primera vez:

- **`config.json`** — Configuración general (duración temporada, cooldowns, recompensas, Discord, etc.)
- **`bans.json`** — Listas de ban Smogon + bans extra por gym

### Ejemplo de `config.json`
```json
{
  "seasonDurationDays": 30,
  "minBattlesForReward": 25,
  "minWinrateForReward": 0.5,
  "challengeCooldownSeconds": 86400,
  "tempBanMaxHours": 72,
  "discordBotToken": "TU_TOKEN_AQUI",
  "discordWebhookUrl": "",
  "leaderDiscordChannels": {
    "UUID_DEL_LIDER": "ID_CANAL_DISCORD"
  },
  "gymLeaderWeeklyRewardCommand": "give %player% minecraft:diamond 5",
  "e4WeeklyRewardCommand": "give %player% minecraft:diamond_block 1",
  "championWeeklyRewardCommand": "give %player% minecraft:netherite_ingot 1"
}
```

---

## 🔧 Instalación

### Requisitos
- Minecraft **1.21.1**
- Fabric Loader **≥0.16.0**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cobblemon](https://modrinth.com/mod/cobblemon) (para MC 1.21.1)

### Pasos
1. Descarga el `.jar` de CobbleGyms y colócalo en la carpeta `mods/` de tu servidor.
2. Inicia el servidor. Se generará el directorio `config/cobblegyms/` automáticamente.
3. Edita `config/cobblegyms/config.json` con tus ajustes.
4. Reinicia el servidor.
5. Usa `/gymsadmin setleader <jugador> <tipo> [singles|doubles]` para asignar el primer líder.

---

## 🤖 Integración Discord

1. Crea un bot en [Discord Developer Portal](https://discord.com/developers/applications).
2. Copia el token del bot en `config.json` → `discordBotToken`.
3. Invita el bot a tu servidor Discord.
4. Para cada líder, añade su UUID y el ID del canal de texto correspondiente en `leaderDiscordChannels`.
5. El bot enviará automáticamente el registro de combates al canal del líder cada vez que termine un combate.

---

## 🏗️ Compilación desde código fuente

```bash
git clone https://github.com/56l39j09y-ship-it/CobbleGyms.git
cd CobbleGyms
./gradlew build
```

El `.jar` final se genera en `build/libs/cobblegyms-1.0.0.jar`.

---

## 📐 Arquitectura del mod

```
com.cobblegyms/
├── CobbleGyms.java           # Entrypoint principal
├── config/                   # Configuración (JSON, Gson)
│   ├── CobbleGymsConfig      # Config general
│   └── SmogonBanConfig       # Listas de ban
├── database/                 # SQLite vía JDBC
│   └── DatabaseManager       # 12 tablas, CRUD completo
├── model/                    # Objetos de datos
│   ├── PokemonType (18 tipos)
│   ├── BattleFormat, GymRole
│   ├── GymLeaderData, BattleRecord, QueueEntry
│   ├── Season, GymBan, WeeklyStats
├── system/                   # Lógica de negocio
│   ├── GymManager            # Líderes, E4, Campeón
│   ├── QueueManager          # Cola de retadores
│   ├── BattleManager         # Inicio/fin de combates
│   ├── SeasonManager         # Temporadas (scheduler)
│   ├── ValidationManager     # Validación de equipo
│   ├── RewardManager         # Recompensas semanales
│   ├── RankingManager        # Rankings
│   ├── BanManager            # Bans temporales
│   └── TeamManager           # Equipos y backup
├── command/                  # Comandos Brigadier
│   ├── GymsCommand           # /gyms
│   ├── ChallengeCommand      # /challenge
│   └── GymsAdminCommand      # /gymsadmin
├── gui/                      # GUIs de cofre (54 slots)
│   ├── GymsGui, MedalGui, GymLeaderListGui
│   ├── ChallengeGui, AdminGui
│   ├── RecordsGui, RulesGui, StatsGui
│   ├── RankingGui, TeamManageGui
│   └── GuiManager
├── validation/               # Validación Smogon
│   ├── SmogonValidator
│   └── BanListManager
├── team/                     # Equipos de gym
│   ├── PokePasteImporter     # Importa desde pokepaste.me
│   └── TeamEquipManager      # Backup y restauración
├── discord/                  # Integración JDA
│   └── DiscordManager
├── listener/                 # Eventos Fabric/Cobblemon
│   ├── ServerLifecycleListener
│   ├── BattleEventListener
│   └── PlayerJoinListener
└── util/
    ├── MessageUtil, TimeUtil, CobblemonUtil
```

---

## 📄 Licencia

MIT — libre para uso en servidores públicos y privados.
