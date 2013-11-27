package bm.gfx;

import java.awt.Graphics2D;

import bm.Bomb;
import bm.Player;
import bm.sfx.SoundManager;

/**
 * This class provides objects that will be interpreted as Bombs. They are
 * usually held inside a two-dimensional array in objects of the type Level.
 * 
 * This is a subclass of Bomb that provides a draw method as well as sound
 * effects via the static SoundManager.
 * 
 * @author tobi
 * 
 */
public class FxBomb extends Bomb implements Drawable {

    private byte animCounter = (byte) (EXPLODINGTIME / EXPLBOT.length);
    protected byte animFrame = 0;

    protected FxLevel bLevel;

    public FxBomb(int posX, int posY, FxLevel bLevel, Player player) {
        super(posX, posY, bLevel, player);
        this.bLevel = bLevel;
    }

    public FxBomb(int posX, int posY, FxLevel bLevel, Player player,
            byte radius, boolean quadDamage) {
        super(posX, posY, bLevel, player, radius, quadDamage);
        this.bLevel = bLevel;
    }

    @Override
    protected void updateCountingBomb() {
        super.updateCountingBomb();
        if (animCounter > 0)
            animCounter--;
        else {
            animFrame = (byte) ((animFrame + 1) % BOMB.length);
            animCounter = (byte) (EXPLODINGTIME / EXPLBOT.length);
        }
    }

    @Override
    protected void updateExplodingBomb() {
        super.updateExplodingBomb();
        if (animCounter > 0)
            animCounter--;
        else {
            animFrame = (byte) ((animFrame + 1) % BOMB.length);
            animCounter = (byte) (EXPLODINGTIME / EXPLBOT.length);
        }
    }

    @Override
    public void explode() {
        explode(true);
    }

    public void explode(boolean playSound) {
        if (playSound)
            playExplodingSound();
        animCounter = animFrame = 0;
        super.explode();
    }

    protected void playExplodingSound() {
        SoundManager.staticInstance.playSound(1);
    }

    public void draw(Graphics2D g) {
        if (state == EXPLODED)
            return;
        if (state == COUNTING)
            drawBomb(g);
        else if (state == EXPLODING) {
            drawMiddleFire(g);
            for (int i = 1; i <= right; i++)
                drawHorizontalFire(i, true, g);
            for (int i = 1; i <= left; i++)
                drawHorizontalFire(-i, false, g);
            for (int i = 1; i <= top; i++)
                drawVerticalFire(-i, true, g);
            for (int i = 1; i <= bottom; i++)
                drawVerticalFire(i, false, g);
        }
    }

    private void drawBomb(Graphics2D g) {
        bLevel.markForUpdate(posX, posY);
        int dim = bLevel.getTileDim();
        BOMB[animFrame].draw(posX * dim, posY * dim, dim, dim, g);
    }

    protected void drawMiddleFire(Graphics2D g) {
        bLevel.markForUpdate(posX, posY);
        int dim = bLevel.getTileDim();
        EXPLMID[animFrame].draw(posX * dim, posY * dim, dim, dim, g);
    }

    protected void drawHorizontalFire(int i, boolean toRight, Graphics2D g) {
        bLevel.markForUpdate(posX + i, posY);
        int dim = bLevel.getTileDim();
        if (toRight && i == right)
            EXPLRIG[animFrame].draw((posX + right) * dim, posY * dim, dim, dim,
                    g);
        else if (i == -left)
            EXPLLEF[animFrame].draw((posX - left) * dim, posY * dim, dim, dim,
                    g);
        else EXPLHOR[animFrame].draw((posX + i) * dim, posY * dim, dim, dim, g);
    }

    protected void drawVerticalFire(int i, boolean toTop, Graphics2D g) {
        bLevel.markForUpdate(posX, posY + i);
        int dim = bLevel.getTileDim();
        if (toTop && i == -top)
            EXPLTOP[animFrame]
                    .draw(posX * dim, (posY - top) * dim, dim, dim, g);
        else if (i == bottom)
            EXPLBOT[animFrame].draw(posX * dim, (posY + bottom) * dim, dim,
                    dim, g);
        else EXPLVER[animFrame].draw(posX * dim, (posY + i) * dim, dim, dim, g);
    }

    public static Texture BOMB1 = new Texture(0, 64, 32, 32);
    public static Texture BOMB2 = new Texture(32, 64, 32, 32);
    public static Texture BOMB3 = new Texture(64, 64, 32, 32);
    public static Texture[] BOMB = { BOMB1, BOMB2, BOMB3, BOMB2 };

    public static Texture EXPLMID1 = new Texture(32, 128, 32, 32);
    public static Texture EXPLMID2 = new Texture(0, 96, 32, 32);
    public static Texture EXPLMID3 = new Texture(96, 32, 32, 32);
    public static Texture[] EXPLMID = { EXPLMID1, EXPLMID2, EXPLMID3, EXPLMID2,
            EXPLMID1 };

    public static Texture EXPLHOR1 = new Texture(64, 128, 32, 32);
    public static Texture EXPLHOR2 = new Texture(32, 96, 32, 32);
    public static Texture EXPLHOR3 = new Texture(128, 32, 32, 32);
    public static Texture[] EXPLHOR = { EXPLHOR1, EXPLHOR2, EXPLHOR3, EXPLHOR2,
            EXPLHOR1 };

    public static Texture EXPLRIG1 = new Texture(96, 128, 32, 32);
    public static Texture EXPLRIG2 = new Texture(64, 96, 32, 32);
    public static Texture EXPLRIG3 = new Texture(160, 32, 32, 32);
    public static Texture[] EXPLRIG = { EXPLRIG1, EXPLRIG2, EXPLRIG3, EXPLRIG2,
            EXPLRIG1 };

    public static Texture EXPLVER1 = new Texture(32, 160, 32, 32);
    public static Texture EXPLVER2 = new Texture(0, 128, 32, 32);
    public static Texture EXPLVER3 = new Texture(96, 64, 32, 32);
    public static Texture[] EXPLVER = { EXPLVER1, EXPLVER2, EXPLVER3, EXPLVER2,
            EXPLVER1 };

    public static Texture EXPLBOT1 = new Texture(32, 192, 32, 32);
    public static Texture EXPLBOT2 = new Texture(0, 160, 32, 32);
    public static Texture EXPLBOT3 = new Texture(96, 96, 32, 32);
    public static Texture[] EXPLBOT = { EXPLBOT1, EXPLBOT2, EXPLBOT3, EXPLBOT2,
            EXPLBOT1 };

    public static Texture EXPLTOP1 = EXPLBOT1.mirrorVertically();
    public static Texture EXPLTOP2 = EXPLBOT2.mirrorVertically();
    public static Texture EXPLTOP3 = EXPLBOT3.mirrorVertically();
    public static Texture[] EXPLTOP = { EXPLTOP1, EXPLTOP2, EXPLTOP3, EXPLTOP2,
            EXPLTOP1 };

    public static Texture EXPLLEF1 = EXPLRIG1.mirrorHorizontally();
    public static Texture EXPLLEF2 = EXPLRIG2.mirrorHorizontally();
    public static Texture EXPLLEF3 = EXPLRIG3.mirrorHorizontally();
    public static Texture[] EXPLLEF = { EXPLLEF1, EXPLLEF2, EXPLLEF3, EXPLLEF2,
            EXPLLEF1 };
}
