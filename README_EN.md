# Universal Talent Editor 1.2.0

An in-game talent editor for Minecraft 1.20.1 / Forge 47.3.22–47.x. The mod requires no additional dependencies. Features that reference another mod's attributes, items or effects require that mod to be installed.

## Installation and use

Place `talent_editor-forge-1.20.1-1.2.0.jar` in the `mods` folders of both the client and server, and launch with Java 17. Administrators press `F8` to open the editor; players press `J` to open the talent book. Keys can be changed in the controls menu. Clients and servers must use the same version.

The editor manages talents, rarities, draw pools, counters, progression and synthesis rules, talent book layouts, and custom items. Talents support attributes, event actions, conditions, lifecycle settings, HUD elements and player model effects. The talent tree preview displays and edits relationships between talents. Configurations and resource packs can be imported and exported in-game.

Version 1.2.0 adds an acquisition and synthesis performance timeline with text, sounds, particles and player model effects, plus a complete preview. Configured items support an attribute list, multiple equipment slots and Curios slot types.

World configurations are stored in `<world>/datapacks/talent_editor_generated/`. Custom icons, backgrounds and sounds require a client resource pack. Multiplayer servers do not automatically distribute local resource files to other players.

## Building

With Java 17, run `./gradlew build` (`gradlew.bat build` on Windows). Runtime and source JARs are written to `build/libs/`. See the [documentation](docs/SCHEMA_V2.md) for configuration details.

License: MIT.
