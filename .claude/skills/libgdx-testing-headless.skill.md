---
name: libgdx-testing-headless
description: >
  Estrategia de testing para juegos libGDX: JUnit5 + Mockito, HeadlessApplication
  con GL mockeado para integración de Actors/Stage, Testcontainers MongoDB para
  persistencia, y e2e headless con input scripteado. Activar al escribir o pedir
  tests (unit/integración/e2e), al tocar build.gradle de test, GdxHeadlessExtension,
  HeadlessApplication, Mockito, Testcontainers, JUnit, o al implementar una
  funcionalidad que deba quedar cubierta por pruebas.
---

# Testing headless de libGDX

Stack elegido: **JUnit 5 + Mockito + gdx-backend-headless (GL mockeado) + Testcontainers MongoDB (Docker)**. El proyecto no tiene infraestructura de pruebas; esta skill define cómo añadirla y qué probar en cada capa.

Ver `docs/ARQUITECTURA-AUDITORIA.md` §6 (estrategia completa + matriz de bloqueo).

## Capas de prueba
1. **Unit (sin GL, sin Mongo)** — funciones puras extraídas: `ScoreCalculator`, `WinRateCalculator`, `BestScoreCalculator`, `DurationFormatter`, `TurnBonus`, `BattlePhase.next()`, `RankingComparator`, `CollisionGeometry.overlaps` (usa gdx-math, sin GL).
2. **Integración headless (GL mockeado)** — `Actor`/`Stage`: `BoxHeart` resize, `Stage` add/remove lifecycle, geometría de `BoxAttack`.
3. **Integración Mongo (Docker/Testcontainers)** — `MongoRankingRepository` round-trips (requiere refactor Repository primero).
4. **E2E headless** — driver scriptea input y avanza frames; asertar **estado, no píxeles**.

## Reglas
- ✅ Asertar **geometría/estado/hitbox**, nunca píxeles (el GL está mockeado, no hay subida real).
- ✅ La `GdxHeadlessExtension` DEBE bootear `HeadlessApplication` + `Gdx.gl = mock(GL20.class)` **antes** de que se cargue cualquier clase con estáticos derivados de `Gdx` (p. ej. `BlackScreen.VH_WIDTH`).
- ✅ Tests Mongo con `@Testcontainers` + `MongoDBContainer("mongo:7.0")`; gate con `assumeTrue(DockerClientFactory.instance().isDockerAvailable())` para **skip** (no fail) sin Docker.
- ✅ Etiquetar: `@Tag("unit")`, `@Tag("headless")`, `@Tag("integration")`, `@Tag("e2e")`.
- ❌ No depender de un Mongo local corriendo.
- ❌ No asertar sobre el frame exacto en e2e (timing sensible al latch `canSelect`); asertar tras un periodo quiescente.
- ❌ No compartir estáticos de `BlackScreen` entre clases de test hasta que aterrice el refactor de estado (`libgdx-no-static-state`).

## Wiring de Gradle (`core/build.gradle`)

```gradle
testImplementation platform('org.junit:junit-bom:5.10.2')
testImplementation 'org.junit.jupiter:junit-jupiter'
testRuntimeOnly   'org.junit.platform:junit-platform-launcher'
testImplementation 'org.mockito:mockito-core:5.11.0'
testImplementation 'org.mockito:mockito-junit-jupiter:5.11.0'
testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
testRuntimeOnly   "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
testImplementation platform('org.testcontainers:testcontainers-bom:1.19.7')
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mongodb'

test {
  useJUnitPlatform()
  jvmArgs '-XX:+EnableDynamicAgentLoading'   // JDK 21 + Mockito/ByteBuddy
}
```

## Harness headless

```java
public class GdxHeadlessExtension implements BeforeAllCallback {
    private static volatile boolean started = false;
    @Override public synchronized void beforeAll(ExtensionContext ctx) {
        if (started) return;
        var cfg = new HeadlessApplicationConfiguration();
        cfg.updatesPerSecond = -1;
        new HeadlessApplication(new ApplicationAdapter() {}, cfg);
        GL20 gl = mock(GL20.class);
        when(gl.glGenTexture()).thenReturn(1);   // handle no-cero
        Gdx.gl = gl; Gdx.gl20 = gl; Gdx.gl30 = mock(GL30.class);
        started = true;
    }
}
```

## Mongo (Testcontainers)

```java
@Tag("integration") @Testcontainers
class MongoRankingRepositoryIT {
    @Container static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
    @Test void topTenSortedByScoreThenWinRate() { /* expone el bug del .sort() encadenado */ }
}
```

## Matriz de bloqueo (qué test depende de qué refactor)
| Capa | Estado | Desbloqueado por |
|------|--------|------------------|
| Unit puros (Score/WinRate/Duration/Collision) | Corre ya (tras extraer la función) | — |
| Headless: BoxHeart/Stage/BoxAttack | Corre ya | `GdxHeadlessExtension` |
| Headless: Heart.move() clamp limpio | Bloqueado | input fuera de `draw()` (`libgdx-input-handling`) |
| Integración Mongo | Bloqueado | `RankingRepository` (`repository-pattern-mongo`) |
| E2E reach-GameOver/Win | Bloqueado | estado de instancia (`libgdx-no-static-state`) + input |

## Checklist al añadir una funcionalidad
- [ ] ¿La lógica pura quedó extraída a una clase testeable sin GL/Mongo y con test unitario?
- [ ] ¿Los Actors/Stage nuevos tienen test headless de estado/geometría?
- [ ] ¿El acceso a datos nuevo tiene test de integración con Testcontainers?
- [ ] ¿Los tests están etiquetados con `@Tag` correcto?
