package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import static com.game.FabricElements.*;
import static com.game.Sounds.stopAllSounds;
import static com.game.Undertale.*;

public class InputNameScreen implements Screen {
    private final Undertale game;

    private TextField nameField;

    private Texture cursorTexture;

    public InputNameScreen(Undertale game) {
        this.game = game;
        dispose();
    }

    public void createButtons() {
        TextField.TextFieldStyle textFieldStyle = createTextFieldStyle();
        Pixmap cursorPixmap = createCursor((int) textFieldStyle.font.getLineHeight());
        this.cursorTexture = new Texture(cursorPixmap);
        textFieldStyle.cursor = new TextureRegionDrawable(new TextureRegion(this.cursorTexture));
        cursorPixmap.dispose();

        Label inputName = new Label("Input your name: ", getLabelStyle(30));
        inputName.setPosition((float) Gdx.graphics.getWidth() /2 - inputName.getWidth()/2, 500);

        nameField = createTextField(inputName.getWidth(), textFieldStyle);

        stage.addActor(createConfirmButton(game, 200, nameField, inputName.getWidth()));
        stage.addActor(inputName);
        stage.addActor(nameField);
        stage.addActor(createButtonBack(game, stage.getActors().get(0).getY() - stage.getActors().get(0).getHeight()));
    }
    @Override
    public void show() {
        createButtons();
        Gdx.input.setInputProcessor(stage);
        stage.setKeyboardFocus(nameField);
    }

    @Override
    public void render(float v) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act();
        stage.draw();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(nameField.getX() - 10, nameField.getY() - 2, nameField.getWidth() + 20, 43);
        shapeRenderer.end();


    }

    @Override
    public void resize(int i, int i1) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        if (cursorTexture != null) {
            cursorTexture.dispose();
        }

        stopAllSounds();
        stage.clear();
    }
}
