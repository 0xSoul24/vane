package org.oddlama.vane.core.command.enums

enum class WeatherValue(private val isStorm: Boolean, private val isThunder: Boolean) {
    Clear(false, false),
    Sun(false, false),
    Rain(true, false),
    Thunder(true, true);

    fun storm(): Boolean {
        return isStorm
    }

    fun thunder(): Boolean {
        return isThunder
    }
}
