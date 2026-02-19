package org.oddlama.vane.core.resourcepack;

import static java.nio.file.StandardWatchEventKinds.*;

import java.io.*;
import java.nio.file.*;
import java.util.Iterator;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class ResourcePackFileWatcher {

    private final ResourcePackDistributor resourcePackDistributor;
    private final File file;

    public ResourcePackFileWatcher(ResourcePackDistributor resourcePackDistributor, File file)
        throws IOException, InterruptedException {
        this.resourcePackDistributor = resourcePackDistributor;
        this.file = file;
    }

    public void watchForChanges() throws IOException {
        var eyes = FileSystems.getDefault().newWatchService();
        var langFileMatch = FileSystems.getDefault().getPathMatcher("glob:**/lang-*.yml");
        registerDirectories(Paths.get("plugins"), eyes, this::isVaneModuleFolder);

        watchAsync(eyes, langFileMatch, this::updateAndSendResourcePack).runTaskAsynchronously(
            resourcePackDistributor.getModule()
        );
    }

    private void updateAndSendResourcePack() {
        resourcePackDistributor.counter++;
        resourcePackDistributor.getModule().generateResourcePack();
        resourcePackDistributor.updateSha1(file);
        for (Player player : Bukkit.getOnlinePlayers()) {
            resourcePackDistributor.sendResourcePack(player);
        }
    }

    private boolean isVaneModuleFolder(Path p) {
        return p.getFileName().toString().startsWith("vane-");
    }

    private static class TrackRunned extends BukkitRunnable {

        final Runnable r;
        boolean hasRun = false;
        boolean hasStarted = false;

        public TrackRunned(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            hasStarted = true;
            r.run();
            hasRun = true;
        }
    }

    private @NotNull BukkitRunnable watchAsync(WatchService eyes, PathMatcher matchLang, Runnable onHit) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                boolean shouldSchedule = false;
                TrackRunned runner = null;
                for (;;) {
                    final WatchKey key;
                    try {
                        key = eyes.take();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    // process events
                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.kind() == OVERFLOW) continue;

                        // This generic is always Path for WatchEvent kinds other than OVERFLOW
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path dir = (Path) key.watchable();
                        Path filename = ev.context();
                        if (!matchLang.matches(dir.resolve(filename))) continue;
                        shouldSchedule = true;
                    }

                    if (shouldSchedule) {
                        if (runner != null) {
                            if (!runner.hasStarted) runner.cancel();
                        }
                        runner = new TrackRunned(onHit);
                        runner.runTaskLater(resourcePackDistributor.getModule().core, 20L);
                        shouldSchedule = false;
                    }

                    // reset the key
                    boolean valid = key.reset();
                    if (!valid) {
                        return;
                    }
                }
            }
        };
    }

    private void registerDirectories(Path root, WatchService watcher, Predicate<Path> pathMatch) throws IOException {
        // register vane sub-folders.
        final Iterator<Path> interestingPaths = Files.walk(root)
            .filter(Files::isDirectory)
            .filter(pathMatch)
            .iterator();
        // quirky, but checked exceptions inside streams suck.
        while (interestingPaths.hasNext()) {
            Path p = interestingPaths.next();
            p.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        }
    }
}
