# VoranixConditions

Plugin para **Spigot 1.20.1** (Java 17 o superior) que permite:

> Esta es la rama `spigot-1.20.1`. La versión para Paper 26.3 está en la rama `main`.

- Crear **scripts** en YAML que cambian cómo se comportan los mobs y los jugadores (daño, efectos de poción, rayos, mensajes...), y activarlos o desactivarlos en cualquier momento.
- Crear **mobs nuevos** con modelos de **BetterModel** y darles comportamientos propios.

## Instalación

1. Compila con `gradlew build` → `build/libs/VoranixConditions-1.0.0-spigot1.20.1.jar` (ya incluye ACF y MiniMessage).
2. Copia el jar a `plugins/`.
3. Para modelos instala [BetterModel 1.4.2](https://modrinth.com/plugin/bettermodel/version/1.4.2), la última versión compatible con 1.20.1.
   **Ojo:** BetterModel necesita que el servidor corra con **Java 21** (Spigot 1.20.1 funciona bien con Java 21).
   Sin BetterModel el plugin funciona igual, pero los mobs se ven con su modelo vanilla.
4. Al iniciar se crean `plugins/VoranixConditions/scripts/` y `mobs/` con ejemplos.

## Comandos (permiso `voranixconditions.admin`, alias `/vc`)

| Comando | Qué hace |
|---|---|
| `/vcondition script <nombre> <true\|false>` | Activa o desactiva un script (se guarda en `states.yml`) |
| `/vcondition script <nombre>` | Muestra si está activo |
| `/vcondition list` | Lista los scripts y su estado |
| `/vcondition reload` | Recarga scripts y mobs sin reiniciar |
| `/vcondition mob spawn <id> [jugador]` | Spawnea un mob personalizado |
| `/vcondition mob list` | Lista los mobs y cuántos hay vivos |
| `/vcondition mob kill [id]` | Elimina los mobs personalizados vivos |
| `/vcondition debug` | Muestra en consola qué trigger se ejecutó y qué condición falló (con sus valores) |

## Scripts (`scripts/*.yml`)

```yaml
name: furia_nocturna
description: "Zombies +50% daño de noche"
enabled: false            # estado inicial
triggers:
  - on: damage
    conditions:
      - "{attacker.type} == ZOMBIE"
      - "{world.time} >= 13000"
    effects:
      potion:
        type: poison
        target: victim
        level: 1
        duration: 5s
    actions:
      - damage_multiply: 1.5
```

Orden de ejecución: primero se revisan las `conditions`, después se aplican los `effects` y al final las `actions`.

### Eventos (`on:`)

| Evento | Roles disponibles | Variables |
|---|---|---|
| `damage` | `attacker`, `victim`, `projectile` | `{damage}`, `{cause}` |
| `hurt` (cualquier daño, incluso caídas o fuego) | `victim`, `attacker` (si hay) | `{damage}`, `{cause}` |
| `death` | `victim`, `killer` | |
| `spawn` | `entity` | `{reason}` |
| `interact` (clic derecho a una entidad) | `player`, `target` | |
| `join`, `respawn` | `player` | |
| `timer` (`interval:` ej. `2s`) | `player` si `per: player`, nada si `per: none` | |
| `enable` / `disable` (al cambiar el script con el comando) | igual que `timer` | |

### Condiciones

Todas las líneas de `conditions:` deben cumplirse. En una línea puedes usar `&&` (y), `||` (o) y `!` (no).
Operadores: `==`, `!=`, `>`, `<`, `>=`, `<=`, `contains`. Los textos no distinguen mayúsculas.

### Placeholders

- `{rol.propiedad}`: `type`, `name`, `uuid`, `custom_mob`, `is_player`, `exists`, `world`, `x`, `y`, `z`, `biome`, `light`,
  `health`, `max_health`, `on_fire`, `in_water`, `on_ground`, `has_target`, `holding`,
  `tag:<tag>`, `has_effect:<efecto>`, `effect_level:<efecto>`, `distance:<otro rol>`,
  solo jugadores: `gamemode`, `sneaking`, `sprinting`, `level`, `food`, `permission:<permiso>`.
- `{world.time}`, `{world.day}`, `{world.storm}`, `{world.thunder}`, `{world.players}`, `{world.name}`, `{world.difficulty}`
- `{random}` (0 a 1, para probabilidades), `{online}`, `{damage}`, `{cause}`, `{reason}`

### Duraciones

En `duration`, `interval` y `delay` puedes usar unidades: `20t` (ticks), `5s` (segundos), `2m` (minutos), `1h` (horas)
o `infinite`. Un número sin unidad se cuenta en ticks (20 ticks = 1 segundo).

### Effects

```yaml
effects:
  potion:
    type: poison        # efecto (nombres de Minecraft: speed, strength, regeneration, wither, ...)
    target: victim      # a quién (también se puede escribir trigger:)
    level: 2            # nivel como en el juego: 1 = I, 2 = II...
    duration: 10s
    particles: true     # opcional
```

Para varios efectos a la vez, pon una lista debajo de `potion:` (ver `evento_velocidad.yml`).

### Acciones

Cada acción se escribe con `:`. Si solo necesita un valor va en la misma línea; si necesita más, los parámetros van debajo:

```yaml
actions:
  - damage_multiply: 2
  - message:
      target: player
      text: "<red>Hola {player.name}"
  - delay: 1s
```

`target` acepta un rol (`player`, `victim`, `attacker`, `entity`, `target`, `killer`), `all` (todos los jugadores),
`world_players`, `nearby:<radio>` (entidades vivas cerca) o `nearby_players:<radio>`.

| Acción | Valor en la misma línea | Parámetros |
|---|---|---|
| `damage_multiply` / `damage_add` / `damage_set` | número | solo en eventos de daño (`damage`, `hurt`, `attack`) |
| `cancel` | — | cancela el evento (daño, spawn, interacción...) |
| `potion` | efecto | `target`, `effect`, `level`, `duration`, `particles` |
| `clear_potion` | efecto | `target`, `effect` (sin efecto = quita todos) |
| `heal` / `damage` | cantidad | `target`, `amount` |
| `ignite` | duración | `target`, `duration` |
| `velocity` | — | `target`, `x`, `y`, `z` |
| `knockback` | fuerza | `target`, `strength`, `y` (fuerza negativa = atrae) |
| `leap` | rol destino | `target`, `towards`, `strength`, `y` |
| `teleport` | rol destino | `target`, `towards` |
| `target` | rol | `target` (el mob), `towards` (a quién ataca) |
| `lightning` | — | `target`, `fake` (true = solo visual) |
| `explosion` | poder | `target`, `power`, `fire`, `break` |
| `message` / `actionbar` | texto | `target`, `text` (MiniMessage) |
| `title` | título | `target`, `title`, `subtitle`, `duration` |
| `broadcast` | texto | `text` |
| `sound` | sonido | `target`, `sound` (ej. `entity.wither.spawn`), `volume`, `pitch` |
| `particle` | partícula | `target`, `particle`, `count`, `spread`, `color` (para `dust`: `#ff0000`) |
| `command` | comando | `command` (lo ejecuta la consola) |
| `player_command` | comando | `target`, `command` |
| `spawn` | entidad | `entity`, `at` (rol donde aparece) |
| `spawn_mob` | id del mob | `mob`, `at` |
| `animate` | animación | `target`, `animation` (BetterModel) |
| `toggle_script` | nombre | `script`, `enabled` (true/false) |
| `delay` | duración | espera antes de las acciones siguientes |

> El formato antiguo en una sola línea (`- potion victim poison 100 0`) sigue funcionando.

> Efectos "permanentes" mientras el script esté activo: usa un `timer` que reaplique el efecto con una duración
> un poco mayor que el intervalo (ver `evento_velocidad.yml`). Al desactivar el script el efecto se acaba solo.

### Diferencias con la versión Paper

- No existe el atributo `scale` (llegó en Minecraft 1.20.5).
- Las partículas se pueden escribir con su nombre de Minecraft (`large_smoke`, `dust`, `explosion`...) o con el nombre antiguo de Bukkit (`SMOKE_LARGE`, `REDSTONE`...).
- Los textos usan MiniMessage (`<red>`, `<gradient:...>`) y también aceptan códigos `&c`.

## Mobs personalizados (`mobs/*.yml`)

Mira `mobs/golem_oscuro.yml`. Un mob tiene una entidad `base` vanilla (da la IA y la hitbox), un `model` de BetterModel
(el nombre del `.bbmodel` en `plugins/BetterModel/models/`), atributos (`health`, `damage`, `speed`, `armor`,
`knockback-resistance`, `follow-range`), `natural-spawn` para reemplazar spawns vanilla, `animations` que suenan solas
y `behaviors` con los mismos eventos/condiciones/acciones que los scripts.

Eventos de mob: `spawn`, `tick` (con `interval`), `attack` (rol `victim`), `hurt` (rol `attacker`), `death` (rol `killer`),
`target` (rol `target`), `interact` (rol `player`). El mob siempre es `entity` y su objetivo actual es `target`.

Si pones `script: <nombre>` en un mob, sus comportamientos y su spawn natural solo funcionan con ese script activo.
