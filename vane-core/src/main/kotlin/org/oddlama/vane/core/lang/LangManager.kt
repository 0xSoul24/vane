package org.oddlama.vane.core.lang

import org.bukkit.configuration.file.YamlConfiguration
import org.oddlama.vane.annotation.lang.LangMessage
import org.oddlama.vane.annotation.lang.LangMessageArray
import org.oddlama.vane.annotation.lang.LangVersion
import org.oddlama.vane.core.YamlLoadException
import org.oddlama.vane.core.module.Module
import org.oddlama.vane.core.resourcepack.ResourcePackGenerator
import org.reflections.ReflectionUtils
import java.io.File
import java.lang.reflect.Field
import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import java.util.logging.Level
import java.util.stream.Collectors

class LangManager(var module: Module<*>) {
    private val langFields: MutableList<LangField<*>> = ArrayList<LangField<*>>()
    var fieldVersion: LangVersionField? = null

    init {
        compile(module) { s: String? -> s ?: "" }
    }

    fun expectedVersion(): Long {
        return module.annotation.langVersion
    }

    private fun hasLangAnnotation(field: Field): Boolean {
        for (a in field.annotations) {
            // Tratar la anotación como kotlin.Annotation y usar annotationClass.java
            val ann = a
            if (ann.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.lang.Lang")) {
                return true
            }
        }
        return false
    }

    private fun assertFieldPrefix(field: Field) {
        if (!field.name.startsWith("lang")) {
            throw RuntimeException("Language fields must be prefixed lang. This is a bug.")
        }
    }

    private fun compileField(owner: Any?, field: Field, mapName: Function<String?, String>): LangField<*> {
        assertFieldPrefix(field)

        // Get the annotation
        var annotation: Annotation? = null
        for (a in field.annotations) {
            if (a.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.lang.Lang")) {
                if (annotation == null) {
                    annotation = a
                } else {
                    throw RuntimeException("Language fields must have exactly one @Lang annotation.")
                }
            }
        }
        val annNotNull = checkNotNull(annotation)
        // Return a correct wrapper object
        when (val atype: Class<out Annotation> = annNotNull.annotationClass.java) {
            LangMessage::class.java -> {
                return LangMessageField(module, owner, field, mapName, annotation as LangMessage)
            }
            LangMessageArray::class.java -> {
                return LangMessageArrayField(module, owner, field, mapName, annotation as LangMessageArray)
            }
            LangVersion::class.java -> {
                if (owner !== module) {
                    throw RuntimeException("@LangVersion can only be used inside the main module. This is a bug.")
                }
                if (fieldVersion != null) {
                    throw RuntimeException(
                        "There must be exactly one @LangVersion field! (found multiple). This is a bug."
                    )
                }
                return LangVersionField(module, owner, field, mapName, annotation as LangVersion).also { fieldVersion = it }
            }
            else -> {
                throw RuntimeException("Missing LangField handler for @" + atype.name + ". This is a bug.")
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
                module.log.severe("This language file is for an older version of " + module.name + ".")
                module.log.severe("Please update your file or use an officially supported language file.")
            } else {
                module.log.severe("This language file is for a future version of " + module.name + ".")
                module.log.severe("Please use the correct file for this version, or use an officially")
                module.log.severe("supported language file.")
            }

            return false
        }

        return true
    }

    fun compile(owner: Any, mapName: Function<String?, String>) {
        // Compile all annotated fields
        langFields.addAll(
            ReflectionUtils.getAllFields(owner.javaClass)
                .stream()
                .filter { field: Field? -> this.hasLangAnnotation(field!!) }
                .map { f: Field? -> compileField(owner, f!!, mapName) }
                .toList()
        )

        if (owner === module && fieldVersion == null) {
            throw RuntimeException("There must be exactly one @LangVersion field! (found none). This is a bug.")
        }
    }

    fun getField(name: String?): LangField<*>? {
        return langFields
            .stream()
            .filter { f: LangField<*>? -> f!!.name == name }
            .findFirst()
            .orElseThrow(Supplier { RuntimeException("Missing lang field lang$name") })
    }

    fun reload(file: File): Boolean {
        // Load file
        val yaml = YamlConfiguration.loadConfiguration(file)

        // Check version
        val version = yaml.getLong("Version", -1)
        if (!verifyVersion(file, version)) {
            return false
        }

        try {
            // Check languration for errors
            for (f in langFields) {
                f.checkLoadable(yaml)
            }

            for (f in langFields) {
                f.load(module.namespace(), yaml)
            }
        } catch (e: YamlLoadException) {
            module.log.log(Level.SEVERE, "error while loading '" + file.absolutePath + "'", e)
            return false
        }
        return true
    }

    fun generateResourcePack(pack: ResourcePackGenerator?, yaml: YamlConfiguration, langFile: File) {
        val langCode =
            yaml.getString("ResourcePackLangCode") ?: throw RuntimeException("Missing yaml key: ResourcePackLangCode")
        val errors = LinkedList<YamlLoadException.Lang?>()
        for (f in langFields) {
            try {
                f.addTranslations(pack, yaml, langCode)
            } catch (e: YamlLoadException.Lang) {
                errors.add(e)
            } catch (e: YamlLoadException) {
                module.log.log(Level.SEVERE, "Unexpected YAMLLoadException: ", e)
            }
        }
        if (errors.isNotEmpty()) {
            val erroredLangNodes = errors
                .stream()
                .map { obj: YamlLoadException.Lang? -> obj!!.message }
                .collect(Collectors.joining("\n\t\t"))
            module.log.log(
                Level.SEVERE,
                "The following errors were identified while adding translations from \n\t" +
                        langFile.absolutePath +
                        " \n\t\t" +
                        erroredLangNodes
            )
        }
    }
}
