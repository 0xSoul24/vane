# Module vane-admin

Server administration conveniences: automatic shutdown when the server sits empty, spawn
protection, and a handful of operator commands.

Each feature is an independent `ModuleComponent` or `ModuleGroup` instantiated from
[org.oddlama.vane.admin.Admin]'s `init` block, so any of them can be switched off through
configuration without affecting the rest.

The autostop feature is split across three types on purpose:
[org.oddlama.vane.admin.AutostopGroup] owns the state, the schedule and the messages;
`AutostopListener` reacts to players joining and leaving; and the `Autostop` command exposes manual
control. The group carries the `enabled` flag that gates all three.

# Package org.oddlama.vane.admin

Module entry point and the feature components — autostop, spawn protection, gamemode, time,
weather and slime chunk detection.

# Package org.oddlama.vane.admin.commands

Operator-facing commands for the features above.
