---
name: libgdx-strategy-movement
description: >
  Comportamiento de movimiento/control seleccionable de entidades modelado con
  Strategy (interface o enum-strategy) inyectado, no con un int switcheado dentro
  de draw(). Activar al tocar "int option", "Heart.draw", "moveGravityApplied",
  "moveOptions", "moveOptionsItems", movimiento del corazón/alma, gravedad,
  comportamiento de control, o al añadir un nuevo modo de movimiento.
---

# Strategy para comportamiento de movimiento

`Heart` selecciona su comportamiento con `int option` (0-4) en un `switch` dentro de `draw()` (`Heart.java:182-192`). Esto mezcla render con lógica, polling de input y selección de estrategia. Sepáralo con Strategy.

Hallazgos: F04, STATE-04 (ver `docs/ARQUITECTURA-AUDITORIA.md`). Relacionada: `libgdx-render-discipline` (sacar input de `draw`), `libgdx-state-pattern`.

## Prohibido
- ❌ `int option` (0=menú, 1=libre, 2=gravedad, 3=lanzado, 4=ítems) con `switch` dentro de `draw()`.
- ❌ Setear el comportamiento desde ~16 sitios externos mutando `heart.option`.
- ❌ Reimplementar el clamp "dentro de la caja" en cada modo de movimiento (`Heart.move()`, `moveGravityApplied()`, `drawPlatforms()`).

## Obligatorio
1. **`interface MovementBehavior { void update(Heart heart, float delta, InputState input); }`** con impls: `FreeMove`, `GravityMove`, `ThrownMove`, `MenuNavigate`, `ItemSelect`, `Inert`. (Un `enum` que implemente la interfaz es válido y más liviano si no hay estado por-instancia.)
2. `Heart.setBehavior(MovementBehavior)` seteado **explícitamente al entrar a la fase**, no por mutación de un int desde fuera.
3. El `update` corre en `Heart.act(delta)`, no en `draw()`. `draw()` solo dibuja.
4. **Clamp centralizado**: `Heart.clampWithin(BoxHeart box)` (usa `MathUtils.clamp`), con constantes nombradas para los márgenes (`-15/-20/...`). Las velocidades (`+20/+5/+4`) son intencionales y NO parte de la duplicación.

## Before → After

```java
// ❌ BEFORE — Heart.java:182
@Override public void draw(Batch batch, float parentAlpha) {
    batch.draw(sprite, ...);
    switch (option) {
        case 0 -> moveOptions(fightOp, actOp, itemOp, mercyOp);
        case 1 -> move();
        case 2 -> moveGravityApplied();   // lee Gdx.input AQUÍ, dentro de draw
        case 3 -> moveGravityAppliedThrew();
        case 4 -> moveOptionsItems();
    }
    updateHitBox();
}
```

```java
// ✅ AFTER
private MovementBehavior behavior = MovementBehavior.MENU_NAVIGATE;
public void setBehavior(MovementBehavior b) { this.behavior = b; }

@Override public void act(float delta) {            // lógica + input aquí
    super.act(delta);
    behavior.update(this, delta, input);
    clampWithin(box);
    updateHitBox();
}
@Override public void draw(Batch batch, float parentAlpha) {  // solo render
    batch.draw(sprite, getX(), getY(), ...);
}
// La fase setea el comportamiento explícitamente:
heart.setBehavior(MovementBehavior.GRAVITY);
```

## Checklist al añadir/cambiar un modo de movimiento
- [ ] ¿Es una `MovementBehavior`, no un `case` de un int?
- [ ] ¿Se setea explícitamente al entrar a la fase (no mutando `option` desde fuera)?
- [ ] ¿El `update` corre en `act(delta)` y `draw()` quedó puro?
- [ ] ¿El clamp usa `clampWithin(box)` con constantes nombradas?
