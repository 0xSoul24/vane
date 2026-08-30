# Module vane-core

The framework every other vane plugin is built on, plus the shared gameplay systems they all reach
for. `vane-core` is a real plugin in its own right, but its main job is to provide the module
lifecycle, the config and translation machinery, the custom item and enchantment registries, the
menu toolkit and the resource pack generator.

Every other vane plugin declares a hard dependency on it, and most of them shade against its
`shadow` configuration as well as depending on the project directly.

## The module system

Four types in `org.oddlama.vane.core.module` carry the whole design:

- **`Module`** — the `JavaPlugin` subclass each plugin extends. It is also its own root `Context`
  and a Bukkit `Listener`.
- **`Context`** — an association to a module plus a namespace for config and language keys.
  Calling `namespace(...)` yields a nested `ModuleContext`, and the resulting chain of names is what
  produces dotted paths in `config.yml` and the lang files.
- **`ModuleComponent`** — a context-bound unit of behaviour with its own enable/disable hooks and
  scheduling helpers. Listeners, commands, menus and map integrations are all components.
- **`ModuleGroup`** — a `Context` that automatically contributes an `enabled` config flag. When the
  flag is off, `onModuleEnable()` is never called for anything inside the group, which is how vane
  makes individual features switchable without scattering `if` checks.

Constructing a component registers it with its context, so most modules configure themselves purely
by instantiating components in their `init` block and never referring to them again.

## Configuration, language and storage

`ConfigManager` and `LangManager` reflect over a module's fields looking for the annotations
declared in `vane-annotations`, wrap each in a `ConfigField` / `LangField`, and use those both to
*load* values and to *generate* the annotated default `config.yml` and lang files. The `desc` on a
config annotation becomes the YAML comment. `org.oddlama.vane.core.persistent` does the equivalent
for `@Persistent` fields against `storage.json`.

Each of the three has an independent version number declared on `@VaneModule`, compared against the
on-disk file to decide whether to migrate or regenerate.

# Package org.oddlama.vane.core

Module entry point and top-level pieces: [org.oddlama.vane.core.Core] itself, the base `Listener`
helper, and loot table support.

# Package org.oddlama.vane.core.command

The command framework — a builder-style tree of parameters that produces both execution and
tab-completion behaviour from a single declaration.

# Package org.oddlama.vane.core.command.argumentType

Brigadier-style argument types for command parameters.

# Package org.oddlama.vane.core.command.check

Pre-execution checks such as permission and sender-type guards.

# Package org.oddlama.vane.core.command.enums

Enumerations usable directly as command parameters, such as time-of-day and weather values.

# Package org.oddlama.vane.core.command.params

Parameter node implementations that make up a command tree.

# Package org.oddlama.vane.core.commands

The commands `vane-core` itself provides.

# Package org.oddlama.vane.core.config

Runtime half of the config annotations. One `ConfigField` subclass per `Config*` annotation, plus
`ConfigManager`, which discovers them reflectively by annotation package name.

Adding a config type means adding a field class here *and* a branch in `ConfigManager.compileField`.

# Package org.oddlama.vane.core.config.loot

Configuration-driven additions to vanilla loot tables, used to seed custom items and tomes into
world generation.

# Package org.oddlama.vane.core.config.recipes

Configurable crafting recipe definitions.

# Package org.oddlama.vane.core.data

Reusable per-item and per-entity state helpers backed by Bukkit persistent data containers, such as
`CooldownData`.

# Package org.oddlama.vane.core.dynmap

Shared Dynmap integration. Modules that draw markers build on this rather than talking to Dynmap
directly, so that a missing Dynmap installation degrades quietly.

# Package org.oddlama.vane.core.enchantments

`CustomEnchantment` — the base class `@VaneEnchantment` requires — and the manager that registers
enchantments with the server.

# Package org.oddlama.vane.core.functional

Small functional interfaces with explicit arities (`Consumer1`, `Function2`, …), used so the command
and menu builders can accept lambdas without boxing everything into `Object`.

# Package org.oddlama.vane.core.item

Implementation of the custom item system backing `@VaneItem`: the item and model data registries,
durability handling, conversion of pre-existing items, and the inhibitor that suppresses the vanilla
behaviour of a base material when a vane item reuses it.

# Package org.oddlama.vane.core.item.api

The stable surface of the item system — `CustomItem`, the two registries and `InhibitBehavior` —
kept separate so other plugins can depend on it without reaching into the implementation.

# Package org.oddlama.vane.core.lang

Runtime half of the lang annotations: `TranslatedMessage`, `TranslatedMessageArray`, their field
wrappers, and `LangManager`.

# Package org.oddlama.vane.core.material

`ExtendedMaterial` — a material identifier that can also name a vane custom item, which is what
`@ConfigExtendedMaterial` fields resolve to.

# Package org.oddlama.vane.core.menu

The inventory-menu toolkit shared by portals, regions and the core menus.

# Package org.oddlama.vane.core.misc

Self-contained features that ship with core rather than a feature module: the offline/online
account multiplexer, command hiding, the head library, and loot chest protection.

# Package org.oddlama.vane.core.module

The module lifecycle types described above.

# Package org.oddlama.vane.core.persistent

Storage layer for `@Persistent` fields, serialized to each module's `storage.json`.

# Package org.oddlama.vane.core.resourcepack

Generates the client resource pack from the custom items and model data that modules register.

# Package org.oddlama.vane.util

General-purpose helpers shared across all vane plugins — block, item, time and formatting utilities.
