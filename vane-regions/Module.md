# Module vane-regions

A grief-protection system built around player-claimed regions, role-based permissions and
configurable environment rules.

## Model

A `Region` is a named, owned cuboid described by a `RegionExtent`, with minimum and maximum sizes
enforced by configuration. Access is expressed through `Role`s: each region defines roles, each role
carries a set of `RoleSetting` flags, and players are assigned to roles. `EnvironmentSetting`
controls world behaviour inside the region — mob spawning, fire spread, explosions and similar —
independently of who is present.

Regions can be collected into a `RegionGroup` so that a set of claims shares one role and setting
configuration instead of each being managed separately.

Server-wide defaults live in `RegionGlobalRoleOverrides` and `RegionGlobalEnvironmentOverrides`,
which let an administrator force settings regardless of what a region owner chooses.

## Integrations

Claiming can be tied to an economy through `RegionEconomyDelegate`, so regions cost money to create
or expand. `RegionPortalIntegration` bridges to `vane-portals` so that portal use inside a region
respects that region's roles. Both are optional and inert when the corresponding plugin is absent,
as are the Dynmap and BlueMap layers.

# Package org.oddlama.vane.regions

Module entry point, the global override components, the economy and portal integration delegates,
and the map marker layers.

Like `vane-portals`, each map layer is split into a component and a delegate so the classes that
touch the map plugin are only loaded when that plugin is installed.

# Package org.oddlama.vane.regions.commands

The `region` command.

# Package org.oddlama.vane.regions.event

Listeners that enforce the region model against gameplay: the role, environment and portal-role
setting enforcers, plus the listener backing interactive region selection.

Despite the package name these are enforcement listeners, not event definitions.

# Package org.oddlama.vane.regions.menu

The inventory menus for managing regions, region groups and roles, along with the name-entry menus
and shared menu helpers.

# Package org.oddlama.vane.regions.region

The region model: `Region`, `RegionGroup`, `RegionExtent`, `RegionSelection`, `Role`, `RoleSetting`,
`EnvironmentSetting` and the JSON serialization helpers backing persistence.
