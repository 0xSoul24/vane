# Module vane-trifles

The catch-all module for vane's smaller quality-of-life features and its custom items.

Unlike the other feature modules there is no single system here. [org.oddlama.vane.trifles.Trifles]
instantiates a long list of largely independent components, each gated by its own configuration so
that servers can enable exactly the pieces they want.

Features include harvesting crops by right-clicking, sickles that harvest in an area, chest sorting,
double door linking, item frame handling, anvil repair cost limiting, recipe unlocking, a
fast-walking group, and an item finder that locates items across nearby containers.

## Custom items

The `items` package holds vane's custom items, each declared with `@VaneItem` from
`vane-annotations` and registered through `vane-core`'s custom item registry. The annotation's
`modelData` value reserves a slot in the generated resource pack, so those values must stay unique
across the whole plugin suite.

Items are frequently declared in pairs: a single item class plus a plural holder class that
registers the tiered variants (`Sickle` / `Sickles`, `Scroll` / `Scrolls`, `XpBottles`).

# Package org.oddlama.vane.trifles

Module entry point and the individual quality-of-life feature components and listeners.

# Package org.oddlama.vane.trifles.commands

The `finditem`, `heads` and `setspawn` commands.

# Package org.oddlama.vane.trifles.event

Events fired by this module, currently the teleport event raised when a player uses a scroll.

# Package org.oddlama.vane.trifles.items

The custom items: sickles, the several kinds of scroll, the north compass, papyrus scroll,
reinforced elytra, slime bucket, trowel, file, and the experience bottles.

# Package org.oddlama.vane.trifles.items.storage

Portable container items — the pouch and the backpack — and the shared `StorageItem` base they
extend.
