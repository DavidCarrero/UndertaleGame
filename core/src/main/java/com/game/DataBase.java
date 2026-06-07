package com.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.game.logic.DurationFormatter;
import com.game.logic.RankingSort;
import com.game.logic.StatsCalculator;
import org.bson.Document;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.conversions.Bson;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

public class DataBase {
    public static MongoClient mongoClient;

    private static MongoCollection<Document> collection;

    public static void createConnection() {
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.WARNING);
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("Undertale");
        collection = database.getCollection("Ranking");
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    public static void updatePlayerStats(String name, int score, Duration durationPlayed, boolean isWon) {
        createConnection();
        Document query = collection.find(eq("name", name)).first();
        // Formato y cálculos extraídos a clases puras (DurationFormatter/StatsCalculator).
        // Comportamiento idéntico al original. (FASE 0)
        String format = DurationFormatter.format(durationPlayed);

        if (query == null) {
            Document document = new Document("name", name)
                    .append("amount played", 1)
                    .append("amount won", isWon ? 1 : 0)
                    .append("score", score)
                    .append("time played", format)
                    .append("win rate", StatsCalculator.firstGameWinRate(isWon));
            collection.insertOne(document);
        } else {
            int actualScore = query.getInteger("score");
            int actualAmountWon = StatsCalculator.amountWonOnUpdate(query.getInteger("amount won"), isWon);

            int amountPlayed = query.getInteger("amount played") + 1;
            Bson update = combine(
                inc("amount played", 1),
                set("score", StatsCalculator.bestScore(actualScore, score)),
                set("amount won", actualAmountWon),
                set("time played", format),
                set("win rate", StatsCalculator.winRateOnUpdate(actualAmountWon, amountPlayed))
            );
            collection.findOneAndUpdate(
                eq("name", name),
                update
            );
        }
    }

    public static List<Document> getRanking() {
        if (mongoClient == null) {
            createConnection();
        }

        List<Document> topTenScores;

        // Orden extraído a RankingSort. Comportamiento idéntico al original: el código
        // encadenaba 4 .sort(), de los cuales el driver SOLO aplica el último
        // (descending("score")), perdiendo los desempates. Se preserva ese comportamiento
        // (currentBuggyOrder); el orden multi-clave correcto vive en RankingSort.INTENDED_ORDER
        // y se adoptará en FASE 2. Ver docs/ARQUITECTURA-AUDITORIA.md (DA-2). (FASE 0)
        topTenScores = collection.find()
            .sort(RankingSort.currentBuggyOrder())
            .limit(10)
            .into(new ArrayList<>());
        return topTenScores;
    }
}
