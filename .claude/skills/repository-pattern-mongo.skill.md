---
name: repository-pattern-mongo
description: >
  Acceso a datos con patrón Repository instanciable e inyectable sobre MongoDB,
  con DTO/record de dominio y lógica de cálculo en clases puras. Activar al tocar
  DataBase.java, MongoClient, MongoCollection, MongoDatabase, "mongodb://",
  org.bson.Document, ranking, persistencia de stats/score, win-rate, o al añadir
  cualquier acceso a base de datos. Triggers: Mongo, repositorio, persistencia,
  DAO, connection string, Document, upsert.
---

# Repository pattern sobre MongoDB

`DataBase` es 100% `static`, con la URI `"mongodb://localhost:27017"` hardcodeada, abre conexión por operación, mezcla lógica de dominio (win-rate, best-score, sort) con persistencia, y opera sobre `Document` crudos. No es mockeable ni testeable, y esconde dos bugs reales.

Hallazgos: DA-1, DA-2, SOC-2 (ver `docs/ARQUITECTURA-AUDITORIA.md`). Habilita la integración Mongo con Testcontainers (skill `libgdx-testing-headless`).

## Prohibido
- ❌ Métodos `static` para acceso a datos.
- ❌ Connection string hardcodeada (debe venir de config/env).
- ❌ Abrir/cerrar conexión por operación (reabrir `MongoClient` en cada write — fuga del anterior).
- ❌ Lógica de dominio (cálculo de win-rate, best-score, formato de tiempo, ordenamiento) dentro de la capa de persistencia.
- ❌ `Document` crudos cruzando la frontera de la capa de datos hacia la UI (`FabricElements` lee keys string del Document).
- ❌ **Bug a no repetir**: encadenar `.sort()` (cada llamada sobreescribe la anterior — solo aplica la última). Usar `Sorts.orderBy(...)` en **una** llamada.

## Obligatorio
1. **`record PlayerStats(String name, int amountPlayed, int amountWon, int score, String timePlayed, double winRate)`** como DTO de dominio.
2. **`interface RankingRepository`** con `save(PlayerStats)`, `findByName(String)`, `topTen()`.
3. **`MongoRankingRepository`** que recibe una `MongoDatabase` (o la URI) **por constructor** — inyectada desde config (`System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017")`).
4. **`StatsCalculator`** puro (sin Mongo, sin libGDX) para win-rate, best-score, formato de duración → testeable con JUnit5 sin Docker.
5. **`Sorts.orderBy(...)`** en una sola llamada para el ranking (arregla DA-2).
6. `MongoClient` de larga vida (uno por app), cerrado una vez al salir — no por operación.

## Before → After

```java
// ❌ BEFORE — DataBase.java
public class DataBase {
    public static MongoClient mongoClient;
    public static void createConnection() {
        mongoClient = MongoClients.create("mongodb://localhost:27017");  // hardcoded, por-op
    }
    public static void updatePlayerStats(String name, int score, Duration d, boolean won) {
        createConnection();
        ... int actualScore = query.getInteger("score");                  // dominio + persistencia
        set("win rate", Math.round(...)));                                // cálculo inline
    }
    public static List<Document> getRanking() {
        return collection.find()
            .sort(Sorts.ascending("time played"))     // BUG: cada sort sobreescribe
            .sort(Sorts.descending("amount played"))   // la anterior — solo
            .sort(Sorts.descending("win rate"))        // descending("score") aplica
            .sort(Sorts.descending("score")).limit(10).into(new ArrayList<>());
    }
}
```

```java
// ✅ AFTER
public record PlayerStats(String name, int amountPlayed, int amountWon,
                          int score, String timePlayed, double winRate) {}

public interface RankingRepository {
    void save(PlayerStats stats);
    Optional<PlayerStats> findByName(String name);
    List<PlayerStats> topTen();
}

public class MongoRankingRepository implements RankingRepository {
    private final MongoCollection<Document> coll;
    public MongoRankingRepository(MongoDatabase db) {           // inyectada
        this.coll = db.getCollection("Ranking");
    }
    @Override public List<PlayerStats> topTen() {
        return coll.find()
            .sort(Sorts.orderBy(                                // UNA llamada
                Sorts.descending("score"),
                Sorts.descending("win rate"),
                Sorts.descending("amount played"),
                Sorts.ascending("time played")))
            .limit(10).map(MongoRankingRepository::toStats).into(new ArrayList<>());
    }
}

public final class StatsCalculator {                            // puro, testeable
    public static double winRate(int won, int played) {
        return played == 0 ? 0.0 : Math.round((double) won / played * 10000) / 100.0;
    }
    public static int bestScore(int existing, int incoming) { return Math.max(existing, incoming); }
}
```

## Checklist al tocar persistencia
- [ ] ¿El acceso pasa por una interfaz `Repository` instanciable?
- [ ] ¿La conexión/URI se inyecta (no hardcoded, no por-operación)?
- [ ] ¿El cálculo de dominio vive en una clase pura testeable?
- [ ] ¿Se devuelve un `record`/DTO, no `Document` crudo?
- [ ] ¿El ranking usa `Sorts.orderBy(...)` en una sola llamada?
