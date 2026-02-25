package org.oddlama.vane.core.persistent

import org.json.JSONObject
import org.oddlama.vane.annotation.persistent.Persistent
import org.oddlama.vane.core.module.Module
import org.reflections.ReflectionUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.function.Consumer
import java.util.function.Function
import java.util.logging.Level
import kotlin.Annotation
import kotlin.Any
import kotlin.Boolean
import kotlin.RuntimeException
import kotlin.String
import kotlin.checkNotNull

class PersistentStorageManager(var module: Module<*>) {
    class Migration(var to: Long, var name: String?, var migrator: Consumer<JSONObject?>)

    private val persistentFields: MutableList<PersistentField> = ArrayList()
    private val migrations: MutableList<Migration?> = ArrayList<Migration?>()
    var isLoaded: Boolean = false

    init {
        compile(module) { s: String? -> s }
    }

    private fun hasPersistentAnnotation(field: Field): Boolean {
        for (a in field.annotations) {
            // Tratar la anotación como kotlin.Annotation y usar annotationClass.java
            val ann = a
            if (ann.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.persistent.Persistent")) {
                return true
            }
         }
         return false
     }

     private fun assertFieldPrefix(field: Field) {
         if (!field.name.startsWith("storage")) {
             throw RuntimeException("Configuration fields must be prefixed storage. This is a bug.")
         }
     }

     private fun compileField(owner: Any?, field: Field, mapName: Function<String?, String?>): PersistentField {
         assertFieldPrefix(field)

         // Get the annotation
        var annotation: Annotation? = null
        for (a in field.annotations) {
            if (a.annotationClass.java.name.startsWith("org.oddlama.vane.annotation.persistent.Persistent")) {
                if (annotation == null) {
                    annotation = a
                } else {
                    throw RuntimeException("Persistent fields must have exactly one @Persistent annotation.")
                }
            }
        }
        val annNotNull = checkNotNull(annotation)
        val atype: Class<out Annotation> = annNotNull.annotationClass.java

         // Return a correct wrapper object
         if (atype == Persistent::class.java) {
             return PersistentField(owner, field, mapName)
         } else {
             throw RuntimeException("Missing PersistentField handler for @" + atype.getName() + ". This is a bug.")
         }
     }

     fun compile(owner: Any, mapName: Function<String?, String?>) {
         // Compile all annotated fields
         persistentFields.addAll(
             ReflectionUtils.getAllFields(owner.javaClass)
                 .stream()
                 .filter { field: Field? -> this.hasPersistentAnnotation(field!!) }
                 .map { f: Field? -> compileField(owner, f!!, mapName) }
                 .toList()
         )
     }

     fun addMigrationTo(to: Long, name: String?, migrator: Consumer<JSONObject?>) {
         migrations.add(Migration(to, name, migrator))
     }

     fun load(file: File): Boolean {
         if (!file.exists() && isLoaded) {
             module.log.severe("Cannot reload persistent storage from nonexistent file '" + file.getName() + "'")
             return false
         }

         // Reset loaded status
         isLoaded = false
         val json: JSONObject = if (file.exists()) {
             // Open file and read JSON
             try {
                 JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8))
             } catch (e: IOException) {
                 module.log.severe("error while loading persistent data from '" + file.getName() + "':")
                 module.log.severe(e.message)
                 return false
             }
         } else {
             JSONObject()
         }

         // Check version and migrate if necessary
         val versionPath = module.storagePathOf("storageVersion")
         val version = json.optString(versionPath, "0").toLong()
         val neededVersion = module.annotation.storageVersion
         if (version != neededVersion && migrations.isNotEmpty()) {
             module.log.info("Persistent storage is out of date.")
             module.log.info("§dMigrating storage from version §b$version → $neededVersion§d:")

             // Sort migrations by target version,
             // then apply new migrations in order.
             migrations
                 .stream()
                 .filter { m: Migration? -> m!!.to >= version }
                 .sorted { a: Migration?, b: Migration? -> a!!.to.compareTo(b!!.to) }
                 .forEach { m: Migration? ->
                     module.log.info("  → §b" + m!!.to + "§r : Applying migration '§a" + m.name + "§r'")
                     m.migrator.accept(json)
                 }
         }

         // Overwrite new version
         json.put(versionPath, neededVersion.toString())

         try {
             for (f in persistentFields) {
                 // If we have just initialized a new JSON object, we only load values that
                 // have defined keys (e.g., from initialization migrations)
                 if (version == 0L && !json.has(f.path())) {
                     continue
                 }

                 f.load(json)
             }
         } catch (e: IOException) {
             module.log.log(Level.SEVERE, "error while loading persistent variables from '" + file.getName() + "'", e)
             return false
         }

         isLoaded = true
         return true
     }

     fun save(file: File) {
         if (!isLoaded) {
             // Don't save if never loaded or a previous load was faulty.
             return
         }

         // Create JSON with whole content
         val json = JSONObject()

         // Save version
         val versionPath = module.storagePathOf("storageVersion")
         json.put(versionPath, module.annotation.storageVersion.toString())

         // Save fields
         for (f in persistentFields) {
             try {
                 f.save(json)
             } catch (e: IOException) {
                 module.log.log(Level.SEVERE, "error while serializing persistent data!", e)
             }
         }

         // Save to tmp file, then move atomically to prevent corruption.
         val tmpFile = File(file.absolutePath + ".tmp")
         try {
             Files.writeString(tmpFile.toPath(), json.toString())
         } catch (e: IOException) {
             module.log.log(Level.SEVERE, "error while saving persistent data to temporary file!", e)
             return
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
         }
     }
 }
