## Plan: LunaManhunt Match System

**Vision (Original)**

- there will be a lobby where players can create a match, invite other players into the match and start the match.
- each match will have an unique 7 leters ID.
- other players have the ability to join the match too using a GUI item or using the match unique invite ID.
- match owner can configure settings for the match before the match start:
    + specify speedrunner (random/specific player in the lobby)
	+ specify world seed
	+ world border limit (default 1000)
	+ speedrunner starting kit (weapons, armors, foods, etc...)
	+ match visibility (public, invite only/join code, private).
	+ *we will add more settings as i'm now out of idea*
- when the match started, a new world with nether and the end is created using [multiverse-core](https://mvplugins.org/core/developers/api-usage/)
- after all 3 worlds has completed initializing, all players in the match are teleported to the world spawn, with the world border initialy set to 10 blocks. the game start when the speedrunner hit any of the hunters. when the game started, each hunters are given a compass for tracking the speedrunner.
- when the speedrunner is killed by any of the hunters, the game is ended with the victory of the hunters. speedrunner dying by natural causes does not end the game.
- when the speedrunner killed the ender dragon, the game is finished with the victory of the speedrunner. their speedrun time is recorded into their profile, and an ending sequence is played where all hunters have their gamemode set to spectator and teleport hunters to the end if they are not in the end already, a firework show is played all over the end island. all players will be teleported to the lobby and cleanup of the worlds is initialized when the speedrunner enter the central end portal.
- there will be a power-up cards and effect cards system. the power-up cards will assist the speedrunner during their run, and effect cards help slowing down the hunters by applying bad effects on them. each draw cost a point. and each achievements the speedrunner got will award a point to use the card system.
- during the run, the speedrunners can draw power-up cards or effect cards.

with those basic informations in mind. analyze the technical aspect, generate a plan file to prepare for the implementation

**Vision (Clear Summary)**
You want a lobby-first Manhunt system where players create matches, invite others, and configure settings before the game begins. Each match has a unique 7-letter ID so players can join through a GUI or by entering the code.

Before the match starts, the owner can set the speedrunner (random or selected), world seed, world border limit (default 1000), a starting kit for the speedrunner, and visibility (public, invite-only, or private). More settings will be added later.

When the match starts, a fresh Overworld, Nether, and End are created using Multiverse-Core. After all three worlds finish generating, all match players are teleported to the Overworld spawn and the world border is set to 10 blocks. The run officially begins only when the speedrunner hits any hunter. At that moment, all hunters receive tracking compasses.

Win conditions:
- Hunters win only if a hunter kills the speedrunner.
- The speedrunner dying to natural causes does not end the match.
- The speedrunner wins by killing the Ender Dragon. The run time is recorded to their profile, hunters are set to spectator and sent to the End, a firework show plays over the End island, and after the speedrunner enters the central End portal everyone returns to the lobby and the match worlds are cleaned up.

You also want a card system:
- Power-up cards help the speedrunner.
- Effect cards hinder hunters.
- Each draw costs points.
- Points are earned through achievements during the run.
- Points only exist during the match and reset afterward.
- Only the speedrunner earns points from achievements.
- Speedrunners can draw cards during the match.

Build a multi-match lobby + world lifecycle system around Paper + Multiverse-Core, with command + GUI surfaces from day one and YAML persistence. The core work is to model match state, world provisioning, player routing, and win conditions, then layer GUI flows and a card/points subsystem on top. This plan assumes a hard dependency on Multiverse-Core and supports multiple concurrent matches with unique 7-letter IDs.

**Steps**
1. Define domain models and state boundaries: `Match`, `MatchSettings`, `MatchState`, `MatchPlayerRole`, `CardType`, `PlayerProfile`, `MatchWorlds`; decide where runtime state lives vs persisted YAML; add initial config defaults in a new config file under [src/main/resources](src/main/resources).
2. Introduce services with clear ownership (singletons registered from [src/main/java/dev/belikhun/lunamanhunt/LunaManhuntPlugin.java](src/main/java/dev/belikhun/lunamanhunt/LunaManhuntPlugin.java)): `MatchManager`, `LobbyManager`, `WorldManager` (Multiverse API), `ProfileStore` (YAML), `CardService`, `InviteService`, `GuiService`.
3. Wire Multiverse-Core hard dependency and API usage: add dependency declarations and a compile-only API if available; declare `depend: [Multiverse-Core]` in both [src/main/resources/plugin.yml](src/main/resources/plugin.yml) and [src/main/resources/paper-plugin.yml](src/main/resources/paper-plugin.yml); add a startup check that fails fast with a clear log if Multiverse is missing.
4. Implement match lifecycle: create match (generate 7-letter ID), invite/join flows (public / invite-only / private), configure settings in lobby, start match -> create three worlds via Multiverse, wait for world init, set border to 10, teleport all match players to spawn, register "first hit starts game" trigger, then grant hunters tracking compasses when the run officially begins.
5. Implement win/lose rules and end sequence: hunter kills speedrunner ends match; speedrunner death by non-player causes doesn't end; ender dragon kill triggers speedrunner win, records time to profile, sets hunters to spectator, teleports hunters to end, plays fireworks, then returns everyone to lobby after speedrunner enters end portal and queues world cleanup.
6. Build command + GUI layer: commands for match creation/join/invite/config/start plus admin utilities; GUI screens for match list, create match, invite list, settings editor, card draw; add permissions and help output.
7. Add the card/points system: match-scoped points earned by speedrunner achievements (define initial trigger list), points spent to draw power-up/effect cards, card inventory/selection UI, effect application logic gated to match state and roles.
8. Create persistence schema: YAML files for profiles, match templates, and last known lobby state; ensure safe load/save with versioning and migration hooks.

**Verification**
- Start a Paper server with Multiverse-Core installed; confirm plugin enables and registers commands/GUI.
- Create multiple matches concurrently; verify unique IDs, invite flows, and lobby visibility.
- Start a match: verify world creation, border set to 10, teleport, and "first hit starts game."
- Test win conditions: hunter kill of speedrunner ends match; dragon kill ends match and runs end sequence.
- Validate profile persistence by restarting server and verifying stored times/points.

**Decisions**
- Persistence uses YAML/JSON files in the plugin data folder.
- Commands and GUI are both implemented from day one.
- Multiverse-Core is a hard dependency.
- Multiple matches can run in parallel.

**Implementation TODO (Detailed)**
1. Define core domain models
2. Set up persistence layer
3. Integrate Multiverse dependency
4. Implement match lifecycle
5. Build lobby and invites
6. Implement win conditions
7. Add compass tracking
8. Add commands and permissions
9. Create GUI flows
10. Implement cards and points
11. Add config defaults
12. Add tests and validation
