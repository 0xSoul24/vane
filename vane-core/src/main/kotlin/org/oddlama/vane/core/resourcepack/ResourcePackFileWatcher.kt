package org.oddlama.vane.core.resourcepack

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.IOException
import java.nio.file.*

class ResourcePackFileWatcher(private val resourcePackDistributor: ResourcePackDistributor, private val file: File) {
    private var eyes: WatchService? = null
    private var watchTask: BukkitTask? = null

    @Throws(IOException::class)
    fun watchForChanges() {
        val plugin = resourcePackDistributor.module as? Plugin ?: return
        val watcher = FileSystems.getDefault().newWatchService()
        eyes = watcher
        val langFileMatch = FileSystems.getDefault().getPathMatcher("glob:**/lang-*.yml")
        registerDirectories(Paths.get("plugins"), watcher, ::isVaneModuleFolder)
        watchTask = watchAsync(watcher, langFileMatch) { updateAndSendResourcePack() }
            .runTaskAsynchronously(plugin)
    }

    fun stop() {
        watchTask?.cancel()
        watchTask = null
        try { eyes?.close() } catch (_: IOException) {}
        eyes = null
    }

    private fun updateAndSendResourcePack() {
        val mod = resourcePackDistributor.module ?: return
        resourcePackDistributor.counter++
        mod.generateResourcePack()
        resourcePackDistributor.updateSha1(file)
        Bukkit.getOnlinePlayers().forEach { resourcePackDistributor.sendResourcePack(it) }
    }

    private fun isVaneModuleFolder(p: Path): Boolean = p.fileName.toString().startsWith("vane-")

    private class TrackRunned(val r: Runnable) : BukkitRunnable() {
        var hasRun = false
        var hasStarted = false

        override fun run() {
            hasStarted = true
            r.run()
            hasRun = true
        }
    }

    private fun watchAsync(eyes: WatchService, matchLang: PathMatcher, onHit: Runnable): BukkitRunnable {
        val plugin = resourcePackDistributor.module as? Plugin
        return object : BukkitRunnable() {
            override fun run() {
                var shouldSchedule = false
                var runner: TrackRunned? = null
                while (true) {
                    val key = try {
                        eyes.take()
                    } catch (_: ClosedWatchServiceException) {
                        return
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return
                    }
                    key.pollEvents()
                        .filterIsInstance<WatchEvent<*>>()
                        .filter { it.kind() !== StandardWatchEventKinds.OVERFLOW }
                        .forEach { ev ->
                            val dir = key.watchable() as? Path ?: return@forEach
                            val filename = ev.context() as? Path ?: return@forEach
                            if (matchLang.matches(dir.resolve(filename))) shouldSchedule = true
                        }
                    if (shouldSchedule) {
                        if (runner?.hasStarted == false) runner.cancel()
                        runner = TrackRunned(onHit).also {
                            if (plugin != null) it.runTaskLater(plugin, 20L)
                        }
                        shouldSchedule = false
                    }
                    if (!key.reset()) return
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun registerDirectories(root: Path, watcher: WatchService, pathMatch: (Path) -> Boolean) {
        Files.walk(root)
            .filter { Files.isDirectory(it) && pathMatch(it) }
            .forEach { p ->
                p.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
            }
    }
}
