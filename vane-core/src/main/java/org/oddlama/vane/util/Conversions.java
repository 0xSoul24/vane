package org.oddlama.vane.util;

public class Conversions {

    public static long msToTicks(long ms) {
        return ms / 50;
    }

    public static long ticksToMs(long ticks) {
        return ticks * 50;
    }

    public static int expForLevel(int level) {
        if (level < 17) {
            return level * level + 6 * level;
        } else if (level < 32) {
            return (int) (2.5 * level * level - 40.5 * level) + 360;
        } else {
            return (int) (4.5 * level * level - 162.5 * level) + 2220;
        }
    }
}
