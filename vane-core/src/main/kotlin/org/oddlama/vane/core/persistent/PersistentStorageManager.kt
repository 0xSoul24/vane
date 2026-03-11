package org.oddlama.vane.core.persistent

import org.json.JSONObject
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.module.Module
import org.reflections.ReflectionUtils
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.function.Consumer
import java.util.logging.Level

class PersistentStorageManager(var module: Module<*>) {
    class Migration(val to: Long, val name: String?, val migrator: Consumer<JSONObject?>)

    private val persistentFields: MutableList<PersistentField> = mutableListOf()
    private val migrations: MutableList<Migration> = mutableListOf()
    var isLoaded: Boolean = false

    init {
        compile(module) { it }
    }

    private fun hasPersistentAnnotation(field: Field): Boolean =
        field.annotations.any {
            it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.persistent.Persistent")
        }

    private fun assertFieldPrefix(field: Field) {
        if (!field.name.startsWith("storage"))
            throw RuntimeException("Configuration fields must be prefixed storage. This is a bug.")
    }

    private fun compileField(owner: Any?, field: Field, mapName: (String?) -> String?): PersistentField {
        assertFieldPrefix(field)

        val annotation = field.annotations
            .filter { it.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.persistent.Persistent") }
            .also { require(it.size <= 1) { "Persistent fields must have exactly one @Persistent annotation." } }
            .firstOrNull() ?: error("No @Persistent annotation found on field ${field.name}")

        return when (val atype = annotation.annotationClass.java) {
            Persistent::class.java -> PersistentField(owner, field, mapName)
            else -> throw RuntimeException("Missing PersistentField handler for @${atype.name}. This is a bug.")
        }
    }

    fun compile(owner: Any, mapName: (String?) -> String?) {
        persistentFields.addAll(
            ReflectionUtils.getAllFields(owner.javaClass)
                .filter { hasPersistentAnnotation(it) }
                .map { compileField(owner, it, mapName) }
        )
    }

    fun addMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
        migrations.add(Migration(to, name, migrator))
    }

    fun load(file: File): Boolean {
        if (!file.exists() && isLoaded) {
            module.log.severe("Cannot reload persistent storage from nonexistent file '${file.name}'")
            return false
        }

        isLoaded = false
        val json: JSONObject = if (file.exists()) {
            try {
                JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8))
            } catch (e: IOException) {
                module.log.severe("error while loading persistent data from '${file.name}':")
                module.log.severe(e.message)
                return false
            }
        } else {
            JSONObject()
        }

        val versionPath = module.storagePathOf("storageVersion")
        val version = json.optString(versionPath, "0").toLong()
        val neededVersion = module.annotation.storageVersion
        if (version != neededVersion && migrations.isNotEmpty()) {
            module.log.info("Persistent storage is out of date.")
            module.log.info("§dMigrating storage from version §b$version → $neededVersion§d:")

            migrations
                .filter { it.to >= version }
                .sortedBy { it.to }
                .forEach { m ->
                    module.log.info("  → §b${m.to}§r : Applying migration '§a${m.name}§r'")
                    m.migrator.accept(json)
                }
        }

        json.put(versionPath, neededVersion.toString())

        try {
            persistentFields.forEach { f ->
                if (version == 0L && !json.has(f.path())) return@forEach
                f.load(json)
            }
        } catch (e: IOException) {
            module.log.log(Level.SEVERE, "error while loading persistent variables from '${file.name}'", e)
            return false
        }

        isLoaded = true
        return true
    }

    fun save(file: File) {
        if (!isLoaded) return

        val json = JSONObject()
        val versionPath = module.storagePathOf("storageVersion")
        json.put(versionPath, module.annotation.storageVersion.toString())

        persistentFields.forEach { f ->
            try {
                f.save(json)
            } catch (e: IOException) {
                module.log.log(Level.SEVERE, "error while serializing persistent data!", e)
            }
        }

        val tmpFile = File("${file.absolutePath}.tmp")
        try {
            Files.writeString(tmpFile.toPath(), json.toString())
        } catch (e: IOException) {
            module.log.log(Level.SEVERE, "error while saving persistent data to temporary file!", e)
            return
        }

        try {
            Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            module.log.log(Level.SEVERE,
                "error while atomically replacing '$file' with temporary file (very recent changes might be lost)!", e)
        }
    }
}
