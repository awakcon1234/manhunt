# Copilot Instructions - LunaManhunt

## Formatting
- Java/Gradle/Groovy: **tab indent** (tab width=4)
- YAML: **spaces only** (see .editorconfig)

## GUI UX rules
- **Scannable UI text**: add blank spacer lines between logical sections in GUI item lore

## Big Picture
- Plugin entry point wires services and listeners in [src/main/java/dev/belikhun/lunamanhunt/LunaManhuntPlugin.java](src/main/java/dev/belikhun/lunamanhunt/LunaManhuntPlugin.java).
- Match lifecycle lives in [src/main/java/dev/belikhun/lunamanhunt/service/match/MatchManager.java](src/main/java/dev/belikhun/lunamanhunt/service/match/MatchManager.java) and drives `MatchState` transitions.
- Lobby/invite flow is in [src/main/java/dev/belikhun/lunamanhunt/service/lobby/LobbyManager.java](src/main/java/dev/belikhun/lunamanhunt/service/lobby/LobbyManager.java), using a player->match index.
- Win conditions are handled by [src/main/java/dev/belikhun/lunamanhunt/service/match/WinConditionService.java](src/main/java/dev/belikhun/lunamanhunt/service/match/WinConditionService.java) + [src/main/java/dev/belikhun/lunamanhunt/listener/MatchWinListener.java](src/main/java/dev/belikhun/lunamanhunt/listener/MatchWinListener.java).
- Compass tracking uses PDC keys in [src/main/java/dev/belikhun/lunamanhunt/service/compass/CompassService.java](src/main/java/dev/belikhun/lunamanhunt/service/compass/CompassService.java) and [src/main/java/dev/belikhun/lunamanhunt/listener/CompassListener.java](src/main/java/dev/belikhun/lunamanhunt/listener/CompassListener.java).
- Persistence is YAML-based in [src/main/java/dev/belikhun/lunamanhunt/service/persistence](src/main/java/dev/belikhun/lunamanhunt/service/persistence).

## External Dependencies
- Multiverse-Core API (v5.5.2) is a hard dependency; repository is `https://repo.onarandombox.com/content/groups/public/` and the artifact is `org.mvplugins.multiverse.core:multiverse-core:5.5.2` (see [build.gradle.kts](build.gradle.kts)).
- Use `MultiverseCoreApi.get()` via [src/main/java/dev/belikhun/lunamanhunt/service/integration/MultiverseService.java](src/main/java/dev/belikhun/lunamanhunt/service/integration/MultiverseService.java).

## Language/localization
- All player-facing text **must be Vietnamese** with full accents (chat, GUI, HUD, templates)
- Text must look professional and consistent; formatting/icons allowed (see below)
- Use helpers: `Text.msg`, `Text.item`, `Text.title`, MiniMessage in config.yml
- Console logs may be English; player-facing must not

## UI icons
- **Minecraft-safe symbols only** (do not use lookalikes):
  + Bullet: `●` | Check: `✔` | Cross: `❌` | Info: `ℹ` | Time: `⌚` | Wait: `⌛ ⏳`
  + `🪓` `⛏` `⚔` `🗡` `🏹` `⬤` `█` `»` `▶` `★` `❤` `•` `☀` `⛀⛁💰` `☠` `☹` `👾` `👻` `🔧` `🌧` `☁` `☄` `⭐` `🔍` `☮` `☯` `⛔` `⚠` `🔔` `♣` `♦` `♥` `♠` `🔥` `❣` `💢💬` `💤` `☹` `☻` `☺` `☂` `☃` `™` `®` `©` `₪` `★` `☆` `✦` `🧪` `⚗`
  + `≈⌀⌂【】⊻⊼⊽⋃↔↑↓→←◎☽ⒶⒷⒸⒹⒺⒻⒼⒽⒾⒿⓀⓁⓂⓃⓄⓅⓆⓇⓈⓉⓊⓋⓌⓍⓎⓏⓐⓑⓒⓓⓔⓕⓖⓗⓘⓙⓚⓛⓜⓝⓞⓟⓠⓡⓢⓣⓤⓥⓦⓧⓨⓩ∃∧∨∩⊂⊃∀ΞΓɐəɘεβɟɥɯɔиɹʁяʌʍλчΣΠηαʊїз¢№¿¡ƸӜƷξЖ`
  + All block drawing unicode characters

## Conventions
- Indentation rules are enforced by [.editorconfig](.editorconfig): tabs for Java, 2-space for YAML.
- All player-facing text **must be Vietnamese** and use MiniMessage via `Text.msg`/`Text.tell` in [src/main/java/dev/belikhun/lunamanhunt/util/Text.java](src/main/java/dev/belikhun/lunamanhunt/util/Text.java).
- Legacy `&` formatting is available (`Text.msgLegacy`) but discouraged.

## Data Model Notes
- Match state, settings, and world info are stored in [src/main/java/dev/belikhun/lunamanhunt/model/match](src/main/java/dev/belikhun/lunamanhunt/model/match).
- `Match` holds ownership, speedrunner, hunters, invites, and winner.
- Profiles are persisted under `data/profiles/{uuid}.yml` and managed by [src/main/java/dev/belikhun/lunamanhunt/service/persistence/ProfileStore.java](src/main/java/dev/belikhun/lunamanhunt/service/persistence/ProfileStore.java).
- Match templates and lobby state are stored in `match-templates.yml` and `lobby-state.yml` via their stores.

## Build/Run
- Build with `./gradlew build` (Paper API + Multiverse are compileOnly).
- Always run `./gradlew shadowJar` at the end of your work to verify build errors.
- No automated tests are defined yet.
