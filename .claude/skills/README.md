# Skills de arquitectura — UndertaleGame (libGDX)

Estas skills codifican los patrones y buenas prácticas a aplicar **cada vez que Claude implemente o refactorice una funcionalidad** en este proyecto. Derivan de la auditoría arquitectónica en [`docs/ARQUITECTURA-AUDITORIA.md`](../../docs/ARQUITECTURA-AUDITORIA.md).

Cada archivo `*.skill.md` tiene frontmatter (`name`, `description` con triggers) seguido de reglas concretas y ejemplos *before/after* anclados en el código real del repo.

| Skill | Enforza |
|-------|---------|
| [libgdx-resource-management](libgdx-resource-management.skill.md) | Carga de `Texture`/`Sound`/`Music`/`BitmapFont` una sola vez vía `AssetManager`; nunca `new Texture`/`newMusic` en constructores, loops de spawn o métodos de play. |
| [libgdx-asset-pooling](libgdx-asset-pooling.skill.md) | Entidades transitorias implementan `Pool.Poolable` y se reciclan con `obtain()/free()`; `delete()` nunca toca recursos GL ni colecciones del controlador. |
| [libgdx-state-pattern](libgdx-state-pattern.skill.md) | FSMs modeladas con `enum`/`State` polimórfico y transiciones explícitas, nunca `int`+`switch`; score desacoplado del ordinal. |
| [libgdx-strategy-movement](libgdx-strategy-movement.skill.md) | Comportamiento seleccionable como `MovementBehavior`/enum-strategy inyectado, no un `int option` switcheado dentro de `draw()`. |
| [libgdx-no-static-state](libgdx-no-static-state.skill.md) | Estado de gameplay en instancias inyectadas (`BattleContext`); cero `import static` de campos mutables; `VH_WIDTH/HEIGHT` en un `Viewport`/`LayoutConfig`. |
| [libgdx-screen-lifecycle](libgdx-screen-lifecycle.skill.md) | Recursos adquiridos en `show()`, dispuestos una vez (idempotente) en `dispose()`; nunca `dispose()` desde un constructor; cada Screen posee su `Stage`. |
| [libgdx-input-handling](libgdx-input-handling.skill.md) | Un solo `InputProcessor`/`InputMultiplexer` seteado en `show()`; `keyDown` para acciones edge, `isKeyPressed` solo para movimiento sostenido; nunca leer `Gdx.input` dentro de `draw()`. |
| [libgdx-render-discipline](libgdx-render-discipline.skill.md) | Lógica en `Actor.act(delta)`, render en `draw()` puro; `batch.begin()/end()` una vez por frame; movimiento escalado por `delta`. |
| [repository-pattern-mongo](repository-pattern-mongo.skill.md) | Acceso a datos vía interfaz `Repository` instanciable con `MongoDatabase` inyectada; lógica de dominio en calculadoras puras con DTO/`record`, no en `Document` crudos ni métodos `static`. |
| [libgdx-testing-headless](libgdx-testing-headless.skill.md) | Tests con `HeadlessApplication` + `Gdx.gl = mock(GL20.class)` vía extensión JUnit5; asertar geometría/estado, no píxeles; integración Mongo con Testcontainers. |
| [dead-code-hygiene](dead-code-hygiene.skill.md) | Sin clases scratch (`Prueba`, `TestScreen`) ni deps no usadas (`gdx-box2d/bullet/ai`) en `main`; logging por slf4j, nunca `System.out.println`. |

## Estado del refactor

Estas skills describen el **objetivo**. El código aún NO está refactorizado (ver el plan por fases en el informe). Al implementar nueva funcionalidad, sigue la skill aunque el código circundante todavía viole la regla — el objetivo es no añadir más deuda y migrar incrementalmente.
