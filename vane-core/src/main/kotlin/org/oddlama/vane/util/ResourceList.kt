package org.oddlama.vane.util

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern
import java.util.zip.ZipFile

/**
 * List resources available from the jar file of the given class.
 * from: forums.devx.com/showthread.php?t=153784
 */
object ResourceList {
    /**
     * For all elements of java.class.path get a Collection of resources matching [pattern].
     *
     * @param pattern the pattern to match; use `Pattern.compile(".*")` to get all resources
     * @return the resources in the order they are found
     */
    @JvmStatic
    fun getResources(clazz: Class<*>, pattern: Pattern): List<String> {
        val jarUrl = clazz.protectionDomain.codeSource.location
        return try {
            getResources(URI(jarUrl.toString()).path, pattern)
        } catch (_: URISyntaxException) {
            emptyList()
        }
    }

    private fun getResources(path: String, pattern: Pattern): List<String> {
        val file = java.io.File(path)
        return when {
            file.isDirectory -> getResourcesFromDirectory(file, pattern)
            else -> getResourcesFromJarFile(file, pattern)
        }
    }

    private fun getResourcesFromJarFile(file: java.io.File, pattern: Pattern): List<String> {
        return try {
            ZipFile(file).use { zf ->
                zf.entries().asSequence()
                    .map { it.name }
                    .filter { pattern.matcher(it).matches() }
                    .toList()
            }
        } catch (e: IOException) {
            throw Error(e)
        }
    }

    private fun getResourcesFromDirectory(directory: java.io.File, pattern: Pattern): List<String> {
        return buildList {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isDirectory -> addAll(getResourcesFromDirectory(file, pattern))
                    else -> try {
                        val fileName = file.canonicalPath
                        if (pattern.matcher(fileName).matches()) add(fileName)
                    } catch (e: IOException) {
                        throw Error(e)
                    }
                }
            }
        }
    }
}
