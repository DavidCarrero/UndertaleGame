package com.game.phase;

import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;

import static com.game.BlackScreen.boxHeart;
import static com.game.BlackScreen.heart;

/**
 * NIVEL NUEVO: alma roja + Gaster Blasters rotatorios que apuntan a tu última posición.
 *
 * La caja se EXPANDE (modo amplio) para dar libertad. Durante {@link #DURATION} segundos
 * aparecen tandas de {@link #BLASTERS_PER_WAVE} blasters desde puntos aleatorios del borde
 * de la pantalla; cada uno apunta hacia donde está el alma en el momento de aparecer y
 * dispara un rayo recto en esa dirección. Te persiguen, así que hay que moverse.
 *
 * Implementa {@link AttackPhase} (arquitectura de fases).
 */
public class RedSoulGasterPhase implements AttackPhase {

    private static final float DURATION = 7.0f;        // segundos generando blasters
    private static final float SPAWN_INTERVAL = 0.5f;  // segundos entre cada blaster (consecutivos)

    private final ArrayList<RotatingGasterBlaster> blasters = new ArrayList<>();
    private float elapsed = 0;
    private float waveTimer = 0;
    private boolean boxExpanded = false;

    // Caja cuadrada-mediana CENTRADA para este nivel (más estrecha que MAX_WIDTH, para que
    // los blasters queden cerca del área de juego, no en los bordes de la pantalla).
    private static final float BOX_WIDTH = com.game.BlackScreen.VH_WIDTH * 45f;
    private float boxTargetX;

    @Override
    public void onEnter() {
        heart.setOption(1); // alma roja, movimiento libre
        blasters.clear();
        elapsed = 0;
        waveTimer = SPAWN_INTERVAL; // primer blaster en cuanto la caja se expanda
        boxExpanded = false;
        boxTargetX = (com.badlogic.gdx.Gdx.graphics.getWidth() - BOX_WIDTH) / 2f;
    }

    @Override
    public void update(float delta) {
        // 1) Animar la caja hacia un tamaño cuadrado-mediano centrado (animación de expansión).
        animateBoxToTarget();
        if (Math.abs(boxHeart.getWidth() - BOX_WIDTH) < 20f) {
            boxHeart.setWidth(BOX_WIDTH);
            boxHeart.setX(boxTargetX);
            boxExpanded = true;
        }
        if (!boxExpanded) {
            return;
        }

        // 2) Durante DURATION s, generar blasters UNO TRAS OTRO apuntando al alma.
        elapsed += delta;
        if (elapsed < DURATION) {
            waveTimer += delta;
            if (waveTimer >= SPAWN_INTERVAL) {
                waveTimer = 0;
                spawnOneAtSoul();
            }
        }

        // 3) Actualizar/dibujar blasters; eliminar terminados.
        for (Iterator<RotatingGasterBlaster> it = blasters.iterator(); it.hasNext(); ) {
            RotatingGasterBlaster b = it.next();
            b.update(delta);
            b.draw();
            if (b.isDone()) {
                b.dispose();
                it.remove();
            }
        }
    }

    /** Anima el ancho de la caja hacia BOX_WIDTH manteniéndola centrada (±20 px/frame). */
    private void animateBoxToTarget() {
        float current = boxHeart.getWidth();
        if (Math.abs(current - BOX_WIDTH) < 20f) {
            return;
        }
        float step = current < BOX_WIDTH ? 20f : -20f;
        float prevWidth = current;
        boxHeart.setWidth(current + step);
        // Mantener centrada respecto al cambio de ancho.
        boxHeart.setX(boxHeart.getX() - (boxHeart.getWidth() - prevWidth) / 2f);
    }

    /**
     * Genera UN blaster en un punto aleatorio del borde de la caja, apuntando hacia la
     * posición ACTUAL del alma (tu última ubicación). Aparece con pop-in (sin deslizarse).
     */
    private void spawnOneAtSoul() {
        float soulX = heart.getHitBox().x;
        float soulY = heart.getHitBox().y;
        float gap = 70f; // distancia del blaster al borde de la caja

        int side = MathUtils.random(3);
        float bx;
        float by;
        switch (side) {
            case 0 -> { // izquierda
                bx = boxHeart.getX() - gap;
                by = MathUtils.random(boxHeart.getY(), boxHeart.getY() + boxHeart.getHeight());
            }
            case 1 -> { // derecha
                bx = boxHeart.getX() + boxHeart.getWidth() + gap;
                by = MathUtils.random(boxHeart.getY(), boxHeart.getY() + boxHeart.getHeight());
            }
            case 2 -> { // arriba
                bx = MathUtils.random(boxHeart.getX(), boxHeart.getX() + boxHeart.getWidth());
                by = boxHeart.getY() + boxHeart.getHeight() + gap;
            }
            default -> { // abajo
                bx = MathUtils.random(boxHeart.getX(), boxHeart.getX() + boxHeart.getWidth());
                by = boxHeart.getY() - gap;
            }
        }
        // Ángulo del rayo: desde el blaster hacia el alma (tu última ubicación).
        float fireAngle = MathUtils.atan2(soulY - by, soulX - bx) * MathUtils.radiansToDegrees;
        blasters.add(new RotatingGasterBlaster(bx, by, fireAngle));
    }

    @Override
    public boolean isFinished() {
        // Termina cuando pasaron los 7 s y no quedan blasters activos.
        return elapsed >= DURATION && blasters.isEmpty();
    }

    @Override
    public void onExit() {
        for (RotatingGasterBlaster b : blasters) {
            b.dispose();
        }
        blasters.clear();
    }
}
