# Changelog

## 1.0.0 — Initial release

- Added a server-authoritative, data-driven talent runtime for Minecraft 1.20.1 and Forge 47.x.
- Added in-game editors for talents, rarities, draw pools, counters, talent-book layouts, configured items, synthesis, presentation actions, and complex event effects.
- Added multi-attribute talents, maintained effects, effect immunity, dynamic attributes, creative flight, draw-search constraints, and event actions for entity attributes, damage, healing, and retaliation.
- Added configurable 1–5 choice draws, refreshes, draw limits, luck weighting, pity behavior, automatic/manual synthesis, and paid refreshes using draw tokens.
- Added a per-pool 8–64 pixel draw-card icon size with responsive downscaling on narrow or short screens.
- Simplified the talent effect section to Add/Edit/Delete list actions and moved all effect creation and parameter editing into one explicit editor; applying values now writes back immediately.
- Reused the localized attribute catalog for ordinary attributes, so registered IDs and translated names can both be searched.
- Event triggers now appear as individual selectable effect rows on both the talent and complex-effect pages and reopen the selected trigger for editing.
- Removed the registered-attribute browser and attribute search box from the editor home page; attribute selection remains available where an effect actually needs it.
- Replaced the exhaustive README with concise Chinese and English installation and quick-start guides.
- Added the built-in Talent Book and Talent Point as editable system-item definitions while preserving their fixed interactions.
- Added controlled PNG/JPG/JPEG texture import, OGG sound import, batch reload, resource lookup, SHA-256 auditing, complete settings export/import, and new-world templates.
- Added independent personal/global policies for synthesis sounds and synthesis messages, plus immediate/queued custom-sound playback.
- Automatic synthesis mode now hides the talent book's synthesis-target page and manual synthesis action; manual mode retains the original workflow.
- Preserved player talents, counters, draw tokens, pity state, and trigger lifecycle data across death, dimension changes, and login.

Network protocol: 31. Clients and servers must use the same mod build.
