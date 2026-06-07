package com.game.logic;

/**
 * Cálculo puro del puntaje de un ataque del jugador (FIGHT). Extraído de
 * {@code Events.selectTarget()} ({@code Events.java:174-192}) sin cambiar el comportamiento
 * observable: reproduce exactamente la misma aritmética que el código original.
 *
 * <p>Función pura — sin libGDX, sin estado global. Testeable con JUnit5 puro.
 * Ver {@code .claude/skills/libgdx-testing-headless.skill.md}.
 */
public final class ScoreCalculator {

    /** Bono por acertar el centro exacto. */
    public static final int CENTER_BONUS = 300;
    /** Bono de precisión "perfecta" (ver {@link #PERFECT_POSITION} y el BUG documentado abajo). */
    public static final int PERFECT_BONUS = 200;
    public static final int GREEN_BONUS = 150;
    public static final int YELLOW_BONUS = 50;
    public static final int RED_BONUS = 15;
    /** Multiplicador por la fase de combate (act). */
    public static final int ACT_MULTIPLIER = 100;

    /**
     * Posición normalizada de la barra que el código original compara con {@code ==} para
     * otorgar {@link #PERFECT_BONUS}.
     *
     * <p><b>BUG (documentado, no corregido en FASE 0):</b> el código original
     * ({@code Events.java:176}) hace {@code if (POSITION_BAR_NORMALIZED == 273)} sobre un
     * {@code float} calculado en tiempo real. La igualdad exacta de floats casi nunca se
     * cumple, por lo que el {@link #PERFECT_BONUS} es prácticamente código muerto. Se
     * preserva el comportamiento aquí para no alterar el juego; el arreglo (usar una
     * tolerancia o derivar el "perfect" de la geometría) se hará en FASE 4 junto al
     * refactor de scoring. Ver {@code docs/ARQUITECTURA-AUDITORIA.md} (SOC-2).
     */
    public static final float PERFECT_POSITION = 273f;

    private ScoreCalculator() {
    }

    /**
     * Puntaje total de un ataque, replicando la lógica original:
     * <ul>
     *   <li>CENTER: {@code CENTER_BONUS} (+ {@code PERFECT_BONUS} solo si
     *       {@code positionBarNormalized == 273}, ver BUG).</li>
     *   <li>GREEN/YELLOW/RED: su bono respectivo.</li>
     *   <li>MISS: 0 de bono de zona.</li>
     * </ul>
     * En todos los casos se suma {@code act * ACT_MULTIPLIER} (igual que
     * {@code Events.java:192}, que corre fuera del if/else).
     *
     * @param zone                 zona de impacto
     * @param act                  fase de combate actual
     * @param positionBarNormalized posición normalizada de la barra (solo relevante para CENTER)
     * @return puntaje a sumar por este ataque
     */
    public static int attackScore(HitZone zone, int act, float positionBarNormalized) {
        int score = 0;
        switch (zone) {
            case CENTER -> {
                if (positionBarNormalized == PERFECT_POSITION) {
                    score += PERFECT_BONUS;
                }
                score += CENTER_BONUS;
            }
            case GREEN -> score += GREEN_BONUS;
            case YELLOW -> score += YELLOW_BONUS;
            case RED -> score += RED_BONUS;
            case MISS -> {
                // sin bono de zona
            }
        }
        score += act * ACT_MULTIPLIER;
        return score;
    }
}
