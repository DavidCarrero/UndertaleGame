package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.game.logic.HitZone;
import com.game.logic.ScoreCalculator;
import java.util.ArrayList;
import static com.badlogic.gdx.graphics.Color.WHITE;
import static com.badlogic.gdx.graphics.Color.YELLOW;
import static com.game.BattleController.act;
import static com.game.BlackScreen.*;
import static com.game.FabricElements.createLabelMessage;
import static com.game.Heart.iteratorItems;
import static com.game.Sounds.*;
import static com.game.StateMessage.*;
import static com.game.Undertale.stage;
import static listener.CollisionListener.isCollided;


public class Events {
    public static int i = 0, iteratorDeleted = 0;

    public static final ArrayList<LabelMessage> items = new ArrayList<>(4);

    public static int optionSelected = 0;
    public static LabelMessage labelSans;
    public static boolean canSelect = true, canAddItems;

    public static boolean isSparing = false;

    private static boolean isWritingMessage = false;

    private static BoxAttack boxAttack;

    public static Hit hit;

    private static BarAttack barAttack;

    private static MissLabel missLabel;

    public static ArrayList<LabelMessage> getItems() {
        return items;
    }

    public static void createTarget(StateMessage state) {
        if (labelSans == null) {
            labelSans = createLabelMessage(state, 40);
        }
        else {
            labelSans.setText(state.getValue());
            labelSans.setState(state);
        }
        labelSans.setWrap(true);
        labelSans.setWidth(500);
        labelSans.setColor(WHITE);
        labelSans.setPosition(boxHeart.getX() + VH_WIDTH * 6, boxHeart.getY() + boxHeart.getHeight() - VH_HEIGHT * 4 - labelSans.getHeight());
        stage.addActor(labelSans);
    }

    public static void createMessage(StateMessage state) {
        heart.remove();
        labelSans.setText(state.getValue());
    }


    public static void writeMessage(String description, StateMessage state) {
        if (labelSans == null ) {
            labelSans = createLabelMessage(state, 40);
            labelSans.setWrap(true);
            labelSans.setWidth(500);
        }

        if (labelSans.getText().length < description.length()) {
            if( !isDialoguePlaying() ) { dialogueSound(); }
            labelSans.setText(labelSans.getText().toString() + description.charAt(i));
            labelSans.setState(state);
            labelSans.setPosition(boxHeart.getX() + VH_WIDTH * 6, boxHeart.getY() + boxHeart.getHeight() - VH_HEIGHT * 12 - labelSans.getHeight());
            i ++;
        } else {
            isWritingMessage = false;
            i = 0;
        }
    }

    public static void createAction() {
        labelSans.setText(CHECK.getValue());
        labelSans.setState(CHECK);
    }

    public static void  createBoxAttack() {
        if (boxAttack == null) {
            boxAttack = new BoxAttack(boxHeart.getX() + boxHeart.getWidth()/2, boxHeart.getY() + boxHeart.getHeight()/2, boxHeart.getWidth(), boxHeart.getHeight());
            stage.addActor(boxAttack);
        }
    }

    public static void createBarAttack() {
        if (barAttack == null) {
            barAttack = new BarAttack(boxHeart.getY() + VH_HEIGHT );
            barAttack.setSpeedBar();
            stage.addActor(barAttack);
        }
    }

    public static void createHit() {
        attackSwipeSound();
        if (hit == null) {
            hit = new Hit(sans.getX(), sans.getY() - 4 * VH_HEIGHT);
            stage.addActor(hit);
        } else if (hit.getStage() == null) {
            stage.addActor(hit);
        }
        hit.animationHit();
    }

    public static void moveBarAttack() {
        if (barAttack != null) {
            barAttack.move();
        }
    }

    public static void createMissLabel() {
        if (missLabel == null) {
            missLabel = new MissLabel(sans.getX(), sans.getY() + 15 * VH_HEIGHT);
        }
    }

    public static void disposeResourceAnimationAttack() {
        if (hit != null) {
            hit.dispose();
            hit = null;
        }
        if (barAttack != null) {
            barAttack.dispose();
            barAttack = null;
        }
        if (boxAttack != null) {
            boxAttack.dispose();
            boxAttack = null;
        }
    }

    public static void selectTarget() {
        moveBarAttack();
        if (sans.isCanAnimateEvadeAttack()) {
            sans.animateVoidAttack();
        }

        if ((labelSans == null || labelSans.getStage() == null || labelSans.getState() == SANS_SPARING ) && boxAttack == null) {
            createTarget(SANS);
            heart.setPositionSelectTarget();
        }
        if ( labelSans != null &&  labelSans.getStage() != null && boxAttack == null && Gdx.input.isKeyPressed(Keys.ENTER) && canSelect) {
            heart.remove();
            labelSans.remove();
            selectSound();
            canSelect = false;
            createBoxAttack();
            createBarAttack();
            createMissLabel();
        }
        if (boxAttack != null && boxAttack.getStage() != null && Gdx.input.isKeyPressed(Keys.ENTER) && canSelect) {
            labelSans.remove();
            barAttack.verifyRegion();
            barAttack.stop();
            sans.activateAnimationEvade();
            sans.animateVoidAttack();
            createHit();
            canSelect = false;
        }

        // BUG FIX: si la barra llega al extremo sin que golpees, se auto-removía del stage y
        // el turno quedaba en limbo (boxAttack visible, nada lo cerraba). Detectamos ese caso
        // (boxAttack presente, barra ya fuera del stage y sin animación de golpe activa) y lo
        // tratamos como FALLO: se cierra el turno sin sumar puntaje.
        if (boxAttack != null && boxAttack.getStage() != null
                && barAttack != null && barAttack.getStage() == null
                && !sans.isCanAnimateEvadeAttack() && !sans.isAnimationEvadeFinished()) {
            disposeResourceAnimationAttack();
            if (labelSans != null) {
                labelSans.remove();
            }
            act = !isSparing ? BattleController.nextPhaseAfter(act) : 8;
            heart.isTurn = false;
            heart.setPositionFight();
            sans.setIsAnimationVoidFinishedFalse();
            canSelect = false;
            return;
        }
        if (barAttack != null && barAttack.getStage() != null && sans.isAnimationEvadeFinished()) {
            float POSITION_BAR_NORMALIZED = barAttack.getX() + barAttack.getWidth()/2 - boxAttack.getX() * VH_WIDTH * 0.16f;

            stage.addActor(missLabel);
            labelSans.remove();
            // Zona de impacto extraída a HitZone; el puntaje lo calcula ScoreCalculator
            // (lógica pura, testeable). Comportamiento idéntico al original. (FASE 0)
            HitZone zone;
            if (isCollided(barAttack, boxAttack.getHitBoxCenter())) {
                zone = HitZone.CENTER;
            } else if (isCollided(barAttack, boxAttack.getHitBoxGreenLeft()) || isCollided(barAttack, boxAttack.getHitBoxGreenRight())) {
                zone = HitZone.GREEN;
            } else if (isCollided(barAttack, boxAttack.getHitBoxYellowLeft()) || isCollided(barAttack, boxAttack.getHitBoxYellowRight())) {
                zone = HitZone.YELLOW;
            } else if (isCollided(barAttack, boxAttack.getHitBoxRedLeft()) || isCollided(barAttack, boxAttack.getHitBoxRedRight())) {
                zone = HitZone.RED;
            } else {
                zone = HitZone.MISS;
            }
            score += ScoreCalculator.attackScore(zone, act, POSITION_BAR_NORMALIZED);
            disposeResourceAnimationAttack();
            act = !isSparing ? BattleController.nextPhaseAfter(act) : 8;

            heart.isTurn = false;
            heart.setPositionFight();
            sans.setIsAnimationVoidFinishedFalse();
        }

        if (Gdx.input.isKeyPressed(Keys.ESCAPE) && barAttack == null && heart.isTurn) {
            disposeResourceAnimationAttack();
            selectSound();
            labelSans.remove();
            heart.setOption(0);
            optionSelected = 0;
            canSelect = false;
            i = 0;
            heart.setPositionFightOp();
        }
    }

    public static void selectAct() {
        if (labelSans == null || labelSans.getStage() == null || labelSans.getState() == SANS_SPARING) {
            createTarget(SANS);
            heart.setPositionSelectTarget();
        }

        if (isWritingMessage) {
            writeMessage("* SANS 1 ATK 1 DEF\n* The easiest enemy.\n* Can only deal 1 damage", ACT_DESCRIPTION);
            return;
        }

        if (labelSans.getStage() != null && Gdx.input.isKeyPressed(Keys.ENTER) && canSelect) {
            switch (labelSans.getState()) {
                case SANS -> {
                    selectSound();
                    createAction();
                    canSelect = false;
                }
                case CHECK -> {
                    selectSound();
                    createMessage(EMPTY);
                    isWritingMessage = true;
                    canSelect = false;
                }
                case ACT_DESCRIPTION -> {
                    selectSound();
                    if (isSparing) {
                        System.out.println("HI");
                        labelSans.remove();
                        heart.setPositionActOp();
                        heart.setOption(0);
                        optionSelected = 0;
                        canSelect = false;
                        stage.addActor(heart);
                        return;
                    }
                        labelSans.remove();
                        act = act == 0 ? 1 : act;
                        heart.isTurn = false;
                        heart.setPositionFight();
                }
            }

        }

        if (Gdx.input.isKeyPressed(Keys.ESCAPE) && canSelect) {
            switch (labelSans.getState()) {
                case SANS -> {
                    selectSound();
                    labelSans.remove();
                    heart.setOption(0);
                    optionSelected = 0;
                    canSelect = false;
                    i = 0;
                    heart.setPositionActOp();
                }
                case CHECK -> {
                    selectSound();
                    createTarget(SANS);
                    canSelect = false;
                }
            }
        }
    }

    public static void doNotShowItems() {
        for (LabelMessage message: items) {
            message.remove();
        }
    }

    public static void doShowItems() {
        for (LabelMessage message: items) {
            stage.addActor(message);
        }
    }

    public static void updateListItems() {
        items.get(iteratorDeleted).remove();
        items.remove(iteratorDeleted);
    }

    public static boolean isItemsInStage() {
        for (LabelMessage message: items) {
            if (message.getStage() == null) {
                return false;
            }
        }
        return true;
    }

    public static void createItems() {
        for (int i = 0; i < 4; i ++) {
            switch (i) {
                case 0 -> {
                    items.add(createLabelMessage(PIE, 40));
                    items.get(i).setPosition(boxHeart.getX() + VH_WIDTH * 7, boxHeart.getY() + boxHeart.getHeight() - VH_HEIGHT * 12);
                }
                case 1 -> {
                    items.add(createLabelMessage(SNOW_PIECE, 40));
                    items.get(i).setPosition(boxHeart.getX() + boxHeart.getWidth()/2 + VH_WIDTH * 7, boxHeart.getY() + boxHeart.getHeight() - VH_HEIGHT * 12);
                }
                case 2, 3 -> {
                    items.add(createLabelMessage(GLAMBURGUER, 40));
                    if (i == 2) {
                        items.get(i).setPosition(boxHeart.getX() + VH_WIDTH * 7, boxHeart.getY() + items.get(i).getHeight());
                    } else {
                        items.get(i).setPosition(boxHeart.getX() + VH_WIDTH * 7+ boxHeart.getWidth()/2, boxHeart.getY() +  items.get(i).getHeight());
                    }
                }
            }
            stage.addActor(items.get(i));
        }
    }

    public static void recoveryHealth() {
        switch (items.get(iteratorDeleted).getState()) {
            case PIE -> heart.setHp(Math.min(heart.getHp() + 90, 90));
            case SNOW_PIECE -> heart.setHp(Math.min(heart.getHp() + 70, 90));
            case GLAMBURGUER -> heart.setHp(Math.min(heart.getHp() + 40, 90));
        }
    }

    public static void selectItem() {
        if (items.isEmpty() && canAddItems) {
            if (labelSans != null) {
                labelSans.remove();
            }
            createItems();
            heart.setPositionSelectItem();
            heart.setOption(4);
            canAddItems = false;
        } else if (!isItemsInStage() && (labelSans.getState() == null || labelSans.getStage() == null || labelSans.getState() == SANS_SPARING)){
            labelSans.remove();
            if (labelSans == null || labelSans.getStage() == null || labelSans.getState() == SANS_SPARING) {
                doShowItems();
            }
            heart.setPositionSelectItem();
            heart.setOption(4);
            canAddItems = false;
        } else if (items.isEmpty() && !canAddItems) {
            return;
        }

        if (isWritingMessage) {
            if (items.get(iteratorDeleted).getState() == PIE) {
                writeMessage("* You have eaten the Pie you have\nrecovered 90 HP", ITEM_CONSUMED);
            } else if(items.get(iteratorDeleted).getState() == SNOW_PIECE) {
                writeMessage("* You have eaten the SnowmanPiece\nyou have recovered 70 HP", ITEM_CONSUMED);
            } else {
                writeMessage("* You have eaten the Glamburguer\nyou have recovered 40 HP", ITEM_CONSUMED);
            }
            return;
        }

        if (labelSans != null && Gdx.input.isKeyPressed(Keys.ENTER) && canSelect && labelSans.getStage() != null && labelSans.getState() == ITEM_CONSUMED) {
            selectSound();
            labelSans.remove();
            updateListItems();
            isWritingMessage = false;
            canSelect = false;
            if (isSparing) {
                heart.setPositionItemOp();
                stage.addActor(heart);
                return;
            }
            act = act == 0 ? 1 : act;
            heart.isTurn = false;
            heart.setPositionFight();
        }
        if (Gdx.input.isKeyPressed(Keys.ENTER) && canSelect) {
            soundHeal();
            doNotShowItems();
            heart.remove();
            createTarget(EMPTY);
            isWritingMessage = true;
            canSelect = false;
            iteratorDeleted = iteratorItems;
            recoveryHealth();
        }
        if (Gdx.input.isKeyPressed(Keys.ESCAPE) && canSelect && (labelSans == null || labelSans.getStage() == null)) {
            selectSound();
            doNotShowItems();
            heart.setOption(0);
            optionSelected = 0;
            canSelect = false;
            i = 0;
            heart.setPositionItemOp();
        }
    }

    public static void selectMercy() {
        if (labelSans == null || labelSans.getStage() == null || labelSans.getState() == SANS_SPARING) {
            createTarget(SPARE);
            if (isSparing) {
                labelSans.setColor(YELLOW);
            }
            heart.setPositionSelectTarget();
        }
        if (labelSans.getStage() != null && Gdx.input.isKeyPressed(Keys.ENTER) && canSelect && !isSparing) {
            selectSound();
            labelSans.remove();
            heart.isTurn = false;
            act = act == 0 ? 1 : act;
            canSelect = false;
            heart.setPositionFight();
        } else if (labelSans.getStage() != null && Gdx.input.isKeyPressed(Keys.ENTER) && canSelect && isSparing) {
            selectSound();
            labelSans.remove();
            heart.isTurn = false;
            act = 7;
            heart.setOption(1);
            heart.setPositionFight();
        }

        if (Gdx.input.isKeyPressed(Keys.ESCAPE) && canSelect) {
            selectSound();
            labelSans.remove();
            heart.setOption(0);
            optionSelected = 0;
            canSelect = false;
            heart.setPositionMercyOp();
        }
    }

    public static void isFightSelected(boolean isOverFightOp) {
        fightOp.updateIsCollided(isCollided(heart, fightOp));
        if (Gdx.input.isKeyPressed(Keys.ENTER) && isOverFightOp && canSelect && boxHeart.mode == 0) {
            selectSound();
            canSelect = false;
            optionSelected = 1;
        }
    }

    public static void messageSparing() {
        if (labelSans == null || labelSans.getStage() == null && boxHeart.mode == 0) {
            createTarget(EMPTY);
            isWritingMessage = true;
        }

        if (isWritingMessage) {
            writeMessage("* Sans is sparing you", SANS_SPARING);
            labelSans.setPosition(labelSans.getX(), boxHeart.getY() +boxHeart.getHeight()/2 + 3 * VH_HEIGHT);

        }
    }

    public static void isActSelected(boolean isOverAct) {
        actOp.updateIsCollided(isCollided(heart, actOp));
        if (Gdx.input.isKeyPressed(Keys.ENTER) && isOverAct && canSelect && boxHeart.mode == 0) {
            selectSound();
            canSelect = false;
            optionSelected = 2;
        }
    }

    public static void isItemSelected(boolean isOverItem) {
        itemOp.updateIsCollided(isCollided(heart, itemOp));
        if (Gdx.input.isKeyPressed(Keys.ENTER) && isOverItem && canSelect && boxHeart.mode == 0) {
            selectSound();
            canSelect = false;
            optionSelected = 3;
        }
    }

    public static void isMercySelected(boolean isOverMercy) {
        mercyOp.updateIsCollided(isCollided(heart, mercyOp));
        if (Gdx.input.isKeyPressed(Keys.ENTER) && isOverMercy && canSelect && boxHeart.mode == 0) {
            selectSound();
            canSelect = false;
            optionSelected = 4;
        }
    }

    public static void detectEvent() {
        if (!Gdx.input.isKeyPressed(Keys.ENTER) && !Gdx.input.isKeyPressed(Keys.ESCAPE) && !canSelect) { canSelect = true;  }
        switch (optionSelected) {
            case 0 -> {
                if (isSparing) messageSparing();
            }
            case 1 -> selectTarget();
            case 2 -> selectAct();
            case 3 -> selectItem();
            case 4 -> selectMercy();

        }

        isFightSelected(isCollided(heart, fightOp));
        isActSelected(isCollided(heart, actOp));
        isItemSelected(isCollided(heart, itemOp));
        isMercySelected(isCollided(heart,mercyOp));
    }
}
