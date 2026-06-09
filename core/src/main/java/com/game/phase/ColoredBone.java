package com.game.phase;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

import static com.game.BlackScreen.heart;
import static com.game.Sounds.soulDamaged;
import static com.game.Undertale.batch;

/**
 * Hueso de color para la fase azul/naranja (mecánica de Undyne/Undertale):
 *   AZUL    → solo daña si el alma se MUEVE al tocarlo (quédate quieto para pasar).
 *   NARANJA → solo daña si el alma está QUIETA al tocarlo (muévete para pasar).
 *   BLANCO  → siempre daña.
 *
 * Reutiliza la región de hueso del spritesheet (SansSprite.png) y la tiñe con setColor.
 * Se mueve en línea recta a una velocidad dada y se marca como fuera cuando sale de pantalla.
 */
public class ColoredBone {

    public enum BoneColor { WHITE, BLUE, ORANGE }

    // Región del hueso largo vertical en el spritesheet (la misma que usan los huesos del juego).
    private static final int BONE_X = 398;
    private static final int BONE_Y = 573;
    private static final int BONE_W = 204;
    private static final int BONE_H = 102;

    private final Sprite sprite;
    private final BoneColor color;
    private float x;
    private float y;
    private final float vx;
    private final float vy;
    private final float drawW;
    private final float drawH;

    public ColoredBone(BoneColor color, float x, float y, float vx, float vy, float drawW, float drawH) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.drawW = drawW;
        this.drawH = drawH;
        sprite = new Sprite(new Texture("images/SansSprite.png"));
        sprite.setRegion(BONE_X, BONE_Y, BONE_W, BONE_H);
        switch (color) {
            case BLUE -> sprite.setColor(0.3f, 0.5f, 1f, 1f);
            case ORANGE -> sprite.setColor(1f, 0.55f, 0.1f, 1f);
            default -> sprite.setColor(Color.WHITE);
        }
    }

    public void update(float delta) {
        x += vx;
        y += vy;
    }

    /**
     * Comprueba colisión con el alma y aplica la regla de color.
     * @param soulMoving true si el alma se está moviendo este frame.
     */
    public void checkCollision(boolean soulMoving) {
        if (heart == null) {
            return;
        }
        float hr = heart.getHitBox().radius;
        float hx = heart.getHitBox().x + hr / 2f;
        float hy = heart.getHitBox().y + hr / 2f;
        // AABB del hueso (centrado en x,y).
        float left = x;
        float right = x + drawW;
        float bottom = y;
        float top = y + drawH;
        // Punto del alma más cercano al rectángulo del hueso.
        float cx = Math.max(left, Math.min(hx, right));
        float cy = Math.max(bottom, Math.min(hy, top));
        float d2 = (hx - cx) * (hx - cx) + (hy - cy) * (hy - cy);
        float r = hr * 0.4f;
        if (d2 > r * r) {
            return; // no toca
        }
        // Toca: aplica la regla de color.
        boolean damages = switch (color) {
            case BLUE -> soulMoving;     // azul daña solo si te mueves
            case ORANGE -> !soulMoving;  // naranja daña solo si estás quieto
            default -> true;             // blanco siempre
        };
        if (damages) {
            soulDamaged();
            heart.setHp(Math.max(heart.getHp() - 1.0f, 0));
        }
    }

    public boolean isOffScreen(float screenW, float screenH) {
        return x + drawW < -50 || x > screenW + 50 || y + drawH < -50 || y > screenH + 50;
    }

    public void draw() {
        batch.begin();
        batch.draw(sprite, x, y, drawW, drawH);
        batch.end();
    }

    public void dispose() {
        sprite.getTexture().dispose();
    }
}
