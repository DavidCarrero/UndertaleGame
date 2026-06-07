package com.game.logic;

import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;

/**
 * Criterio de ordenamiento del ranking, extraído de {@code DataBase.getRanking()}
 * ({@code DataBase.java:86-94}).
 *
 * <p><b>BUG (documentado, no corregido en FASE 0):</b> el código original encadena cuatro
 * llamadas {@code .sort(...)} sobre el {@code FindIterable}:
 * <pre>
 *   .sort(Sorts.ascending("time played"))
 *   .sort(Sorts.descending("amount played"))
 *   .sort(Sorts.descending("win rate"))
 *   .sort(Sorts.descending("score"))
 * </pre>
 * En el driver de MongoDB, cada {@code .sort(...)} <b>reemplaza</b> al anterior, así que el
 * ranking efectivo SOLO ordena por {@code descending("score")} y pierde todos los
 * desempates (win rate, partidas jugadas, tiempo). El orden multi-clave pretendido es el de
 * {@link #INTENDED_ORDER}. El arreglo (usar {@code Sorts.orderBy(...)} en una sola llamada)
 * se hará en FASE 2 al introducir el {@code RankingRepository}. Ver
 * {@code docs/ARQUITECTURA-AUDITORIA.md} (DA-2) y
 * {@code .claude/skills/repository-pattern-mongo.skill.md}.
 */
public final class RankingSort {

    private RankingSort() {
    }

    /** Lo que el código original APLICA hoy (solo score). */
    public static Bson currentBuggyOrder() {
        return Sorts.descending("score");
    }

    /**
     * Orden multi-clave PRETENDIDO (todas las claves en una sola llamada {@code orderBy}).
     * Este es el comportamiento correcto que reemplazará al actual en FASE 2.
     */
    public static final Bson INTENDED_ORDER = Sorts.orderBy(
            Sorts.descending("score"),
            Sorts.descending("win rate"),
            Sorts.descending("amount played"),
            Sorts.ascending("time played"));
}
