---
name: libgdx-no-static-state
description: >
  Eliminación de estado global mutable y acoplamiento por import static entre
  clases de gameplay; el estado vive en instancias inyectadas por constructor
  (un BattleContext/GameState propiedad del Screen). Activar SIEMPRE que se vaya
  a declarar/usar "public static" mutable, "import static com.game.*",
  campos compartidos entre BattleController/Events/BlackScreen/Heart/Sans,
  VH_WIDTH/VH_HEIGHT, score, act, optionSelected, o al crear una clase de gameplay
  nueva. Triggers: estado global, singleton, static mutable, acoplamiento.
---

# Sin estado estático global

La deuda dominante del proyecto: el estado de combate vive en campos `public static` compartidos entre 5 clases vía `import static`, sin propiedad ni dirección. Razonar sobre `act`/`score`/`heart.isTurn` exige grep en 3-5 archivos. Esto bloquea el testing (no hay seams) y rompe en class-load bajo test (NPE de `VH_WIDTH` desde `Gdx.graphics`).

Hallazgos: F02, COUPLE-01, SOC-1 (el más importante del informe). Ver `docs/ARQUITECTURA-AUDITORIA.md` §3.

## Prohibido
- ❌ `public static` mutable para estado de gameplay (`BlackScreen.score/heart/sans/boxHeart`, `Events.optionSelected/canSelect/isSparing/i`, `BattleController.act/...`).
- ❌ `import static com.game.OtraClase.*` para leer/escribir campos mutables de otra clase.
- ❌ Que una clase escriba un campo "propiedad" de otra (`Events` escribiendo `act`; `BattleController` escribiendo `score`; dos clases escribiendo `Sans.timeHead`).
- ❌ Inicializar `VH_WIDTH/VH_HEIGHT` desde `Gdx.graphics.getWidth()` en class-init (NPE al cargar la clase en test).

## Obligatorio
1. **`BattleContext`/`GameState`** instanciable que es **dueño** de los campos mutables (`heart`, `boxHeart`, `sans`, `phase`, `score`, `optionSelected`, `isSparing`, ...). Creado **fresco** en `BlackScreen.show()` → resetea estado entre combates automáticamente.
2. **Inyección por constructor.** `Events`/`BattleController` pasan de métodos `static` a métodos de instancia que reciben el `BattleContext`. Las entidades reciben sus colaboradores por ctor.
3. **Cero `import static` de campos mutables.** Las constantes inmutables (`Direction`, `StateMessage`) sí pueden importarse estáticamente.
4. **`VH_WIDTH/HEIGHT` → `Viewport`/`LayoutConfig`** pasado como dependencia, no derivado de `Gdx` en class-init.
5. **NO usar un framework de DI** (Dagger/Guice) — es overkill. DI manual: el `Screen` construye el grafo en `show()`.

## Before → After

```java
// ❌ BEFORE
public class BlackScreen implements Screen {
    public static int score = 0;
    public static Heart heart;
    public static float VH_WIDTH = (float) Gdx.graphics.getWidth() / 100; // NPE en test
}
public class Events {
    public static int optionSelected = 0;
    public static void selectTarget() {
        score += act * 100;          // escribe estáticos de OTRAS clases
        act = !isSparing ? act + 1 : 8;
    }
}
```

```java
// ✅ AFTER
public class BattleContext {
    final Heart heart; final Sans sans; final BoxHeart boxHeart;
    final LayoutConfig layout;       // vh width/height
    BattlePhase phase; int score; int optionSelected; boolean sparing;
    BattleContext(Heart h, Sans s, BoxHeart b, LayoutConfig l) { ... }
    void addScore(int n) { score += n; }
}
public class BlackScreen implements Screen {
    private BattleContext ctx;
    @Override public void show() {
        ctx = new BattleContext(new Heart(...), new Sans(...), new BoxHeart(...), layout); // fresco
        events = new Events(ctx);
        battle = new BattleController(ctx);
    }
}
public class Events {
    private final BattleContext ctx;
    Events(BattleContext ctx) { this.ctx = ctx; }
    void selectTarget() { ctx.addScore(ctx.phase.scoreBonus()); ctx.phase = ctx.phase.next(); }
}
```

## Checklist al crear/tocar estado de gameplay
- [ ] ¿El campo vive en una instancia con dueño claro (no `public static`)?
- [ ] ¿Se accede vía referencia inyectada, no `import static`?
- [ ] ¿Solo el dueño lo muta?
- [ ] ¿Se crea fresco en `show()` para que un 2º combate arranque limpio?
- [ ] ¿Nada lee `Gdx.graphics` en class-init?
