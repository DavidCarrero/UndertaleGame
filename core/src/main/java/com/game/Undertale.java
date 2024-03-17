package com.game;

import com.badlogic.gdx.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Undertale extends Game {
    @Override
    public void create() {
        setScreen(new BlackScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
        getScreen().dispose();
    }
}
