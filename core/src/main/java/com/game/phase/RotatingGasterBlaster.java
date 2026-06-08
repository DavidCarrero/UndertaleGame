package com.game.phase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import static com.game.BlackScreen.heart;
import static com.game.Sounds.gasterBlasterAttackSound;
import static com.game.Sounds.soulDamaged;
import static com.game.Undertale.batch;
import static com.game.Undertale.shapeRenderer;

/**
 * Gaster Blaster que aparece YA orientado hacia donde dispara (sin girar), con una
 * animación de entrada "pop-in" (crece de 0 a su tamaño con rebote, estilo Undertale).
 * Luego abre la boca (misma animación que los blasters de la fase 4) y dispara un rayo
 * recto en su dirección.
 *
 * Estados: POPPING (crece con rebote) -> OPENING (abre boca y dispara) -> DONE.
 */
public class RotatingGasterBlaster {

    public enum State { POPPING, OPENING, DONE }

    // Animación de apertura de boca (coordenada X de la región del sprite, como la fase 4).
    private static final int[] MOUTH_FRAMES = {
            6, 6, 6, 6, 6, 6, 55, 55, 104, 104, 104,
            149, 149, 197, 197, 245, 245, 245, 245, 197, 197, 149, 149
    };
    private static final int SPR_Y = 861;
    private static final int SPR_W = 44;
    private static final int SPR_H = 57;
    private static final float SCALE = 1.6f;

    private static final float POP_TIME = 0.22f;       // s de la animación de aparición
    private static final float FRAME_TIME = 0.04f;     // s por frame de apertura de boca
    private static final float BEAM_HALF_THICKNESS = 8f * SCALE;
    // Posición de la boca POR ÁNGULO (8 puntos cada 45°, empezando en 0°), calibrada
    // arrastrando el punto rojo a la boca en el banco. El sprite es asimétrico, así que la boca
    // no está a una distancia constante: cada ángulo tiene su (FORWARD, SIDE). Para fireAngle
    // intermedio se interpola linealmente entre los puntos vecinos (interpCal).
    private static final float[] CAL_FORWARD = {60, 54, 55, 76,  91,  100, 96,  78};
    private static final float[] CAL_SIDE    = {-16, -4, 12, 20,  16,  3,   -12, -22};

    /** FORWARD interpolado para el fireAngle de este blaster. */
    private float mouthForward() {
        return interpCal(CAL_FORWARD);
    }

    /** SIDE interpolado para el fireAngle de este blaster. */
    private float mouthSide() {
        return interpCal(CAL_SIDE);
    }

    /** Interpola linealmente un array de calibración (8 puntos cada 45°) para fireAngle. */
    private float interpCal(float[] values) {
        float a = ((fireAngle % 360) + 360) % 360; // normaliza a [0,360)
        float seg = a / 45f;                        // segmento entre dos puntos vecinos
        int i0 = (int) seg % 8;
        int i1 = (i0 + 1) % 8;
        float t = seg - (int) seg;
        return values[i0] + (values[i1] - values[i0]) * t;
    }

    private final Sprite sprite;
    private final float x;
    private final float y;
    private final float fireAngle;   // dirección del rayo (0=derecha, 90=arriba, 180=izq, 270=abajo)

    private State state = State.POPPING;
    private float popTimer = 0;
    private float currentScale = 0f;
    private int frame = 0;
    private float frameTimer = 0;
    private boolean firedSound = false;

    /** Aparece centrado en (x,y), ya apuntando a fireAngle. */
    public RotatingGasterBlaster(float x, float y, float fireAngle) {
        sprite = new Sprite(new Texture("images/SansSprite.png"));
        sprite.setRegion(MOUTH_FRAMES[0], SPR_Y, SPR_W, SPR_H);
        sprite.setOriginCenter();
        this.x = x;
        this.y = y;
        this.fireAngle = fireAngle;
    }

    /** El sprite (boca hacia arriba en el spritesheet) se orienta a fireAngle + 90. */
    private float spriteRotation() {
        return fireAngle + SPRITE_ANGLE_OFFSET;
    }

    // Offset de orientación del sprite. La cara apuntaba al lado contrario del rayo, así que
    // se gira 180° respecto al -90 anterior: -90 + 180 = +90.
    private static final float SPRITE_ANGLE_OFFSET = 90f;

    public boolean isDone() {
        return state == State.DONE;
    }

    private boolean isBeamActive() {
        return state == State.OPENING && MOUTH_FRAMES[frame] >= 149;
    }

    private float beamHeightFactor() {
        int v = MOUTH_FRAMES[frame];
        if (v == 149) return 0.6f;
        if (v == 197) return 0.85f;
        if (v == 245) return 1f;
        return 0f;
    }

    public void update(float delta) {
        switch (state) {
            case POPPING -> {
                popTimer += delta;
                float t = Math.min(1f, popTimer / POP_TIME);
                // Rebote: crece pasándose un poco y vuelve (Interpolation.swingOut).
                currentScale = Interpolation.swingOut.apply(0f, SCALE, t);
                if (t >= 1f) {
                    currentScale = SCALE;
                    state = State.OPENING;
                    frame = 0;
                    frameTimer = 0;
                }
            }
            case OPENING -> {
                currentScale = SCALE;
                frameTimer += delta;
                if (frameTimer >= FRAME_TIME) {
                    frameTimer = 0;
                    frame++;
                    if (frame >= MOUTH_FRAMES.length) {
                        state = State.DONE;
                        return;
                    }
                    sprite.setRegion(MOUTH_FRAMES[frame], SPR_Y, SPR_W, SPR_H);
                }
                if (isBeamActive()) {
                    if (!firedSound) {
                        firedSound = true;
                        gasterBlasterAttackSound();
                    }
                    checkBeamCollision();
                }
            }
            default -> { /* DONE */ }
        }
    }

    private void checkBeamCollision() {
        if (heart == null) {
            return;
        }
        float halfThick = BEAM_HALF_THICKNESS * beamHeightFactor();
        Vector2 dir = new Vector2(MathUtils.cosDeg(fireAngle), MathUtils.sinDeg(fireAngle));
        float hx = heart.getHitBox().x;
        float hy = heart.getHitBox().y;
        Vector2 toHeart = new Vector2(hx - x, hy - y);
        float along = toHeart.dot(dir);
        if (along < 0) {
            return;
        }
        Vector2 closest = new Vector2(dir).scl(along).add(x, y);
        float perpDist = closest.dst(hx, hy);
        if (perpDist <= halfThick + heart.getHitBox().radius) {
            soulDamaged();
            heart.setHp(Math.max(heart.getHp() - 1.0f, 0));
        }
    }

    public void draw() {
        // 1) El rayo: rectángulo largo en dirección fireAngle, calculado con vectores
        // explícitos (cos/sin) para no depender de la convención de shapeRenderer.rotate.
        if (isBeamActive()) {
            float halfThick = BEAM_HALF_THICKNESS * beamHeightFactor();
            float beamLength = Gdx.graphics.getWidth() * 1.5f;
            float beamStartBack = SPR_H * SCALE * 0.5f;
            // Vector "adelante" (dirección del rayo) y "perpendicular" (grosor).
            float fx = MathUtils.cosDeg(fireAngle);
            float fy = MathUtils.sinDeg(fireAngle);
            float px = -fy; // perpendicular al eje del rayo
            float py = fx;
            // La boca está dentro del sprite: forward a lo largo del eje del rayo (fx,fy) y
            // side perpendicular (px,py). Ambos interpolados por ángulo (tabla de calibración),
            // así el rayo nace de la boca en cualquier dirección.
            float mf = mouthForward();
            float ms = mouthSide();
            float originX = x + fx * mf + px * ms;
            float originY = y + fy * mf + py * ms;
            float sx = originX - fx * beamStartBack;
            float sy = originY - fy * beamStartBack;
            float total = beamLength + beamStartBack;
            // 4 esquinas del rectángulo del rayo.
            float ax = sx + px * halfThick;
            float ay = sy + py * halfThick;
            float bx = sx - px * halfThick;
            float by = sy - py * halfThick;
            float cxp = sx + fx * total - px * halfThick;
            float cyp = sy + fy * total - py * halfThick;
            float dx = sx + fx * total + px * halfThick;
            float dy = sy + fy * total + py * halfThick;
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.triangle(ax, ay, bx, by, cxp, cyp);
            shapeRenderer.triangle(ax, ay, cxp, cyp, dx, dy);
            shapeRenderer.end();
        }

        // 2) El sprite del blaster, orientado a spriteRotation(), anclado por su centro (x,y).
        float originXLocal = SPR_W / 2f;
        float originYLocal = SPR_H / 2f;
        batch.begin();
        batch.draw(sprite, x - originXLocal * currentScale, y - originYLocal * currentScale,
                originXLocal, originYLocal,
                SPR_W, SPR_H, currentScale, currentScale, spriteRotation());
        batch.end();
    }

    public void dispose() {
        sprite.getTexture().dispose();
    }
}
