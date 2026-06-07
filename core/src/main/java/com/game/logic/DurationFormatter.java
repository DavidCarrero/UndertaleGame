package com.game.logic;

import java.time.Duration;

/**
 * Formatea una {@link Duration} a {@code HH:mm:ss}. Extraído de {@code DataBase.java:40,43}
 * sin cambiar comportamiento (misma fórmula con {@code String.format}).
 *
 * <p>Función pura — testeable con JUnit5 puro.
 */
public final class DurationFormatter {

    private DurationFormatter() {
    }

    /** Reproduce exactamente {@code DataBase.java:43}. */
    public static String format(Duration durationPlayed) {
        long secondsSum = durationPlayed.getSeconds();
        return String.format("%02d:%02d:%02d",
                secondsSum / 3600,
                (secondsSum % 3600) / 60,
                secondsSum % 60);
    }
}
