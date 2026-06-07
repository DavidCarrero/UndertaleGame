---
name: libgdx-render-discipline
description: >
  Disciplina de render en libGDX: lógica en Actor.act(delta), render puro en
  draw(), batching de SpriteBatch (un begin/end por frame), y movimiento escalado
  por delta (frame-rate independence). Activar al tocar batch.begin/batch.end,
  draw(Batch...), render(float), Actor.act, ShapeRenderer, movimiento por píxeles
  fijos por frame, deltaTime, o al dibujar/animar cualquier entidad.
---

# Disciplina de render

Reglas de render idiomáticas de libGDX que el proyecto viola: `batch.begin()/end()` por cada draw individual (rompe el batching), lógica de juego e input dentro de `draw()`, y movimiento por píxeles fijos por frame (depende del frame rate).

Hallazgos: BATCH-001/PERF-2, GC-001, F06 (timing) (ver `docs/ARQUITECTURA-AUDITORIA.md`).

## Prohibido
- ❌ `batch.begin()` / `batch.end()` alrededor de **cada** draw de un objeto (`ObjetsItems.drawBone/drawPlatform/...` 242-277; `BattleController` bucle por hueso 328-335; `BlackScreen:146` 4 begin/end).
- ❌ Lógica de juego, polling de input o mutación de estado dentro de `draw()`.
- ❌ Movimiento por píxeles fijos por frame (`setX(getX() - 7)`) — corre más rápido a mayor FPS.
- ❌ Mezclar `SpriteBatch` y `ShapeRenderer` sin cerrar uno antes de abrir el otro.
- ❌ Acumuladores manuales de tiempo (`secondsTime += deltaTime; if (secondsTime > 0.7)`) repetidos a mano.

## Obligatorio
1. **Un `batch.begin()/end()` por frame**, idealmente propiedad del `Stage` (`stage.draw()` lo gestiona). Los helpers reciben un `Batch` ya iniciado (como ya hacen `Heart.draw(Batch,float)` y `Sans.draw`).
2. **Lógica en `Actor.act(delta)`**, render en `draw(Batch, parentAlpha)` puro (solo dibuja).
3. **Movimiento escalado por delta**: `setX(getX() - SPEED * delta)`, con `SPEED` en px/segundo.
4. **`ShapeRenderer` y `SpriteBatch` no se solapan**: cierra `batch.end()` antes de `shapeRenderer.begin()` y viceversa.
5. **Cooldowns/Timers** en vez de acumuladores manuales: `com.badlogic.gdx.utils.Timer` o un helper `Cooldown(ready(delta))`.

## Before → After

```java
// ❌ BEFORE — ObjetsItems.java:254 + BattleController.java:328
public void drawBone() {
    batch.begin();                       // begin/end POR hueso
    batch.draw(imageObject, ...);
    batch.end();
}
public void moveLeft() { setX(getX() - 7); }   // px fijos por frame
```

```java
// ✅ AFTER
public void drawBone(Batch batch) {      // recibe batch ya iniciado
    batch.draw(imageObject, getX(), getY(), ...);   // sin begin/end
}
// el caller agrupa:
batch.begin();
for (ObjetsItems bone : bonesLeft) bone.drawBone(batch);
batch.end();

public void moveLeft(float delta) { setX(getX() - SPEED_PX_PER_SEC * delta); }
```

## Checklist al dibujar/animar/mover
- [ ] ¿`draw()` solo dibuja (sin lógica, input ni mutación)?
- [ ] ¿Hay un solo `begin()/end()` por frame, no por objeto?
- [ ] ¿El movimiento se escala por `delta`?
- [ ] ¿`ShapeRenderer` y `SpriteBatch` no se solapan?
- [ ] ¿El timing usa Cooldown/Timer en vez de acumuladores manuales?
