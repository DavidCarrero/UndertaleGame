package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.scenes.scene2d.Actor;

import static com.game.BattleController.act;
import static com.game.BlackScreen.VH_HEIGHT;
import static com.game.BlackScreen.VH_WIDTH;
import static com.game.Direction.BACKWARD;
import static com.game.Direction.TOWARD;
import static com.game.Sounds.inflictingDamageSound;

public class Sans extends Actor {
    private final Sprite body;
    private final Sprite head;
    private final Sprite legs;

    private Animation<TextureRegion> animation, animationBody, animationHead, animationLegs;

    private float stateTime;

    public static float timeHead = 0;

    private float offset = 0;

    private final Sprite image;

    private Direction direction = BACKWARD;

    private boolean isInThirdFrame = false;

    private boolean isAnimationVoidFinished, isAnimationHeadDone = false, isAnimationWonDone = true, canAnimateEvadeAttack = false, isOptionAvailable = true;

    private final float HALF_SCREEN_WIDTH = (float) Gdx.graphics.getWidth() / 2;

    // Velocidades de cada Animation (frameDuration). La consulta a getKeyFrame
    // usa stateTime escalado por estos factores para mantener el timing original.
    private static final float HAND_FRAME_DURATION = 0.25f;
    private static final float HEAD_FRAME_DURATION = 1f;
    private static final float WON_FRAME_DURATION = 1.2f;
    private static final float HAND_SPEED = 20f; // stateTime * HAND_SPEED al consultar el frame de mano

    // Corrección horizontal para centrar la cabeza sobre el cuerpo.
    // Calculado a partir de los píxeles reales del spritesheet:
    //   body w=52 (origin centrado en 26), head w=30 (origin en 15).
    //   Para alinear centros: posHeadX = body.getX() + (26 - 15) = body.getX() + 11.
    //   La fórmula base aporta body.getX() + ~25.72px, así que sobran ~14.72px ≈ 0.767*VH_WIDTH.
    private static final float HEAD_X_CENTER_OFFSET = VH_WIDTH * 0.767f;
    // Eleva la cabeza para que no quede hundida en el cuello/cuerpo, sin pasarse
    // (2.0 dejaba un hueco; 0.8 la hundía). 1.0*VH_HEIGHT ≈ 11px es el punto medio.
    private static final float HEAD_Y_RAISE = VH_HEIGHT * 1.0f;

    // Desplazamiento del cuerpo a la DERECHA durante la animación de brazo horizontal,
    // para que el cuello quede bajo la cabeza fija. Calibrado con el banco de pruebas.
    private static final float BODY_RIGHT_SHIFT = VH_WIDTH * 6.0f;

    /**
     * Corrección por-frame del brazo derecho: el cuello no está en la misma columna en
     * todos los frames. Se identifica el frame por su regionX. Valores calibrados (col del
     * cuello, referencia 50): {-12:50, 90:47, 192:46, 294:52, 396:50, 498:50}.
     * corrección = (50 - colCuello) * scaleX.
     */
    private float armNeckCorrection(int regionX) {
        float neckCol = switch (regionX) {
            case 90 -> 47f;
            case 192 -> 46f;
            case 294 -> 52f;
            default -> 50f; // -12, 396, 498
        };
        return (50f - neckCol) * getScaleX();
    }

    public Sans() {
        image = new Sprite(new Texture(Gdx.files.internal("images/SansSprite.png")));
        // El filtro lineal se aplica UNA vez sobre la textura compartida por todos
        // los frames (idiomático en libGDX: no re-filtrar en cada creación de animación).
        image.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        head = new Sprite(new TextureRegion(image, 5, 468, 30, 28));
        body = new Sprite(new TextureRegion(image, 15, 23, 52, 25));
        legs = new Sprite(new TextureRegion(image, 6, 82, 42, 21));

        setOriginY(new TextureRegion(image, 12, 390, 52, 47).getRegionHeight());
        setScale(VH_WIDTH / 5, VH_HEIGHT / 3);

        head.setScale(getScaleX(), getScaleY());
        body.setScale(getScaleX(), getScaleY());
        legs.setScale(getScaleX(), getScaleY());

        head.setOrigin(head.getWidth() / 2, 0);
        body.setOriginCenter();
        legs.setOrigin(legs.getWidth() / 2, legs.getRegionHeight());

        // Subido de 3/4 a 4/5 de la pantalla: en modo azul (act 4) la caja de combate crece
        // hacia arriba y los pies de Sans se solapaban con ella.
        setPosition(HALF_SCREEN_WIDTH, 4 * (float) Gdx.graphics.getHeight() / 5);
        body.setPosition(getX(), getY());
        head.setPosition(body.getX(), body.getY() + body.getRegionHeight() + VH_HEIGHT * 1.5f);
        legs.setPosition(body.getX(), body.getY() - (float) body.getRegionHeight() / 2 - legs.getRegionHeight() - VH_HEIGHT * 2f);
    }

    /** Crea una región a partir de la textura compartida (filtro ya aplicado en el constructor). */
    private TextureRegion region(int x, int y, int w, int h) {
        return new TextureRegion(image, x, y, w, h);
    }

    /** Helper idiomático: construye una Animation con su PlayMode explícito. */
    private Animation<TextureRegion> buildAnimation(float frameDuration, PlayMode playMode, int[][] coords) {
        TextureRegion[] frames = new TextureRegion[coords.length];
        for (int i = 0; i < coords.length; i++) {
            frames[i] = region(coords[i][0], coords[i][1], coords[i][2], coords[i][3]);
        }
        Animation<TextureRegion> anim = new Animation<>(frameDuration, frames);
        anim.setPlayMode(playMode);
        return anim;
    }

    public Sprite getHead() {
        return head;
    }

    public void setAnimationMercy() {
        head.setRegion(228, 520, head.getRegionWidth(), head.getRegionHeight());
    }

    public void setAnimationHeadWhite() {
        head.setRegion(191, 520, head.getRegionWidth(), head.getRegionHeight());
    }

    public Animation<TextureRegion> getAnimationHead() {
        return animationHead;
    }

    public void animationWon() {
        isAnimationWonDone = false;
        animationBody = buildAnimation(WON_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {6, 23, 70, 34}, {83, 23, 70, 34}, {160, 23, 70, 34}, {237, 23, 70, 34},
                {314, 23, 70, 34}, {391, 23, 70, 34}, {469, 23, 70, 34}, {545, 23, 70, 34}
        });

        animationHead = buildAnimation(WON_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {80, 520, 30, 28}, {42, 520, 30, 28}, {376, 520, 30, 28}, {412, 520, 30, 28},
                {450, 520, 30, 28}, {487, 520, 30, 28}, {450, 520, 30, 28}, {525, 520, 30, 28}
        });

        animationLegs = buildAnimation(WON_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {0, 81, 50, 22}, {0, 81, 50, 22}, {55, 81, 50, 22}, {0, 81, 50, 22},
                {0, 81, 50, 22}, {0, 81, 50, 22}, {0, 81, 50, 22}, {0, 81, 50, 22}
        });
    }

    public void animationHeadMercy() {
        isAnimationHeadDone = true;
        animationHead = buildAnimation(HEAD_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {80, 520, 30, 28}, {42, 520, 30, 28}, {154, 520, 30, 28}, {118, 520, 30, 28}
        });
    }

    public void animationHeadMercy2() {
        isAnimationHeadDone = true;
        animationHead = buildAnimation(HEAD_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {42, 520, 30, 28}, {154, 520, 30, 28}, {118, 520, 30, 28}, {191, 520, 30, 28}
        });
    }

    public void animationRightHand() {
        setOriginX(body.getOriginX() + (VH_WIDTH * getScaleX()));
        animation = buildAnimation(HAND_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {-12, 390, 99, 47}, {90, 390, 99, 47}, {192, 390, 99, 47},
                {294, 390, 99, 47}, {396, 390, 99, 47}, {498, 390, 99, 47}
        });
        stateTime = 0;
    }

    public void animationLeftHand() {
        setOriginX(body.getOriginX() + (VH_WIDTH * getScaleX()));
        animation = buildAnimation(HAND_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {498, 390, 99, 47}, {396, 390, 99, 47}, {294, 390, 99, 47},
                {192, 390, 99, 47}, {90, 390, 99, 47}, {-12, 390, 99, 47}
        });
        stateTime = 0;
    }

    public void animationUpwardHand() {
        setOriginX(body.getOriginX());
        animation = buildAnimation(HAND_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {9, 297, 55, 66}, {78, 297, 55, 66}, {147, 297, 55, 66},
                {216, 297, 55, 66}, {285, 297, 55, 66}, {354, 297, 55, 66}
        });
        stateTime = 0;
    }

    public void animationDownwardHand() {
        setOriginX(body.getOriginX());
        animation = buildAnimation(HAND_FRAME_DURATION, PlayMode.NORMAL, new int[][]{
                {354, 297, 55, 66}, {285, 297, 55, 66}, {216, 297, 55, 66},
                {147, 297, 55, 66}, {78, 297, 55, 66}, {9, 297, 55, 66}
        });
        stateTime = 0;
    }

    public boolean isAnimationEvadeFinished() {
        return isAnimationVoidFinished;
    }

    public void setIsAnimationVoidFinishedFalse() {
        isAnimationVoidFinished = false;
    }

    public void activateAnimationEvade() {
        canAnimateEvadeAttack = true;
    }

    public boolean isCanAnimateEvadeAttack() {
        return canAnimateEvadeAttack;
    }

    public void animateVoidAttack() {
        if (canAnimateEvadeAttack) {
            switch (direction) {
                case BACKWARD -> {
                    float limitX = HALF_SCREEN_WIDTH - VH_WIDTH * 13;
                    head.setX(Math.max(head.getX() - 8, limitX));
                    body.setX(Math.max(body.getX() - 8, limitX));
                    legs.setX(Math.max(legs.getX() - 8, limitX));
                    if (head.getX() == limitX) {
                        direction = TOWARD;
                    }
                }
                case TOWARD -> {
                    head.setX(Math.min(head.getX() + 8, HALF_SCREEN_WIDTH));
                    body.setX(Math.min(body.getX() + 8, HALF_SCREEN_WIDTH));
                    legs.setX(Math.min(legs.getX() + 8, HALF_SCREEN_WIDTH));
                    if (head.getX() == HALF_SCREEN_WIDTH) {
                        direction = BACKWARD;
                        isAnimationVoidFinished = true;
                        canAnimateEvadeAttack = false;
                    }
                }
            }
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        stateTime += delta / 3;
        if (isInThirdFrame) {
            offset = (float) Math.sin(stateTime * 100) * 5; // Movimiento hacia los lados
        } else {
            offset = 0;
        }
    }

    public boolean isAnimationFinished() {
        if (animation == null) {
            return true;
        }
        return animation.isAnimationFinished(stateTime * 4);
    }

    public boolean getIsAnimationHeadDone() {
        return isAnimationHeadDone;
    }

    public void setAnimationHeadFalse() {
        isAnimationHeadDone = false;
    }

    public boolean isAnimationWonDone() {
        if (animation == null && animationHead == null) {
            return true;
        }
        return isAnimationWonDone;
    }

    public boolean isAnimationHeadFinished() {
        if (animationHead == null) {
            return true;
        }
        return animationHead.isAnimationFinished(timeHead);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);

        float movementY = Math.abs((float) Math.sin(stateTime * 20f));
        float pendulumSwingXBody = (float) (0.1f * VH_WIDTH * Math.sin(10f * stateTime / 2)) + 1.3f;
        if ((int) (stateTime / 10) % 2 == 1) {
            pendulumSwingXBody = -pendulumSwingXBody;
        }
        if (isAnimationFinished()) {
            float pendulumSwingX = (float) (2f * Math.sin(10f * stateTime / 2)) - 1.5f;
            float movementYBody = (float) (1.5f * Math.sin(20f * stateTime)) - 1.5f;
            if (act != 8) {
                // Cuerpo y piernas primero; la cabeza se dibuja al final para quedar DELANTE.
                batch.draw(body, body.getX() - pendulumSwingX, body.getY() - (VH_HEIGHT * getScaleY() * 0.3f) - movementYBody, body.getOriginX(), body.getOriginY(), body.getRegionWidth(), body.getRegionHeight(), getScaleX(), getScaleY(), 0);
                // Piernas bajadas para que no se solapen con el torso (estaban metidas en el cuerpo).
                batch.draw(legs, legs.getX() + (VH_WIDTH * 1.9f / getScaleX()), legs.getY() + VH_HEIGHT * 0.2f - VH_HEIGHT * 1.5f, legs.getOriginX(), legs.getOriginY(), legs.getRegionWidth(), legs.getRegionHeight(), getScaleX(), getScaleY(), 0);
                if (isAnimationHeadFinished()) {
                    batch.draw(head, body.getX() + VH_WIDTH * 0.3f * getScaleX() - pendulumSwingXBody + 1 * getScaleY() - HEAD_X_CENTER_OFFSET, HEAD_Y_RAISE - 0.5f * VH_HEIGHT * getScaleY() + body.getY() + (float) body.getRegionHeight() / 2 + head.getRegionHeight() - movementY, head.getOriginX(), 0, head.getRegionWidth(), head.getRegionHeight(), getScaleX(), getScaleY(), 0);
                } else {
                    // Cabeza animada (mercy): ajuste local para alinearla un poco a la derecha
                    // y más arriba respecto a la cabeza fija de reposo.
                    TextureRegion currentFrame = animationHead.getKeyFrame(timeHead);
                    float mercyHeadDx = VH_WIDTH * 0.1f;   // a la derecha (un pelín, casi centrada)
                    float mercyHeadDy = VH_HEIGHT * 0.4f;  // hacia arriba
                    batch.draw(currentFrame, body.getX() + VH_WIDTH * 0.3f * getScaleX() - pendulumSwingXBody + 1 * getScaleY() - HEAD_X_CENTER_OFFSET + mercyHeadDx, mercyHeadDy + HEAD_Y_RAISE - 0.5f * VH_HEIGHT * getScaleY() + body.getY() + (float) body.getRegionHeight() / 2 + head.getRegionHeight() - movementY, head.getOriginX(), 0, currentFrame.getRegionWidth(), currentFrame.getRegionHeight(), getScaleX(), getScaleY(), 0);
                }
            } else {
                if (animationBody.getKeyFrameIndex(timeHead) == 6) {
                    isAnimationWonDone = true;
                }
                TextureRegion currentBody = animationBody.getKeyFrame(timeHead);
                TextureRegion currentHead = animationHead.getKeyFrame(timeHead);
                TextureRegion currentLegs = animationLegs.getKeyFrame(timeHead);
                // Ataque final (act 8): la cabeza un poco más arriba.
                float wonHeadDy = VH_HEIGHT * 0.6f;
                batch.draw(currentHead, body.getX() + VH_WIDTH * 0.3f * getScaleX() - pendulumSwingXBody + 1 * getScaleY() - HEAD_X_CENTER_OFFSET + offset, wonHeadDy + HEAD_Y_RAISE - 0.5f * VH_HEIGHT * getScaleY() + body.getY() + (float) body.getRegionHeight() / 2 + head.getRegionHeight() - movementY, head.getOriginX(), 0, head.getRegionWidth(), head.getRegionHeight(), getScaleX(), getScaleY(), 0);
                batch.draw(currentBody, body.getX() - VH_WIDTH * 0.2f + offset, body.getY() - (float) currentBody.getRegionHeight() / 2 - VH_HEIGHT * 0.3f, (float) currentBody.getRegionWidth() / 2, (float) currentBody.getRegionHeight() / 2, currentBody.getRegionWidth(), currentBody.getRegionHeight(), getScaleX(), getScaleY(), 0);
                // Piernas alineadas con el centro del cuerpo (act 8): un poco a la izquierda.
                float wonLegsDx = -VH_WIDTH * 0.5f;
                batch.draw(currentLegs, legs.getX() + offset + wonLegsDx, legs.getY() + VH_HEIGHT * 0.2f, legs.getOriginX(), legs.getOriginY(), currentLegs.getRegionWidth(), currentLegs.getRegionHeight(), getScaleX(), getScaleY(), 0);
            }
        } else {
            TextureRegion currentFrame = animation.getKeyFrame(stateTime * HAND_SPEED);

            // Para el brazo HORIZONTAL (Y==390) el cuerpo se desplaza a la derecha para que el
            // cuello quede bajo la cabeza fija. BODY_RIGHT_SHIFT es el desplazamiento base y
            // armNeckCorrection() compensa que el cuello no está en la misma columna en cada frame.
            // (Calibrado con el banco de pruebas frame a frame contra el sprite real.)
            float armShiftX = 0;
            if (currentFrame.getRegionY() == 390) {
                armShiftX = BODY_RIGHT_SHIFT + armNeckCorrection(currentFrame.getRegionX());
            }

            // El frame de brazo se dibuja primero; la cabeza al final (DELANTE).
            batch.draw(currentFrame, body.getX() + VH_WIDTH * 0.01f + armShiftX, body.getY() - VH_HEIGHT, getOriginX(), getOriginY(), currentFrame.getRegionWidth(), currentFrame.getRegionHeight(), getScaleX(), getScaleY(), 0);

            // La cabeza queda SIEMPRE en su posición de reposo (X e Y idénticas). Sans mueve
            // solo el brazo; la cabeza no se desplaza. Robusto: no depende del origin desplazado
            // del frame de brazo (que antes la hacía volar arriba-derecha).
            // El brazo VERTICAL (Y==297) sí baja un poco la cabeza con extraVerticalDip.
            float headX = body.getX() + VH_WIDTH * 0.3f * getScaleX() - pendulumSwingXBody + 1 * getScaleY() - HEAD_X_CENTER_OFFSET;
            // En las animaciones del cuadro azul (act 4) la cabeza va un poco más abajo.
            float armHeadDy = -VH_HEIGHT * 0.7f;
            float headY = armHeadDy + HEAD_Y_RAISE - 0.5f * VH_HEIGHT * getScaleY() + body.getY()
                    + (float) body.getRegionHeight() / 2 + head.getRegionHeight() - movementY;

            if (currentFrame.getRegionY() == 297) {
                float extraVerticalDip = 0;
                if (currentFrame.getRegionX() == 9) {
                    extraVerticalDip = -0.2f * VH_HEIGHT * getScaleY();
                } else if (currentFrame.getRegionX() == 78) {
                    extraVerticalDip = -0.3f * VH_HEIGHT * getScaleY();
                } else if (currentFrame.getRegionX() == 285 || currentFrame.getRegionX() == 354) {
                    extraVerticalDip = -0.1f * VH_HEIGHT * getScaleY();
                } else if (currentFrame.getRegionX() == 216) {
                    extraVerticalDip = 0.5f * VH_HEIGHT * getScaleY();
                }
                headY += extraVerticalDip;
            }

            batch.draw(head, headX, headY, head.getOriginX(), 0, head.getRegionWidth(), head.getRegionHeight(), getScaleX(), getScaleY(), 0);
        }
    }

    public int advanceAnimation() {
        isInThirdFrame = false;
        if ((5 < animationBody.getKeyFrameIndex(timeHead) && animationBody.getKeyFrameIndex(timeHead) < 7) || ((0 <= animationBody.getKeyFrameIndex(timeHead) && animationBody.getKeyFrameIndex(timeHead) < 2))) {

            if (!Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                isOptionAvailable = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.ENTER) && isOptionAvailable) {
                isOptionAvailable = false;
                timeHead++;
            }
        } else if (2 <= animationBody.getKeyFrameIndex(timeHead) && animationBody.getKeyFrameIndex(timeHead) <= 5) {
            if (animationBody.getKeyFrameIndex(timeHead) == 2) {
                isInThirdFrame = true;
                inflictingDamageSound();
            }
            timeHead = (animationBody.getKeyFrameIndex(timeHead) == 2) ? timeHead + Gdx.graphics.getDeltaTime() / 5 : timeHead + Gdx.graphics.getDeltaTime() / 3;
        }
        return animationBody.getKeyFrameIndex(timeHead);
    }
}
