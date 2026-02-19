package org.oddlama.vane.core.command.enums;

public enum WeatherValue {
    clear(false, false),
    sun(false, false),
    rain(true, false),
    thunder(true, true);

    private boolean isStorm;
    private boolean isThunder;

    private WeatherValue(boolean isStorm, boolean isThunder) {
        this.isStorm = isStorm;
        this.isThunder = isThunder;
    }

    public boolean storm() {
        return isStorm;
    }

    public boolean thunder() {
        return isThunder;
    }
}
