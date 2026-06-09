package com.game.phase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;

import static com.game.BlackScreen.boxHeart;
import static com.game.BlackScreen.heart;

/**
 * NIVEL: huesos AZUL / NARANJA / blanco (mecánica de Undertale), alma roja.
 *   AZUL    → daña solo si te MUEVES (quédate quieto para pasar).
 *   NARANJA → daña solo si estás QUIETO (muévete para pasar).
 *   BLANCO  → siempre daña.
 * Los huesos cruzan la caja horizontalmente por oleadas; el jugador lee el color y reacciona.
 *
 * Implementa {@link AttackPhase}. Caja mediana centrada (como el nivel Gaster).
 */
public class BlueOrangeBonesPhase implements AttackPhase {

    private static final float DURATION = 9.0f;
    private static final float SPAWN_INTERVAL = 1.1f;
    private static final float BOX_WIDTH = com.game.BlackScreen.VH_WIDTH * 45f;

    private final ArrayList<ColoredBone> bones = new ArrayList<>();
    private float elapsed = 0;
    private float spawnTimer = 0;
    private boolean boxReady = false;
    private float boxTargetX;

    // Para saber si el alma se mueve: comparamos su posición con la del frame anterior.
    private float prevSoulX;
    private float prevSoulY;

    @Override
    public void onEnter() {
        heart.setOption(1); // alma roja, movimiento libre
        bones.clear();
        elapsed = 0;
        spawnTimer = SPAWN_INTERVAL;
        boxReady = false;
        boxTargetX = (Gdx.graphics.getWidth() - BOX_WIDTH) / 2f;
        prevSoulX = heart.getX();
        prevSoulY = heart.getY();
    }

    @Override
    public void update(float delta) {
        // 1) Animar la caja a tamaño mediano centrado.
        animateBox();
        if (Math.abs(boxHeart.getWidth() - BOX_WIDTH) < 20f) {
            boxHeart.setWidth(BOX_WIDTH);
            boxHeart.setX(boxTargetX);
            boxReady = true;
        }
        if (!boxReady) {
            prevSoulX = heart.getX();
            prevSoulY = heart.getY();
            return;
        }

        // 2) ¿El alma se mueve este frame? (cambió de posición desde el frame anterior)
        boolean soulMoving = Math.abs(heart.getX() - prevSoulX) > 0.1f
                || Math.abs(heart.getY() - prevSoulY) > 0.1f;
        prevSoulX = heart.getX();
        prevSoulY = heart.getY();

        // 3) Generar oleadas de huesos.
        elapsed += delta;
        if (elapsed < DURATION) {
            spawnTimer += delta;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnTimer = 0;
                spawnWave();
            }
        }

        // 4) Actualizar, dibujar y colisionar.
        float sw = Gdx.graphics.getWidth();
        float sh = Gdx.graphics.getHeight();
        for (Iterator<ColoredBone> it = bones.iterator(); it.hasNext(); ) {
            ColoredBone b = it.next();
            b.update(delta);
            b.draw();
            b.checkCollision(soulMoving);
            if (b.isOffScreen(sw, sh)) {
                b.dispose();
                it.remove();
            }
        }
    }

    /** Una oleada: 2-3 huesos horizontales a alturas aleatorias, color aleatorio, que cruzan la caja. */
    private void spawnWave() {
        float boxLeft = boxHeart.getX();
        float boxRight = boxHeart.getX() + boxHeart.getWidth();
        float boxBottom = boxHeart.getY();
        float boxTop = boxHeart.getY() + boxHeart.getHeight();
        float boneW = 90f;   // largo del hueso horizontal (ya escalado)
        float boneH = 18f;   // grosor
        float speed = 6f;

        int count = MathUtils.random(2, 3);
        for (int i = 0; i < count; i++) {
            ColoredBone.BoneColor color = randomColor();
            float by = MathUtils.random(boxBottom + 10, boxTop - boneH - 10);
            boolean fromLeft = MathUtils.randomBoolean();
            if (fromLeft) {
                bones.add(new ColoredBone(color, boxLeft - boneW - 20, by, speed, 0, boneW, boneH));
            } else {
                bones.add(new ColoredBone(color, boxRight + 20, by, -speed, 0, boneW, boneH));
            }
        }
    }

    /** Color aleatorio: mezcla de azul, naranja y algo de blanco. */
    private ColoredBone.BoneColor randomColor() {
        int r = MathUtils.random(9);
        if (r < 4) return ColoredBone.BoneColor.BLUE;
        if (r < 8) return ColoredBone.BoneColor.ORANGE;
        return ColoredBone.BoneColor.WHITE;
    }

    private void animateBox() {
        float current = boxHeart.getWidth();
        if (Math.abs(current - BOX_WIDTH) < 20f) {
            return;
        }
        float step = current < BOX_WIDTH ? 20f : -20f;
        float prev = current;
        boxHeart.setWidth(current + step);
        boxHeart.setX(boxHeart.getX() - (boxHeart.getWidth() - prev) / 2f);
    }

    @Override
    public boolean isFinished() {
        return elapsed >= DURATION && bones.isEmpty();
    }

    @Override
    public void onExit() {
        for (ColoredBone b : bones) {
            b.dispose();
        }
        bones.clear();
        // Restaurar caja a estado máximo consistente (igual que RedSoulGasterPhase) para que
        // el cuadro de ataque del turno del jugador tome su tamaño normal.
        boxHeart.setWidth(boxHeart.MAX_WIDTH);
        boxHeart.setX(com.game.BlackScreen.VH_WIDTH * 5);
        boxHeart.mode = 0;
    }
}
