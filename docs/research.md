# Manhunt+ Low-Level Architecture Findings

This document summarizes low-level architecture and behavioral patterns observed in the Manhunt+ plugin (TutlaMC/manhunt-plus). It is intentionally descriptive and avoids reproducing implementation code.

## Scope

- Focused on plugin runtime structure, responsibilities, and data flow.
- Covered command handling, event listeners, scheduled tasks, loot logic, and twist mechanics.
- Observations are based on public repository structure and files.

## Module Overview

- Main plugin class: single entry point and state owner.
- Event listeners: centralized listener class for gameplay events and twist behaviors.
- Helpers/utilities: twist effects and common logic.
- Loot system: weighted pools and leveling tiers.
- Config: runtime tuning for broadcast, compass calibration, and timers.

## Runtime State Model

The main plugin class owns core runtime state and acts as a service locator. Key state fields include:

- Match state: started flag and waiting-for-start flag.
- Time state: timer seconds and scheduled task id; optional countdown limit.
- Role lists: UUID lists for speedrunners, hunters, and active speedrunners.
- Twist state: enum defining current twist behavior.
- Compass state: map from compass ID to target player.
- Loot state: default loot pool plus per-player leveling pools.

Notable characteristics:

- State is held in memory only; no persistence across restarts.
- Role and compass state are modified directly by command handlers and event listeners.

## Lifecycle and Scheduling

### onEnable

- Initializes singleton instance, loot pools, and helper classes.
- Saves default configuration and registers event listeners.
- Starts a periodic compass calibration task.

### onDisable

- Only logs a shutdown message; no explicit cleanup of tasks or lists.

### Scheduled Tasks

- Timer task (1 second interval):
  - Increments an internal counter.
  - Optional broadcast every N seconds.
  - Optional countdown limit; when reached, stops match and broadcasts failure.

- Compass auto-calibration task (configurable interval):
  - Iterates tracked compass map.
  - Removes entries for offline targets.
  - Scans all online players and inventory items to update matching compasses.

## Command Layer

All command handling is in the main plugin class. Commands are routed by command name, then by subcommand string.

Commands include:

- Role management: speedrunner/hunter add/remove.
- Match control: start, stop, prepare, list, countdown, help.
- Twist selection: set twist by enum name.
- Utility: compass distribution and surround action.

Tab completion provides subcommands and online player names, plus twist names for selection.

## Event-Driven Logic

### Match Progression

- Player death (speedrunner): remove from active list; if empty, hunters win.
- Ender Dragon death: if match active and speedrunners exist, speedrunners win.
- Prepare mode: first damage from a speedrunner to a hunter triggers match start.

### Compass Recalibration

- Right-click on compass triggers immediate calibration for the target.
- Calibration rewrites lodestone target for the item when allowed by twist/rules.

### Twist-Driven Events

- Specific twists hook into entity death, interaction, and item consumption.
- Some twists use custom items with PDC metadata for cross-player effects.

## Twist System

The twist system uses:

- An enum to represent the active twist.
- A helper class that encapsulates twist effects.
- Event listener checks to gate behaviors based on active twist.

Example behaviors:

- Loot replacement on pig death for speedrunners.
- Custom item creation and consumption leading to targeted effects.
- Punishment effects: potion debuffs, velocity changes, inventory removal, health adjustments.

## Loot System

The loot system has two layers:

- Weighted loot pools (single roll from a list of entries).
- Tiered leveling pools to increase item strength over time.

Usage patterns:

- Base pool used for random high-tier drops.
- Per-player leveling pool tracked by UUID; item output is based on the current tier.

## Data Flow Patterns

- Commands mutate global state (role lists, timers, twist selection).
- Events consult and mutate state; some events trigger start/stop transitions.
- Scheduled tasks read and update compass state and timer state.

## Risks and Coupling Observations

- Tight coupling to the main plugin class; most logic depends on static getters.
- Single listener handles many event types, increasing complexity.
- In-memory state only; no persistence for role lists or settings.
- Compass auto-calibration scans all players and inventory contents each tick interval.

## Opportunities for a Cleaner Architecture

If re-implementing:

- Split into services: MatchService, RoleService, CompassService, TwistService, LootService.
- Move command parsing into dedicated command handlers.
- Replace global mutable lists with a small state model that can be snapshotted.
- Add persistence for roles and twist selection.
- Reduce compass recalibration cost by tracking item holders or using events to update.

