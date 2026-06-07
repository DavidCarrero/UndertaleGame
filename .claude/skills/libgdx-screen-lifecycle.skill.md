---
name: libgdx-screen-lifecycle
description: >
  Ciclo de vida de pantallas (Screen) y disposición de recursos en libGDX.
  Activar al tocar show()/hide()/dispose()/render() de un Screen, switchScreen,
  transiciones entre pantallas, creación/destrucción de Stage, o al crear una
  pantalla nueva. Triggers: implements Screen, dispose(), switchScreen, stage.clear,
  doble dispose, dispose en constructor, fuga de pantalla.
---

# Ciclo de vida de pantallas

Las pantallas adquieren recursos en `show()` y los disponen **una vez** en `dispose()`. El proyecto hoy llama `dispose()` desde constructores, dispone por frame, y comparte un solo `Stage` global que se reasigna entre pantallas.

Hallazgos: F09, TRANS-01, TRANS-02 (ver `docs/ARQUITECTURA-AUDITORIA.md`).

## Prohibido
- ❌ Llamar `dispose()` desde un **constructor** de Screen (`MainMenu:25`, `InputNameScreen:27`, `BlackScreen:51`).
- ❌ Disponer un recurso **por frame** dentro de `render()` (`GameOverScreen.render` dispone `image` cada frame y otra vez en `dispose()`).
- ❌ Compartir un único `Stage` global mutable entre pantallas y reasignarlo (`CreditsScreen:36`, `HowToPlayScreen:37`).
- ❌ Trabajo pesado de GL/IO en el constructor (cargar texturas, abrir conexión DB).
- ❌ Doble-dispose por rutas múltiples (`hide()` + `switchScreen` + `dispose()`).

## Obligatorio
1. **Adquirir en `show()`**, disponer en `dispose()`. El constructor solo guarda referencias baratas.
2. **`dispose()` idempotente**: poner a `null` tras disponer; guardar con `if (x != null)`.
3. **Cada Screen posee su propio `Stage`**, creado en `show()`/ctor y dispuesto en su `dispose()`. Solo el `SpriteBatch` puede compartirse a nivel app.
4. **`AssetManager` load/unload** alineado a `show()`/`hide()` para assets específicos de pantalla; los globales viven en `Undertale`.
5. La conexión a DB **no** se abre/cierra en `show()` de una pantalla de juego (sacarla a la capa repository).

## Before → After

```java
// ❌ BEFORE — BlackScreen.java:48 + GameOverScreen.render
public BlackScreen(Undertale game) {
    BlackScreen.game = game;
    ...
    dispose();                 // dispose desde el constructor (!)
}
// GameOverScreen.render:
public void render(float v) {
    ...
    image.dispose();           // dispone POR FRAME
}
```

```java
// ✅ AFTER
public BlackScreen(Undertale game) {
    this.game = game;          // solo referencias baratas, sin GL/IO
}
@Override public void show() {
    stage = new Stage(viewport, batch);   // Stage propio
    assets.load(...); assets.finishLoading();
    loadObject();
    Gdx.input.setInputProcessor(stage);
}
@Override public void dispose() {
    if (stage != null) { stage.dispose(); stage = null; }  // una vez, idempotente
}
```

## Checklist al crear/tocar una pantalla
- [ ] ¿El constructor es barato (sin `dispose()`, sin cargar GL, sin abrir DB)?
- [ ] ¿Los recursos se adquieren en `show()`?
- [ ] ¿`dispose()` es idempotente y se ejecuta una sola vez por recurso?
- [ ] ¿La pantalla tiene su propio `Stage` (no uno global compartido)?
- [ ] ¿Nada se dispone dentro de `render()`?
