package com.game.logic;

/**
 * Zona de impacto de la barra de ataque (FIGHT) sobre la caja de objetivo.
 *
 * <p>Modela explícitamente lo que hoy en {@code Events.selectTarget()} se decide con una
 * cadena de {@code if/else if} sobre colisiones con los rectángulos de {@code BoxAttack}
 * ({@code Events.java:174-189}). Extraer la zona a un tipo permite probar el cálculo de
 * puntaje sin libGDX. Ver {@code .claude/skills/libgdx-state-pattern.skill.md}.
 */
public enum HitZone {
    /** Centro exacto. */
    CENTER,
    /** Banda verde (izquierda o derecha del centro). */
    GREEN,
    /** Banda amarilla. */
    YELLOW,
    /** Banda roja (exterior). */
    RED,
    /** Sin impacto en ninguna zona. */
    MISS
}
