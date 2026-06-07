---
name: libgdx-input-handling
description: >
  Manejo de entrada en libGDX con un único InputProcessor/InputMultiplexer.
  Activar al tocar Gdx.input, isKeyPressed, isKeyJustPressed, keyDown, keyUp,
  setInputProcessor, KeyListener, InputProcessor, latches de input (canSelect),
  o al añadir controles/teclas. Triggers: input, teclado, ENTER, ESCAPE, flechas,
  debounce, InputMultiplexer.
---

# Manejo de entrada

El proyecto tiene tres mecanismos de input compitiendo: el `Stage` (Scene2D), un `KeyListener` casi no-op, y polling directo de `Gdx.input.isKeyPressed` dentro de `draw()`/`render()`. Peor: `setInputProcessor` se cambia **cada frame** (`BlackScreen:130` luego `:138`), y `canSelect` es un latch de debounce manual con reglas de reset inconsistentes.

Hallazgo: INPUT-01 (ver `docs/ARQUITECTURA-AUDITORIA.md`).

## Prohibido
- ❌ Llamar `Gdx.input.setInputProcessor(...)` dentro de `render()`/`draw()` (cada frame).
- ❌ Leer `Gdx.input.isKeyPressed(...)` dentro de un método `draw()`.
- ❌ Latches de debounce manuales (`canSelect`) reseteados en sitios distintos con condiciones distintas.
- ❌ Múltiples `InputProcessor` solapados sin un `InputMultiplexer`.

## Obligatorio
1. **Un solo `InputProcessor`** (o `InputMultiplexer` si hace falta combinar Stage + teclado) seteado **una vez** en `show()`.
2. **`keyDown`/`isKeyJustPressed`** para acciones de borde (confirmar/cancelar con ENTER/ESCAPE) — el evento ya es "una vez", elimina el latch `canSelect`.
3. **`isKeyPressed`** solo para movimiento **sostenido** (mantener flecha), consultado en `act(delta)`, no en `draw()`.
4. Encapsular el estado de input en un `InputState`/handler inyectado, testeable sin GL.
5. Borrar `KeyListener` (no aporta) y el `setInputProcessor` por frame.

## Before → After

```java
// ❌ BEFORE — BlackScreen.java:130,138 + Heart.java:152
@Override public void show() { Gdx.input.setInputProcessor(stage); }
@Override public void render(float v) {
    Gdx.input.setInputProcessor(KeyListener.get());   // CADA frame
    ...
}
// y por todos lados, dentro de draw():
if (Gdx.input.isKeyPressed(Input.Keys.ENTER) && canSelect) { ...; canSelect = false; }
```

```java
// ✅ AFTER
@Override public void show() {
    var mux = new InputMultiplexer(stage, gameInput);
    Gdx.input.setInputProcessor(mux);                 // UNA vez
}
// gameInput implements InputProcessor:
@Override public boolean keyDown(int key) {           // edge: sin latch manual
    if (key == Input.Keys.ENTER)  { ctx.confirm(); return true; }
    if (key == Input.Keys.ESCAPE) { ctx.cancel();  return true; }
    return false;
}
// movimiento sostenido en act(delta), no en draw():
if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) heart.moveBy(SPEED * delta, 0);
```

## Checklist al añadir/tocar controles
- [ ] ¿El `InputProcessor` se setea una sola vez en `show()`?
- [ ] ¿Las acciones de confirmar/cancelar usan `keyDown`/`isKeyJustPressed` (sin latch manual)?
- [ ] ¿El movimiento sostenido se lee en `act(delta)`, no en `draw()`?
- [ ] ¿Se eliminó cualquier `setInputProcessor` por frame?
