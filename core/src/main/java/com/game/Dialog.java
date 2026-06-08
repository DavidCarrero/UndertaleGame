package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;

import static com.badlogic.gdx.graphics.Color.BLACK;
import static com.badlogic.gdx.graphics.Color.RED;
import static com.game.BlackScreen.*;
import static com.game.FabricElements.createLabelMessage;
import static com.game.Sounds.*;
import static com.game.StateMessage.EMPTY;

public class Dialog extends Actor {
    private static Sprite bubbleDialog;
    private static int i = 0;

    public static LabelMessage labelDialog;

    @Override
    public Stage getStage() {
        return super.getStage();
    }

    public LabelMessage getLabelDialog() {
        return labelDialog;
    }

    public void writeMessage(StateMessage state) {
        if (labelDialog == null) {
            labelDialog = createLabelMessage(EMPTY, 20);
            labelDialog.setColor(BLACK);
            labelDialog.setWrap(true);
        } else if (state == EMPTY) {
            labelDialog.setText(EMPTY.getValue());
        }

        if (labelDialog.getText().length < state.getValue().length()) {
            sansTalkingSound();
            labelDialog.setText(labelDialog.getText().toString() + state.getValue().charAt(i));
            labelDialog.setState(state);
            System.out.println(labelDialog.getText());
            i++;
        } else {
            i=0;
            stopSansTalkingSound();
        }
    }



    @Override
    public void draw(Batch batch, float parentAlpha) {
        super.draw(batch, parentAlpha);
        if (bubbleDialog == null) {
            bubbleDialog = new Sprite(new Texture("images/BubbleDialogSans.png"));
            bubbleDialog.setPosition(sans.getHead().getX() + sans.getHead().getWidth() + 4 * VH_WIDTH, sans.getHead().getY() -  16 * VH_HEIGHT);
            bubbleDialog.setScale(4f, 3f);

            // El texto se ancla cerca del borde SUPERIOR del bocadillo y crece hacia abajo
            // (wrap activo). Se baja el inicio para que las 3 líneas no se salgan por arriba.
            float bubbleTop = bubbleDialog.getY() + bubbleDialog.getRegionHeight() * bubbleDialog.getScaleY();
            labelDialog.setPosition(sans.getHead().getX() + sans.getHead().getWidth() + 10 * VH_WIDTH, bubbleTop - 5 * VH_HEIGHT);
            labelDialog.setWidth(bubbleDialog.getRegionWidth() * 3.5f - 6*VH_WIDTH);
            labelDialog.setAlignment(com.badlogic.gdx.utils.Align.topLeft);
        }
        batch.draw(bubbleDialog, bubbleDialog.getX() , bubbleDialog.getY(), bubbleDialog.getRegionWidth() * bubbleDialog.getScaleX(), bubbleDialog.getRegionHeight() * bubbleDialog.getScaleY());
    }
}
