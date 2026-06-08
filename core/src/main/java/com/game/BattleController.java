package com.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import listener.CollisionListener;
import java.util.ArrayList;
import java.util.Iterator;
import static com.badlogic.gdx.math.MathUtils.random;
import static com.game.BlackScreen.*;
import static com.game.Direction.*;
import static com.game.Events.*;
import static com.game.Sans.timeHead;
import static com.game.Sounds.*;
import static com.game.StateMessage.*;
import static com.game.Undertale.batch;
import static com.game.Undertale.stage;

public class BattleController {
    static int act = 1, nextAttack = 0;
    static boolean canAdd = true, isCanAddLeft = true, isCanAddRight = true;

    private static boolean isOptionAvailable = true;

    private static Dialog dialog;

    static ArrayList<ObjetsItems> platforms = new ArrayList<>();

    static ObjetsItems platformThatIsOver;
    static ArrayList<ObjetsItems> bonesLeft = new ArrayList<>();
    static ArrayList<ObjetsItems> bonesRight = new ArrayList<>();

    static ArrayList<ObjetsItems> bonesUp = new ArrayList<>();

    static ArrayList<ObjetsItems> bonesDown = new ArrayList<>();

    private static boolean canThrowLeft = true, canThrowRight = true, canThrowDown = true, canThrowUp = true;

    private static int direction = 2;

    private static final Direction[] DIRECTIONS = {TOWARD, BACKWARD, UPWARD, DOWNWARD};
    private static Direction dir;
    public static int getDirection() {
        return direction;
    }

    static float secondsTime = 0, secondTimeBetweenTransitions = 0;

    static void throwHeart() {
        switch (direction) {
            case 0 -> {
                if (heart.getY() == boxHeart.getY() + boxHeart.getHeight() - heart.getSprite().getRegionHeight() - 15) {
                    canThrowUp = false;
                }
                if ( canThrowUp ) {
                    heart.setY(Math.min(heart.getY() + 20, boxHeart.getY() + boxHeart.getHeight() - heart.getSprite().getRegionHeight() - 15) );
                }
            }
            case 1 -> {
                if (heart.getX() == boxHeart.getX() + boxHeart.getWidth() - heart.getSprite().getRegionWidth() - 15) {
                    canThrowRight = false;
                }
                if ( canThrowRight ) {
                    heart.setX(Math.min(heart.getX() + 20, boxHeart.getX() + boxHeart.getWidth() - heart.getSprite().getRegionWidth() - 15));
                }
            }
            case 2 ->
            {
                if (heart.getY() == boxHeart.getY() + heart.getSprite().getRegionHeight()) {
                    canThrowDown = false;
                }
                if ( canThrowDown ) {
                    heart.setY(Math.max(heart.getY() - 20, boxHeart.getY() + heart.getSprite().getRegionHeight()));
                }
            }
            case 3 -> {
                if (heart.getX() == boxHeart.getX() + heart.getSprite().getRegionWidth()) {
                    canThrowLeft = false;
                }
                if ( canThrowLeft ) {
                    heart.setX(Math.max(heart.getX() - 20, boxHeart.getX() + heart.getSprite().getRegionWidth()));
                }
            }
        }
    }


    static void generateBonesTab(int direction)
    {
        switch (direction) {
            case 0:
                bonesUp.add(new ObjetsItems(3, boxHeart.getX(), boxHeart.getY() + boxHeart.getHeight(), DOWNWARD));
                break;
            case 1:
                bonesRight.add(new ObjetsItems(4, boxHeart.getX() + boxHeart.getWidth(), boxHeart.getY(), BACKWARD));
                break;
            case 2:
                bonesDown.add(new ObjetsItems(3, boxHeart.getX() , boxHeart.getY() - 204, UPWARD));
                break;
            case 3:
                bonesLeft.add(new ObjetsItems(4, boxHeart.getX() - 250, boxHeart.getY(), TOWARD));
                break;
        }
    }

    static void bonesTab(boolean isGeneratedRight, boolean isGeneratedLeft, boolean isGeneratedDown, boolean isGeneratedUp, float extension) {
        if (boxHeart.getWidth() == boxHeart.MAX_HEIGHT_SQUARE) {
            secondTimeBetweenTransitions += Gdx.graphics.getDeltaTime();
            if (secondTimeBetweenTransitions < 0.8 ) { // 0.8
                canAdd = true;
                if (isGeneratedRight) {
                    heart.setAngle(90);
                    direction = 1;
                    throwHeart();

                    new AlertBox(extension, boxHeart.getHeight() - VH_HEIGHT*3f, boxHeart.getX() + boxHeart.getWidth() - VH_WIDTH * .7f - extension, boxHeart.getY() + VH_HEIGHT * 1.5f).draw(batch, 1);
                }
                if (isGeneratedLeft) {
                    heart.setAngle(-90);
                    direction = 3;
                    throwHeart();

                    new AlertBox( extension, boxHeart.getHeight() - VH_HEIGHT*3f, boxHeart.getX() + VH_WIDTH * .7f, boxHeart.getY() + VH_HEIGHT * 1.5f).draw(batch, 1);
                }
                if (isGeneratedUp) {
                    heart.setAngle(180);
                    direction = 0;
                    throwHeart();

                    new AlertBox(boxHeart.getWidth() - VH_HEIGHT*2.5f, extension - VH_WIDTH * .7f, boxHeart.getX() + VH_WIDTH * .7f, boxHeart.getY() + boxHeart.getHeight() - extension).draw(batch, 1);
                }
                if (isGeneratedDown) {
                    heart.setAngle(0);
                    direction = 2;
                    throwHeart();

                    new AlertBox(boxHeart.getWidth() - VH_HEIGHT*2.5f , extension - VH_WIDTH * .7f, boxHeart.getX() + VH_WIDTH * .7f, boxHeart.getY() + VH_WIDTH * .7f).draw(batch, 1);
                }
                switch (direction) {
                    case 0 -> {
                        if (sans.isAnimationFinished()) {
                            sans.animationUpwardHand();
                        }
                    }
                    case 1 -> {
                        if (sans.isAnimationFinished()) {
                            sans.animationRightHand();
                        }
                    }
                    case 2 -> {
                        if (sans.isAnimationFinished()) {
                            sans.animationDownwardHand();
                        }
                    }
                    case 3 -> {
                        if (sans.isAnimationFinished()) {
                            sans.animationLeftHand();
                        }
                    }
                }
            } else if (secondTimeBetweenTransitions > 0.8) {
                canThrowLeft = canThrowUp = canThrowRight = canThrowDown = true;

                if (isGeneratedRight) {
                    if (bonesRight.isEmpty() && canAdd) {
                        alertSound();
                        generateBonesTab(1);
                    }
                    moveBonesLeftFast(extension);
                }
                if (isGeneratedLeft) {

                    if (bonesLeft.isEmpty() && canAdd) {
                        alertSound();
                        generateBonesTab(3);
                    }
                    moveBonesRightFast();
                }
                if (isGeneratedUp) {

                    if (bonesUp.isEmpty() && canAdd) {
                        alertSound();
                        generateBonesTab(0);
                    }
                    moveBonesDownFast(extension);
                }
                if (isGeneratedDown) {
                    if (bonesDown.isEmpty() && canAdd) {
                        alertSound();
                        generateBonesTab(2);
                    }
                    moveBonesUpFast();
                }
                canAdd = false;

            }
        }
    }

    static void setConfigTurnPlayer() {
        heart.isTurn = true;
        switch (optionSelected) {
            case 1 -> heart.setPositionFightOp();
            case 2 -> heart.setPositionActOp();
            case 3 -> heart.setPositionItemOp();
            case 4 -> heart.setPositionMercyOp();
        }
        heart.setColorRed();
        secondTimeBetweenTransitions = secondsTime = 0;
        score += act * 100;
        if ( heart.getHp() > 0 ) {
            score += (int) (heart.getHp()/90 * 100);
        }
        optionSelected = 0;
        deleteBones();
    }

    static void generateBones() {
        secondsTime+= Gdx.graphics.getDeltaTime();
        secondTimeBetweenTransitions += Gdx.graphics.getDeltaTime();
        if ( secondTimeBetweenTransitions < 0.7) {
            canAdd = true;
        } else if (canAdd && secondTimeBetweenTransitions > 0.7) {
            bonesLeft.add(new ObjetsItems(1,boxHeart.getX() + boxHeart.getWidth(), boxHeart.getY() - 20, TOWARD));
            bonesLeft.add(new ObjetsItems(2,boxHeart.getX() + boxHeart.getWidth(), boxHeart.getY() + boxHeart.getHeight()/2 - 30, TOWARD));
            bonesRight.add(new ObjetsItems(2, boxHeart.getX() - 20 , boxHeart.getY() + boxHeart.getHeight()/2 - 30, BACKWARD));
            bonesRight.add(new ObjetsItems(1, boxHeart.getX() - 20 , boxHeart.getY() - 20, BACKWARD));
            canAdd = false;
            secondTimeBetweenTransitions = 0;
        }
        moveBonesLeft();
        moveBonesRight();
        if (secondsTime > 8) {
            setConfigTurnPlayer();
        }
    }

    static void generateBones2() {
        secondsTime+= Gdx.graphics.getDeltaTime();
        secondTimeBetweenTransitions += Gdx.graphics.getDeltaTime();
        if ( secondTimeBetweenTransitions < 0.6) {
            canAdd = true;
        } else if (canAdd && secondTimeBetweenTransitions > 0.6) {
            bonesLeft.add(new ObjetsItems(2,boxHeart.getX() + boxHeart.getWidth(), boxHeart.getY() + boxHeart.getHeight()/2 - VH_HEIGHT * 7f, BACKWARD));
            bonesLeft.add(new ObjetsItems(1, boxHeart.getX() - 20 , boxHeart.getY() - VH_HEIGHT * 0.5f, TOWARD));
            canAdd = false;
            secondTimeBetweenTransitions = 0;
        }
        moveBones2();

        if (secondsTime > 8) {
            setConfigTurnPlayer();
        }
    }

    static void generateBonesSparing() {
        secondsTime+= Gdx.graphics.getDeltaTime();
        secondTimeBetweenTransitions += Gdx.graphics.getDeltaTime();
        if ( secondsTime < 0.6) {
            canAdd = true;
        } else if (canAdd && secondsTime > 0.6) {
            bonesDown.add(new ObjetsItems(6, boxHeart.getX(), 0 - VH_HEIGHT * 10f, UPWARD));
            canAdd = false;
        }
        moveBonesSparing();
    }

    static void gastersBlasterAttack(boolean isLeft) {
        if (isLeft && !bonesLeft.isEmpty()) {
            bonesLeft.getFirst().gasterBlasterAttack((float) 1.0, true);
            return;
        }
        if (!bonesRight.isEmpty()) {
            bonesRight.getFirst().gasterBlasterAttack((float) 1.0, false);
        }
    }

    static void generateGastersBlaster() {
        secondsTime += Gdx.graphics.getDeltaTime();
        generatePlatform();
        if ( secondsTime < 0.7) {
            isCanAddRight = isCanAddLeft = true;
        } else if (secondsTime > 0.7) {
            if (bonesLeft.isEmpty()) {
                bonesLeft.add(new ObjetsItems(boxHeart.getX()/2 , Gdx.graphics.getHeight(), true));
                isCanAddLeft = false;
                // El sonido de disparo ya NO se reproduce aquí (al aparecer), sino dentro de
                // gasterBlasterAttack en el instante en que el rayo realmente sale.
            }
            if (bonesRight.isEmpty()) {
                bonesRight.add(new ObjetsItems(boxHeart.getX() + boxHeart.getWidth() + boxHeart.getX()/2, Gdx.graphics.getHeight(), false));
                isCanAddRight = false;
            }
        }
        gastersBlasterAttack(true);
        gastersBlasterAttack(false);


        if (secondsTime > 20) {
            setConfigTurnPlayer();
            deleteBones();
            canAdd = false;
            secondTimeBetweenTransitions = secondsTime = 0;
        }

    }
    public static void clearList(ArrayList<ObjetsItems> bones) {
        Iterator<ObjetsItems> iteratorLeft = bones.iterator();
        while (iteratorLeft.hasNext()) {
            ObjetsItems bone = iteratorLeft.next();
            bone.delete();
            iteratorLeft.remove();
        }
    }

    public static void deleteBones() {
        clearList(bonesLeft);
        clearList(bonesRight);
        clearList(bonesUp);
        clearList(bonesDown);
        clearList(platforms);
        nextAttack = 0;
        platformThatIsOver = null;
        secondsTime = 0;
        secondTimeBetweenTransitions = 0;
        heart.setStatic_y(0);
    }


    public static void drawBones() {
        for(ObjetsItems bone : bonesLeft) {
            bone.drawBone();
        }
        for(ObjetsItems bone : bonesRight ) {
            bone.drawBone();
        }
    }

    public static void drawBonesTab() {
        for(ObjetsItems bone : bonesLeft) {
            bone.drawBoneTab();
        }
        for(ObjetsItems bone : bonesRight ) {
            bone.drawBoneTab();
        }
        for(ObjetsItems bone : bonesUp ) {
            bone.drawBoneTab();
        }
        for(ObjetsItems bone : bonesDown ) {
            bone.drawBoneTab();
        }
    }

    public static void drawBonesMercy() {
        for (ObjetsItems bone: bonesDown) {
            bone.drawBoneMercy();
        }
    }

    public static void drawGasterBlaster(float scale) {
        for(ObjetsItems bone : bonesLeft) {
            bone.drawGhosterBlaster(90, scale);
        }
        for(ObjetsItems bone : bonesRight ) {
            bone.drawGhosterBlaster(-90, scale);
        }
    }

    static void moveBonesLeft() {
        for (Iterator<ObjetsItems> iterator = bonesLeft.iterator(); iterator.hasNext();) {
            ObjetsItems bone = iterator.next();
            if (bone.getDirection() != TOWARD ) {
                continue;
            }

            bone.moveLeft();
            if (CollisionListener.isCollided(heart, bone)) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
            }
            if (bone.getX() < boxHeart.getX() - 30) {
                bone.delete();
                iterator.remove();
            }
        }


    }

    static void moveBonesSparing() {
        for (ObjetsItems bone : bonesDown) {
            if (bone.getDirection() == UPWARD) {
                bone.moveUp(boxHeart.getY() - 3 * VH_HEIGHT);
                if (CollisionListener.isCollided(heart, bone)) {
                    soulDamaged();
                    heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
                }
            }
        }
    }

    static void moveBones2() {
        for (Iterator<ObjetsItems> iterator = bonesLeft.iterator(); iterator.hasNext();) {
            ObjetsItems bone = iterator.next();
            if (bone.getDirection() == TOWARD ) {
                bone.moveRight2();
                if (CollisionListener.isCollided(heart, bone)) {
                    soulDamaged();
                    heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
                }
                if (bone.getX() > boxHeart.getX() + boxHeart.getWidth() + 30) {
                    bone.delete();
                    iterator.remove();
                }
            }
            if (bone.getDirection() == BACKWARD ) {
                bone.moveLeft2();
                if (CollisionListener.isCollided(heart, bone)) {
                    soulDamaged();
                    heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
                }
                if (bone.getX() < boxHeart.getX() - 30) {
                    bone.delete();
                    iterator.remove();
                }
            }
        }


    }

    static void moveBonesLeftFast(float extension) {
        for (Iterator<ObjetsItems> iterator = bonesRight.iterator(); iterator.hasNext();) {
            ObjetsItems bone = iterator.next();
            if (CollisionListener.isCollided(heart, bone)) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
            }
            if(bone.getX() > boxHeart.getX() + boxHeart.getWidth() - 20 - extension && !bone.getSense()) {
                bone.moveLeftFast(boxHeart.getX() + boxHeart.getWidth() - 20 - extension);
            } else {
                bone.setIsReverseSense(true);
            }
            if (bone.getX() < boxHeart.getX() + boxHeart.getWidth() && bone.getSense()) {
                bone.moveRightFast(boxHeart.getX() + boxHeart.getWidth());
                if (bone.getX() >= boxHeart.getX() + boxHeart.getWidth() ) {
                    bone.delete();
                    iterator.remove();
                    if (bonesDown.isEmpty() && bonesUp.isEmpty() && bonesLeft.isEmpty() && bonesRight.isEmpty()) {
                        secondTimeBetweenTransitions = 0;
                        nextAttack ++;
                        dir = null;
                    }
                }
            }
        }


    }

    static void moveBonesRight() {
        for (Iterator<ObjetsItems> iterator = bonesRight.iterator(); iterator.hasNext();) {
            ObjetsItems bone = iterator.next();
            if ( bone.getDirection() != BACKWARD ) {
                continue;
            }
            bone.moveRight();
            if (CollisionListener.isCollided(heart, bone)) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
            }
            if (bone.getX() > boxHeart.getX() + boxHeart.getWidth() + 30) {
                bone.delete();
                iterator.remove();
            }
        }
    }

    static void moveBonesRightFast() {
        for (Iterator<ObjetsItems> iterator = bonesLeft.iterator(); iterator.hasNext();) {
            ObjetsItems bone = iterator.next();
            if (CollisionListener.isCollided(heart, bone)) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 0.5f, 0));

            }
            if(bone.getX() < boxHeart.getX() - 40 - bone.getWidth() && !bone.getSense()) {
                bone.moveRightFast(boxHeart.getX() - 40 - bone.getWidth());
            } else {
                bone.setIsReverseSense(true);
            }
            if (bone.getX() > 100 && bone.getSense()) {
                bone.moveLeftFast(100);
                if (bone.getX() <= 100) {
                    bone.delete();
                    iterator.remove();
                    if (bonesDown.isEmpty() && bonesUp.isEmpty() && bonesLeft.isEmpty() && bonesRight.isEmpty()) {
                        secondTimeBetweenTransitions = 0;
                        nextAttack ++;
                        dir = null;
                    }
                }
            }
        }

    }

    static void moveBonesDownFast(float extension) {
        for (Iterator<ObjetsItems> iterator = bonesUp.iterator(); iterator.hasNext();) {
            ObjetsItems bone = iterator.next();
            if (CollisionListener.isCollided(heart, bone)) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 0.5f, 0));

            }
            if(bone.getY() > boxHeart.getY() + boxHeart.getHeight() - 10 - bone.getHeight() && !bone.getSense()) {
                bone.moveDownFast(boxHeart.getY() + boxHeart.getHeight() - 10 - bone.getHeight());
            } else {
                bone.setIsReverseSense(true);
            }
            if (bone.getY() < boxHeart.getY() + boxHeart.getHeight() + extension && bone.getSense()) {
                bone.moveUpFast(boxHeart.getY() + boxHeart.getHeight() + extension);
                if (bone.getY() >= boxHeart.getY() + boxHeart.getHeight() + extension) {
                    bone.delete();
                    iterator.remove();
                    if (bonesDown.isEmpty() && bonesUp.isEmpty() && bonesLeft.isEmpty() && bonesRight.isEmpty()) {
                        secondTimeBetweenTransitions = 0;
                        nextAttack ++;
                        dir = null;
                    }
                }
            }
        }

    }

    static void generatePlatform() {
        if ( secondTimeBetweenTransitions > 0.8){
            secondTimeBetweenTransitions = 0;
            canAdd = true;
        }
        if (secondTimeBetweenTransitions == 0 && canAdd) {
            canAdd = false;
            platforms.add(new ObjetsItems(boxHeart.getX() + boxHeart.getWidth(), boxHeart.getY() + boxHeart.getHeight()/2 - 60, BACKWARD));
            platforms.add(new ObjetsItems(boxHeart.getX() - 20, boxHeart.getY() + boxHeart.getHeight() - 100, TOWARD));
        }
        secondTimeBetweenTransitions += Gdx.graphics.getDeltaTime();
    }

    static void drawPlatforms() {
        Iterator<ObjetsItems> iterator = platforms.iterator();
        while (iterator.hasNext()) {
            ObjetsItems platform = iterator.next();
            platform.drawPlatform();

            if (platform.getDirection() == TOWARD) {

                if (CollisionListener.isCollided(heart, platform.getHitBox()) && platform.getY() + platform.getHeight()/2 < heart.getY() - heart.getHeight()/2 && platformThatIsOver == null) {
                    platformThatIsOver = platform;
                }

                if (platform == platformThatIsOver) {
                    if (CollisionListener.isCollided(heart, platform.getHitBox()) && platform.getY() + platform.getHeight()/2 < heart.getY() - heart.getHeight()/2 && heart.getYStatic() == 0) {
                        heart.setStatic_y(platform.getY() - heart.getHeight()/2 - 4);
                    } else if (!CollisionListener.isCollided(heart, platform.getHitBox()) ){
                        platformThatIsOver = null;
                        heart.setStatic_y(0);
                    }
                    if (!Gdx.input.isKeyPressed(Input.Keys.LEFT)){
                        heart.setX(Math.min(heart.getX() + 4, boxHeart.getX() + boxHeart.getWidth() - heart.getSprite().getRegionWidth() - 15 ));
                    }
                }


                platform.movePlatformRight();
                if (platform.getX() > boxHeart.getWidth() + boxHeart.getX() + platform.getWidth() /*boxHeart.getX() + boxHeart.getWidth() + platform.getWidth()*/) {
                    platform.delete();
                    iterator.remove();
                }
            }
            else {
                if (CollisionListener.isCollided(heart, platform.getHitBox()) && platform.getY() + platform.getHeight()/2 < heart.getY() - heart.getHeight()/2 && platformThatIsOver == null) {
                    platformThatIsOver = platform;
                }

                if (platform == platformThatIsOver) {
                    if (CollisionListener.isCollided(heart, platform.getHitBox()) && platform.getY() + platform.getHeight()/2 < heart.getY() - heart.getHeight()/2 && heart.getYStatic() == 0) {
                        heart.setStatic_y(platform.getY() - heart.getHeight()/2 - 4);
                    } else if (!CollisionListener.isCollided(heart, platform.getHitBox())) {
                        platformThatIsOver = null;
                        heart.setStatic_y(0);
                    }
                    if (!Gdx.input.isKeyPressed(Input.Keys.RIGHT)){
                        heart.setX(Math.max(heart.getX() - 4, boxHeart.getX() + heart.getSprite().getRegionWidth()));
                    }
                }

                platform.movePlatformLeft();
                if (platform.getX() < boxHeart.getX() - platform.getWidth() ) {
                    platform.delete();
                    iterator.remove();
                }
            }

        }
    }

    static void moveBonesUpFast() {
        Iterator<ObjetsItems> iterator = bonesDown.iterator();
        while (iterator.hasNext()) {
            ObjetsItems bone = iterator.next();
            if (CollisionListener.isCollided(heart, bone)) {
                soulDamaged();
                heart.setHp(Math.max(heart.getHp() - 0.5f, 0));
            }
            if(bone.getY() < boxHeart.getY() - bone.getHeight() && !bone.getSense()) {
                bone.moveUpFast(boxHeart.getY() - bone.getHeight());
            } else {
                bone.setIsReverseSense(true);
            }
            if (bone.getY() > 0 && bone.getSense()) {
                bone.moveDownFast(0);
                if (bone.getY() <= 0) {
                    iterator.remove();
                    bone.delete();
                    if (bonesDown.isEmpty() && bonesUp.isEmpty() && bonesLeft.isEmpty() && bonesRight.isEmpty()) {
                        secondTimeBetweenTransitions = 0;
                        nextAttack ++;
                        dir = null;
                    }
                }
            }
        }

    }

    public static void createDialog() {
        if (dialog == null) {
            dialog = new Dialog();
        }
    }


    static void generateAct() {
        switch (act) {
            case 1 -> {
                heart.setOption(3);
                boxHeart.changeDimensionsMinSquare();
                drawBonesTab();
                drawBlackGround();
                if (nextAttack != 12) {
                    if (dir == null) {
                        dir = DIRECTIONS[random.nextInt(4)];
                    }
                    switch (dir) {
                        case UPWARD -> bonesTab(false, false, false, true, boxHeart.getHeight() / 3 - 20);
                        case TOWARD -> bonesTab(true, false, false, false, boxHeart.getHeight() / 3 - 20);
                        case DOWNWARD -> bonesTab(false, false, true, false, boxHeart.getHeight() / 3 - 20);
                        case BACKWARD -> bonesTab(false, true, false, false, boxHeart.getHeight() / 3 - 20);
                    }
                } else  {
                    nextAttack = 0;
                    heart.setAngle(0);
                    setConfigTurnPlayer();
                }
            }
            case 2-> {
                heart.setOption(2);
                boxHeart.changeDimensionsMin();
                generateBones();
                drawBones();
                drawBlackGround();
            }

            case 3-> {
                heart.setOption(3);
                boxHeart.changeDimensionsMinSquare();
                drawBonesTab();
                drawBlackGround();

                switch (nextAttack) {
                    case 0 -> bonesTab(true, false, false, false, boxHeart.getHeight() / 2 - 20);
                    case 1 -> bonesTab(false, true, false, false, boxHeart.getHeight() / 2 - 20);
                    case 2 -> bonesTab(false, false, true, false, boxHeart.getHeight() / 2 - 20);
                    case 3 -> bonesTab(false, false,false, true, boxHeart.getHeight() / 2 - 20);
                    case 4 -> bonesTab(true, false, true, false, boxHeart.getHeight() / 2 - 20);
                    case 5 -> bonesTab(false, true, false, true, boxHeart.getHeight() / 2 - 20);
                    case 6 -> bonesTab(false, true, true, false, boxHeart.getHeight() / 2 - 20);
                    case 7 -> bonesTab(false, true, true, true, boxHeart.getHeight() / 2 - 20);
                    case 8 -> bonesTab(true, true, true, true, boxHeart.getHeight() / 2 - 20);
                    case 9 -> {nextAttack = 0;
                        setConfigTurnPlayer();
                    }
                }
            }
            case 4 -> {
                boxHeart.changeDimensionsMin();
                heart.setOption(2);
                drawPlatforms();
                drawBlackGround();
                generateGastersBlaster();
                drawGasterBlaster(2f);
            }
            case 5 -> {
                heart.setOption(2);
                boxHeart.changeDimensionsMin();
                generateBones2();
                drawBones();
                drawBlackGround();
            }
            case 6 -> {
                heart.setOption(1);
                boxHeart.changeDimensionsMinSquare();
                isSparing = true;
                sans.setAnimationMercy();
                createDialog();
                if (!sans.getIsAnimationHeadDone() && boxHeart.mode == 2) {
                    sans.animationHeadMercy();
                    dialog.writeMessage(EMPTY);
                    if (dialog.getStage() == null) {
                        stage.addActor(dialog);
                        stage.addActor(dialog.getLabelDialog());
                    }
                    timeHead ++;
                }
                if (sans.getIsAnimationHeadDone() && sans.isAnimationHeadFinished()) {
                    dialog.remove();
                    dialog.getLabelDialog().remove();
                }
                if (!sans.isAnimationHeadFinished() && canSelect && Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                    dialog.writeMessage(EMPTY);
                    timeHead ++;
                    canSelect = false;
                }
                if (dialog.getStage() != null) {
                    switch (sans.getAnimationHead().getKeyFrameIndex(timeHead)) {
                        case 1 -> {
                            dialog.writeMessage(SANS_DIALOG_1);
                        }
                        case 2 -> {
                            dialog.writeMessage(SANS_DIALOG_2);
                        }
                        case 3 -> {
                            dialog.writeMessage(SANS_DIALOG_3);
                        }
                    }
                } else if (timeHead == 4) {
                    timeHead = 1;
                    sans.setAnimationHeadFalse();
                    setConfigTurnPlayer();
                    mercySound();
                    sans.animationWon();
                }
                stopFightSound();
            }
            case 7 -> {
                heart.setOption(1);
                boxHeart.changeDimensionsMinSquare();
                isSparing = true;
                createDialog();
                if (!sans.getIsAnimationHeadDone() && boxHeart.mode == 2) {
                    sans.animationHeadMercy2();

                    dialog.writeMessage(EMPTY);
                    if (dialog.getStage() == null) {
                        stage.addActor(dialog);
                        stage.addActor(dialog.getLabelDialog());
                    }
                }
                if (sans.getIsAnimationHeadDone() && sans.isAnimationHeadFinished()) {
                    dialog.remove();
                    dialog.getLabelDialog().remove();
                }
                if (!sans.isAnimationHeadFinished() && canSelect && Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                    dialog.writeMessage(EMPTY);
                    timeHead ++;
                    canSelect = false;
                }
                if (dialog.getStage() != null) {
                    switch (sans.getAnimationHead().getKeyFrameIndex(timeHead)) {
                        case 0 -> {
                            dialog.writeMessage(SANS_DIALOG_4);
                        }
                        case 1 -> {
                            dialog.writeMessage(SANS_DIALOG_5);
                        }
                        case 2 -> {
                            dialog.writeMessage(SANS_DIALOG_6);
                        }
                        case 3 -> {
                            fallChildSound();
                            dialog.writeMessage(SANS_DIALOG_7);
                            sans.setAnimationHeadWhite();
                        }
                    }
                } else if (timeHead == 4) {
                    sans.setAnimationHeadWhite();
                    generateBonesSparing();
                    drawBonesMercy();
                    drawBlackGround();
                }
            }
            case 8 -> {
                stopHisTheme();
                heart.setOption(1);
                boxHeart.changeDimensionsMinSquare();
                int valueAnimation = sans.advanceAnimation();
                if ((valueAnimation==0 || valueAnimation ==6) && dialog.getStage() == null) {
                    dialog.writeMessage(EMPTY);
                    if (dialog.getStage() == null) {
                        stage.addActor(dialog);
                        stage.addActor(dialog.getLabelDialog());
                    }
                } else if (valueAnimation==0 && dialog.getStage() != null)
                {
                    dialog.writeMessage(SANS_DIALOG_8);
                }


                if(valueAnimation==1 && isOptionAvailable) {
                    isOptionAvailable = false;
                    dialog.remove();
                    dialog.getLabelDialog().remove();
                    attackSwipeSound();
                    createHit();
                } else if (valueAnimation==6) {
                    dialog.writeMessage(SANS_DIALOG_9);
                    if (Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                        timeHead = 0;
                        i = 0;
                        isOptionAvailable = true;
                        updateGameOnHeartWon();
                    }
                }

                if (hit != null && hit.isAnimationFinished() && !isOptionAvailable && timeHead == 2) {
                    timeHead++;
                }

            }


        }
    }


}
