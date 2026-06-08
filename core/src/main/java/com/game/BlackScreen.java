package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import java.time.Duration;
import java.time.LocalTime;

import static com.game.BattleController.*;
import static com.game.DataBase.*;
import static com.game.Events.*;
import static com.game.Sans.timeHead;
import static com.game.Sounds.stopAllSounds;
import static com.game.Undertale.*;

public class BlackScreen implements Screen {

    private static Undertale game;
    public static int score = 0;

    public static Sans sans;

    public static Heart heart;

    private LifeBar lifeBar;
    public static FightOp fightOp;

    public static ActOp actOp;

    public static ItemOp itemOp;

    private Label labelLife, labelLifeValue, labelName;

    public static MercyOp mercyOp;

    public static BoxHeart boxHeart;

    private Texture image, fightImg, actImg, itemImg, mercyImg;

    public static float VH_WIDTH =  (float) Gdx.graphics.getWidth() / 100;
    public static float VH_HEIGHT = (float) Gdx.graphics.getHeight() / 100;

    public BlackScreen(Undertale game) {
        BlackScreen.game = game;
        BlackScreen.game.setTimePlayed(LocalTime.now());
        dispose();
    }

    public static void updateGameOnHeartDeath() {
        if (heart.getHp() == 0) {
            deleteBones();
            timeHead = 0;
            i = 0;
            updatePlayerStats(game.getName(), score, Duration.between(game.getTimePlayed(), LocalTime.now()), false);
            act = 1; score = 0;
            isSparing = false;
            game.showGameOverScreen(heart);
        }
    }

    public static void updateGameOnHeartWon() {
            deleteBones();
            isSparing = false;
            updatePlayerStats(game.getName(), score, Duration.between(game.getTimePlayed(), LocalTime.now()), true);
            act = 1;
            score = 0;
            game.showMainMenuScreen();
    }

    public void loadImages() {
        image = new Texture(Gdx.files.internal("images/heart.png"));
        fightImg = new Texture(Gdx.files.internal("images/FightButton.jpg"));
        actImg = new Texture(Gdx.files.internal("images/ActButton.jpg"));
        itemImg = new Texture(Gdx.files.internal("images/ItemButton.jpg"));
        mercyImg = new Texture(Gdx.files.internal("images/MercyButton.jpg"));
    }

    public void loadObject() {
        sans = new Sans();
        fightOp = new FightOp(fightImg, batch, VH_WIDTH * 2f, VH_HEIGHT * 10);
        actOp = new ActOp(actImg, batch, fightOp.getX() + (fightImg.getWidth() * fightOp.getScaleX()) + VH_WIDTH * 5, fightOp.getY());
        itemOp = new ItemOp(itemImg, batch, actOp.getX() + actImg.getWidth() * actOp.getScaleX() + VH_WIDTH * 5, fightOp.getY());
        mercyOp = new MercyOp(mercyImg, batch, itemOp.getX() + itemImg.getWidth() * itemOp.getScaleX()  + VH_WIDTH * 5, fightOp.getY());
        boxHeart = new BoxHeart(90 * VH_WIDTH, VH_HEIGHT * 30);
        lifeBar = new LifeBar(VH_WIDTH * 40,  (fightOp.getY() + fightOp.getHeight() * fightOp.getScaleY()) + VH_HEIGHT * 7, VH_WIDTH * 40, VH_HEIGHT * 5);
        float heartX = boxHeart.getX() + (boxHeart.getWidth() - image.getWidth() * 0.5f) * 0.5f;
        float heartY = boxHeart.getY() + (boxHeart.getHeight() - image.getHeight() * 0.5f) * 0.5f;
        heart = new Heart(image, heartX, heartY);
        heart.setPositionFightOp();
        heart.updateHitBox();
        stage.addActor(heart);
        stage.addActor(sans);
    }

    public void createLabels()
    {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = getFont(50);
        labelStyle.fontColor = Color.WHITE;

        labelName = new Label(game.getName(), labelStyle);
        labelLife = new Label("PS" + heart.getHp(), labelStyle);

        labelLifeValue = new Label(String.format("%d / 90", (int)(heart.getHp())), labelStyle);

        labelName.setPosition(fightOp.getX() + VH_WIDTH * 2 , lifeBar.getY() - VH_HEIGHT * 1.1f );
        labelLife.setPosition(lifeBar.getX() - labelLife.getWidth()/2, labelName.getY() - VH_HEIGHT * 1.1f  );
        labelLifeValue.setPosition(lifeBar.getX() + lifeBar.getWidth() + 10, labelName.getY() - VH_HEIGHT * 1.1f);

        stage.addActor(labelLife);
        stage.addActor(labelLifeValue);
        stage.addActor(labelName);
    }



    @Override
    public void show() {
        canAddItems = true;
        closeConnection();
        Sounds.fightSound();
        loadImages();
        loadObject();
        createLabels();
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float v) {
        updateGameOnHeartDeath();
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.input.setInputProcessor(KeyListener.get());

        update();

        labelLife.setText("PS");
        labelName.setText(game.getName());
        labelLifeValue.setText(String.format("%d / 90", (int)(heart.getHp())));
        boxHeart.draw(batch, 1);
        fightOp.draw(); actOp.draw(); itemOp.draw(); mercyOp.draw();
        lifeBar.draw();
        lifeBar.setCurrentValue(heart.getHp());
        stage.act(v);
        stage.draw();
    }

    static void drawBlackGround() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 1);
        shapeRenderer.rect(0, boxHeart.getY()+boxHeart.getHeight(), Gdx.graphics.getWidth(), Gdx.graphics.getHeight()-boxHeart.getY()-boxHeart.getHeight());
        shapeRenderer.rect(0, 0, boxHeart.getX(), Gdx.graphics.getHeight());
        shapeRenderer.rect(boxHeart.getX() + boxHeart.getWidth() , 0, Gdx.graphics.getWidth() - (boxHeart.getX()+ boxHeart.getWidth()), Gdx.graphics.getHeight());
        shapeRenderer.rect(0,0, Gdx.graphics.getWidth(), boxHeart.getY());
        shapeRenderer.end();
    }

    void update() {
        if ( !heart.isTurn ) {
            if (!Gdx.input.isKeyPressed(Input.Keys.ENTER) && !canSelect) { canSelect = true;  }
            generateAct();
        } else {
            boxHeart.changeDimensionsMax();
            Heart.canMoveLeft = !Gdx.input.isKeyPressed(Input.Keys.LEFT) || Heart.canMoveLeft;
            Heart.canMoveRight = !Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Heart.canMoveRight;
            stage.draw();
            detectEvent();
            drawBlackGround();
        }

        /*
        *
        * if (boxHeart.getModeBox() == 0) {
            boxHeart.changeDimensionsMinSquare();
        }
        else if( boxHeart.getModeBox() == 2)
        {
            boxHeart.changeDimensionsMin();
        } else if (boxHeart.getModeBox() == 1 )
        {
            boxHeart.changeDimensionsMax();
        }*/

        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            Gdx.app.exit();
        }
    }

    @Override
    public void resize(int width, int height) {
        // Método llamado cuando la pantalla cambia de tamaño.
    }

    @Override
    public void pause() {
        // Método llamado cuando la aplicación se pausa.
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
        optionSelected = 0;
    }

    @Override
    public void dispose() {
        if (labelLife != null && labelLifeValue != null && labelName != null) {
            labelName.clear();
            labelLife.clear();
            labelLifeValue.clear();
        }
        stopAllSounds();
        stage.clear();
        if (fightImg != null) {
            fightImg.dispose();
            actImg.dispose();
            itemImg.dispose();
            mercyImg.dispose();
        }
        if (image != null) {
            image.dispose();
        }
        items.clear();
    }
}
