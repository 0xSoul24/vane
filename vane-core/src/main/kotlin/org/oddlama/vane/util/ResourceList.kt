package org.oddlama.vane.util

import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * from: forums.devx.com/showthread.php?t=153784 list resources available from the jar file of the
 * given class
 */
object ResourceList {
    /**
     * For all elements of java.class.path get a Collection of resource Pattern =
     * Pattern.compile(".*"); gets all resources
     * 
     * @param pattern the pattern to match
     * @return the resources in the order they are found
     */
    @JvmStatic
    fun getResources(clazz: Class<*>, pattern: Pattern): MutableCollection<String?> {
        val jarUrl = clazz.getProtectionDomain().codeSource.location
        return try {
            getResources(URI(jarUrl.toString()).getPath(), pattern)
        } catch (e: URISyntaxException) {
            ArrayList()
        }
    }

    private fun getResources(path: String, pattern: Pattern): MutableCollection<String?> {
        val retval = ArrayList<String?>()
        val file = File(path)
        if (file.isDirectory()) {
            retval.addAll(getResourcesFromDirectory(file, pattern))
        } else {
            retval.addAll(getResourcesFromJarFile(file, pattern))
        }
        return retval
    }

    private fun getResourcesFromJarFile(file: File, pattern: Pattern): MutableCollection<String?> {
        val retval = ArrayList<String?>()
        val zf: ZipFile?
        try {
            zf = ZipFile(file)
        } catch (e: IOException) {
            throw Error(e)
        }
        val e = zf.entries()
        while (e.hasMoreElements()) {
            val ze: ZipEntry = e.nextElement()
            val fileName = ze.getName()
            val accept = pattern.matcher(fileName).matches()
            if (accept) {
                retval.add(fileName)
            }
        }
        try {
            zf.close()
        } catch (e1: IOException) {
            throw Error(e1)
        }
        return retval
    }

    private fun getResourcesFromDirectory(directory: File, pattern: Pattern): MutableCollection<String?> {
        val retval = ArrayList<String?>()
        val fileList = directory.listFiles()
        for (file in fileList!!) {
            if (file!!.isDirectory()) {
                retval.addAll(getResourcesFromDirectory(file, pattern))
            } else {
                try {
                    val fileName = file.getCanonicalPath()
                    val accept = pattern.matcher(fileName).matches()
                    if (accept) {
                        retval.add(fileName)
                    }
                } catch (e: IOException) {
                    throw Error(e)
                }
            }
        }
        return retval
    }
}
