# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

A desktop clone of the Undertale "Sans" boss fight, built with [libGDX](https://libgdx.com/) 1.12.1 on Java 21. Generated from a gdx-liftoff template. Player stats and a top-10 ranking are persisted to a local MongoDB instance.

## Build & Run

Use the Gradle wrapper (`./gradlew` on Unix, `gradlew.bat` on Windows). The build is two subprojects — `core` (platform-agnostic game logic) and `lwjgl3` (desktop launcher) — declared in `settings.gradle`.

- `./gradlew lwjgl3:run` — launch the game (desktop). Entry point: [Lwjgl3Launcher.java](lwjgl3/src/main/java/com/game/lwjgl3/Lwjgl3Launcher.java).
- `./gradlew build` — compile and assemble all subprojects. This is what CI runs.
- `./gradlew lwjgl3:jar` — produce the runnable jar under `lwjgl3/build/lib`.
- `./gradlew clean` (or `core:clean`) — remove build outputs.
- `./gradlew test` — run unit tests. **Note: there are currently no tests in the repository.**

There is no lint task configured. Code style is enforced only by [.editorconfig](.editorconfig): 4-space indent for Java, 2-space for `.gradle`, LF line endings, UTF-8, final newline.

### Runtime prerequisites

- **MongoDB must be running on `mongodb://localhost:27017`** before starting a game or opening the ranking screen. The connection string is hardcoded in [DataBase.java](core/src/main/java/com/game/DataBase.java). Without it, any flow that calls `createConnection()` (game end, ranking) will throw.
- A monitor is required — `Lwjgl3Launcher` sizes the window from `GraphicsEnvironment` at startup.

### Build quirks

- `compileJava.doLast` in [build.gradle](build.gradle) regenerates `assets/assets.txt` (a flat listing of every file under `assets/`) on every compile. This is a generated artifact — do not hand-edit it.
- Java toolchain is pinned to 21 via `JavaLanguageVersion`. CI ([.github/workflows/build.yml](.github/workflows/build.yml)) sets up JDK 17 for the runner but the Gradle toolchain still resolves 21; CI also runs SonarQube analysis (`sonar.projectKey=UndertaleGame`).

## Architecture

### Screen flow (the `Game` state machine)

[Undertale.java](core/src/main/java/com/game/Undertale.java) extends libGDX `Game` and is the single `ApplicationListener`. It owns **static, app-global resources** shared across all screens: `batch` (SpriteBatch), `stage` (Scene2D Stage), `shapeRenderer`, and the FreeType font generator. `initData()` lazily creates these once; `getFont(int size)` regenerates a `BitmapFont` from `determination.otf` on demand.

Navigation goes through `switchScreen()` / the `showXScreen()` helpers, which **dispose the current screen before setting the next**. The screens are: `MainMenu` → `InputNameScreen` → `BlackScreen` (the fight) → `GameOverScreen`, plus `RankingScreen`, `CreditsScreen`, `HowToPlayScreen`, and a debug `TestScreen` (`showTestScreen()` is wired but commented out in `create()`).

### The battle is one giant static singleton

This is the most important thing to understand. The fight is **not** an instance-per-object design — it is a network of classes with mutable `static` fields that read each other directly via `import static`. The three hubs:

- **[BlackScreen.java](core/src/main/java/com/game/BlackScreen.java)** — the `Screen` implementing the fight loop. Holds the canonical static references to `heart`, `sans`, `boxHeart`, the four menu options (`fightOp`, `actOp`, `itemOp`, `mercyOp`), and `score`. `render()` calls `update()` each frame, which branches on `heart.isTurn`: enemy turn → `BattleController.generateAct()`; player turn → `Events.detectEvent()`. Defines `VH_WIDTH`/`VH_HEIGHT` (viewport-percent units) used for all layout across the codebase.
- **[BattleController.java](core/src/main/java/com/game/BattleController.java)** — the **enemy turn**. `generateAct()` is a `switch (act)` over attack phases 1–8 (bone gauntlets, the rotating "tab" attacks, gravity platforms + Gaster Blasters, the mercy/sparing dialogue sequence, the final hit). `act` is the phase counter; `setConfigTurnPlayer()` ends an enemy turn and hands control back to the player.
- **[Events.java](core/src/main/java/com/game/Events.java)** — the **player turn**. `detectEvent()` reads `optionSelected` (1=Fight, 2=Act, 3=Item, 4=Mercy) and dispatches to `selectTarget`/`selectAct`/`selectItem`/`selectMercy`. Owns the FIGHT mini-game (`BoxAttack` + `BarAttack` timing bar that awards score by zone), item inventory, and the typewriter dialogue (`writeMessage`).

When changing battle logic, expect to touch shared static state across all three. Mutating `act`, `score`, `heart.isTurn`, `optionSelected`, or `isSparing` in one class is read immediately by the others on the next frame.

### Input

Two input processors, swapped at runtime. Scene2D `stage` handles menu button clicks (mouse). During the fight, [KeyListener.java](core/src/main/java/com/game/KeyListener.java) (a singleton via `KeyListener.get()`) is installed. In practice most gameplay input is **polled directly** with `Gdx.input.isKeyPressed(...)` inside `render`/`draw` loops rather than event-driven — search for `isKeyPressed` to find input handling. `canSelect` is a debounce latch reset when ENTER/ESCAPE is released.

### The `boxHeart` mode system

[BoxHeart.java](core/src/main/java/com/game/BoxHeart.java) is the resizable battle box and an implicit sub-state machine via its `mode` field (0 = max/menu width, 1 = mid, 2 = min/square). The `changeDimensionsX()` methods animate width ±20px/frame and flip `mode` at the bounds; attack phases gate their logic on `boxHeart.mode` reaching a target. The heart's movement behaviour is selected by its `option` field in `Heart.draw()` (0 = menu navigation, 1 = free move, 2 = gravity, 3 = gravity+thrown, 4 = item selection).

### Persistence

[DataBase.java](core/src/main/java/com/game/DataBase.java) — all MongoDB access is static methods against database `Undertale`, collection `Ranking`. `updatePlayerStats()` upserts by player name (best score wins, win-rate recomputed). `getRanking()` returns the top 10. Connections are opened per-operation and closed in `BlackScreen.show()` — there is no pooled long-lived client.

### UI factories

[FabricElements.java](core/src/main/java/com/game/FabricElements.java) ("Fabric" = Spanish *fábrica*, factory) centralizes creation of styled Scene2D widgets (labels, text fields, buttons with hover-yellow listeners, the ranking `Table`). [BodyFabric.java](core/src/main/java/com/game/BodyFabric.java) and [ObjetsItems.java](core/src/main/java/com/game/ObjetsItems.java) build the attack projectiles (bones, platforms, Gaster Blasters) — `ObjetsItems` has multiple constructors keyed by a `kind` int and a `Direction` enum.

### Assets

Loaded via `Gdx.files.internal("...")` with paths **relative to `assets/`** (e.g. `"images/heart.png"`, `"sound/SansFight.mp3"`, `"fonts/determination.otf"`). Audio is centralized in [Sounds.java](core/src/main/java/com/game/Sounds.java) as static `Music` fields with named play/stop helpers; `stopAllSounds()` is called on most screen transitions and in `dispose()`.

## Conventions & gotchas

- **Disposal is manual and easy to break.** Every `Screen.dispose()`/`hide()` clears `stage`, stops sounds, and disposes textures. New `Texture`/`Music` allocations must be disposed or they leak GPU/native memory. Note `BlackScreen` and `MainMenu` call `dispose()` from their *constructors* to reset shared static stage state before loading.
- **Sprite regions are pixel coordinates into spritesheets** (e.g. `Heart` sets `sprite.setRegion(7,6,16,16)` for the red soul, different coords for blue/broken). Don't treat sprites as whole-image textures.
- Comments are mixed Spanish/English; class/method names are English (with occasional typos kept for consistency, e.g. `ObjetsItems`, `Ghoster`/`Gaster`). The SonarQube `projectName` in `build.gradle` is left as the template default `GovernmentAgency` while the workflow uses `UndertaleGame` — these are inconsistent but not load-bearing.
- `Prueba.java` and `TestScreen.java` are scratch/debug code, not part of the shipped flow.
