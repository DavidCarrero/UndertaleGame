---
name: libgdx-resource-management
description: >
  Carga y disposición de recursos nativos de libGDX (Texture, TextureAtlas,
  BitmapFont, Sound, Music, Pixmap, ShapeRenderer, SpriteBatch). Activar SIEMPRE
  que el código toque "new Texture", "new Sprite", "newMusic", "newSound",
  "FreeTypeFontGenerator", "getFont", carga de assets, dispose, fugas de memoria,
  VRAM, o cuando se cree/destruya cualquier objeto GL/audio. También al añadir un
  nuevo proyectil, sprite, fuente, sonido o pantalla.
---

# Gestión de recursos en libGDX

**Regla de oro:** cada recurso nativo (`Texture`, `Music`, `Sound`, `BitmapFont`, `Pixmap`) se carga **una sola vez** y se dispone **exactamente una vez** al cierre. La carga ocurre vía `AssetManager`, no con `new` disperso.

Hallazgos que esta skill previene: TEX-001, FONT-001, AUDIO-001, PERF-1, GC-001 (ver `docs/ARQUITECTURA-AUDITORIA.md`).

## Prohibido

- ❌ `new Texture(...)` dentro de un **constructor de entidad**, un **loop de spawn**, un método **`draw()`/`render()`**, o cualquier ruta llamada por frame.
- ❌ `Gdx.audio.newMusic(...)` / `newSound(...)` dentro de un método de "play" llamado repetidamente (p. ej. por cada tecla).
- ❌ `new FreeTypeFontGenerator(...)` por cada llamada a obtener una fuente (fuga de generator + font).
- ❌ Disponer un recurso **compartido** desde el `delete()`/`dispose()` de una instancia individual.
- ❌ Asignar `new Color(...)`, `new Rectangle(...)`, `new TextureRegion[]`, `new Animation<>` dentro del loop de render/animación.

## Obligatorio

1. **AssetManager central.** Cargar imágenes/fuentes/sonidos una vez en `Undertale.create()` (o en `Screen.show()` con `manager.finishLoading()`), recuperarlos con `manager.get(path, Type.class)`, y disponer el `AssetManager` una sola vez en `Undertale.dispose()`.
2. **Inyectar regiones, no rutas.** Las entidades reciben un `TextureRegion`/`Sprite` ya cargado (o un `TextureAtlas`), nunca una ruta de archivo que carguen ellas mismas.
3. **Fuentes cacheadas.** `getFont(size)` debe consultar un `Map<Integer,BitmapFont>` y generar solo el primer uso; disponer el generator tras generar todos los tamaños; disponer las fuentes al salir.
4. **SFX como `Sound`, temas como `Music`.** Los efectos cortos (select, hit) van como `Sound` (cargados una vez); reserva `Music` para temas largos en streaming.
5. **Objetos math reutilizables.** Cachea `Color`/`Rectangle` como campo y muta (`color.a = ...`, `rect.set(...)`); construye cada `Animation` una vez (en el ctor o lazy-init), no en cada frame.

## Before → After

```java
// ❌ BEFORE — ObjetsItems.java:61,69 — textura de disco POR proyectil
public ObjetsItems(int kind, float x, float y, Direction direction) {
    this.imageObject = new Sprite(new Texture("images/SansSprite.png"));
    ...
}
public void delete() { this.imageObject.getTexture().dispose(); } // dispone recurso "compartido"
```

```java
// ✅ AFTER — región inyectada desde el atlas/AssetManager
public ObjetsItems(int kind, float x, float y, Direction direction, TextureRegion region) {
    this.imageObject = new Sprite(region); // 0 carga de disco, 0 subida GL
    ...
}
public void reset() { /* solo limpia estado; NO dispone GL */ }
// La textura/atlas se dispone una sola vez en AssetManager.dispose()
```

```java
// ❌ BEFORE — Undertale.java:45 — fuga de generator + font por llamada
public static BitmapFont getFont(int size) {
    generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/determination.otf"));
    ... return generator.generateFont(parameter);
}
```

```java
// ✅ AFTER — fuente generada una vez por tamaño y cacheada
private final Map<Integer, BitmapFont> fontCache = new HashMap<>();
public BitmapFont getFont(int size) {
    return fontCache.computeIfAbsent(size, s -> {
        var p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = s;
        return generator.generateFont(p); // generator disposed tras precargar tamaños
    });
}
```

## Checklist al añadir un asset o entidad
- [ ] ¿El asset se declara/carga en `AssetManager` una sola vez?
- [ ] ¿La entidad recibe la región/sprite por constructor en vez de cargarla?
- [ ] ¿Hay exactamente un punto de `dispose()` para ese recurso?
- [ ] ¿Ningún `new Texture`/`newMusic` quedó en un ctor, loop o `draw()`?
