package bm.gfx;

import java.awt.Graphics2D;

import bm.Player;
import bm.sfx.SoundManager;

/**
 * This class provides objects representing players for this game. It is a
 * subclass of Player; it provides methods for visualization and playing audio
 * effects.
 * 
 * Furthermore, in addition to the states that can be found in the Player class,
 * FxPlayer keeps track of its direction. So, for instance, an FxPlayer could be
 * staring to the right, walking up or dying to the left.
 * 
 * For more details, see Player.java.
 * 
 * @author tobi
 * 
 */
public class FxPlayer extends Player implements Drawable {

    // FIXME
    // FIXME
    // FIXME

    /***** not finished with sorting this... ! */

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////CONSTANT VARIABLES////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /* the following four colors are to be replaced for FxPlayer's textures */
    public static final int[] HOUSECOLORS = Texture.HOUSECOLORS;

    /* FxPlayer's textures (uncolored) */
    private static final Texture PLAYER_MOVE_FRONT1 = new Texture(224, 0, 32,
            48);
    private static final Texture PLAYER_MOVE_FRONT2 = PLAYER_MOVE_FRONT1
            .mirrorHorizontally();
    private static final Texture PLAYER_MOVE_BACK1 = new Texture(224, 48, 32,
            48);
    private static final Texture PLAYER_MOVE_BACK2 = PLAYER_MOVE_BACK1
            .mirrorHorizontally();
    private static final Texture PLAYER_MOVE_LEFT1 = new Texture(256, 48, 32,
            48);
    private static final Texture PLAYER_MOVE_LEFT2 = new Texture(288, 48, 32,
            48);
    private static final Texture PLAYER_MOVE_RIGHT1 = PLAYER_MOVE_LEFT1
            .mirrorHorizontally();
    private static final Texture PLAYER_MOVE_RIGHT2 = PLAYER_MOVE_LEFT2
            .mirrorHorizontally();
    private static final Texture PLAYER_IDLE_FRONT = new Texture(192, 0, 32, 48);
    private static final Texture PLAYER_IDLE_BACK = new Texture(192, 48, 32, 48);
    private static final Texture PLAYER_IDLE_LEFT1 = new Texture(256, 0, 32, 48);
    private static final Texture PLAYER_IDLE_LEFT2 = new Texture(288, 0, 32, 48);
    private static final Texture PLAYER_IDLE_RIGHT1 = PLAYER_IDLE_LEFT1
            .mirrorHorizontally();
    private static final Texture PLAYER_IDLE_RIGHT2 = PLAYER_IDLE_LEFT2
            .mirrorHorizontally();
    private static final Texture PLAYER_DIE1 = new Texture(192, 96, 32, 48);
    private static final Texture PLAYER_DIE2 = new Texture(224, 96, 32, 48);
    private static final Texture PLAYER_DIE3 = new Texture(256, 96, 32, 48);
    private static final Texture PLAYER_DIE4 = new Texture(288, 96, 32, 48);

    /**
     * Two-dimensional array holding textures for a moving FxPlayer. Each array
     * holds a sequence of texture to create an actual animation. The order of
     * directions is LEFT, RIGHT, DOWN, UP.
     */
    public static final Texture[][] PLAYER_MOVE = {
            { PLAYER_MOVE_LEFT1, PLAYER_IDLE_LEFT2, PLAYER_MOVE_LEFT2,
                    PLAYER_IDLE_LEFT1 },
            { PLAYER_MOVE_RIGHT1, PLAYER_IDLE_RIGHT2, PLAYER_MOVE_RIGHT2,
                    PLAYER_IDLE_RIGHT1 },
            { PLAYER_MOVE_BACK1, PLAYER_IDLE_BACK, PLAYER_MOVE_BACK2,
                    PLAYER_IDLE_BACK },
            { PLAYER_MOVE_FRONT1, PLAYER_IDLE_FRONT, PLAYER_MOVE_FRONT2,
                    PLAYER_IDLE_FRONT } };

    /**
     * Array holding textures for an idle FxPlayer. The order of directions is
     * LEFT, RIGHT, DOWN, UP.
     */
    public static final Texture[] PLAYER_IDLE = { PLAYER_IDLE_LEFT1,
            PLAYER_IDLE_RIGHT1, PLAYER_IDLE_BACK, PLAYER_IDLE_FRONT };

    /**
     * Array holding textures for a dying FxPlayer. It holds a sequence of
     * textures to create an actual animation. The dying animation is always
     * directed DOWN.
     */
    public static final Texture[] PLAYER_DIE = { PLAYER_DIE1, PLAYER_DIE2,
            PLAYER_DIE3, PLAYER_DIE4 };

    /* FxPlayer's possible directions */
    /** Constant that an FxPlayer interprets as moving or standing to the LEFT. */
    public static final byte LEFT = 0;
    /** Constant that an FxPlayer interprets as moving or standing to the RIGHT. */
    public static final byte RIGHT = 1;
    /** Constant that an FxPlayer interprets as moving or standing to the UP. */
    public static final byte UP = 2;
    /** Constant that an FxPlayer interprets as moving or standing to the DOWN. */
    public static final byte DOWN = 3;

    /** The minimum radius (in tiles) around an FxPlayer object to be redrawn. */
    public static final byte MINUPDATERADIUS = 1;
    /** Radius (in tiles) of what'll be covered in blood when this Player dies. */
    public static final byte GORERADIUS = 4;

    public static final int DYINGANIMDURATION = DYINGTIME / PLAYER_DIE.length;
    public static final int MOVINGANIMDURATION = 10;

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////////FIELDS//////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    private int animCounter;
    private byte animFrame;

    private byte dir;

    private Texture[] player_idle;
    private Texture[][] player_move;
    private Texture[] player_die;

    private Texture name_text;

    private FxLevel lvl;

    private int drawWidth, drawHeight;

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////////CONSTRUCTORS///////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    public FxPlayer(FxLevel lvl, String name, int fstClr, int sndClr,
            double posX, double posY) {
        super(lvl, name, fstClr, sndClr, posX, posY);
        int[] colors = { fstClr, Texture.darkenColor(fstClr), sndClr,
                Texture.darkenColor(sndClr) };
        this.lvl = lvl;
        animCounter = MOVINGANIMDURATION;
        animFrame = 0;
        dir = DOWN;
        player_idle = Texture.replaceColors(PLAYER_IDLE, HOUSECOLORS, colors);
        player_move = Texture.replaceColors(PLAYER_MOVE, HOUSECOLORS, colors);
        player_die = Texture.replaceColors(PLAYER_DIE, HOUSECOLORS, colors);
        name_text = Texture.drawString(name).replaceColors(Texture.TEXTCOLORS,
                colors);
        updateDimensions();
    }

    public FxPlayer(FxLevel lvl, double posX, double posY) {
        this(lvl, "unnamed", DEFAULTFSTCLR, DEFAULTSNDCLR, posX, posY);
    }

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////GETTERS & SETTERS/////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    @Override
    public void increaseRange() {
        super.increaseRange();
        SoundManager.staticInstance.playSound(2);
    }

    @Override
    public void increaseAmmo() {
        super.increaseAmmo();
        SoundManager.staticInstance.playSound(2);
    }

    @Override
    protected void setChuckMode() {
        super.setChuckMode();
        SoundManager.staticInstance.playSound(2);
    }

    @Override
    protected void setQuadDamageMode() {
        super.setQuadDamageMode();
        SoundManager.staticInstance.playSound(4);
    }

    public Texture getNameTexture() {
        return name_text;
    }

    protected void die() {
        super.die();
        lvl.addGore(posX, posY, GORERADIUS);
        animFrame = (byte) (player_die.length - 1);
    }

    @Override
    public boolean killMe(Player player) {
        if (!super.killMe(player))
            return false;
        animCounter = DYINGANIMDURATION;
        animFrame = 0;
        SoundManager.staticInstance.playSound(3);
        return true;
    }

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////UPDATE METHODS//////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////MOVING & MANIPULATING//////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    @Override
    protected void updateDying() {
        super.updateDying();
        // counter + frame are for the dying animation
        if (animCounter > 0)
            animCounter--;
        else {
            animCounter = DYINGANIMDURATION;
            animFrame++;
        }
    }

    @Override
    public void update() {
        super.update();
        // there was previously no specific action for moving
        // counter + frame for movement animation
        if (state == MOVING) {
            if (animCounter > 0)
                animCounter--;
            else {
                animCounter = MOVINGANIMDURATION;
                animFrame = (byte) ((animFrame + 1) % player_move[0].length);
            }
        }
    }

    @Override
    public byte[] move(byte dirX, byte dirY) {
        byte[] moved = super.move(dirX, dirY);
        // calculate direction
        if (state != MOVING)
            return moved; // no movement -> no direction switch

        // see if horizontal or vertical movement was more important
        if (Math.abs(moved[0]) > Math.abs(moved[1])) {
            // horizontal
            if (moved[0] < 0)
                dir = LEFT;
            else dir = RIGHT;
        } else {
            // vertical
            if (moved[1] < 0)
                dir = UP;
            else dir = DOWN;
        }
        return moved;
    }

    /**
     * This method draws the FxPlayer with the help of a graphics context. It is
     * purely intended to draw and does not update anything, including
     * animations. For the animations, refer to the overwritten update()-method.
     * 
     * The FxPlayer is drawn at its relative position with proportions relative
     * to the FxLevel is is inhabiting.
     * 
     * @param g The Graphics2D context.
     */
    public void draw(Graphics2D g) {
        lvl.markForUpdate(posX, posY, MINUPDATERADIUS);
        int drawX = (int) (posX * lvl.getTileDim());
        int drawY = (int) ((posY - HALFTILEDIM) * lvl.getTileDim());
        switch (state) {
        case DYING:
            player_die[animFrame].draw(drawX, drawY, drawWidth, drawHeight, g);
            break;
        case DEAD:
            player_die[animFrame].draw(drawX, drawY, drawWidth, drawHeight, g);
            break;
        case IDLE:
        case WON:
            player_idle[dir].draw(drawX, drawY, drawWidth, drawHeight, g);
            break;
        case MOVING:
            player_move[dir][animFrame].draw(drawX, drawY, drawWidth,
                    drawHeight, g);
            break;
        }
    }

    /**
     * This method updates the width and height that are used to draw this
     * FxPlayer according to the tileDim() of the FxLevel it is living in.
     * 
     * You should call this method whenever the viewport belonging to the
     * Graphics2D context used by this FxPlayer's draw method is being resized.
     */
    public void updateDimensions() {
        int dim = lvl.getTileDim();
        drawWidth = dim;
        drawHeight = dim * 3 / 2;
    }
} // end of class FxPlayer
