package com.game.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests del formateo de duración a HH:mm:ss. Función pura.
 * Caracteriza {@code DataBase.java:43}.
 */
@Tag("unit")
class DurationFormatterTest {

    @Test
    @DisplayName("0 segundos → 00:00:00")
    void zero() {
        assertEquals("00:00:00", DurationFormatter.format(Duration.ZERO));
    }

    @Test
    @DisplayName("59 segundos → 00:00:59")
    void seconds() {
        assertEquals("00:00:59", DurationFormatter.format(Duration.ofSeconds(59)));
    }

    @Test
    @DisplayName("3661 segundos → 01:01:01")
    void hoursMinutesSeconds() {
        assertEquals("01:01:01", DurationFormatter.format(Duration.ofSeconds(3661)));
    }

    @Test
    @DisplayName("2h 30m 5s → 02:30:05 (cero a la izquierda)")
    void zeroPadding() {
        assertEquals("02:30:05", DurationFormatter.format(Duration.ofSeconds(2 * 3600 + 30 * 60 + 5)));
    }

    @Test
    @DisplayName("CARACTERIZACIÓN: >99h desborda el ancho de 2 dígitos (100:00:00) — comportamiento actual")
    void overflowBeyond99Hours() {
        // %02d no trunca; 100h se imprime como '100'. Documenta el comportamiento real.
        assertEquals("100:00:00", DurationFormatter.format(Duration.ofSeconds(100L * 3600)));
    }
}
