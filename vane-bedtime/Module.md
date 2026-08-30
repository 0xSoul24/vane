# Module vane-bedtime

Skips the night once a configurable fraction of the players in a world are sleeping, instead of
requiring every single one.

[org.oddlama.vane.bedtime.Bedtime] tracks sleeping players per world and compares the count against
the total number of non-spectator players in that world. Spectators are excluded deliberately, so
an AFK spectator cannot block the skip.

The module also publishes who is currently sleeping to whichever web map integrations are present.
`BedtimeDynmapLayer` and `BedtimeBlueMapLayer` are separate components, each of which quietly does
nothing when its map plugin is not installed.

# Package org.oddlama.vane.bedtime

Module entry point, the sleep tracking listener, and the Dynmap and BlueMap marker layers.
