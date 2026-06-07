package com.game.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests del cálculo de puntaje de ataque. Función pura — sin libGDX, sin Mongo.
 * Caracteriza el comportamiento extraído de {@code Events.selectTarget()}.
 */
@Tag("unit")
class ScoreCalculatorTest {

    @Test
    @DisplayName("CENTER en act 4 sin posición perfecta: 300 + 4*100 = 700")
    void centerAct4() {
        // posición != 273 → no aplica PERFECT_BONUS (comportamiento real del juego)
        assertEquals(700, ScoreCalculator.attackScore(HitZone.CENTER, 4, 100f));
    }

    @Test
    @DisplayName("GREEN en act 1: 150 + 1*100 = 250")
    void greenAct1() {
        assertEquals(250, ScoreCalculator.attackScore(HitZone.GREEN, 1, 0f));
    }

    @Test
    @DisplayName("YELLOW en act 3: 50 + 3*100 = 350")
    void yellowAct3() {
        assertEquals(350, ScoreCalculator.attackScore(HitZone.YELLOW, 3, 0f));
    }

    @Test
    @DisplayName("RED en act 8: 15 + 8*100 = 815")
    void redAct8() {
        assertEquals(815, ScoreCalculator.attackScore(HitZone.RED, 8, 0f));
    }

    @Test
    @DisplayName("MISS en act 5: solo el bono de fase = 500")
    void missOnlyActBonus() {
        assertEquals(500, ScoreCalculator.attackScore(HitZone.MISS, 5, 0f));
    }

    @Test
    @DisplayName("CARACTERIZACIÓN: el PERFECT_BONUS solo aplica con posición exacta == 273f")
    void perfectBonusOnlyOnExactPosition() {
        // Documenta el comportamiento actual: con la posición exacta sí suma 200.
        assertEquals(900, ScoreCalculator.attackScore(HitZone.CENTER, 4, 273f)); // 300+200+400
        // Con cualquier otra posición, NO suma el bono perfecto.
        assertEquals(700, ScoreCalculator.attackScore(HitZone.CENTER, 4, 272.99f));
    }

    @Test
    @Disabled("BUG SOC-2: el PERFECT_BONUS depende de una igualdad exacta de float (==273f) "
            + "que en el juego real casi nunca se cumple, así que es código muerto. "
            + "Al arreglarlo en FASE 4 (tolerancia o derivar de geometría), un impacto perfecto "
            + "DEBERÍA otorgar el bono de forma fiable. Habilitar este test entonces.")
    @DisplayName("ESPERADO (post-fix): un impacto perfecto otorga el bono de forma fiable")
    void perfectBonusShouldBeReachable() {
        // Tras el fix, un 'perfect hit' representado de forma robusta debe dar 300+200+400.
        // (La firma exacta cambiará en FASE 4; este test se reescribe al arreglar.)
        assertEquals(900, ScoreCalculator.attackScore(HitZone.CENTER, 4, 273.0001f));
    }
}
