package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import org.bson.Document;
import java.util.List;

import static com.game.BlackScreen.VH_HEIGHT;
import static com.game.BlackScreen.VH_WIDTH;
import static com.game.Undertale.getFont;
import static com.game.Undertale.stage;

public class FabricElements {
    public static Pixmap createCursor(int lineHeight) {
        // El cursor debe caber dentro del marco del campo (43px de alto).
        // Lo limitamos a ~30px de alto y 2px de ancho para que no se salga ni quede invisible.
        int cursorHeight = Math.min(lineHeight - 10, 30);
        Pixmap cursorPixmap = new Pixmap(2, cursorHeight, Pixmap.Format.RGBA8888);
        cursorPixmap.setColor(Color.WHITE);
        cursorPixmap.fill();
        return cursorPixmap;
    }
    public static TextField.TextFieldStyle createTextFieldStyle() {
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = getFont(30);
        textFieldStyle.fontColor = Color.WHITE;
        return textFieldStyle;
    }

    public static Label.LabelStyle getLabelStyle(int font) {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = getFont(font);
        labelStyle.fontColor = Color.WHITE;
        return labelStyle;
    }

    public static TextField createTextField(float inputNameWidth, TextField.TextFieldStyle textFieldStyle) {
        TextField nameField = new TextField("", textFieldStyle);
        nameField.setPosition((float) Gdx.graphics.getWidth() /2 - inputNameWidth/2, 350);
        nameField.setSize(inputNameWidth, createTextFieldStyle().font.getLineHeight() );
        return nameField;
    }

    public static Label createLabel(String message, int font) {
        return new Label(message, getLabelStyle(font));
    }

    public static LabelMessage createLabelMessage(StateMessage state, int font) {
        return new LabelMessage(createLabel(state.getValue(), font), state);
    }

    public static TextButton.TextButtonStyle getTextButtonStyle() {
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = getFont(30);
        textButtonStyle.fontColor = Color.WHITE;
        return textButtonStyle;
    }

    public static TextButton createConfirmButton(Undertale game, float y, TextField nameField, float inputNameWidth) {
        TextButton confirmButton = new TextButton("Confirm", getTextButtonStyle());
        confirmButton.setPosition((float) Gdx.graphics.getWidth() / 2 - confirmButton.getWidth()/2, y);

        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirmButton.getLabel().setColor(Color.YELLOW);
                if (!nameField.getText().isEmpty()) {
                    game.setName(nameField.getText());
                    game.showBlackScreen();
                    return;
                }
                Label errorEmptyName = new Label("Input a name!", getLabelStyle(30));
                errorEmptyName.setColor(Color.RED);
                errorEmptyName.setPosition((float) Gdx.graphics.getWidth() /2 - inputNameWidth/2, nameField.getY() - errorEmptyName.getHeight() - 20);
                stage.addActor(errorEmptyName);

            }
        });

        confirmButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                confirmButton.getLabel().setColor(Color.YELLOW);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (!confirmButton.isOver()) {
                    confirmButton.getLabel().setColor(Color.WHITE);
                }
            }
        });

        return confirmButton;
    }

    public static TextButton createButtonBack(Undertale game, float y) {
        TextButton backButton = new TextButton("Back", getTextButtonStyle());
        backButton.setPosition((float) Gdx.graphics.getWidth() / 2 - backButton.getWidth()/2, y);

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.showMainMenuScreen();
            }
        });

        backButton.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                backButton.getLabel().setColor(Color.YELLOW);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (!backButton.isOver()) {
                    backButton.getLabel().setColor(Color.WHITE);
                }
            }
        });
        return backButton;
    }

    public static Table createTable(List<Document> topTenScores, float y) {
        Table table = new Table();
        table.setFillParent(true);
        table.defaults().pad(10);
        Label.LabelStyle labelStyleFirstElement = new Label.LabelStyle();
        labelStyleFirstElement.font = getFont(30);
        labelStyleFirstElement.fontColor = Color.BLUE;


        table.add(new Label("Rank", getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
        table.add(new Label("Name", getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
        table.add(new Label("Amount played", getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
        table.add(new Label("Score", getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
        table.add(new Label("Win rate %", getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
        table.add(new Label("Time played", getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
        table.row();
        for (int i = 0; i < topTenScores.size(); i++) {
            Document score = topTenScores.get(i);
            if (i == 0) {
                table.add(new Label(String.valueOf(i + 1), labelStyleFirstElement));
                table.add(new Label(score.getString("name"), labelStyleFirstElement));
                table.add(new Label(String.valueOf(score.getInteger("amount played")), labelStyleFirstElement));
                table.add(new Label(String.valueOf(score.getInteger("score")), labelStyleFirstElement));
                table.add(new Label(String.valueOf(score.getDouble("win rate")), labelStyleFirstElement));
                table.add(new Label(String.valueOf(score.getString("time played")), labelStyleFirstElement));
            } else {
                table.add(new Label(String.valueOf(i + 1), getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
                table.add(new Label(score.getString("name"), getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
                table.add(new Label(String.valueOf(score.getInteger("amount played")), getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
                table.add(new Label(String.valueOf(score.getInteger("score")), getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
                table.add(new Label(String.valueOf(score.getDouble("win rate")), getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
                table.add(new Label(String.valueOf(score.getString("time played")), getLabelStyle((int) (VH_WIDTH*VH_HEIGHT * .3f))));
            }
            table.row();
        }
        table.setPosition(VH_WIDTH * 2.5f, y);
        return table;
    }
}
