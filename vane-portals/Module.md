# Module vane-portals

Player-built portals for long distance travel, supporting every entity type including minecarts.

## Model

A portal is a `Portal` in the `portal` package: an owner, an `Orientation`, a spawn location, the
list of `PortalBlock`s making up the structure, a visibility mode and an optional fixed target.
Portals are persisted through `vane-core`'s storage layer.

Saving is deferred rather than immediate. Every mutating setter sets the transient `invalidated`
flag, and a periodic pass writes out only the portals that are actually dirty. Code that changes a
portal outside its setters must set the flag itself or the change will be lost.

## Construction and activation

`PortalConstructor` recognises a valid structure from the blocks a player places and builds the
`Portal` from it. `PortalBoundary` and `PortalBlockLookup` handle the geometry and the reverse
mapping from a world block back to the portal that owns it. `PortalActivator` and
`PortalTeleporter` drive the runtime behaviour, with `EntityMoveProcessor` polling entity movement
so that non-player entities are carried through too.

`Style` controls the visual appearance of a portal's blocks and is configurable per portal, falling
back to a server default.

# Package org.oddlama.vane.portals

Module entry point plus the runtime components: construction, activation, teleportation, block
protection, entity movement processing, and the Dynmap and BlueMap marker layers.

The map layers are each split into a component and a delegate so that the component can be loaded
unconditionally while the delegate — which touches the map plugin's classes — is only loaded when
that plugin is actually present.

# Package org.oddlama.vane.portals.entity

Floating display entities used for portal decoration.

# Package org.oddlama.vane.portals.event

Bukkit events fired by this module so other plugins can observe or cancel portal behaviour:
construction and destruction, activation and deactivation, console linking and opening, target
selection, settings changes, and the synthetic `EntityMoveEvent` that `EntityMoveProcessor` raises.

# Package org.oddlama.vane.portals.menu

The inventory menus for configuring a portal — its console, settings, style picker and name entry.

# Package org.oddlama.vane.portals.portal

The portal model and geometry: `Portal`, `PortalBlock`, `PortalBoundary`, `Orientation`, `Plane`
and `Style`.
