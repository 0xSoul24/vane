package org.oddlama.vane.core.resourcepack

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.util.function.Predicate

class ResourcePackFileWatcher(private val resourcePackDistributor: ResourcePackDistributor, private val file: File) {
    private var eyes: WatchService? = null
    private var watchTask: BukkitTask? = null

    @Throws(IOException::class)
    fun watchForChanges() {
        eyes = FileSystems.getDefault().newWatchService()
        val langFileMatch = FileSystems.getDefault().getPathMatcher("glob:**/lang-*.yml")
        registerDirectories(Paths.get("plugins"), eyes!!) { p: Path? -> this.isVaneModuleFolder(p!!) }

        watchTask =
            watchAsync(eyes!!, langFileMatch) { this.updateAndSendResourcePack() }.runTaskAsynchronously(
                (resourcePackDistributor.module!! as org.bukkit.plugin.Plugin)
            )
    }

    fun stop() {
        if (watchTask != null) {
            watchTask!!.cancel()
            watchTask = null
        }
        if (eyes != null) {
            try {
                eyes!!.close()
            } catch (_: IOException) {
            }
            eyes = null
        }
    }

    private fun updateAndSendResourcePack() {
        resourcePackDistributor.counter++
        resourcePackDistributor.module!!.generateResourcePack()
        resourcePackDistributor.updateSha1(file)
        for (player in Bukkit.getOnlinePlayers()) {
            resourcePackDistributor.sendResourcePack(player)
        }
    }

    private fun isVaneModuleFolder(p: Path): Boolean {
        return p.fileName.toString().startsWith("vane-")
    }

    private class TrackRunned(val r: Runnable) : BukkitRunnable() {
        var hasRun: Boolean = false
        var hasStarted: Boolean = false

        override fun run() {
            hasStarted = true
            r.run()
            hasRun = true
        }
    }

    private fun watchAsync(eyes: WatchService, matchLang: PathMatcher, onHit: Runnable): BukkitRunnable {
        return object : BukkitRunnable() {
            override fun run() {
                var shouldSchedule = false
                var runner: TrackRunned? = null
                while (true) {
                    val key: WatchKey
                    try {
                        key = eyes.take()
                    } catch (_: ClosedWatchServiceException) {
                        return
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return
                    }
                    // process events
                    for (event in key.pollEvents()) {
                        if (event.kind() === StandardWatchEventKinds.OVERFLOW) continue

                        // Use a safe cast and runtime checks to avoid unchecked generic cast warnings
                        val ev = event as WatchEvent<*>
                        val dir = key.watchable() as? Path ?: continue
                        val filename = ev.context() as? Path ?: continue
                        if (!matchLang.matches(dir.resolve(filename))) continue
                        shouldSchedule = true
                    }

                    if (shouldSchedule) {
                        if (runner != null) {
                            if (!runner.hasStarted) runner.cancel()
                        }
                        runner = TrackRunned(onHit)
                        runner.runTaskLater((resourcePackDistributor.module!! as org.bukkit.plugin.Plugin), 20L)
                        shouldSchedule = false
                    }

                    // reset the key
                    val valid = key.reset()
                    if (!valid) {
                        return
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun registerDirectories(root: Path, watcher: WatchService, pathMatch: Predicate<Path?>?) {
        // register vane sub-folders.
        val interestingPaths = Files.walk(root)
            .filter { path: Path -> Files.isDirectory(path) }
            .filter(pathMatch)
            .iterator()
        // quirky, but checked exceptions inside streams suck.
        while (interestingPaths.hasNext()) {
            val p = interestingPaths.next()
            p.register(
                watcher,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_DELETE,
                StandardWatchEventKinds.ENTRY_MODIFY
            )
        }
    }
}
