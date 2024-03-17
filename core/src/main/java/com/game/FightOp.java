package com.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;

import java.awt.*;

public class FightOp extends Actor {

    private final Rectangle hitBox;
    private final Sprite sprite;
    private final SpriteBatch batch;

    private final int height;

    private final int halfWidth;

    public FightOp(Texture image, SpriteBatch batch, float x, float y) {
        this.height = image.getHeight();
        this.sprite = new Sprite(image);
        this.batch = batch;
        halfWidth = image.getWidth() / 2;

        setX(x); setY(y);
        sprite.setRegion(0, -1, halfWidth - 60, height + 1);
        setHeight(image.getHeight());
        hitBox = new Rectangle(getX(), getY(), getWidth() * 0.2f, getHeight()* 0.2f);
    }

    public Rectangle getHitBox() {
        return hitBox;
    }

    @Override
    public float getWidth() {
        return this.sprite.getWidth();
    }

    @Override
    public float getHeight() {
        return this.sprite.getHeight();
    }

    public void draw() {
        batch.begin();
        float scale = 0.20f;
        batch.draw(sprite,  getX(), getY(), sprite.getWidth() * scale  , sprite.getHeight() * scale ) ;
        batch.end();
    }

    public void updateIsCollided(boolean isCollided) {
         if (isCollided) {
             sprite.setRegion(halfWidth + 55, 0, halfWidth - 60, height);
         }
         else {
             sprite.setRegion(0, -1, halfWidth - 60, height+1);
         }

    }
}
