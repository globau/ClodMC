# Architecture

ClodMC is a Minecraft Paper plugin that provides gameplay enhancements for a
single server instance. The plugin follows a modular architecture where all
functionality is implemented as discrete modules managed by a central registry.

## Module System

The core architectural pattern is the module system. All plugin functionality
is encapsulated in modules that extend the `Module` base class and are managed
by `ModuleRegistry`. The main class `ClodMC` bootstraps the registry and
coordinates the module lifecycle.

Modules are organised into functional domains:

- **player/** - Player-centric features (homes, teleportation, AFK detection, offline messages)
- **inventory/** - Inventory manipulation (deep pockets, sorting, restoration)
- **interactions/** - Block and item interactions (gateways, vein mining, named storage)
- **mobs/** - Mob behaviour modifications (drops, spawn control, grief prevention)
- **server/** - Server-wide systems (MOTD, status monitoring, heat maps)
- **bluemap/** - BlueMap integration and overlays
- **crafting/** - Custom crafting recipes

Each module domain contains related functionality grouped by feature rather
than technical concern. Modules can depend on each other through the registry
and share common utilities.

Modules interact with the game world through Paper's event-driven
architecture, registering event listeners to respond to player actions, world
changes, and server events.

## Utilities Layer

The `util/` package provides shared functionality across modules:

- **Players** - Player lookups and utilities
- **Chat/ChatStyle** - Message formatting and styling
- **TeleportUtil** - Safe teleportation logic
- **ConfigUtil** - Configuration file management
- **Logger** - Logging abstraction
- **Schedule** - Task scheduling wrapper
- **HttpClient/HttpResponse** - HTTP operations
- **Mailer** - Email notifications
- **Bedrock** - Geyser integration utilities

Position and location utilities (`BlockPos`, `Vector3i`, `LocationUtil`) handle
coordinate system abstractions.

## Data Layer

Player data persistence is handled through:

- **PlayerDataFile** - Individual player data storage
- **PlayerDataFiles** - Player data lifecycle management

Data is stored as YAML files with automatic serialisation. The data layer is
intentionally simple as the plugin serves a single server instance.

## Command System

The plugin implements a custom command framework in the `command/` package:

- **CommandBuilder** - Fluent command definition API
- **Executor** - Type-safe command execution interfaces
- **Completor** - Tab completion implementations

The framework prioritises caller experience over implementation complexity,
using multiple executor interfaces internally to provide clean, type-safe
lambda signatures externally.

## Integration Boundaries

- **Paper API Boundary** - All Minecraft server interaction goes through the Paper API. The plugin does not use NMS (net.minecraft.server) code, maintaining compatibility across server versions.
- **Optional Dependencies** - Integration with GriefPrevention, BlueMap, and Geyser-Spigot is handled through optional dependency patterns with runtime capability detection.
- **File System Boundary** - All persistent data is stored under the plugin's data folder using standard Paper configuration APIs.
- **Vendored Code Boundary** - Code in `src/main/java/vendored/` is third-party and must not be modified. Vendored dependencies are documented in `vendored.properties` files.

## Architectural Invariants

- **Single Instance** - The plugin assumes it runs on exactly one server instance with no need for multi-server coordination or plugin disabling
- **Module Independence** - Modules do not directly import from each other's packages; inter-module communication goes through the registry
- **UK Spelling** - All code, comments, and user-facing text uses UK English spelling conventions

## Cross-Cutting Concerns

- **Logging** - All logging goes through the `Logger` utility which wraps the plugin logger with contextual information.
- **Messaging** - All messaging to players goes through the `Chat` utility which provides standardised colouring.
- **Scheduling** - Async and sync task scheduling uses the `Schedule` utility which provides cleaner abstractions over Paper's scheduler.
- **Code Quality** - Checkstyle, SpotLess, and Error Prone enforce consistent code style and catch common issues at build time. Custom checkstyle rules in `checkstyleChecks/` enforce project-specific patterns.
