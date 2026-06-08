package com.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import java.time.LocalTime;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Undertale extends Game {
    public static SpriteBatch batch;

    public static FreeTypeFontGenerator generator;

    public static Stage stage;

    public static ShapeRenderer shapeRenderer;

    private String name;
    private LocalTime timePlayed;

    public static FileHandle fontFile;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public LocalTime getTimePlayed() {
        return timePlayed;
    }

    public void setTimePlayed(LocalTime timePlayed) {
        this.timePlayed = timePlayed;
    }

    public static BitmapFont getFont(int size) {
        if (generator != null) {
            generator.dispose();
        }
        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/determination.otf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        return generator.generateFont(parameter);
    }

    public void switchScreen(Screen newScreen) {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        setScreen(newScreen);
    }


    public void showBlackScreen() {
        switchScreen(new BlackScreen(this));
    }

    public void showInputNameScreen() {
        switchScreen(new InputNameScreen(this));
    }

    public void showGameOverScreen(Heart heart) { switchScreen(new GameOverScreen(this, heart));}

    public void showMainMenuScreen() {
        switchScreen(new MainMenu(this));
    }
    public void showRankingScreen() { switchScreen(new RankingScreen(this)); }

    public void showTestScreen() { switchScreen(new TestScreen()); }

    public void showCreditsScreen() { switchScreen(new CreditsScreen(this));}

    public void showHowToPlayScreen() { switchScreen(new HowToPlayScreen(this));}


    public static void initData( ) {
        if (shapeRenderer == null) {
            shapeRenderer = new ShapeRenderer();
        }
        if (batch == null) {
            batch = new SpriteBatch();
        }
        if (stage == null) {
            stage = new Stage();
        }

        if (fontFile == null) {
            fontFile = Gdx.files.internal("fonts/determination.otf");
        }
    }


    @Override
    public void create() {
        initData( );
        //showTestScreen();
        showMainMenuScreen();
    }

    @Override
    public void dispose() {
        super.dispose();
        getScreen().dispose();
        if (generator != null) {
            generator.dispose();
        }
        batch.dispose();
        stage.dispose();
    }
}
