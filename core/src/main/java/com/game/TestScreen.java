package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;

import static com.game.BlackScreen.VH_HEIGHT;
import static com.game.Sans.timeHead;
import static com.game.Sounds.attackSwipeSound;
import static com.game.Undertale.stage;

public class TestScreen implements Screen {

    private Sans sans;

    private boolean isOptionAvailable = true;

    public Hit hit;

    @Override
    public void show() {
        hit = new Hit((float) Gdx.graphics.getWidth() / 2, (float) Gdx.graphics.getHeight() / 2 + 4 * VH_HEIGHT);
        sans = new Sans();

        sans.animationWon();
        stage.addActor(sans);
    }

    @Override
    public void render(float v) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (sans.advanceAnimation() == 1 && isOptionAvailable) {
            isOptionAvailable = false;
            attackSwipeSound();
            hit.animationHit();
            stage.addActor(hit);
        }
        if (hit.isAnimationFinished() && !isOptionAvailable && timeHead == 1) {
            timeHead++;
        }
        stage.act(v);
        stage.draw();
    }

    @Override public void resize(int i, int i1) { }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { }
}
