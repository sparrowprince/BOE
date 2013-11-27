package bm.gfx;

import java.awt.Graphics2D;

import bm.Player;
import bm.sfx.SoundManager;

public class QdBomb extends FxBomb {

    public QdBomb(int posX, int posY, FxLevel bLevel, Player player) {
        super(posX, posY, bLevel, player);
        this.bLevel = bLevel;
    }

    public QdBomb(int posX, int posY, FxLevel bLevel, Player player,
            byte radius, boolean quadDamage) {
        super(posX, posY, bLevel, player, radius, quadDamage);
        this.bLevel = bLevel;
    }

    protected void playExplodingSound() {
        super.playExplodingSound();
        SoundManager.staticInstance.playSound(5);
    }

    protected void drawMiddleFire(Graphics2D g) {
        bLevel.markForUpdate(posX, posY);
        int dim = bLevel.getTileDim();
        QDEXPLMID[animFrame].draw(posX * dim, posY * dim, dim, dim, g);
    }

    protected void drawHorizontalFire(int i, boolean toRight, Graphics2D g) {
        bLevel.markForUpdate(posX + i, posY);
        int dim = bLevel.getTileDim();
        if (toRight && i == right)
            QDEXPLRIG[animFrame].draw((posX + right) * dim, posY * dim, dim,
                    dim, g);
        else if (i == -left)
            QDEXPLLEF[animFrame].draw((posX - left) * dim, posY * dim, dim,
                    dim, g);
        else QDEXPLHOR[animFrame].draw((posX + i) * dim, posY * dim, dim, dim,
                g);
    }

    protected void drawVerticalFire(int i, boolean toTop, Graphics2D g) {
        bLevel.markForUpdate(posX, posY + i);
        int dim = bLevel.getTileDim();
        if (toTop && i == -top)
            QDEXPLTOP[animFrame].draw(posX * dim, (posY - top) * dim, dim, dim,
                    g);
        else if (i == bottom)
            QDEXPLBOT[animFrame].draw(posX * dim, (posY + bottom) * dim, dim,
                    dim, g);
        else QDEXPLVER[animFrame].draw(posX * dim, (posY + i) * dim, dim, dim,
                g);
    }

    public static final int[] FIRECOLORS = { 0xffea4242, 0xfffb7600, 0xffffffff };

    public static final int[] QDCOLORS = { 0xff0d499d, 0xff1881ee, 0xff18e2ee };

    public static final int QDCOLOR2 = 0xff18e2ee; // bright
    public static final int QDCOLOR1 = 0xff1881ee; // dark
    public static final int QDCOLOR3 = 0xff0d499d; // darkes

    public static Texture[] QDEXPLMID = Texture.replaceColors(EXPLMID,
            FIRECOLORS, QDCOLORS);
    public static Texture[] QDEXPLHOR = Texture.replaceColors(EXPLHOR,
            FIRECOLORS, QDCOLORS);
    public static Texture[] QDEXPLRIG = Texture.replaceColors(EXPLRIG,
            FIRECOLORS, QDCOLORS);
    public static Texture[] QDEXPLVER = Texture.replaceColors(EXPLVER,
            FIRECOLORS, QDCOLORS);
    public static Texture[] QDEXPLBOT = Texture.replaceColors(EXPLBOT,
            FIRECOLORS, QDCOLORS);
    public static Texture[] QDEXPLTOP = Texture.replaceColors(EXPLTOP,
            FIRECOLORS, QDCOLORS);
    public static Texture[] QDEXPLLEF = Texture.replaceColors(EXPLLEF,
            FIRECOLORS, QDCOLORS);

}
