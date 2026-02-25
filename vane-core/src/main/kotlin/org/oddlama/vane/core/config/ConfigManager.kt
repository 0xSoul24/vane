package org.oddlama.vane.core.config

import org.apache.commons.text.WordUtils
import org.bstats.bukkit.Metrics
import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.config.*
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Context
import org.oddlama.vane.core.module.Module
import org.reflections.ReflectionUtils
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.function.Function
import java.util.logging.Level
import kotlin.math.max

class ConfigManager(var module: Module<*>) {
    private val configFields: MutableList<ConfigField<*>> = ArrayList<ConfigField<*>>()
    private val sectionDescriptions: MutableMap<String?, String?> = HashMap<String?, String?>()
    var fieldVersion: ConfigVersionField? = null

    init {
        compile(module) { s: String? -> s }
    }

    fun expectedVersion(): Long {
        return module.annotation.configVersion
    }

    private fun hasConfigAnnotation(field: Field): Boolean {
        for (a in field.annotations) {
            // field.annotations may contain java annotations; treat as kotlin.Annotation
            val ann = a
            if (ann.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.config.Config")) {
                return true
            }
        }
        return false
    }

    private fun assertFieldPrefix(field: Field) {
        if (!field.name.startsWith("config")) {
            throw RuntimeException("Configuration fields must be prefixed config. This is a bug.")
        }
    }

    private fun compileField(owner: Any?, field: Field, mapName: Function<String?, String?>): ConfigField<*> {
        assertFieldPrefix(field)

        // Get the annotation
        var annotation: Annotation? = null
        for (a in field.annotations) {
            // Treat annotation as kotlin.Annotation to access annotationClass
            val ann = a
            if (ann.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.config.Config")) {
                if (annotation == null) {
                    annotation = ann
                } else {
                    throw RuntimeException("Configuration fields must have exactly one @Config annotation.")
                }
            }
        }
        val annNotNull = checkNotNull(annotation)
        // Return a correct wrapper object
        when (val atype: Class<out Annotation> = annNotNull.annotationClass.java) {
            ConfigBoolean::class.java -> {
                return ConfigBooleanField(owner, field, mapName, annotation as ConfigBoolean)
            }
            ConfigDict::class.java -> {
                return ConfigDictField(owner, field, mapName, annotation as ConfigDict)
            }
            ConfigDouble::class.java -> {
                return ConfigDoubleField(owner, field, mapName, annotation as ConfigDouble)
            }
            ConfigDoubleList::class.java -> {
                return ConfigDoubleListField(owner, field, mapName, annotation as ConfigDoubleList)
            }
            ConfigExtendedMaterial::class.java -> {
                return ConfigExtendedMaterialField(owner, field, mapName, annotation as ConfigExtendedMaterial)
            }
            ConfigInt::class.java -> {
                return ConfigIntField(owner, field, mapName, annotation as ConfigInt)
            }
            ConfigIntList::class.java -> {
                return ConfigIntListField(owner, field, mapName, annotation as ConfigIntList)
            }
            ConfigItemStack::class.java -> {
                return ConfigItemStackField(owner, field, mapName, annotation as ConfigItemStack)
            }
            ConfigLong::class.java -> {
                return ConfigLongField(owner, field, mapName, annotation as ConfigLong)
            }
            ConfigMaterial::class.java -> {
                return ConfigMaterialField(owner, field, mapName, annotation as ConfigMaterial)
            }
            ConfigMaterialMapMapMap::class.java -> {
                return ConfigMaterialMapMapMapField(owner, field, mapName, annotation as ConfigMaterialMapMapMap)
            }
            ConfigMaterialSet::class.java -> {
                return ConfigMaterialSetField(owner, field, mapName, annotation as ConfigMaterialSet)
            }
            ConfigString::class.java -> {
                return ConfigStringField(owner, field, mapName, annotation as ConfigString)
            }
            ConfigStringList::class.java -> {
                return ConfigStringListField(owner, field, mapName, annotation as ConfigStringList)
            }
            ConfigStringListMap::class.java -> {
                return ConfigStringListMapField(owner, field, mapName, annotation as ConfigStringListMap)
            }
            ConfigVersion::class.java -> {
                if (owner !== module) {
                    throw RuntimeException("@ConfigVersion can only be used inside the main module. This is a bug.")
                }
                if (fieldVersion != null) {
                    throw RuntimeException(
                        "There must be exactly one @ConfigVersion field! (found multiple). This is a bug."
                    )
                }
                return ConfigVersionField(owner, field, mapName, annotation as ConfigVersion).also { fieldVersion = it }
            }
            else -> {
                throw RuntimeException("Missing ConfigField handler for @" + atype.name + ". This is a bug.")
            }
        }
    }

    private fun verifyVersion(file: File, version: Long): Boolean {
        if (version != expectedVersion()) {
            module.log.severe(file.getName() + ": expected version " + expectedVersion() + ", but got " + version)

            if (version == 0L) {
                module.log.severe("Something went wrong while generating or loading the configuration.")
                module.log.severe("If you are sure your configuration is correct and this isn't a file")
                module.log.severe(
                    "system permission issue, please report this to https://github.com/oddlama/vane/issues"
                )
            } else if (version < expectedVersion()) {
                module.log.severe("This config is for an older version of " + module.name + ".")
                module.log.severe("Please update your configuration. A new default configuration")
                module.log.severe("has been generated as 'config.yml.new'. Alternatively you can")
                module.log.severe("delete your configuration to have a new one generated next time.")

                generateFile(File(module.dataFolder, "config.yml.new"), null)
            } else {
                module.log.severe("This config is for a future version of " + module.name + ".")
                module.log.severe("Please use the correct file for this version, or delete it and")
                module.log.severe("it will be regenerated next time the server is started.")
            }

            return false
        }

        return true
    }

    fun addSectionDescription(yamlPath: String?, description: String?) {
        sectionDescriptions[yamlPath] = description
    }

    fun compile(owner: Any, mapName: Function<String?, String?>) {
        // Compile all annotated fields
        configFields.addAll(
            ReflectionUtils.getAllFields(owner.javaClass)
                .stream()
                .filter { field: Field? -> this.hasConfigAnnotation(field!!) }
                .map { f: Field? -> compileField(owner, f!!, mapName) }
                .toList()
        )

        // Sort fields alphabetically, and by precedence (e.g., put a version last and lang first)
        configFields.sort()

        if (owner === module && fieldVersion == null) {
            throw RuntimeException("There must be exactly one @ConfigVersion field! (found none). This is a bug.")
        }
    }

    private fun indentStr(level: Int): String {
        return "  ".repeat(level)
    }

    fun generateYaml(builder: StringBuilder, existingCompatibleConfig: YamlConfiguration?) {
        builder.append("# vim: set tabstop=2 softtabstop=0 expandtab shiftwidth=2:\n")
        builder.append("# This config file will automatically be updated, as long\n")
        builder.append("# as there are no incompatible changes between versions.\n")
        builder.append("# This means that additional comments will not be preserved!\n")

        // Use the version field as a neutral field in the root group
        var lastField: ConfigField<*>? = fieldVersion
        var indent = ""

        for (f in configFields) {
            builder.append("\n")

            if (!ConfigField.sameGroup(lastField, f)) {
                val newIndentLevel = f.groupCount()
                val commonIndentLevel = ConfigField.commonGroupCount(lastField, f)

                // Build a full common section path
                var sectionPath: String? = ""
                for (i in 0..<commonIndentLevel) {
                    sectionPath = Context.appendYamlPath(sectionPath!!, f.components()[i], ".")
                }

                // For each unopened section
                for (i in commonIndentLevel..<newIndentLevel) {
                    indent = indentStr(i)

                    // Get a full section path
                    sectionPath = Context.appendYamlPath(sectionPath!!, f.components()[i], ".")

                    // Append section description, if given.
                    val sectionDesc = sectionDescriptions[sectionPath]
                    if (sectionDesc != null) {
                        val descriptionWrapped = WordUtils.wrap(
                            sectionDesc,
                            max(60, 80 - indent.length),
                            "\n$indent# ",
                            false
                        )
                        builder.append(indent)
                        builder.append("# ")
                        builder.append(descriptionWrapped)
                        builder.append("\n")
                    }

                    // Append section
                    val sectionName = f.components()[i]
                    builder.append(indent)
                    builder.append(sectionName)
                    builder.append(":\n")
                }

                indent = indentStr(newIndentLevel)
            }

            // Append field YAML
            f.generateYaml(builder, indent, existingCompatibleConfig)
            lastField = f
        }
    }

    fun standardFile(): File {
        return File(module.dataFolder, "config.yml")
    }

    fun generateFile(file: File, existingCompatibleConfig: YamlConfiguration?): Boolean {
        val builder = StringBuilder()
        generateYaml(builder, existingCompatibleConfig)
        val content = builder.toString()

        // Save to tmp file, then move atomically to prevent corruption.
        val tmpFile = File(file.absolutePath + ".tmp")
        try {
            Files.writeString(tmpFile.toPath(), content)
        } catch (e: IOException) {
            module.log.log(Level.SEVERE, "error while writing config file '$file'", e)
            return false
        }

        // Move atomically to prevent corruption.
        try {
            Files.move(
                tmpFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: IOException) {
            module.log.log(
                Level.SEVERE,
                "error while atomically replacing '" +
                        file +
                        "' with temporary file (very recent changes might be lost)!",
                e
            )
            return false
        }

        return true
    }

    fun reload(file: File): Boolean {
        // Load file
        var yaml = YamlConfiguration.loadConfiguration(file)

        // Check version
        val version = yaml.getLong("Version", -1)
        if (!verifyVersion(file, version)) {
            return false
        }

        // Upgrade config to include all necessary keys (version-compatible extensions)
        val tmpFile = File(module.dataFolder, "config.yml.tmp")
        if (!generateFile(tmpFile, yaml)) {
            return false
        }

        // Move atomically to prevent corruption.
        try {
            Files.move(
                tmpFile.toPath(),
                standardFile().toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (e: IOException) {
            module.log.log(
                Level.SEVERE,
                "error while atomically replacing '" +
                        standardFile() +
                        "' with updated version. Please manually resolve the conflict (new file is named '" +
                        tmpFile +
                        "')",
                e
            )
            return false
        }

        // Load newly written file
        yaml = YamlConfiguration.loadConfiguration(file)

        try {
            // Check configuration for errors
            for (f in configFields) {
                f.checkLoadable(yaml)
            }

            for (f in configFields) {
                f.load(yaml)
            }
        } catch (e: YamlLoadException) {
            module.log.log(Level.SEVERE, "error while loading '" + file.getName() + "'", e)
            return false
        }

        return true
    }

    fun registerMetrics(metrics: Metrics?) {
        // Track config values. Fields automatically know whether they want to be tracked or not via
        // the annotation.
        // By default, annotations use sensible defaults, so e.g., no strings will be tracked
        // automatically, except
        // when explicitly requested (e.g., language).
        for (f in configFields) {
            f.registerMetrics(metrics)
        }
    }
}