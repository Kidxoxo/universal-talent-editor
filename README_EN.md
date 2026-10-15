# Universal Talent Editor 1.3.0

An in-game talent editor for Minecraft 1.20.1 / Forge 47.3.22–47.x. The editor has no mandatory third-party dependencies; talent packs that reference other mods require those mods.

## Install and use

Install `talent_editor-forge-1.20.1-1.3.0.jar` in the client and server `mods` folders and use Java 17. Administrators open the editor with `F8`; players open the talent book with `J`. Keys can be rebound. Multiplayer clients and servers must use the same build.

Configure talents, rarities, draw pools, counters, progression, synthesis, layouts and custom items. Import and export definitions and resource packs in game.

Version 1.3.0 adds a visual canvas for talent-book and draw screens, with drag, resize and preview controls for backgrounds, buttons, talent slots and text. Customize images, opacity, positioning and button destinations. The editor has a unified style and streamlined controls. Acquisition timelines support text, sound, particles and model effects, with improved combined previews, line wrapping and per-message display duration.

Talent points have a default 5% chance to appear in chest loot, one point per successful roll. Adjust or disable this in the item editor; see the [loot rule](docs/DEFAULT_LOOT_1.3.0.md) for matching scope.

World definitions live in `<world>/datapacks/talent_editor_generated/`. Enable the matching client resource pack for custom images and audio; other multiplayer clients need these resources too.

## Build

Use Java 17 and run `./gradlew build` (`gradlew.bat build` on Windows). Artifacts are written to `build/libs/`. See the [schema](docs/SCHEMA_V2.md) and [UI guide](docs/UI_EDITOR_1.3.0.md).

License: MIT.
