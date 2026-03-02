package org.oddlama.vane.enchantments

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.registry.event.RegistryEvents
import org.oddlama.vane.enchantments.enchantments.registry.*

class EnchantmentsBootstrapper : PluginBootstrap {
    override fun bootstrap(context: BootstrapContext) {
        context
            .lifecycleManager
            .registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose()
                    .newHandler(LifecycleEventHandler { event ->
                        AngelRegistry(event)
                        GrapplingHookRegistry(event)
                        HellBentRegistry(event)
                        LeafchopperRegistry(event)
                        LightningRegistry(event)
                        RakeRegistry(event)
                        SeedingRegistry(event)
                        WingsRegistry(event)
                        SouldboundRegistry(event)
                        TakeOffRegistry(event)
                        UnbreakableRegistry(event)
                    })
            )
    }
}
