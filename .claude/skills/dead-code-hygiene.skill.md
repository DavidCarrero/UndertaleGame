---
name: dead-code-hygiene
description: >
  Higiene de código: sin clases scratch/debug ni dependencias no usadas en main,
  logging por slf4j en vez de System.out.println, sin código comentado muerto.
  Activar al tocar build.gradle (dependencias), al ver Prueba.java/TestScreen.java,
  System.out.println, código comentado, imports/deps no usados, o al añadir una
  dependencia o clase nueva. Triggers: dead code, scratch, debug, println, dependencia
  no usada, box2d, bullet, gdx-ai.
---

# Higiene de código

El proyecto shippa clases scratch en `main`, declara dependencias pesadas que no usa, y tiene `System.out.println` de depuración y FSMs comentadas.

Hallazgos: parte de P0 del informe, F06, SOC-2 (ver `docs/ARQUITECTURA-AUDITORIA.md`).

## Prohibido
- ❌ Clases scratch/debug en `main`: `Prueba.java`, `TestScreen.java` (y `BodyFabric` si queda sin uso).
- ❌ Dependencias declaradas y no usadas: `gdx-ai`, `gdx-box2d`, `gdx-bullet`, `mongodb-driver-reactivestreams` (la colisión es manual vía `Intersector` en `CollisionListener`; no hay cuerpos de física ni uso de IA ni reactive streams).
- ❌ `System.out.println(...)` para depuración (p. ej. `Events.java:175,182,185,188`, `selectAct` "HI").
- ❌ Bloques de código comentado muerto (FSM comentada en `BlackScreen.update` 176-187).
- ❌ Código muerto lógico: branches inalcanzables (`Events` `score += 200` gateado por float-equality `== 273`; `verifyRegion` con guard `getTexture()==null` inalcanzable).

## Obligatorio
1. Borrar clases scratch antes de mergear a `main`. Si se necesitan para experimentar, dejarlas fuera del control de versiones o en una rama de spike.
2. Mantener `build.gradle` mínimo: solo dependencias realmente referenciadas. Antes de añadir una, confirmar que se usa; antes de dejar una, confirmar lo mismo.
3. Usar slf4j (ya está en el classpath: `slf4j-jdk14`, `logback-classic`) para diagnósticos, no `System.out`.
4. Borrar código comentado: el historial de git lo preserva.
5. Eliminar branches inalcanzables o documentar/arreglar la condición rota (el `== 273` float-equality es un bug, no una optimización).

## Before → After

```java
// ❌ BEFORE — Events.java + core/build.gradle
System.out.println("Centred at: " + POSITION_BAR_NORMALIZED);
if (POSITION_BAR_NORMALIZED == 273) { score += 200; }   // float == : casi nunca true
score += 300;                                            // este sí corre siempre
...
// core/build.gradle
api "com.badlogicgames.gdx:gdx-ai:$aiVersion"            // no usado
api "com.badlogicgames.gdx:gdx-box2d:$gdxVersion"        // no usado
api "com.badlogicgames.gdx:gdx-bullet:$gdxVersion"       // no usado
```

```java
// ✅ AFTER
// usar la zona de impacto explícita en vez de float-equality:
if (zone == HitZone.PERFECT) score += PERFECT_BONUS;
score += BASE_HIT;
// build.gradle: solo lo usado
api "com.badlogicgames.gdx:gdx:$gdxVersion"
api "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
implementation 'org.mongodb:mongodb-driver-sync:5.0.0'
```

## Checklist al añadir código/dependencias
- [ ] ¿Toda clase nueva tiene un uso real (no es scratch)?
- [ ] ¿Toda dependencia nueva se referencia de verdad?
- [ ] ¿Los diagnósticos van por slf4j, no `System.out`?
- [ ] ¿Se eliminó el código comentado y los branches inalcanzables?
