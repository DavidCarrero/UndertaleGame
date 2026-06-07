---
name: libgdx-asset-pooling
description: >
  Reciclaje de entidades transitorias en libGDX con com.badlogic.gdx.utils.Pool y
  Pool.Poolable. Activar al crear/destruir proyectiles, balas, huesos, plataformas,
  Gaster Blasters, labels efímeros o cualquier objeto que se spawnee y borre
  repetidamente en el loop de juego. Triggers: "new ObjetsItems", "spawn",
  "bonesLeft.add", "pool", "obtain", "free", "delete()", "ArrayList<ObjetsItems>",
  churn de GC, alocación en el loop.
---

# Object Pooling de entidades transitorias

Los proyectiles (`ObjetsItems`: huesos, plataformas, blasters) se crean y destruyen ~cada 0.6-0.8s. Crearlos con `new` y descartarlos genera churn de GC y, combinado con la carga de textura por spawn, fugas. Recíclalos con un `Pool`.

Hallazgos: PERF-1, TEX-002 (ver `docs/ARQUITECTURA-AUDITORIA.md`). **Depende de** `libgdx-resource-management` (un objeto pooleado NO debe poseer/disponer su textura).

## Prohibido
- ❌ `bonesLeft.add(new ObjetsItems(...))` en sitios de spawn (`BattleController` líneas 223-226, 244, 284-289, 542).
- ❌ `bone.delete()` que dispone GL o hace `getTexture().dispose()`.
- ❌ Que una entidad-hoja haga `bonesLeft.clear()`/`bonesRight.clear()` sobre la colección del controlador (`ObjetsItems.java:210,216` — inversión de propiedad).

## Obligatorio
1. **`ObjetsItems implements Pool.Poolable`** con un método `reset()` que limpia estado (posición, opacidad, índices, flags) pero **no toca recursos GL**.
2. **`init(...)`** en vez de constructor para reconfigurar un objeto reciclado (kind, x, y, direction, región).
3. **`Pool<ObjetsItems>`** (o `Pools.get(ObjetsItems.class)`) propiedad del controlador/contexto: `obtain()` al spawnear, `free(bone)` al "borrar".
4. La textura/región es **compartida e inyectada** (skill `libgdx-resource-management`); el pool nunca la dispone.
5. La colección de activos la posee el **controlador**, no la entidad; una entidad nunca limpia la lista de su contenedor.

## Before → After

```java
// ❌ BEFORE — BattleController.java:223 + ObjetsItems.delete()
bonesLeft.add(new ObjetsItems(1, x, y, TOWARD));     // alloc + new Texture
...
bone.delete();                                        // dispose GL compartido
iterator.remove();
```

```java
// ✅ AFTER
private final Pool<ObjetsItems> bonePool = new Pool<>() {
    @Override protected ObjetsItems newObject() { return new ObjetsItems(); }
};
...
ObjetsItems bone = bonePool.obtain();
bone.init(1, x, y, TOWARD, atlas.findRegion("boneSmall")); // región compartida
bonesLeft.add(bone);
...
iterator.remove();
bonePool.free(bone);   // reset() limpia estado; sin dispose

// En ObjetsItems:
@Override public void reset() {
    opacity = 1f; angle = 0; index = 0; isReverseSense = false;
    seconds = 0; startFadeOut = false; direction = null;
    // NO: imageObject.getTexture().dispose()
}
```

## Checklist al añadir/spawnear una entidad transitoria
- [ ] ¿Implementa `Pool.Poolable` con `reset()` que solo limpia estado?
- [ ] ¿Se obtiene con `pool.obtain().init(...)` y se libera con `pool.free(...)`?
- [ ] ¿La textura es compartida/inyectada (no creada ni dispuesta por la entidad)?
- [ ] ¿La colección de activos la posee el controlador, no la entidad?
