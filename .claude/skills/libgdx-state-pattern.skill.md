---
name: libgdx-state-pattern
description: >
  Modelado de máquinas de estado de juego (fases de combate, modos de caja,
  estados de pantalla) con enum/State polimórfico y transiciones explícitas.
  Activar al tocar "int act", "switch (act)", "generateAct", "boxHeart.mode",
  "nextAttack", fases de combate, ataques, transición de turno, o cualquier
  campo int/boolean usado como código de estado con un switch. También al
  AÑADIR un nuevo ataque o fase al combate.
---

# State pattern para FSMs de juego

El combate es una FSM implícita codificada como `int act` (1-8) con un `switch` de 200 líneas (`BattleController.generateAct()` 642-842). `boxHeart.mode` (0/1/2) es otra FSM por `int`. Modélalas con tipos explícitos.

Hallazgos: F01, F03, F05, F06, STATE-01, STATE-02 (ver `docs/ARQUITECTURA-AUDITORIA.md`).

## Prohibido
- ❌ `int act` / `int mode` / `int nextAttack` como código de fase con `switch`.
- ❌ `act = act + 1` sin verificación de límites (overflow de fase — bug latente).
- ❌ Añadir un ataque editando el `switch` central monolítico.
- ❌ Acoplar el score al ordinal de la fase (`score += act * 100`) — rompe al reordenar fases.
- ❌ Estado de fase en campos `static` que sobreviven al `Screen` y se resetean parcialmente (bug STATE-01: `isOptionAvailable` no se resetea en muerte durante act 8).

## Obligatorio
1. **`enum BattlePhase`** (o `interface BattlePhase` con clases por fase) con `onEnter()`, `update(float delta)`, `render(Batch)`, `next()`.
2. El controlador tiene `BattlePhase current` y hace `current.update(delta); if (current.isDone()) current = current.next();` — sin `switch` sobre int.
3. **Transiciones explícitas** en una tabla/`next()`, exhaustivas, con default que falle ruidoso.
4. **Score desacoplado del ordinal**: cada fase expone su propio `scoreBonus()`.
5. **Estado de fase en instancia** (ver skill `libgdx-no-static-state`), creado fresco al iniciar el combate.
6. `BoxHeart.mode` → `enum BoxState {OPEN, MID, SQUARE}` + `isOpen()/isSquare()`; colapsar `changeDimensions*` en `resizeTowards(target, delta)`.

## Before → After

```java
// ❌ BEFORE — BattleController.java:642 + Events.java:194
static int act = 4;
static void generateAct() {
    switch (act) {
        case 4 -> { boxHeart.changeDimensionsMin(); generateGastersBlaster(); ... }
        case 5 -> { ... }
        // ... 8 casos, 200 líneas
    }
}
// en Events:
act = !isSparing ? act + 1 : 8;   // +1 sin bound check; score acoplado al ordinal
score += act * 100;
```

```java
// ✅ AFTER
interface BattlePhase {
    void onEnter(BattleContext ctx);
    void update(BattleContext ctx, float delta);
    boolean isDone();
    BattlePhase next();
    int scoreBonus();
}
// El controlador:
current.update(ctx, delta);
if (current.isDone()) { current = current.next(); current.onEnter(ctx); }
// Score por fase, no por ordinal:
ctx.addScore(current.scoreBonus());
```

```java
// ✅ AFTER — BoxHeart
private enum BoxState { OPEN, MID, SQUARE }
private BoxState state = OPEN;
public boolean isSquare() { return state == SQUARE; }
public void resizeTowards(BoxState target, float delta) { /* clamp + flip una vez */ }
```

## Checklist al añadir una fase/ataque o tocar un modo
- [ ] ¿La fase es un tipo (enum/clase), no un `int`+`case`?
- [ ] ¿Las transiciones son explícitas y acotadas (sin `+1` desnudo)?
- [ ] ¿El score lo provee la fase, no su número?
- [ ] ¿El estado vive en instancia y se resetea completo al (re)iniciar?
