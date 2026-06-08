package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import listener.CollisionListener;
import java.util.ArrayList;
import static com.badlogic.gdx.math.MathUtils.random;
import static com.game.BattleController.*;
import static com.game.BlackScreen.*;
import static com.game.Sounds.*;
import static com.game.Undertale.batch;
import static com.game.Undertale.shapeRenderer;

public class ObjetsItems extends Actor {
    private Rectangle hitBox;

    private Direction direction;

    private boolean startFadeOut = false;
    private boolean hasFiredSound = false; // el sonido de disparo suena UNA vez, al salir el rayo

    private float opacity = 1f, angle = 0;

    private static final int[] valuesX = { 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6,6, 6, 6, 55, 55, 55, 55,104, 104 , 104, 104, 104, 104 , 149, 149, 149, 197, 197,  245, 245, 245, 245, 245, 197, 197, 149, 149};

    private final float[] Y_DESTINIES = {boxHeart.getY(), boxHeart.getY() + boxHeart.getHeight()/2 - 50, boxHeart.getY() + boxHeart.getHeight() - 80};

    private float yDestinyGasterBlaster = 0;

    private int index = 0;
    private boolean isReverseSense = false;

    private float seconds = 0;
    private Sprite imageObject;

    public Direction getDirection() {
        return direction;
    }

    public void setAngle(float angel) {
        this.angle = angel;
    }

    public float getAngle() {
        return angle;
    }

    public void setIsReverseSense(boolean sense) {
        isReverseSense = sense;
    }

    public boolean getSense() {
        return this.isReverseSense;
    }

    public ObjetsItems(int kind, float x, float y, Direction direction) {
        this.imageObject = new Sprite(new Texture("images/SansSprite.png"));
        this.direction = direction;

        setX(x);
        setY(y);
        switch (kind) {
            case 0 -> {
                // Word sans
                this.imageObject = new Sprite(new Texture("images/Sans.png"));
                hitBox = new Rectangle(getX() - 40, getY(), getWidth() * 0.2f, getHeight());
            }
            case 1 -> {
                // small bone
                this.imageObject.setRegion( 398 , 693 , 12, 52);
                hitBox = new Rectangle( getX() - getWidth() / 2, getY() - getHeight()/2, getWidth()*2.5f-5, getHeight()-8);
            }
            case 2 -> {
                // long bone
                this.imageObject.setRegion( 278 , 573 , 12, 202);
                hitBox = new Rectangle( getX() - getWidth() / 2,  getY() - getHeight() / 2, (getWidth()*2.5f)-4, getHeight()-4);

            }
            case 3 -> {
                // long bones vertical
                setOriginX(getWidth() / 2);
                setOriginY(getHeight() / 2);
                this.imageObject.setRegion( 398 , 573 , 204, 102);
                hitBox = new Rectangle(getX() , getY() , (getWidth()*2.5f)-4, getHeight()*2f-4);
            }
            case 4 -> {
                // long bones horizontal
                setOriginX(getWidth() / 2);
                setOriginY(getHeight() / 2);
                this.imageObject.setRegion( 293 , 573 , 102, 204);
                hitBox = new Rectangle( getX() , getY() , (getWidth()*2.5f)-4, getHeight()*2f-4);
            }
            case 5 -> {
                this.imageObject = new Sprite(new Texture("images/Platform.png"));
                setOriginX(getWidth() / 2);
                setOriginY(getHeight() / 2);
                hitBox = new Rectangle(getX(), getY() + 1, getWidth() * 0.2f, getHeight());
            }
            case 6 -> {
                // long bones vertical
                setOriginX(getWidth() / 2);
                setOriginY(getHeight() / 2);
                this.imageObject.setRegion( 398 , 573 , 204, 102);
                hitBox = new Rectangle(getX() , getY() , (getWidth()*2.5f)-4, getHeight()*3f-4);
            }
        }

    }

    public ObjetsItems(float x, float y, Direction direction) {
        this.imageObject = new Sprite(new Texture("images/Platform.png"));
        setOriginX(getWidth() / 2);
        setOriginY(getHeight() / 2);
        setX(x);
        setY(y);
        this.direction = direction;
        hitBox = new Rectangle(getX(), getY(), getWidth(), getHeight() - 6);
    }

    public ObjetsItems(float x, float y, boolean isLeft) {
        this.imageObject = new Sprite(new Texture("images/SansSprite.png"));
        this.imageObject.setRegion( 104 , 861 , 44, 57);
        setX(x); setY(y);
        setOriginX(getWidth()/2); setOriginY(getHeight()/2);
        if (isLeft) {
            generateUniqueYDestiny(BattleController.bonesRight);
        } else {
            generateUniqueYDestiny(bonesLeft);
        }
    }

    private void generateUniqueYDestiny(ArrayList<ObjetsItems> bones) {
        yDestinyGasterBlaster = Y_DESTINIES[random.nextInt(3)] ;
        if (bones.isEmpty()) {
            return;
        }
        while (bones.getFirst().yDestinyGasterBlaster == yDestinyGasterBlaster) {
            yDestinyGasterBlaster = Y_DESTINIES[random.nextInt(3)];
        }
    }


    public void drawAttackerGasterBlaster(int valueX, float scale) {
        float heightIncrease = 0;
        if (valueX == 149) {
            heightIncrease = 16*scale;
        } else if (valueX == 197) {
            heightIncrease = 24*scale;
        } else if (valueX == 245) {
            heightIncrease = 26*scale;
        } else {
            startFadeOut = true;
        }

        // El sonido de disparo se reproduce justo cuando el rayo aparece (1ª vez), sincronizado
        // con el frame. Antes sonaba al crear el blaster, mucho antes de disparar.
        if (!hasFiredSound) {
            hasFiredSound = true;
            gasterBlasterAttackSound();
        }

        if (startFadeOut && opacity > 0) {
            opacity -= Gdx.graphics.getDeltaTime() / 2;
            if (opacity < 0) {
                opacity = 0;
            }
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(1, 1, 1, opacity));
        float width = Gdx.graphics.getWidth();

        if (getAngle() == 90) {
            shapeRenderer.rect(getX() + getWidth() ,getY() + getHeight()/2 - heightIncrease, width, 2 * heightIncrease);
            if (CollisionListener.isCollided(heart, new Rectangle(getX() + getWidth() ,getY() + getHeight()/2 - heightIncrease, width, 2 * heightIncrease))) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 1.0f, 0));
            }
        }
        else {
            shapeRenderer.rect(0,getY() + getHeight()/2 - heightIncrease, getX() - getWidth()/2, 2 * heightIncrease);
            if (CollisionListener.isCollided(heart, new Rectangle(0,getY() + getHeight()/2 - heightIncrease, width, 2 * heightIncrease))) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 1.0f, 0));
            }
        }
        shapeRenderer.end();
    }

    public void gasterBlasterAttack(float scale, boolean isLeft) {
        this.imageObject.setRegion( valuesX[index], 861 , 44, 57);
        if (valuesX[index] >= 149) {
            drawAttackerGasterBlaster(valuesX[index], scale);
        }
        if (heart.getHp() == 0) {
            return;
        }
        if (getY() == yDestinyGasterBlaster) {
            seconds += Gdx.graphics.getDeltaTime();
            // Tras bajar (rápido), el Gaster Blaster espera ~0.5 s quieto antes de cargar/disparar.
            // Carga entre 0.5 s y 2 s; dispara a partir de 2 s.
            if ( seconds < 2 && seconds > 0.5) {
                if (index < valuesX.length - 1) {
                    index ++;
                }
                else {
                    index = valuesX.length - 14;
                }
            } else if (seconds > 2) {
                index -= Math.max(index, 0);
                if(index == 0) {
                    if (isLeft) {
                        setX(getX() - 15);
                        if (getX() < 0 - getWidth()) {
                            this.delete();
                            bonesLeft.clear();
                        }
                    } else {
                         setX(getX() + 15);
                        if (getX() > Gdx.graphics.getWidth() + getWidth()) {
                            this.delete();
                            bonesRight.clear();
                        }
                    }
                }

            }
        } else {
            setY(Math.max(getY() - 25, yDestinyGasterBlaster));
        }

    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    @Override
    public float getWidth() {
        return this.imageObject.getRegionWidth();
    }

    @Override
    public float getHeight() {
        return this.imageObject.getRegionHeight();
    }

    public void drawPlatform() {
        batch.begin();
        batch.draw(imageObject, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), 1f, 1f, 0);
        batch.end();
    }

    public void draw() {
        batch.begin();
        batch.draw(imageObject,  getX(), getY(), imageObject.getWidth()  * 0.2f , imageObject.getHeight() * 0.2f);
        batch.end();
    }

    public void drawBone() {
        batch.begin();
        batch.draw(imageObject, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), 2.5f, 1f, 0);
        batch.end();
    }

    public void drawBoneMercy() {
        batch.begin();
        batch.draw(imageObject, getX(), getY(), hitBox.getWidth(), hitBox.getHeight());
        batch.end();
    }

    public void drawBoneTab() {
        batch.begin();
        batch.draw(imageObject,  getX(), getY(), getWidth()*2.5f, getHeight()*2f);
        batch.end();
    }

    public void drawGhosterBlaster(float angle, float scale) {
        batch.begin();
        setAngle(angle);
        batch.draw(imageObject, getX(), getY(), getOriginX(), getOriginY(), getWidth(), getHeight(), scale + .5f, scale + 1f, getAngle());
        batch.end();
    }

    public void updateHitBox() {
        hitBox.setX(getX());
        hitBox.setY(getY());
    }

    public void moveLeft() {
        setX(getX() - 7);
        updateHitBox();
    }

    public void moveLeft2() {
        setX(getX() - 5);
        updateHitBox();
    }

    public void movePlatformLeft() {
        setX(getX() - 4);
        updateHitBox();
    }

    public void moveRight() {
        setX(getX() + 7);
        updateHitBox();
    }

    public void moveRight2() {
        setX(getX() + 5);
        updateHitBox();
    }

    public void movePlatformRight() {
        setX(getX() + 4);
        updateHitBox();
    }

    public void moveLeftFast(float min) {
        setX(Math.max(getX() - 15 , min));
        updateHitBox();
    }

    public void moveRightFast(float max) {
        setX( Math.min( getX() + 15 , max));
        updateHitBox();
    }

    public void moveDownFast(float min) {
        setY(Math.max(getY() - 15 , min) );
        updateHitBox();
    }

    public void moveUpFast(float max) {
        setY(Math.min(getY() + 15 , max) );
        updateHitBox();
    }

    public void moveUp(float max) {
        setY(Math.min(getY() + 20 , max) );
        updateHitBox();
    }

    public void delete() {
        this.imageObject.getTexture().dispose();
    }

}
