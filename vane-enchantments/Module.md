# Module vane-enchantments

Custom enchantments for vanilla tools, armour and the elytra, together with the ancient tomes used
to apply them.

Each enchantment is declared with `@VaneEnchantment` from `vane-annotations` and extends
`CustomEnchantment` from `vane-core`, which is what `VaneEnchantmentProcessor` enforces at compile
time. The annotation supplies the level cap, rarity and the flags controlling whether the
enchantment can be traded, treasured or generated into loot.

Enchantments come in pairs: a behaviour class in `enchantments` holding the listeners and effects,
and a matching class in `enchantments.registry` describing how the enchantment attaches to items.
Adding an enchantment means adding both halves and instantiating it in
[org.oddlama.vane.enchantments.Enchantments]'s `init` block.

# Package org.oddlama.vane.enchantments

Module entry point, the bootstrapper that hooks the registry into the server's startup, and the
custom enchantment registry.

# Package org.oddlama.vane.enchantments.enchantments

The enchantments themselves — Angel, Grappling Hook, Hell Bent, Leafchopper, Lightning, Rake,
Seeding, Soulbound, Take Off, Unbreakable and Wings — plus the shared lookup helper used to find an
enchanted item on a player.

# Package org.oddlama.vane.enchantments.enchantments.registry

Per-enchantment registry definitions describing which items each enchantment applies to and how it
is offered.

# Package org.oddlama.vane.enchantments.items

Ancient tomes, the custom items through which players apply these enchantments.
