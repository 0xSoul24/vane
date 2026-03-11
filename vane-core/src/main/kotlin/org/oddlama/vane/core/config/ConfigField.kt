package org.oddlama.vane.core.config

import org.apache.commons.text.WordUtils
import org.bstats.bukkit.Metrics
import org.bstats.charts.SimplePie
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.functional.Consumer2
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import kotlin.math.max
import kotlin.math.min

@Suppress("UNCHECKED_CAST")
abstract class ConfigField<T>(
    protected var owner: Any?,
    protected var field: Field,
    mapName: (String?) -> String?,
    typeName: String?,
    description: String?
) : Comparable<ConfigField<*>> {
    protected var path: String = mapName(field.name.substring("config".length))!!
    protected var typeName: String?
    protected var sortPriority: Int = 0

    private val yamlPathComponents: Array<String?> = path.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    private val yamlGroupPath: String?
    private val basename: String?
    private val description: () -> String?

    init {
        val lastDot = path.lastIndexOf(".")
        this.yamlGroupPath = if (lastDot == -1) "" else path.substring(0, lastDot)
        this.basename = yamlPathComponents[yamlPathComponents.size - 1]
        this.typeName = typeName

        // lang, enabled, metrics_enabled should be at the top
        when (this.path) {
            "Lang"           -> this.sortPriority = -10
            "Enabled"        -> this.sortPriority = -9
            "MetricsEnabled" -> this.sortPriority = -8
        }

        field.isAccessible = true

        // Dynamic description — allows subclasses to override via ${fieldName}Desc()
        this.description = {
            try {
                @Suppress("UNCHECKED_CAST")
                owner!!.javaClass.getMethod("${field.name}Desc").invoke(owner) as String?
            } catch (_: NoSuchMethodException) {
                description
            } catch (e: InvocationTargetException) {
                throw RuntimeException("Could not call ${owner!!.javaClass.name}.${field.name}Desc() to override description value", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Could not call ${owner!!.javaClass.name}.${field.name}Desc() to override description value", e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    protected fun overriddenDef(): T? =
        try {
            owner!!.javaClass.getMethod("${field.name}Def").invoke(owner) as T?
        } catch (_: NoSuchMethodException) {
            null
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Could not call ${owner!!.javaClass.name}.${field.name}Def() to override default value", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Could not call ${owner!!.javaClass.name}.${field.name}Def() to override default value", e)
        }

    @Suppress("UNCHECKED_CAST")
    protected fun overriddenMetrics(): Boolean? =
        try {
            owner!!.javaClass.getMethod("${field.name}Metrics").invoke(owner) as Boolean?
        } catch (_: NoSuchMethodException) {
            null
        } catch (e: InvocationTargetException) {
            throw RuntimeException("Could not call ${owner!!.javaClass.name}.${field.name}Metrics() to override metrics status", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Could not call ${owner!!.javaClass.name}.${field.name}Metrics() to override metrics status", e)
        }

    protected fun escapeYaml(s: String?): String =
        (s ?: "").replace("\\", "\\\\").replace("\"", "\\\"")

    fun getYamlGroupPath(): String = path
    fun yamlPath(): String = path
    fun yamlGroupPath(): String? = yamlGroupPath
    fun basename(): String? = basename

    private fun modifyYamlPathForSorting(path: String): String =
        if (path.endsWith(".enabled")) path.substring(0, path.lastIndexOf(".enabled"))
        else path

    override fun compareTo(other: ConfigField<*>): Int {
        if (sortPriority != other.sortPriority) return sortPriority - other.sortPriority
        for (i in 0..<min(yamlPathComponents.size, other.yamlPathComponents.size) - 1) {
            val c = yamlPathComponents[i]!!.compareTo(other.yamlPathComponents[i]!!)
            if (c != 0) return c
        }
        return modifyYamlPathForSorting(yamlPath()).compareTo(modifyYamlPathForSorting(other.yamlPath()))
    }

    protected fun appendDescription(builder: StringBuilder, indent: String) {
        builder.append("$indent# ${WordUtils.wrap(description(), max(60, 80 - indent.length), "\n$indent# ", false)}\n")
    }

    protected fun <U> appendListDefinition(
        builder: StringBuilder?,
        indent: String?,
        prefix: String?,
        list: MutableCollection<out U?>,
        append: Consumer2<StringBuilder?, U?>
    ) {
        if (builder == null) return
        list.forEach { i ->
            builder.append(indent)
            builder.append(prefix)
            builder.append("  - ")
            append.apply(builder, i)
            builder.append("\n")
        }
    }

    protected fun <U> appendValueRange(
        builder: StringBuilder,
        indent: String?,
        min: U?,
        max: U?,
        invalidMin: U?,
        invalidMax: U?
    ) {
        builder.append("$indent# Valid values: ")
        builder.append(when {
            min != invalidMin && max != invalidMax -> "[$min,$max]"
            min != invalidMin                     -> "[$min,)"
            max != invalidMax                     -> "(,$max]"
            else                                  -> "Any $typeName"
        })
        builder.append("\n")
    }

    protected fun appendDefaultValue(builder: StringBuilder, indent: String?, def: Any?) {
        builder.append("$indent# Default: $def\n")
    }

    protected fun appendFieldDefinition(builder: StringBuilder, indent: String?, def: Any?) {
        builder.append("$indent$basename: $def\n")
    }

    @Throws(YamlLoadException::class)
    protected fun checkYamlPath(yaml: YamlConfiguration) {
        if (!yaml.contains(path, true)) throw YamlLoadException("yaml is missing entry with path '$path'")
    }

    protected fun validateDoubleRange(yamlPath: String, value: Double, min: Double, max: Double) {
        if (!min.isNaN() && value < min) throw YamlLoadException("Configuration '$yamlPath' has an invalid value: Value must be >= $min")
        if (!max.isNaN() && value > max) throw YamlLoadException("Configuration '$yamlPath' has an invalid value: Value must be <= $max")
    }

    protected fun validateIntRange(yamlPath: String, value: Int, min: Int, max: Int) {
        if (min != Int.MIN_VALUE && value < min) throw YamlLoadException("Configuration '$yamlPath' has an invalid value: Value must be >= $min")
        if (max != Int.MAX_VALUE && value > max) throw YamlLoadException("Configuration '$yamlPath' has an invalid value: Value must be <= $max")
    }

    protected fun requireNamespacedKeyParts(yamlPath: String, str: String?, kind: String): Pair<String, String> {
        if (str == null) throw YamlLoadException("Invalid type for yaml path '$yamlPath', expected string")
        val parts = str.split(":").filter { it.isNotEmpty() }
        if (parts.size != 2) throw YamlLoadException("Invalid $kind entry in list '$yamlPath': '$str' is not a valid namespaced key")
        return Pair(parts[0], parts[1])
    }

    abstract fun def(): T?

    open fun metrics(): Boolean = false

    abstract fun generateYaml(builder: StringBuilder, indent: String, existingCompatibleConfig: YamlConfiguration?)

    @Throws(YamlLoadException::class)
    abstract fun checkLoadable(yaml: YamlConfiguration)

    abstract fun load(yaml: YamlConfiguration)

    fun get(): T? =
        try {
            field.get(owner) as T?
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Invalid field access on '${field.name}'. This is a bug.", e)
        }

    open fun registerMetrics(metrics: Metrics?) {
        if (metrics == null || !metrics()) return
        metrics.addCustomChart(SimplePie(yamlPath()) { get().toString() })
    }

    fun components(): Array<String?> = yamlPathComponents
    fun groupCount(): Int = yamlPathComponents.size - 1

    companion object {
        fun sameGroup(a: ConfigField<*>?, b: ConfigField<*>?): Boolean {
            if (a == null || b == null) return false
            if (a.yamlPathComponents.size != b.yamlPathComponents.size) return false
            for (i in 0..<a.yamlPathComponents.size - 1) {
                if (a.yamlPathComponents[i] != b.yamlPathComponents[i]) return false
            }
            return true
        }

        fun commonGroupCount(a: ConfigField<*>?, b: ConfigField<*>?): Int {
            if (a == null || b == null) return 0
            var i = 0
            while (i < min(a.yamlPathComponents.size, b.yamlPathComponents.size) - 1) {
                if (a.yamlPathComponents[i] != b.yamlPathComponents[i]) return i
                ++i
            }
            return i
        }
    }
}