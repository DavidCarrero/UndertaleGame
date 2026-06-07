package com.game.logic;

/**
 * Cálculos puros de estadísticas de jugador: win-rate y best-score. Extraídos de
 * {@code DataBase.updatePlayerStats()} ({@code DataBase.java:61-70}) preservando la
 * aritmética exacta del código original.
 *
 * <p>Función pura — sin Mongo, sin libGDX. Testeable con JUnit5 puro.
 * Ver {@code .claude/skills/repository-pattern-mongo.skill.md}.
 */
public final class StatsCalculator {

    private StatsCalculator() {
    }

    /**
     * Win-rate al actualizar un jugador existente. Reproduce exactamente
     * {@code DataBase.java:70}: {@code Math.round((float) won / played * 100 * 100.0) / 100.0}.
     *
     * <p><b>Nota:</b> el código original NUNCA llama esta ruta con {@code played == 0}
     * (en el primer juego inserta win-rate literal 100.0/0.0, ver
     * {@link #firstGameWinRate(boolean)}). Esta función documenta la división tal cual; un
     * {@code played == 0} produciría {@code NaN}/división por cero, condición que el código
     * de producción evita por construcción. El guard explícito se añadirá en el refactor de
     * FASE 2.
     */
    public static double winRateOnUpdate(int won, int played) {
        return Math.round((float) won / played * 100 * 100.0) / 100.0;
    }

    /**
     * Win-rate del primer juego (rama de insert). Reproduce {@code DataBase.java:52,58}:
     * 100.0 si ganó, 0.0 si perdió.
     */
    public static double firstGameWinRate(boolean won) {
        return won ? 100.0 : 0.0;
    }

    /**
     * Mejor puntaje al actualizar: conserva el existente salvo que el nuevo sea
     * estrictamente mayor. Reproduce {@code DataBase.java:67}
     * ({@code score > actualScore ? score : actualScore}).
     */
    public static int bestScore(int existingScore, int incomingScore) {
        return incomingScore > existingScore ? incomingScore : existingScore;
    }

    /**
     * Cuenta de victorias al actualizar. Reproduce {@code DataBase.java:62}
     * ({@code isWon ? actualWon + 1 : actualWon}).
     */
    public static int amountWonOnUpdate(int existingWon, boolean won) {
        return won ? existingWon + 1 : existingWon;
    }
}
