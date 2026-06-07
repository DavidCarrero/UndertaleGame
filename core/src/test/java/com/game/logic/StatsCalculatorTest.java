package com.game.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de los cálculos puros de estadísticas (win-rate, best-score, victorias).
 * Caracteriza el comportamiento extraído de {@code DataBase.updatePlayerStats()}.
 */
@Tag("unit")
class StatsCalculatorTest {

    @Test
    @DisplayName("firstGameWinRate: ganó → 100.0, perdió → 0.0")
    void firstGameWinRate() {
        assertEquals(100.0, StatsCalculator.firstGameWinRate(true));
        assertEquals(0.0, StatsCalculator.firstGameWinRate(false));
    }

    @Test
    @DisplayName("winRateOnUpdate: 1 de 3 → 33.33 (redondeo a 2 decimales)")
    void winRateOneOfThree() {
        assertEquals(33.33, StatsCalculator.winRateOnUpdate(1, 3));
    }

    @Test
    @DisplayName("winRateOnUpdate: 1 de 1 → 100.0; 0 de 5 → 0.0")
    void winRateEdges() {
        assertEquals(100.0, StatsCalculator.winRateOnUpdate(1, 1));
        assertEquals(0.0, StatsCalculator.winRateOnUpdate(0, 5));
    }

    @Test
    @DisplayName("bestScore: conserva el mayor; empate conserva el existente")
    void bestScore() {
        assertEquals(500, StatsCalculator.bestScore(500, 300));
        assertEquals(300, StatsCalculator.bestScore(200, 300));
        assertEquals(300, StatsCalculator.bestScore(300, 300)); // '>' estricto → mantiene existente
    }

    @Test
    @DisplayName("amountWonOnUpdate: +1 si ganó, igual si perdió")
    void amountWon() {
        assertEquals(4, StatsCalculator.amountWonOnUpdate(3, true));
        assertEquals(3, StatsCalculator.amountWonOnUpdate(3, false));
    }

    @Test
    @Disabled("CARACTERIZACIÓN DA-1: winRateOnUpdate(x, 0) produce división por cero (NaN/Infinity). "
            + "El código de producción nunca llega aquí con played==0 (el primer juego usa "
            + "firstGameWinRate), pero la función pura no lo guarda. Al introducir el "
            + "RankingRepository en FASE 2 se añadirá un guard que devuelva 0.0; habilitar entonces.")
    @DisplayName("ESPERADO (post-fix): winRateOnUpdate con 0 partidas no lanza y devuelve 0.0")
    void winRateZeroPlayedShouldBeSafe() {
        assertEquals(0.0, StatsCalculator.winRateOnUpdate(0, 0));
    }
}
