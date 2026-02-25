package org.oddlama.vane.core.module

import org.oddlama.vane.core.functional.Consumer1
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import java.io.IOException

/**
 * A ModuleContext is an association to a specific Module and also a grouping of config and language
 * variables with a common namespace.
 */
open class ModuleContext<T : Module<T?>?> @JvmOverloads constructor(
    override var context: Context<T?>,
    protected var name: String?,
    private val description: String?,
    private val separator: String?,
    compileSelf: Boolean = true
) : Context<T?> {
    override var module: T? = null // cache to not generate chains of getContext()
        protected set
    private val subcontexts: MutableList<Context<T?>> = ArrayList<Context<T?>>()
    private val components: MutableList<ModuleComponent<T?>> = ArrayList<ModuleComponent<T?>>()

    // Guard to avoid compiling the same context multiple times
    private var compiled: Boolean = false

    init {
        this.module = context.module

        if (compileSelf) {
            compileSelf()
        }
    }

    override fun yamlPath(): String? {
        return Context.appendYamlPath(context.yamlPath()!!, name, separator)
    }

    override fun variableYamlPath(variable: String?): String? {
        return Context.appendYamlPath(yamlPath()!!, variable, separator)
    }

    override fun enabled(): Boolean {
        return context.enabled()
    }

    private fun compileComponent(component: Any) {
        module!!.langManager.compile(component) { variable: String? -> this.variableYamlPath(variable) ?: "" }
        module!!.configManager.compile(component) { variable: String? -> this.variableYamlPath(variable) ?: "" }
        if (description != null) {
            module!!.configManager.addSectionDescription(yamlPath(), description)
        }
        module!!.persistentStorageManager.compile(
            component
        ) { variable: String? -> this.variableYamlPath(variable) ?: "" }
    }

    // Changed visibility to internal so Module can safely trigger compilation after initialization
    internal fun compileSelf() {
        // Prevent reentrant / duplicate compilation
        if (compiled) return
        compiled = true

        // Compile localization and config fields
        compileComponent(this)

        // Try to add this compiled context as child of the parent context.
        // Some parent contexts (e.g. Module) may forward addChild to the same ModuleGroup,
        // which would create a self-reference; that must be handled by the parent.
        try {
            context.addChild(this)
        } catch (e: Exception) {
            // Swallow exceptions here to avoid breaking initialization; the parent
            // should manage subcontext relationships correctly. Log if necessary.
        }
    }

    override fun compile(component: ModuleComponent<T?>?) {
        components.add(component!!)
        compileComponent(component)
    }

    override fun addChild(subcontext: Context<T?>?) {
        subcontexts.add(subcontext!!)
    }

    override fun enable() {
        onModuleEnable()
        for (component in components) {
            component.dispatchEnable()
        }
        for (subcontext in subcontexts) {
            subcontext.enable()
        }
    }

    override fun disable() {
        for (i in subcontexts.indices.reversed()) {
            subcontexts[i].disable()
        }
        for (i in components.indices.reversed()) {
            components[i].dispatchDisable()
        }
        onModuleDisable()
    }

    override fun configChange() {
        onConfigChange()
        for (component in components) {
            component.dispatchConfigChange()
        }
        for (subcontext in subcontexts) {
            subcontext.configChange()
        }
    }

    @Throws(IOException::class)
    override fun generateResourcePack(pack: ResourcePackGenerator?) {
        onGenerateResourcePack(pack)
        for (component in components) {
            component.onGenerateResourcePack(pack)
        }
        for (subcontext in subcontexts) {
            subcontext.generateResourcePack(pack)
        }
    }

    override fun forEachModuleComponent(f: Consumer1<ModuleComponent<*>?>?) {
        for (component in components) {
            f!!.apply(component)
        }
        for (subcontext in subcontexts) {
            subcontext.forEachModuleComponent(f)
        }
    }

    override fun onModuleEnable() {}

    override fun onModuleDisable() {}

    override fun onConfigChange() {}

    @Throws(IOException::class)
    override fun onGenerateResourcePack(pack: ResourcePackGenerator?) {}
}
