# Module vane-geyser-extension

A Geyser extension that makes the vane plugin suite usable from Bedrock Edition clients.

This is not a Bukkit plugin. It is loaded by Geyser's own extension loader from `extension.yml`,
and it solves two problems Bedrock players otherwise have.

**Custom items do not render.** Bedrock clients cannot read the Java resource pack that vane
generates, so `ItemRegistration` re-declares every vane custom item as a Geyser
`CustomItemDefinition`, mapping the Java base item and model data onto a Bedrock-compatible
definition. Item definitions here must be kept in step with the `@VaneItem` declarations in
`vane-core` and `vane-trifles`; a mismatch shows up as a missing or wrong texture on Bedrock only.

**Typing commands is painful on touch devices.** `CommandsRegistration` presents vane's features as
a hierarchy of Geyser Cumulus forms behind a single `/menu` command, covering core, admin,
permissions, regions, trifles and velocity.

`VaneGeyser` is the entry point and subscribes to Geyser's lifecycle events to drive both.

# Package org.oddlama.vane.geyserextension

The extension entry point plus the item and command registrations described above.
