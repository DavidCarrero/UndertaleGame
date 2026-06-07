# Informe consolidado de auditoría, refactorización y testing
## Clon de Undertale (Sans Fight) sobre libGDX

**Fecha:** 2026-06-07 · **Stack:** Java 21, libGDX 1.12.1, Gradle 8.6, MongoDB (mongodb-driver-sync 5.0.0) · **Módulo principal:** `core/src/main/java/com/game`

---

## 1. Resumen ejecutivo

### Estado de salud arquitectónica

El juego **funciona y se juega correctamente**. No se ha encontrado ningún defecto de seguridad, corrupción de datos ni crash garantizado en las rutas de ejecución reales. Todos los hallazgos son de **mantenibilidad, evolutividad, gestión de recursos y testabilidad** — deuda técnica, no fallos en producción.

Dicho esto, la deuda es **estructural y profunda**. El subsistema de combate está construido como una máquina de estados procedural sobre estado global `static`, compartido entre cinco clases vía `import static`. Esto produce tres consecuencias que se refuerzan entre sí:

1. **Nada es testeable en aislamiento** — no existe ni una sola prueba, ni infraestructura de pruebas, y el estado global hace imposible instanciar componentes con dobles de prueba.
2. **Casi cualquier cambio arrastra riesgo de regresión** — para razonar sobre un campo (`act`, `score`, `heart.isTurn`) hay que hacer grep global en varios archivos.
3. **Fugas de recursos nativos continuas** — cada proyectil, fuente y sonido se carga desde disco/VRAM en caliente, sin `AssetManager`.

**Veredicto de calibración:** la auditoría rebajó múltiples severidades de CRITICAL/HIGH a su nivel real. Para un juego mono-jugador offline con una sola instancia lógica de combate, **ningún hallazgo es CRITICAL**. La calibración final es: 0 CRITICAL, 3 HIGH, ~20 MEDIUM, varios LOW.

### Top 5 riesgos (priorizados)

| # | Riesgo | Severidad | Por qué importa |
|---|--------|-----------|-----------------|
| 1 | **Acoplamiento estático mutable cross-clase** (`import static` entre BattleController, BlackScreen, Events, Sans, Heart, ObjetsItems) | HIGH (F02, COUPLE-01, SOC-1) | Es la deuda dominante. Toca casi toda clase de gameplay; bloquea el testing y amplifica el riesgo de todo cambio. Punto de partida de los demás problemas. |
| 2 | **`new Texture(...)` por cada proyectil generado** (decode + subida a GPU por hueso/plataforma/blaster) | HIGH (TEX-001) | Fuente dominante de crecimiento de VRAM, hitches de frame por subida síncrona GL y presión de GC. Incluye una fuga parcial real (texturas de línea 61 sobreescritas sin disponer). |
| 3 | **Estado FSM de combate vive en `static` que sobrevive al Screen y se resetea solo parcialmente** | MEDIUM (STATE-01) | Hay un bug latente concreto (`isOptionAvailable` no se resetea en muerte durante act 8). Cualquier ruta de salida futura distinta de muerte/victoria arrastra estado sucio al siguiente combate. |
| 4 | **God class + FSM procedural: `BattleController`** (845 líneas, todo `static`, `generateAct()` = switch de 200 líneas) | MEDIUM (F01, F06) | Mayor hotspot de complejidad ciclomática y acoplamiento. Añadir un ataque = editar el switch central. Imposible de probar. |
| 5 | **Sin infraestructura de pruebas + persistencia no testeable** (`DataBase` 100% `static`, URI hardcodeada, bug de `.sort()` encadenado) | MEDIUM (TST-1, DA-1, DA-2) | Precondición que bloquea todas las demás mejoras. Además esconde un bug real: el ranking solo ordena por `score`, ignorando los desempates. |

---

## 2. Hallazgos por severidad

### HIGH

| Sev | Lente | Hallazgo | Evidencia (file:line) | Fix |
|-----|-------|----------|----------------------|-----|
| HIGH | OOP/design | **Estado estático mutable global vía `import static` (acoplamiento temporal)** | `BattleController.java:9-16`; `BlackScreen.java:24-46` (`public static score/sans/heart/boxHeart/...`); `Events.java:19-37`; cross-mutación: `Events.java:194` escribe `act`, `BattleController.java:209` escribe `score` | Encapsular estado en instancias inyectadas (constructor/método). Un único `BattleContext`. `VH_WIDTH/HEIGHT` → `Viewport`. (F02) |
| HIGH | State/lifecycle | **Acoplamiento estático cross-clase generalizado** (cada clase lee/escribe los estáticos de las demás) | `Events`↔`BattleController`↔`BlackScreen` se mutan mutuamente; `Sans.timeHead` escrito por dos clases; `ObjetsItems.java:210,216` un proyectil hace `bonesLeft/bonesRight.clear()` sobre el controlador | Invertir dependencias: `BattleState`/controlador es dueño de los campos; entidades reciben colaboradores por constructor. Cero `import static` de campos mutables. (COUPLE-01). *Nota: el riesgo de CME citado NO es real en la ruta actual (sin iteración activa, hilo único).* |
| HIGH | Perf/recursos | **Cada spawn de proyectil carga su Texture de disco** (decode + subida GPU por hueso/plataforma/blaster) | `ObjetsItems.java:61,69,98,115,125` (`new Texture(...)` en constructores); spawns en `BattleController.java:223-226,284-289,542-543` cada ~0.6-0.8s | `AssetManager`: cargar cada imagen una vez en `create()/show()`; `ObjetsItems` recibe `TextureRegion`/`Sprite` compartido o atlas. Nunca `new Texture` al spawnear. *Fuga adicional real: texturas de línea 61 sobreescritas en cases 0/5 nunca se disponen.* (TEX-001) |

### MEDIUM (agrupados por lente)

**Lente: OOP & design-smell**

| Sev | Hallazgo | Evidencia | Fix |
|-----|----------|-----------|-----|
| MEDIUM | **God class + FSM procedural: `BattleController`** | `BattleController.java:18-845` (clase entera `static`); `generateAct()` `642-842` switch de 200 líneas sobre `act` 1-8 | `Battle`/`BattleState` no estático; cada `case` → `AttackPhase` (Strategy, `update(delta)/render(Batch)`) en `List<AttackPhase>`, avanzada por índice. Inyectar Heart/BoxHeart/Sans. (F01) |
| MEDIUM | **Primitive obsession: `int act` (1-8) como código de fase** | `BattleController.java:19` (`act=4`); switch `643`; mutado en `Events.java:194,250,379,416,423`, `BlackScreen.java:60,70` | `enum BattlePhase` + `setPhase()`/`phase.next()`, switch exhaustivo, encapsular tras setter. (F03) |
| MEDIUM | **Primitive obsession: `int option` (0-4) y `int direction` (0-3) como strategy codes** | `Heart.java:23` (`option`), dispatch `184-190`; `BattleController.java:38` (`direction`), switches paralelos `49-83`/`138-159` | `option`→`enum` con `apply(Heart)` (enum-as-strategy, más liviano que interface). `direction`→reusar `Direction` enum existente, **con remapeo semántico cuidadoso** (3 convenciones int distintas en una clase). (F04) |
| MEDIUM | **God method / alta complejidad ciclomática: `generateAct()` + familia `move*Fast`** | `generateAct()` `642-842`; `bonesTab()` `105-197`; `Heart.moveGravityAppliedThrew()` `283-341` con 3 switches y cases vacíos `303-305,310-312,322-324` | Extraer cada `case` a método/phase object. Borrar cases vacíos. Reemplazar acumuladores `secondsTime` manuales por abstracción `Timer`/cooldown (`com.badlogic.gdx.utils.Timer`). (F06) |
| MEDIUM | **Feature envy: clamp "heart dentro de la caja" reimplementado en 4+ sitios** | `BattleController.throwHeart()` `51-80` (`heart.getSprite().getRegionWidth()`); duplicado en `Heart.move()/moveGravityApplied()` `196-206,258-263` y `drawPlatforms()` `568,592` | `Heart.clampWithin(BoxHeart)` / `moveBy(dx,dy,bounds)` (usa `MathUtils.clamp`). Constantes nombradas para `-15/-20/-30/-40`. *Los steps (+20/+5/+4) son velocidades intencionales, NO parte de la duplicación.* (F08) |
| MEDIUM | **`dispose()` llamado desde constructor; doble-dispose** | `BlackScreen.java:48-52` (ctor llama `dispose()` línea 51); `Undertale.java:109` + `switchScreen 52-57` + `hide() 209` = múltiples rutas de dispose | Nunca `dispose()` desde ctor. `AssetManager` con load/unload en `show()/hide()`. Cada Screen dispone solo lo que creó. Sacar la conexión DB de `show()`. *El dispose del ctor es casi no-op (campos null-guardados); el doble-dispose de texturas es real.* (F09) |

**Lente: Perf & gestión de recursos**

| Sev | Hallazgo | Evidencia | Fix |
|-----|----------|-----------|-----|
| MEDIUM | **`batch.begin()/end()` por cada draw individual** (destruye el batching) | `ObjetsItems.java:243-277` (drawPlatform/draw/drawBone/...); `FightOp.java:51-54`; `BlackScreen.java:146` (4 begin/end); `BattleController.java:328-335` (bucle por hueso) | Un `batch.begin()` por frame (o que `stage.draw()` lo posea). Helpers reciben `Batch` ya iniciado (como `Heart.draw(Batch,float)` y `Sans.draw` ya hacen). *Cuidado: no fusionar regiones de SpriteBatch con ShapeRenderer.* (BATCH-001/PERF-2) |
| MEDIUM | **Streams de Music recreados de disco en cada trigger; SFX cortos como Music** | `Sounds.java` métodos `78-182` (`Gdx.audio.newMusic(...)` por llamada); `selectSound()` disparado por cada tecla en `Heart.java:152,163,211,234` | Cargar SFX una vez vía `AssetManager` como `Sound`; reservar `Music` para temas largos. Disponer en un solo lugar. *Bug real: el `newMusic` no se dispone (el branch `if(!isPlaying())dispose()` no dispara en desktop), fuga de handles nativos por trigger.* (AUDIO-001) |
| MEDIUM | **Animation, `TextureRegion[]` y `Color` asignados en el loop de render/animación** | `Sans.java:157-225` (`new TextureRegion[6]`+`new Animation<>`); reconstruido en `BattleController.java:140-158` cada `isAnimationFinished()`; `ObjetsItems.java:166` (`new Color`), `171,178` (`new Rectangle`); `CollisionListener.java:41` | Construir cada `Animation` una vez (ctor de Sans o lazy-init). Cachear `Color`, mutar `.a`. Reusar `Rectangle` vía `rect.set(...)`. *Bursty (~cada 1.5s), no 60Hz — impacto menor pero anti-patrón real.* (GC-001) |
| MEDIUM | **`getFont()` fuga `FreeTypeFontGenerator` + `BitmapFont` en cada llamada** | `Undertale.java:45-50` (sobreescribe `generator` static sin disponer); llamado desde `BlackScreen:103`, `CreditsScreen:44/51`, `FabricElements:31/38/60/135`, `MainMenu:30`; `dispose()` `110` solo dispone el último | Generar cada tamaño una vez, cachear en `Map<Integer,BitmapFont>`, disponer el generator tras generar todo, disponer fuentes al salir. O `AssetManager` + `FreetypeFontLoader`. (FONT-001) |

**Lente: State management & lifecycle**

| Sev | Hallazgo | Evidencia | Fix |
|-----|----------|-----------|-----|
| MEDIUM | **Estado FSM de combate en `static` que sobrevive al Screen; reset incompleto en nuevo combate** | `BlackScreen.java:24` (`score`), `BattleController.java:19` (`act`); `show()` `122-131` NO resetea; resets solo en rutas terminales `54-64`/`66-73` | `BattleState` propiedad de BlackScreen, creado fresco en `show()`. Mover `act/score/nextAttack/optionSelected/isSparing/isOptionAvailable/boxAttack/...` a campos de instancia. *Bug live concreto y estrecho: `isOptionAvailable` (`Events.java:22`) solo se resetea en victoria, no en muerte durante act 8.* (STATE-01) |
| MEDIUM | **Comportamiento de Heart como strategy en `int option` switcheado dentro de `draw()`** | `Heart.java:23,182-192`; `option` seteado desde ~16 sitios (`BattleController` `645-803`, `Events` `205-431`, `GameOverScreen:59`=-1); setters de posición fuerzan `option=0` | `MovementBehavior` (FreeMove/GravityMove/ThrownMove/MenuNavigate/Inert) seteado explícitamente en entrada de fase; sacar polling de `draw()`. (STATE-04) |
| MEDIUM | **`switchScreen` dispone el Screen viejo y reusa singletons globales; un solo `Stage/Batch` global** | `Undertale.java:52-57`; cada `dispose()` hace `stage.clear()` sobre el `stage` global (`BlackScreen:222`, `MainMenu:182`, etc.); `CreditsScreen:36/137` y `HowToPlayScreen:37/153` reasignan el `stage` static | Cada Screen con su propio `Stage` creado en `show()`/ctor y dispuesto en `dispose()`; compartir solo el `SpriteBatch`. Ctors baratos (sin carga GL). (TRANS-01) |
| MEDIUM | **`dispose()` desde ctores + doble-invocado vía `hide()`+`switchScreen`** | `MainMenu.java:25`, `InputNameScreen.java:27`, `BlackScreen.java:51`; `GameOverScreen.render()` llama `image.dispose()` por frame (`:102`), luego `dispose()` lo dispone otra vez (`:160`) | Nunca `dispose()` en ctor; adquirir en `show()`, disponer una vez, idempotente (null tras disponer). Quitar el `image.dispose()` por frame. *`Texture.dispose()` repetido es no-op GL silencioso, no crash.* (TRANS-02) |
| MEDIUM | **Tres mecanismos de input compitiendo; `InputProcessor` swapeado cada frame; `KeyListener` no-op** | `BlackScreen.java:130` (`setInputProcessor(stage)`), luego `:138` lo cambia a `KeyListener.get()` CADA frame; `KeyListener.java:22-29` devuelve solo DOWN/UP; latch `canSelect` reseteado en `Events.java:488` (ENTER+ESCAPE) vs `BlackScreen.java:165` (solo ENTER) | Un solo `InputProcessor`/`InputMultiplexer` seteado una vez en `show()`. `keyDown` para acciones edge (borra `canSelect`), `isKeyPressed` solo para movimiento sostenido. Borrar `KeyListener` y el `setInputProcessor` por frame. (INPUT-01) |

**Lente: Data access & testability**

| Sev | Hallazgo | Evidencia | Fix |
|-----|----------|-----------|-----|
| MEDIUM | **`DataBase` 100% static, URI hardcodeada, conexión por operación — no mockeable** | `DataBase.java:20-31` (todo `static`, `"mongodb://localhost:27017"`); `:41` `createConnection()` dentro de `updatePlayerStats` | `RankingRepository` interface + `MongoRankingRepository(MongoDatabase)` inyectado desde config. Mockito para callers, Testcontainers para integración. *Fuga: cada write sobreescribe `mongoClient` sin cerrar el anterior.* (DA-1) |
| MEDIUM | **Lógica de dominio (win-rate, high-score, sort) fusionada en persistencia con `Document` crudos** | `DataBase.java:61-71` (win-rate inline, ternario best-score), `:43` (formato tiempo), `:86-94` (`.sort()` encadenado), `FabricElements.java:150-161` (keys string) | `record PlayerStats` + `StatsCalculator` puro (tests JUnit5). **BUG REAL: las 4 llamadas `.sort()` encadenadas se sobreescriben — solo aplica `descending("score")`, los desempates se pierden.** Fix: `Sorts.orderBy(...)` en una sola llamada. (DA-2) |
| MEDIUM | **Sin infraestructura de pruebas** (sin `src/test`, sin deps JUnit/Mockito/Testcontainers/headless-gdx, sin task) | Glob `**/src/test/**` → vacío; `core/build.gradle:4-14` sin `testImplementation`; root `build.gradle` solo sonarqube | Añadir deps + `test { useJUnitPlatform() }`. Crear `core/src/test/java`. (TST-1) |
| MEDIUM | **Estado estático mutable hace todo componente no instanciable/aislable en test** | `BlackScreen.java:24-46`, `BattleController.java:19`, `Events.java:19-37`; `VH_WIDTH/HEIGHT` (`:45-46`) init desde `Gdx.graphics` en class-load → **NPE al solo cargar la clase en test** | `BattleContext`/`GameState` inyectado; métodos de instancia. `VH` como `Viewport` pasado, no derivado de Gdx en class-init. (SOC-1) |
| MEDIUM | **Lógica de score/win-rate entrelazada con `Gdx.input` y mutación de Stage en el mismo método static** | `Events.java:169-199` (`score += ...` junto a `Gdx.input`, `stage.addActor`); `BattleController.java:209-211` | `ScoreCalculator.hitScore(HitZone, act)`/`turnBonus(act, hp)` puros. **BUG REAL: `Events.java:176-179` el `score += 300` corre incondicionalmente; el `+200` está gateado por `== 273` float-equality (código muerto).** (SOC-2) |
| MEDIUM | **Efectos de construcción (`new Texture/Stage`, `dispose()` en ctor) impiden crear objetos en test** | `ObjetsItems.java:61,69,98,115,125`; `Heart.java:59-64`; `BlackScreen.java:48-52`; `Undertale.java:45-49,82-96` | Inyectar `Texture`/`TextureRegion` (o `AssetManager`/`TextureProvider`) en ctores. En tests headless, `Gdx.gl = mock(GL20.class)`. Sacar `dispose()` de ctores. (SOC-3) |
| MEDIUM | **Fuga VRAM/GC por textura-por-proyectil + dispose por delete** | `ObjetsItems.java:61,...,125`; `:339-341` `delete()` dispone `getTexture()` | `AssetManager`/cache `TextureRegion`; ctores reciben `TextureRegion`; `delete()` solo remueve. *El "use-after-dispose de textura compartida" NO ocurre hoy (cada objeto tiene su propia Texture); fuga real pero sin corrupción.* (PERF-1) |
| MEDIUM | **`Sounds` servicio all-static llama `Gdx.audio` en cada método** | `Sounds.java:7` (15 `static Music`), métodos `9-182`; consumido vía `import static` en Events/BattleController/BlackScreen | `SoundService` interface inyectado; impl libGDX en prod, mock Mockito en tests. *Bug play-then-dispose presente en 9 métodos (más de los citados).* (SOC-4) |

### LOW (resumen)

| Sev | Hallazgo | Evidencia | Fix |
|-----|----------|-----------|-----|
| LOW | **Primitive obsession: `int mode` (0/1/2) FSM implícito en `BoxHeart`** | `BoxHeart.java:15`; `changeDimensions*` `38-84`; leído en `BattleController.java:715,759`, `Events.java:440-480` | `enum BoxState {OPEN, MID, SQUARE}` + `isSquare()/isOpen()`. *Ningún externo escribe `mode`; clase de 85 líneas.* (F05) |
| LOW | **`delete()` por objeto dispone una textura "compartida"** (footgun latente) | `ObjetsItems.java:339-341`; siblings en `BarAttack:144`, `BoxAttack:68`, `Hit:54` | Quitar dispose de `delete()`; centralizar en `AssetManager`. *Daño condicional a un refactor de compartición que no existe aún.* (TEX-002) |
| LOW | **`verifyRegion()` puede asignar `new Texture` en gameplay** | `BarAttack.java:64-69`; +copias en `BoxAttack:19`, `Hit:21`, `MissLabel:24` | Compartir un `HitSprite` vía AssetManager. *El branch de fuga está guardado por `getTexture()==null`, inalcanzable hoy = código muerto.* (TEX-003) |
| LOW | **`act` FSM no lineal; fase 4 es start state Y ataque de mid-game; `+1` sin bound check** | `BattleController.java:19,642-841`; `Events.java:194,423`; score acoplado al ordinal (`:209`, `Events.java:192`) | `enum BattlePhase` + tabla de transición; desacoplar score del ordinal. *El soft-lock por `act≥9` NO es alcanzable (clamp incidental vía `isSparing`).* (STATE-02) |

---

## 3. Mapa de acoplamiento / estado global

### El grafo de estado

El combate es una FSM implícita repartida sobre **estado `static` mutable** que se consume vía `import static` en una malla de dependencias sin dirección ni propiedad:

```
                 ┌─────────────────────────────────────────────┐
                 │  BlackScreen (static: score, heart, sans,    │
                 │   boxHeart, fightOp/actOp/itemOp/mercyOp,    │
                 │   VH_WIDTH, VH_HEIGHT)                        │
                 │   render() → update() → ramifica heart.isTurn│
                 └───────┬─────────────────────────┬───────────┘
            escribe act/score │          │ escribe score, isSparing, i, optionSelected
            (60,70,71)        │          │ (genera turno enemigo)
                 ┌────────────▼───┐  ┌───▼──────────────────────┐
   escribe act → │     Events     │←→│   BattleController        │
   (194,250,...) │ (static: i,    │  │ (static: act=4,           │
   escribe score │  optionSelected│  │  nextAttack, direction,   │
   (179-192)     │  canSelect,    │  │  bonesLeft/Right/Up/Down) │
                 │  isSparing,hit)│  │  generateAct() switch 1-8 │
                 └───────┬────────┘  └───┬───────────────┬───────┘
            lee Events.* │   lee BC.*    │ escribe       │ lee/escribe
                 ┌───────▼───────┐   ┌───▼────────┐  ┌───▼──────────────┐
                 │     Heart     │   │ ObjetsItems│  │ Sans (static:    │
                 │ (lee BlackScr,│   │ (lee BC.*, │  │  timeHead) ←─────┤ escrito por BC
                 │  Events.items)│   │  BlackScr.*│  │  lee act (321)   │ (722,730,...)
                 └───────────────┘   │  CLEAR bonesLeft/Right (210,216) ◄── inversión
                                     └────────────┘     de propiedad
```

### El problema del static global

1. **Sin propiedad ni dirección.** No hay un dueño del estado. `act` vive en `BattleController` pero lo escriben `Events` (5 sitios), `BlackScreen` (2 sitios) y lo lee `Sans`. `score` vive en `BlackScreen` pero lo escriben `Events` y `BattleController`. `Sans.timeHead` lo escriben dos clases externas. Razonar sobre cualquier campo exige grep en 3-5 archivos.

2. **Acoplamiento temporal severo.** La corrección depende del orden implícito `BlackScreen.render() → update() → (generateAct() | detectEvent())`. La condición de victoria requiere `act==8` **Y** `isSparing` **Y** `timeHead` avanzado **Y** `isOptionAvailable` — cuatro campos en cuatro clases distintas mutados desde una quinta.

3. **Inversión de propiedad.** `ObjetsItems` (un proyectil, una hoja del árbol) hace `BattleController.bonesLeft.clear()` / `bonesRight.clear()` (`ObjetsItems.java:210,216`) — una entidad muta la colección de su propio contenedor. *(Nota: el riesgo de `ConcurrentModificationException` citado NO se materializa — no hay iteración activa en esa pila de llamadas, y el loop de render de libGDX es de hilo único.)*

4. **NPE en class-load bajo test.** `VH_WIDTH/VH_HEIGHT` se inicializan desde `Gdx.graphics.getWidth()` en class-init (`BlackScreen.java:45-46`). Cualquier clase con `import static com.game.BlackScreen.*` (Heart, Events, ObjetsItems, BattleController) lanza NPE con solo cargarse en un test sin backend Gdx activo.

5. **Estado que sobrevive al Screen.** Los `static` no se resetean al crear un `new BlackScreen`. Hoy las dos rutas terminales (muerte/victoria) resetean lo crítico (`act`, `score`, `isSparing`), pero el reset es incompleto: `isOptionAvailable` solo se resetea en victoria → un combate perdido durante act 8 envenena el siguiente que llegue a act 8 en el mismo proceso.

**Objetivo del refactor:** cero `import static` de campos mutables entre clases de gameplay. Un único `BattleContext`/`BattleState` dueño de los campos, creado fresco en `show()`, inyectado por constructor a las entidades. `VH_WIDTH/HEIGHT` → `Viewport`/`LayoutConfig`.

---

## 4. Patrones a aplicar

**Decisión global de patrones:** OOP + GoF, **NO ECS** (ver §5). libGDX Scene2D (`Actor`/`Stage`/`act`/`draw`) ya es un runtime liviano tipo componente — inclinarse hacia él (mover lógica a `Actor.act(delta)`, dejar que `Stage` posea el batching) en vez de añadir un segundo framework de entidades.

| Prioridad | Patrón | Clases objetivo | Esfuerzo | Before → After |
|-----------|--------|-----------------|----------|----------------|
| **P0** | **Shared Assets (Flyweight) — AssetManager + TextureAtlas + FontCache** | NEW `AssetService`/`Assets` (carga en `Undertale.create`); `FontCache` (`Map<Integer,BitmapFont>`); `ObjetsItems`, `Sans.image`, `BlackScreen`, `GameOverScreen`, `Sounds` | M | `new Sprite(new Texture("images/SansSprite.png"))` por spawn → `new ObjetsItems(kind,x,y,dir, atlas.findRegion("sansSprite"))`; `delete(){ remove(); }` (textura dispuesta una vez en `AssetManager.dispose()`) |
| **P0** | **Borrado de código muerto + strip de deps** | DELETE `Prueba`, `TestScreen`, `BodyFabric`; remover `gdx-ai/box2d/bullet/reactivestreams` de `core/build.gradle`; quitar FSM comentada en `BlackScreen.update`, `System.out.println` en `Events` | S | `api "...gdx-box2d"` + clases scratch → removidas; `CollisionListener` (Intersector) queda como única autoridad de colisión |
| **P1** | **Manual DI vía `BattleContext`** | NEW `class BattleContext { Heart; BoxHeart; Sans; BattlePhase; int score; ... }` creado fresco en `BlackScreen.show()`; convertir métodos static de `Events`/`BattleController` a instancia; remover `public static` | L | `import static com.game.BlackScreen.*; score += act*100;` → `ctx.addScore(ctx.phase().scoreBonus());` con `new BattleController(new BattleContext(...))`. **NO Dagger/Guice — overkill.** |
| **P1** | **Repository + Domain Model (DTO)** | NEW `record PlayerStats(...)`; `interface RankingRepository`; `MongoRankingRepository(MongoDatabase)`; NEW `StatsCalculator`; DELETE `DataBase`; consumidores `BlackScreen:59/69`, `FabricElements:146-162` | L | `DataBase.updatePlayerStats(...)` (abre+consulta+calcula inline) → `repo.save(stats.applyResult(prev, score, dur, won))` |
| **P1** | **Object Pool (`Pool<ObjetsItems>`)** | NEW `ObjetsItemsPool extends Pool<ObjetsItems>`; `ObjetsItems implements Pool.Poolable` con `reset()`; sitios de spawn en `BattleController` (`223-226,244,284-289,542`) | M | `bonesLeft.add(new ObjetsItems(...))` + `bone.delete()` → `pool.obtain().init(...)` + `pool.free(bone)`. **Requiere AssetManager primero** (objetos pooleados no deben disponer texturas) |
| **P2** | **Single InputProcessor + InputState** | DELETE `KeyListener`; un `InputProcessor`/`InputMultiplexer` en `show()`; `keyDown` para ENTER/ESCAPE (borra `canSelect`); `isKeyPressed` solo movimiento sostenido | M | `render(){ setInputProcessor(KeyListener.get()); if(isKeyPressed(ENTER)&&canSelect){...;canSelect=false;} }` → `show(){ setInputProcessor(input); } input.keyDown(ENTER){ ctx.confirm(); }`. **Command pattern NO aún warranted** (un solo teclado) |
| **P2** | **Strategy para movimiento de Heart + encapsular bounds** | NEW `interface MovementBehavior`; impls FreeMove/Gravity/Thrown/MenuNavigate/Inert; `Heart.setBehavior(...)`; NEW `Heart.clampWithin(BoxHeart)` + constantes para márgenes | M | `switch(option){ case 2 → moveGravityApplied(); }` en `draw()` → `behavior.update(this, delta, input)` en `act(delta)`; `draw()` solo `batch.draw(...)` |
| **P2** | **SoundService interface** | NEW `interface SoundService`; `LibGdxSoundService` (AssetManager: `Sound` para SFX, `Music` para temas); inyectar vía `BattleContext`; reemplazar `import static com.game.Sounds.*` | M | `selectSound(); // new Music de disco` → `sound.playSelect(); // assets.get(...,Sound.class).play()`. Arregla play-then-dispose |
| **P2** | **Timing frame-rate-independent + Cooldown helper** | NEW `Cooldown/Timer`; `ObjetsItems.move*`, `Heart.move*` (`196+,284-337`); generate* en `BattleController` | M | `setX(getX()-7)` + `secondsTime += deltaTime; if(secondsTime>0.7){...}` → `setX(getX()-SPEED*delta)` + `if(spawnCooldown.ready(delta)) spawn()` |
| **P3** | **State pattern para fases de combate** | NEW `interface BattlePhase { onEnter; update; next; }` con clases por fase (IntroPhase…FinalePhase); `BattleController` tiene `BattlePhase current` en vez de `int act` | L | `switch(act){ case 4 → {...generateGastersBlaster();} } act=act+1;` → `current.update(delta); current=current.next();`. **Después de BattleContext+Pool+InputState** |
| **P3** | **State pattern para `BoxHeart.mode`** | `BoxHeart`: `private enum BoxState {OPEN,MID,SQUARE}` + `isOpen()/isSquare()`; colapsar `changeDimensions*` en `resizeTowards(target, delta)` | S | `if(mode==0||mode==1){...; mode=2;}` + externo `boxHeart.mode==0` → `box.resizeTowards(SQUARE, delta)` + `box.isOpen()` |
| **P4** | **Test infrastructure** | `core/build.gradle` (JUnit5/Mockito/headless-gdx/Testcontainers); `core/src/test/java/com/game` | M | (sin tests) → suite por capas (ver §6) |

---

## 5. Decisión: ECS vs OOP + patrones

### Recomendación: **quedarse con OOP + patrones GoF. NO adoptar Ashley ni ningún ECS.**

**Fundamento, anclado en el código confirmado:**

- Es un **boss-fight de un solo enemigo, una sola alma**, con un vocabulario de entidades diminuto y fijo: un `Heart`, un `Sans`, un `BoxHeart`, y proyectiles homogéneos (huesos/plataformas/blasters).
- ECS gana su valor cuando hay **muchas entidades heterogéneas con mezclas combinatorias de componentes** y sistemas iterando sobre archetypes grandes para updates data-oriented cache-friendly. **Nada de eso aplica aquí.** El conteo de proyectiles es pequeño (un puñado por tick) y comparten una sola forma de comportamiento.
- La única variabilidad tipo "componente" (el movimiento) la cubre completamente un **Object Pool + Strategy `MovementBehavior`**.
- **El dolor real NO es un modelo de componentes faltante.** Es: (1) acoplamiento estático mutable generalizado, (2) FSMs por primitive-obsession (`int act`/`option`/`mode`/`direction`), (3) carga de textura por spawn. **ECS no arregla los estáticos ni la fuga de assets** y añadiría un framework pesado, un nuevo modelo mental y un rewrite grande por cero beneficio de gameplay — patrón por el patrón.
- Los fixes de la auditoría (State, Strategy, Pool, AssetManager, BattleContext) son precisamente el camino OOP-con-patrones, y son mucho menor riesgo.
- **Matiz clave:** libGDX Scene2D ya ES un runtime liviano tipo componente (`Actor/Stage/act/draw`). El código debería **inclinarse hacia él** (mover lógica a `Actor.act(delta)`, dejar que `Stage` posea el batching) en lugar de atornillar un segundo framework de entidades encima.

**Veredicto:** OOP + los patrones priorizados de §4. Revisar ECS **solo si** el juego crece luego a muchos tipos distintos de enemigos/balas con combinaciones variadas de componentes.

---

## 6. Estrategia de testing en capas

### 6.1 Wiring de Gradle (`core/build.gradle`)

```gradle
dependencies {
  // JUnit 5 (BOM alinea engine+api+params)
  testImplementation platform('org.junit:junit-bom:5.10.2')
  testImplementation 'org.junit.jupiter:junit-jupiter'
  testRuntimeOnly   'org.junit.platform:junit-platform-launcher'   // Gradle 8.6 lo necesita explícito

  // Mockito
  testImplementation 'org.mockito:mockito-core:5.11.0'
  testImplementation 'org.mockito:mockito-junit-jupiter:5.11.0'

  // Headless libGDX (HeadlessApplication + mock GL)
  testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
  testImplementation "com.badlogicgames.gdx:gdx:$gdxVersion"
  testRuntimeOnly   "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"  // REQUERIDO: HeadlessApplication necesita natives

  // Mongo integración (Testcontainers, primario; Docker es el stack elegido)
  testImplementation platform('org.testcontainers:testcontainers-bom:1.19.7')
  testImplementation 'org.testcontainers:testcontainers'
  testImplementation 'org.testcontainers:junit-jupiter'
  testImplementation 'org.testcontainers:mongodb'
  // Fallback sin Docker: de.flapdoodle.embed:de.flapdoodle.embed.mongo:4.12.2 (gate por @Tag)
}

test {
  useJUnitPlatform()
  testLogging { events 'passed', 'skipped', 'failed' }
  jvmArgs '-XX:+EnableDynamicAgentLoading'   // JDK 21 + Mockito/ByteBuddy
}
```

**Layout por tags:** todo bajo `core/src/test/java` espejando `com.game`/`listener`. `@Tag("unit")` (sin Docker/GL), `@Tag("headless")` (boota HeadlessApplication), `@Tag("integration")` (Testcontainers Mongo), `@Tag("e2e")`.

### 6.2 Harness headless (`core/src/test/java/com/game/GdxHeadlessExtension.java`)

Extensión JUnit5 que bootea `HeadlessApplication` una vez e instala un GL20/GL30 mockeado para que `Texture/SpriteBatch/Stage` no hagan NPE.

```java
public class GdxHeadlessExtension implements BeforeAllCallback {
  private static volatile boolean started = false;
  @Override public synchronized void beforeAll(ExtensionContext ctx) {
    if (started) return;
    HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
    cfg.updatesPerSecond = -1;
    new HeadlessApplication(new ApplicationAdapter() {}, cfg);
    GL20 gl = mock(GL20.class);
    when(gl.glGenTexture()).thenReturn(1);          // handle no-cero
    Gdx.gl = gl; Gdx.gl20 = gl; Gdx.gl30 = mock(GL30.class);
    ((MockGraphics) Gdx.graphics).setWindowSize(1280, 720);  // VH_WIDTH=12.8, VH_HEIGHT=7.2 deterministas
    started = true;
  }
}
```

**ORDEN CRÍTICO:** `BlackScreen.java:45-46` inicializa `VH_WIDTH = Gdx.graphics.getWidth()/100` en class-load, y BoxHeart/BarAttack/BoxAttack/Heart hacen `import static com.game.BlackScreen.*` — la extensión DEBE correr antes de que cualquier clase de esas se cargue.

**Caveats a documentar:** (1) la subida de píxeles real se omite (GL mockeado) — asertar sobre geometría/hitbox/estado, no píxeles. (2) El estado static sobrevive al JVM → ordenamiento frágil entre tests hasta que aterrice STATE-01; usar `@TestInstance(PER_METHOD)`, no compartir estáticos de BlackScreen entre clases de test. (3) Tests con `new Texture("...")` necesitan assets en el classpath — diferir hasta el refactor de AssetManager.

### 6.3 Targets unitarios (puros — extraer primero la función pura)

| Target | Extraer de | Casos clave |
|--------|-----------|-------------|
| `ScoreCalculator.hitScore(HitZone, act)` | `Events.java:174-192` | CENTER@act4 → 700; GREEN@act1 → 250; RED@act8 → 815; MISS → solo `act*100`. **Documentar/matar el bonus `==273`** |
| `WinRateCalculator.winRate(won, played)` | `DataBase.java:70` | 1/1→100.0; 1/3→33.33; 0/5→0.0; **0/0 no debe lanzar (div-by-zero — guardar)** |
| `BestScoreCalculator.bestScore(existing, incoming)` | `DataBase.java:67` | 500 vs 300→500; 200 vs 300→300; 300/300→300 (`>` estricto mantiene el existente) |
| `DurationFormatter.format(Duration)` | `DataBase.java:43` | 0s→`00:00:00`; 3661s→`01:01:01`; 59s→`00:00:59` |
| `RankingComparator orderBy(...)` | `DataBase.java:86-92` | **Este test FALLA contra el código actual y documenta el bug del `.sort()` encadenado.** Empate en score rompe por winRate, etc. |
| `BoxResize.resizeStep(width, target, step)` | `BoxHeart.java:38-84` | clamp + flip de mode SQUARE/MID/OPEN; deltaX=(prev-new)/2 mantiene centrado |
| `BattlePhase.next()` | `BattleController.java:194` | act4 no-sparing→5; sparing→8; `+1` desde 8 overflow (bug a fijar) |
| `TurnBonus.hpBonus(hp)` | `BattleController.java:211` | hp90→100; hp45→50; hp0→0 |
| `Direction` mapping | `BattleController.java:38` vs `Direction.java` | cada ordinal mapea al enum esperado y vuelta; fuera de rango lanza |
| `CollisionGeometry.overlaps(Circle, Rectangle)` | `listener/CollisionListener.java:41` | **Pura (gdx-math, sin GL) — corre SIN la extensión.** Dentro→true; tocando borde→true; disjunto 1px→false |
| `selectUpsert(existingDoc?, won)` | `DataBase.java:45` | null+won→INSERT_WIN; null+loss→INSERT_LOSS rate=0; existing+won→UPDATE |

### 6.4 Targets de integración headless

- **BoxHeart resize como Actor** (`@ExtendWith(GdxHeadlessExtension)`): construir `BoxHeart`, llamar `changeDimensionsMin/Max/MinSquare()` y asertar `getWidth()/getX()/mode` alcanzan los clamps y la caja queda centrada. `draw()` NO se llama (toca shapeRenderer+glLineWidth). **Corre AHORA.**
- **Stage add/remove lifecycle**: añadir `BarAttack` a un Stage headless, correr `stage.act(delta)`, asertar que se auto-remueve al llegar al borde (`BarAttack.java:108/113`) y `getStage()==null`. **Corre AHORA.**
- **BoxAttack zone geometry**: asertar que los 7 Rectangles anidados (`BoxAttack.java:27-35`) teselan contiguos y un BarAttack centrado solapa solo `hitBoxCenter`. **Corre AHORA.**
- **Heart.move() clamping**: BLOQUEADO para aserción limpia hasta INPUT-01 (input desacoplado de `draw()`); parcialmente posible llamando `move()` directo.
- **AssetManager cache (cache size == 1 tras N spawns)**: BLOQUEADO hasta PERF-1 (ObjetsItems recibe `TextureRegion` inyectada).
- **SoundService verify**: BLOQUEADO hasta SOC-4 (Sounds → interface inyectada).

### 6.5 Integración Mongo (Docker / Testcontainers)

**BLOQUEADO hasta refactor de `DataBase` → `RankingRepository` + `MongoRankingRepository(MongoDatabase)` inyectado.**

```java
@Tag("integration") @Testcontainers
class MongoRankingRepositoryIT {
  @Container static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
  MongoRankingRepository repo;
  @BeforeEach void setUp() {
    MongoDatabase db = MongoClients.create(mongo.getConnectionString()).getDatabase("Undertale");
    db.getCollection("Ranking").drop();
    repo = new MongoRankingRepository(db);
  }
  @Test void insertsNewPlayerOnFirstWin() { /* score=500, played=1, winRate=100 */ }
  @Test void keepsBestScoreOnUpdate() { /* pin DataBase.java:67: 500 no sobreescrito por 200; winRate 50 */ }
  @Test void topTenSortedByScoreThenWinRate() {
    // EXPONE el bug de .sort() encadenado de DataBase.java:86-92.
    // El repo debe usar Sorts.orderBy(descending("score"), descending("win rate"),
    //   descending("amount played"), ascending("time played"))
    assertEquals(List.of("B","C","A"), top.stream().map(PlayerStats::name).toList());
  }
  @Test void roundTripDocumentMapping() { /* save → findByName → todos los campos iguales */ }
}
```

Cubrir: first-win insert, first-loss insert (rate 0.0), best-score upsert, win-rate recompute en N-ésima jugada, formato de tiempo persistido, `topTen` límite==10 y orden correcto. Para `docker-compose`: inyectar URI vía `System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017")`. Gate con `assumeTrue(DockerClientFactory.instance().isDockerAvailable())` para skip (no fail) sin Docker.

### 6.6 E2E (headless con input scripteado)

Driver: `HeadlessApplication` + `InputProcessor` programable + loop que llama el path update/render N veces con `delta=1/60f`. Asertar **estado, no píxeles**: transición a GameOverScreen/Win, `score>0`, progresión `act 4→5`, `heart.getHp()` decrece en colisión y muerte en hp≤0.

**Caveats fuertes:** (a) el loop poll-ea `Gdx.input.isKeyPressed` en `draw()` con latch `canSelect` manual — timing sensible al conteo de frames; asertar sobre estado tras periodo quiescente, nunca sobre frame exacto. (b) **BLOQUEADO hasta STATE-01 + INPUT-01** (sin ellos la corrida no es reproducible). (c) Muchas transiciones gatean sobre flags animation-finished que dependen de acumulación de `stateTime` — avanzar suficientes frames. (d) Mantener e2e a 1-2 smoke tests happy-path; deep-assert en capas unit/integration.

### 6.7 CI (`.github/workflows/build.yml`)

1. **Bump JDK 17 → 21** (los subprojects fuerzan toolchain 21 + `sourceCompatibility=21` — la CI ya está inconsistente y romperá al compilar tests). `actions/setup-java@v4` (temurin, 21, `cache: gradle`). Reemplazar `actions/cache@v1` por `gradle/actions/setup-gradle@v3`.
2. **Servicio Mongo** para integración (Testcontainers usa el Docker propio del runner en ubuntu-latest; el bloque `services` cubre el path docker-compose). Exportar `MONGO_URI`.
3. **Sin xvfb** — GL está mockeado en la extensión, gdx-backend-headless no necesita display.
4. **Split fast/slow** vía `excludeTags`/`includeTags`. Publicar resultados con `mikepenz/action-junit-report@v4` leyendo `core/build/test-results/test/*.xml`.
5. **Correr tests ANTES de Sonar**; añadir JaCoCo (`id 'jacoco'` + `jacocoTestReport`, `sonar.coverage.jacoco.xmlReportPaths`).

### 6.8 Matriz de bloqueo (qué test depende de qué refactor)

| Capa de test | Estado | Desbloqueado por |
|--------------|--------|------------------|
| Unit (ScoreCalculator, WinRate, BestScore, DurationFormatter, TurnBonus, BattlePhase, Direction, CollisionGeometry) | **Corre ya** (tras extraer la función pura — refactor mecánico mínimo) | — |
| Headless: BoxHeart resize, Stage add/remove, BoxAttack geometry | **Corre ya** | GdxHeadlessExtension |
| Headless: Heart.move() clamping limpio | Bloqueado | INPUT-01 (input fuera de draw) |
| Headless: AssetManager cache==1 | Bloqueado | PERF-1 (TextureRegion inyectada) |
| Headless: SoundService verify | Bloqueado | SOC-4 (Sounds → interface) |
| Integración Mongo (todo) | Bloqueado | DA-1 + DA-2 (RankingRepository + PlayerStats + StatsCalculator) |
| E2E reach-GameOver / reach-Win | Bloqueado | STATE-01 + INPUT-01 + (TRANS-02 dispose cleanup) |

---

## 7. Plan de refactorización por fases

### FASE 0 — Harness de testing + caracterización
- **Objetivo:** poder ejecutar tests y fijar el comportamiento actual antes de tocar arquitectura.
- **Cambios:** wiring Gradle (§6.1); `GdxHeadlessExtension`; bump CI JDK 17→21 + modernizar actions; extraer funciones puras mecánicamente (`ScoreCalculator`, `WinRate`, `BestScore`, `DurationFormatter`, `TurnBonus`, `RankingComparator`); escribir sus tests unitarios.
- **Tests que habilita:** toda la capa unit pura + headless de BoxHeart/Stage/BoxAttack. El test de `RankingComparator` documenta el bug del `.sort()`; el de score documenta el bug del `==273`.
- **Riesgo:** muy bajo (solo añade tests, extracción mecánica). **Esfuerzo:** M.

### FASE 1 — Quick wins de recursos/perf (sin cambio arquitectónico)
- **Objetivo:** detener las fugas peores y el código muerto, encogiendo la superficie del refactor.
- **Cambios:** (P0) borrar `Prueba`/`TestScreen`/`BodyFabric` + strip `gdx-ai/box2d/bullet/reactivestreams` + FSM comentada + `System.out.println`; cachear fuentes en `Undertale.getFont` (FONT-001); construir Animations una vez en Sans (GC-001); cachear `Color`/`Rectangle` en ObjetsItems/CollisionListener; quitar `image.dispose()` por frame en `GameOverScreen.render` (TRANS-02); borrar cases vacíos en `moveGravityAppliedThrew` (F06).
- **Tests que habilita:** nada nuevo, pero limpia la superficie y reduce ruido para Sonar.
- **Riesgo:** bajo (cambios localizados, sin tocar el grafo de estado). **Esfuerzo:** S-M.

### FASE 2 — Repository + DI (DataBase) + AssetManager base
- **Objetivo:** aislar persistencia y assets — el trabajo de mayor valor que es independiente del refactor de gameplay y puede ir en paralelo.
- **Cambios:** (P1) `record PlayerStats` + `StatsCalculator` puro + `RankingRepository`/`MongoRankingRepository(MongoDatabase)` inyectado, URI desde env; arreglar el `Sorts.orderBy(...)` en una sola llamada (DA-2); borrar `DataBase`. (P0) introducir `AssetManager`/`Assets` cargando atlas/fuentes/sonidos una vez en `Undertale.create`.
- **Tests que habilita:** integración Mongo completa (Testcontainers, §6.5); StatsCalculator unit; cache de assets headless tras adaptar ObjetsItems.
- **Riesgo:** medio (Repository es aislado; AssetManager toca muchos sitios de carga pero mecánicamente). **Esfuerzo:** L.

### FASE 3 — BattleContext + DI de gameplay (la keystone)
- **Objetivo:** matar el acoplamiento estático mutable y el bug de reset-en-nuevo-combate.
- **Cambios:** (P1) `BattleContext` dueño de `heart/boxHeart/sans/score/act/optionSelected/...`, creado fresco en `BlackScreen.show()`; convertir métodos static de `Events`/`BattleController` a instancia; remover `public static`; `VH_WIDTH/HEIGHT` → `Viewport`/`LayoutConfig`. (P2) Single InputProcessor + InputState, borrar `KeyListener` y `setInputProcessor` por frame (INPUT-01); arreglar lifecycle de Screen (dispose fuera de ctor, Stage por Screen — TRANS-01/02).
- **Tests que habilita:** Heart.move() clamping headless; e2e reproducible (reach-GameOver / reach-Win); elimina la fragilidad de ordenamiento entre tests por estado static.
- **Riesgo:** **alto** (rewrite de Events/BattleController/Heart/BlackScreen — el cambio más grande). Mitigado por los tests de caracterización de FASE 0. **Esfuerzo:** L.

### FASE 4 — State/Strategy/Pooling
- **Objetivo:** payoff estructural — eliminar las FSMs por primitive-obsession y la churn de GC.
- **Cambios:** (P3) State pattern para fases (`int act` → `BattlePhase`), desacoplar score del ordinal; State para `BoxHeart.mode` (enum + `resizeTowards`). (P2) Strategy `MovementBehavior` para Heart + `clampWithin` + márgenes nombrados (F04/F08); timing delta-scaled + `Cooldown` helper. (P1) `Pool<ObjetsItems>` (requiere AssetManager de FASE 2). (P2) `SoundService` interface.
- **Tests que habilita:** SoundService verify (Mockito); tests de transición de fase contra el enum; pool size assertions.
- **Riesgo:** medio-alto (las fases dependen de colaboradores limpios — por eso van DESPUÉS de FASE 3). **Esfuerzo:** L.

### FASE N — Consolidación de testing + batching
- **Objetivo:** cerrar la cobertura y la última optimización de render.
- **Cambios:** (P2/BATCH-001) hoistear `batch.begin()/end()` al caller alrededor de los loops; helpers reciben `Batch`. Completar suite e2e (1-2 smoke). Wire JaCoCo→Sonar. Lock-in de cada seam con tests a medida que aterriza.
- **Tests que habilita:** draw-count assertions con mock batch; cobertura E2E happy-path.
- **Riesgo:** bajo. **Esfuerzo:** M.

---

## 8. Skills a crear en `.claude/skills/`

| Skill | Qué enforza (una frase) |
|-------|-------------------------|
| `libgdx-resource-management` | Toda `Texture`/`BitmapFont`/`Sound`/`Music` se carga una sola vez vía `AssetManager` y se dispone exactamente una vez al cierre — nunca `new Texture`/`newMusic` en constructores, loops de spawn o métodos de play. |
| `libgdx-asset-pooling` | Entidades transitorias (proyectiles, labels) implementan `Pool.Poolable` y se reciclan con `obtain()/free()` — `delete()`/remove nunca toca recursos GL ni la colección del controlador. |
| `libgdx-state-pattern` | Las máquinas de estado de fase/comportamiento se modelan con `enum`/`State` polimórfico y transiciones explícitas (`next()`/`setPhase`), nunca con `int`+`switch` ni `+1` sin bound check; el score se desacopla del ordinal. |
| `libgdx-strategy-movement` | El comportamiento seleccionable (movimiento del Heart) es un `MovementBehavior`/enum-strategy inyectado, no un `int option` switcheado dentro de `draw()`. |
| `libgdx-no-static-state` | El estado de gameplay vive en instancias inyectadas por constructor (un `BattleContext`/`GameState` propiedad del Screen) — cero `import static` de campos mutables entre clases; `VH_WIDTH/HEIGHT` viven en un `Viewport`/`LayoutConfig`. |
| `libgdx-screen-lifecycle` | Los recursos se adquieren en `show()` y se disponen una sola vez (idempotente, null tras disponer) en `dispose()` — nunca `dispose()` desde un constructor ni doble-dispose; cada Screen posee su propio `Stage`. |
| `libgdx-input-handling` | Un único `InputProcessor`/`InputMultiplexer` seteado una vez en `show()`; `keyDown`/`isKeyJustPressed` para acciones edge (sin latches `canSelect` manuales), `isKeyPressed` solo para movimiento sostenido; nunca leer `Gdx.input` dentro de `draw()`. |
| `libgdx-render-discipline` | La lógica de juego vive en `Actor.act(delta)` y el render en `draw()` puro; `batch.begin()/end()` se agrupa una vez por frame; el movimiento se escala por `delta`, no en píxeles-por-frame. |
| `repository-pattern-mongo` | El acceso a datos pasa por una interfaz `Repository` instanciable con la `MongoDatabase`/URI inyectada desde config; la lógica de dominio (win-rate, best-score, sort) vive en calculadoras puras con un DTO/`record`, no en `Document` crudos ni métodos `static`. |
| `libgdx-testing-headless` | Los tests bootean `HeadlessApplication` con `Gdx.gl = mock(GL20.class)` vía una extensión JUnit5 que corre antes de cargar cualquier clase con estáticos derivados de Gdx; se asertan geometría/estado, no píxeles; integración Mongo con Testcontainers. |
| `dead-code-hygiene` | Las clases scratch/debug (`Prueba`, `TestScreen`) y las dependencias no usadas (`gdx-box2d/bullet/ai`) no se shippean en `main`; el logging va por slf4j, nunca `System.out.println`. |

---

**Archivos clave (rutas absolutas) para la próxima sesión:**
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\BattleController.java` (845 líneas, God class, `generateAct()` 642-842)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\BlackScreen.java` (estáticos 24-46, ciclo de vida 48-233)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\Events.java` (estáticos 19-37, scoring 169-199)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\Heart.java` (`option` 23, dispatch 182-192)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\ObjetsItems.java` (`new Texture` 61/69/98/115/125, begin/end 242-277)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\Sounds.java` (15 `static Music`, newMusic por trigger)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\DataBase.java` (100% static, URI 28, sort encadenado 86-94)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\com\game\Undertale.java` (singletons 16-22, getFont 45-50, switchScreen 52-57)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\build.gradle` (sin deps de test)
- `c:\Users\User\Documents\02_PROJECTS\Videogames\UndertaleGame\core\src\main\java\listener\CollisionListener.java` (Intersector, línea 41)