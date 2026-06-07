package com.game.logic;

import org.bson.BsonDocument;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del criterio de ordenamiento del ranking. Documenta el BUG del {@code .sort()}
 * encadenado (DA-2): hoy el ranking SOLO ordena por {@code score} y pierde los desempates.
 *
 * <p>Estos tests inspeccionan el {@link Bson} resultante (sin Mongo real) convirtiéndolo a
 * {@link BsonDocument}, que es la representación que el driver envía al servidor.
 */
@Tag("unit")
class RankingSortTest {

    private static BsonDocument toDoc(Bson sort) {
        // Driver sync 5.x: el registro de codecs por defecto vive en MongoClientSettings.
        return sort.toBsonDocument(BsonDocument.class,
                com.mongodb.MongoClientSettings.getDefaultCodecRegistry());
    }

    @Test
    @DisplayName("CARACTERIZACIÓN DA-2: el orden actual SOLO contiene 'score' (los desempates se perdieron)")
    void currentOrderHasOnlyScore() {
        BsonDocument doc = toDoc(RankingSort.currentBuggyOrder());
        assertEquals(1, doc.size(), "El orden actual debería tener una sola clave (el bug)");
        assertTrue(doc.containsKey("score"));
        assertEquals(-1, doc.getInt32("score").getValue(), "descending");
    }

    @Test
    @DisplayName("El orden PRETENDIDO contiene las 4 claves en orden de prioridad")
    void intendedOrderHasAllKeys() {
        BsonDocument doc = toDoc(RankingSort.INTENDED_ORDER);
        assertEquals(4, doc.size());
        assertTrue(doc.containsKey("score"));
        assertTrue(doc.containsKey("win rate"));
        assertTrue(doc.containsKey("amount played"));
        assertTrue(doc.containsKey("time played"));
        // score, win rate y amount played descendentes; time played ascendente.
        assertEquals(-1, doc.getInt32("score").getValue());
        assertEquals(-1, doc.getInt32("win rate").getValue());
        assertEquals(-1, doc.getInt32("amount played").getValue());
        assertEquals(1, doc.getInt32("time played").getValue());
    }
}
