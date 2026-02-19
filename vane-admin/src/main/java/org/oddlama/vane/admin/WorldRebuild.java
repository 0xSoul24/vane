package org.oddlama.vane.admin;

import static org.oddlama.vane.util.Conversions.msToTicks;
import static org.oddlama.vane.util.Nms.setAirNoDrops;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.oddlama.vane.annotation.config.ConfigDouble;
import org.oddlama.vane.annotation.config.ConfigLong;
import org.oddlama.vane.core.Listener;
import org.oddlama.vane.core.module.Context;

public class WorldRebuild extends Listener<Admin> {

    @ConfigLong(def = 2000, min = 0, desc = "Delay in milliseconds until the world will be rebuilt.")
    private long configDelay;

    @ConfigDouble(
        def = 0.175,
        min = 0.0,
        desc = "Determines rebuild speed. Higher falloff means faster transition to quicker rebuild. After n blocks, the delay until the next block will be d_n = delay * exp(-x * delay_falloff). For example 0.0 will result in same delay for every block."
    )
    private double configDelayFalloff;

    @ConfigLong(
        def = 50,
        min = 50,
        desc = "Minimum delay in milliseconds between rebuilding two blocks. Anything <= 50 milliseconds will be one tick."
    )
    private long configMinDelay;

    public WorldRebuild(Context<Admin> context) {
        super(
            context.group(
                "WorldRebuild",
                "Instead of cancelling explosions, the world will regenerate after a short amount of time."
            )
        );
    }

    private final List<Rebuilder> rebuilders = new ArrayList<>();

    public void rebuild(final List<Block> blocks) {
        // Store a snapshot of all block states
        final var states = new ArrayList<BlockState>();
        for (final var block : blocks) {
            states.add(block.getState());
        }

        // Set everything to air without triggering physics
        for (final var block : blocks) {
            setAirNoDrops(block);
        }

        // Schedule rebuild
        rebuilders.add(new Rebuilder(states));
    }

    @Override
    public void onDisable() {
        // Finish all pending rebuilds now!
        for (final var r : new ArrayList<>(rebuilders)) {
            r.finishNow();
        }
        rebuilders.clear();
    }

    public class Rebuilder implements Runnable {

        private List<BlockState> states;
        private BukkitTask task = null;
        private long amountRebuild = 0;

        public Rebuilder(final List<BlockState> blockStates) {
            this.states = blockStates;
            if (this.states.isEmpty()) {
                return;
            }

            // Find top center point for rebuild order reference
            Vector center = new Vector(0, 0, 0);
            int maxY = 0;
            for (final var state : this.states) {
                maxY = Math.max(maxY, state.getY());
                center.add(state.getLocation().toVector());
            }
            center.multiply(1.0 / this.states.size());
            center.setY(maxY + 1);

            // Sort blocks to rebuild them in an ordered fashion
            this.states.sort(new RebuildComparator(center));

            // Initialize delay
            task = getModule().scheduleTask(this, msToTicks(configDelay));
        }

        private void finish() {
            task = null;
            WorldRebuild.this.rebuilders.remove(this);
        }

        private void rebuildNextBlock() {
            rebuildBlock(states.remove(states.size() - 1));
        }

        private void rebuildBlock(final BlockState state) {
            final var block = state.getBlock();
            ++amountRebuild;

            // Break any block that isn't air first
            if (block.getType() != Material.AIR) {
                block.breakNaturally();
            }

            // Force update without physics to set a block type
            state.update(true, false);
            // Second update forces block state specific update
            state.update(true, false);

            // Play sound
            block
                .getWorld()
                .playSound(
                    block.getLocation(),
                    block.getBlockSoundGroup().getPlaceSound(),
                    SoundCategory.BLOCKS,
                    1.0f,
                    0.8f
                );
        }

        public void finishNow() {
            if (task != null) {
                task.cancel();
            }

            for (final var state : states) {
                rebuildBlock(state);
            }

            finish();
        }

        @Override
        public void run() {
            if (states.isEmpty()) {
                finish();
            } else {
                // Rebuild next block
                rebuildNextBlock();

                // Adjust delay
                final var delay = msToTicks(
                    Math.max(configMinDelay, (int) (configDelay * Math.exp(-amountRebuild * configDelayFalloff)))
                );
                WorldRebuild.this.getModule().scheduleTask(this, delay);
            }
        }
    }

    public static class RebuildComparator implements Comparator<BlockState> {

        private Vector referencePoint;

        public RebuildComparator(final Vector referencePoint) {
            this.referencePoint = referencePoint;
        }

        @Override
        public int compare(final BlockState a, final BlockState b) {
            // Sort by distance to top-most center. The Last block will be rebuilt first.
            final var da = a.getLocation().toVector().subtract(referencePoint).lengthSquared();
            final var db = b.getLocation().toVector().subtract(referencePoint).lengthSquared();
            return Double.compare(da, db);
        }
    }
}
