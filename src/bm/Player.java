package bm;

import java.util.ArrayList;
import java.util.List;

import bm.log.Logger;

/**
 * This class provides objects representing players for this game. It does not
 * provide any visuals or audio as this is subject for the subclass FxPlayer.
 * One would want to use Player instead of FxPlayer for a dedicated server to
 * reduce the needed memory.
 * 
 * There is one small exception: objects of the type Player do store the colors
 * of that object; that is because a dedicated server probably wants to manage
 * the individual colors of the Player objects (i.e. to send the colors to other
 * clients).
 * 
 * @author tobi
 * 
 */
public class Player implements Updateable {

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////CONSTANT VARIABLES////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /* states a Player can be in */
    /** Constant which a Player object will interpret as itself being IDLE. */
    public static final byte IDLE = 0;
    /** Constant which a Player object will interpret as itself MOVING around. */
    public static final byte MOVING = 1;
    /** Constant which a Player object will interpret as itself having WON. */
    public static final byte WON = 2;
    /** Constant which a Player object will interpret as itself DYING. */
    public static final byte DYING = 3;
    /** Constant which a Player object will interpret as itself being DEAD. */
    public static final byte DEAD = 4;

    /* powerup limits */
    /** Smallest amount of Bombs a Player can plant at once. */
    public static final byte MINAMMO = 1;
    /** Biggest amount of Bombs a Player can plant at once. */
    public static final byte MAXAMMO = Byte.MAX_VALUE;
    /** Smallest range that Bombs of a Player can have. */
    public static final byte MINRANGE = 1;
    /** Largest range that Bombs of a Player can have. */
    public static final byte MAXRANGE = Byte.MAX_VALUE;
    /** Constant holding the time (in ticks) that a player will be Chuck Norris. */
    public static final short CHUCKMODETIME = 1800;
    /** Constant holding time (in ticks) that a player will have Quad Damage. */
    public static final short QDTIME = 900;

    /** Constant holding the time (in ticks) that it takes for a player to die. */
    public static final byte DYINGTIME = 20;

    /** Constant holding a tile's width/height. */
    public static final float TILEDIM = 1.0f;
    /** Constant holding half a tile's width/height. */
    public static final float HALFTILEDIM = 0.5f;
    /** Constant holding a Player object's width/height. */
    public static final float DIM = 0.75f;
    /** Constant holding an offset used for collision detection. */
    public static final float PLOFFSET = 0.125f;
    /** Constant holding the Player's velocity in tiles per tick. */
    public static final double STEPSIZE = 0.075f;

    /** The default name for an object of the type Player. */
    public static final String DEFAULTNAME = "unnamed";

    /** The (first) default color for objects of the type Player. */
    public static final int DEFAULTFSTCLR = 0xFFAA2222;
    /** The (second) default color for objects of the type Player. */
    public static final int DEFAULTSNDCLR = 0xFF2222AA;

    /**
     * List holding all Player objects. A new Player is added to the list upon
     * creation.
     */
    public static final List<Player> PLAYERS = new ArrayList<Player>();

    /** Format for creating a String reporting about this Player's creation. */
    public static final String NEWPLAYER = "Created new Player. Position: (%f3.3, %f3.3). Name: %s. Colors: %d & %d.";

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////// STATIC METHODS//////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This method clears PLAYERS, the List holding all the Player object. You
     * probably want to invoke this method when restarting a game or similar.
     */
    public static final void resetPlayers() {
        PLAYERS.clear();
    }

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////////FIELDS//////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    protected double posX, posY; // Player's position in tiles (see Level.java)
    protected byte state; // state of the player, see above
    private Level lvl; // the Level where this Player resides in

    private byte range, ammo; // range, amount and force of the Bombs
    private byte ammoCount; // keeps track of planted bombs
    private boolean chuckMode; // true if Player is Chuck Norris
    private boolean quadDamage; // true if Player has quadDamage ( = 4 x range)

    protected String name; // this Player's name; useful for e.g. mp stats
    private int fstClr, sndClr; // this Player's colors

    protected int counter; // use obvious; is reused
    private int qdCounter; // own counter so chuckMode and qd are seperate

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////////CONSTRUCTORS///////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new object of the type Player. These objects represent, as the
     * name suggest, the players in this Bomberman variant. This object is
     * solely updateable, not drawable.
     * 
     * @param lvl The Level where this Player moves around and plants Bombs.
     * @param name The name of this Player (nicknames in mp etc.).
     * @param fstClr The first color of this Player.
     * @param sndClr The second color of this Player.
     * @param posX The initial x coordinate of this Player in tiles.
     * @param posY The initial y coordinate of this Player in tiles.
     */
    public Player(Level lvl, String name, int fstClr, int sndClr, double posX,
            double posY) {
        this.lvl = lvl;
        this.posX = posX;
        this.posY = posY;
        this.fstClr = fstClr;
        this.sndClr = sndClr;
        this.name = name;
        // set some default values
        range = MINRANGE;
        ammo = MINAMMO;
        ammoCount = 0;
        state = IDLE;
        PLAYERS.add(this);
        if (Logger.verbose())
            Logger.writeln(String.format(NEWPLAYER, posX, posY, name, fstClr,
                    sndClr));
    }

    /**
     * Creates a new object of the type Player. These objects represent, as the
     * name suggest, the players in this Bomberman variant. This object is
     * solely updateable, not drawable.
     * 
     * Name and colors of this Player object will use the default values.
     * 
     * @param lvl The Level where this Player moves around and plants Bombs.
     * @param posX The initial x coordinate of this Player in tiles.
     * @param posY The initial y coordinate of this Player in tiles.
     */
    public Player(Level lvl, double posX, double posY) {
        this(lvl, DEFAULTNAME, DEFAULTFSTCLR, DEFAULTSNDCLR, posX, posY);
    }

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////GETTERS & SETTERS/////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns this Player's current x coordinate in tiles.
     * 
     * @return This Player's current x coordinate in tiles.
     */
    public double getPosX() {
        return posX;
    }

    /**
     * Returns this Player's current y coordinate in tiles.
     * 
     * @return This Player's current y coordinate in tiles.
     */
    public double getPosY() {
        return posY;
    }

    /**
     * Returns true if the specified point is inside this Player's collision
     * box.
     * 
     * @param posX X coordinate to be checked of being inside this Player.
     * @param posY Y coordinate to be checked of being inside this Player.
     * @return true if (posX, posY) is inside this Player's collision box, false
     * otherwise.
     */
    public boolean withinBounds(double posX, double posY) {
        double playerX = this.posX + PLOFFSET;
        double playerY = this.posY + PLOFFSET;
        return (posX >= playerX && posX <= playerX + DIM && posY >= playerY && posY <= playerY
                + DIM);
    }

    /**
     * Sets this Player's position (in tiles) to the specified coordinate. There
     * is no collision detection whatsoever.
     * 
     * @param newX The new x coordinate of this Player.
     * @param newY The new y coordinate of this Player.
     */
    public void setPos(double newX, double newY) {
        this.posX = newX;
        this.posY = newY;
    }

    /**
     * Sets this Player's state to WON, which usually means it has either
     * reached the Level's EXIT or every other opponent is dead. This method is
     * meant to be overwritten to provide sounds or animation.
     */
    protected void win() {
        state = WON;
    }

    /**
     * Returns true if this Player has 'won' the match; usually this means it
     * has either reached the Level's EXIT or every other opponent is dead.
     * 
     * @return true is this Player is considered to have won, false otherwise.
     */
    public boolean hasWon() {
        return state == WON;
    }

    /**
     * Returns true if this Player is dead.
     * 
     * @return true this Player's state equals DEAD, false otherwise.
     */
    public boolean isDead() {
        return state == DEAD;
    }

    /**
     * This method starts killing this Player. It will not set it's state
     * directly to being dead, but rather to DYING. A dying Player object will
     * ultimately reach the DEAD state.
     * 
     * This method has no effect as long as this Player is Chuck Norris or
     * already DYING/DEAD.
     * 
     * @param player Player that initiated this Player's death.
     * @return true is Player has successfully been killed.
     */
    public boolean killMe(Player player) {
        if (chuckMode || state == DYING || state == DEAD)
            return false;
        state = DYING;
        counter = DYINGTIME;
        if (this == player)
            System.out.println(name + " killed himself.");
        else System.out.println(player.name + " killed " + name + ".");
        return true;
    }

    /**
     * This method directly kills this Player, i.e. sets his state to DEAD.
     * 
     * You usually don't want to invoke this manually but rather via the
     * Player's update method.
     */
    protected void die() {
        state = DEAD;
    }

    /**
     * Returns the maximum range of the Bombs that this Player plants.
     * 
     * @return The maximum range of this Player's Bombs.
     */
    public byte getRange() {
        return range;
    }

    /**
     * Increases the maximum range of this Player's bombs by one unless it has
     * already reached MAXRANGE.
     */
    public void increaseRange() {
        if (range < MAXRANGE)
            range++;
    }

    /**
     * Returns the maximum amount of Bombs this Player can plant.
     * 
     * @return The maximum amount of Bombs this Player can plant.
     */
    public byte getAmmo() {
        return ammo;
    }

    /**
     * Increases the amount of Bombs this Player can plant by one, unless the
     * number has already reached MAXAMMO.
     */
    public void increaseAmmo() {
        if (ammo < MAXAMMO)
            ammo++;
    }

    /**
     * Returns true if this Player is still able to plant a Bomb, i.e. that the
     * amount of Bombs planted by it that are currently active is less than its
     * ammo.
     * 
     * @return true if this Player can plant a Bomb, false otherwise.
     */
    public boolean hasAmmo() {
        return (ammoCount < ammo);
    }

    /**
     * Increments the Bomb counter of this Player, unless it has already reached
     * the maximum amount of Bombs that this Player can plant. Is usually
     * invoked from a Bomb's constructor.
     */
    public void incrementAmmoCount() {
        if (ammoCount < ammo)
            ammoCount++;
    }

    /**
     * Decrements the Bomb counter of this Player, unless it has already reached
     * zero. Is usually invoked by an exploding Bomb.
     */
    public void decrementAmmoCount() {
        if (ammoCount > 0)
            ammoCount--;
    }

    /**
     * Returns true if this Player is considered to be Chuck Norris, i.e.
     * invincible and extremely powerful.
     * 
     * @return true is this Player is currently Chuck Norris, false otherwise.
     */
    public boolean isChuck() {
        return chuckMode;
    }

    /**
     * This method turns this Player into Chuck Norris. In the case that it
     * already is Chuck Norris, the timer for staying so will be reset.
     */
    protected void setChuckMode() {
        chuckMode = true;
        counter = CHUCKMODETIME;
    }

    /**
     * This method activates QuadDamage mode for this Player. In the case that
     * QuadDamage is still active, the timer will be reset;
     */
    protected void setQuadDamageMode() {
        quadDamage = true;
        qdCounter = QDTIME;
    }

    public boolean hasQuadDamage() {
        return quadDamage;
    }

    /**
     * Returns the name (i.e. nickname) of this Player.
     * 
     * @return The name of this Player in from of a String.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the first color of this Player.
     * 
     * @return The first color of this Player.
     */
    public int getFstClr() {
        return fstClr;
    }

    /**
     * Returns the second color of this Player.
     * 
     * @return The second color of this Player.
     */
    public int getSndClr() {
        return sndClr;
    }

    /**
     * Returns the current value of this Player's counter. It may either be
     * unused or counting down the Player's chuckMode or dying respectively.
     * 
     * @return The current value of this Player's counter.
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Returns the current value of this Player's Quad Damage counter. It is
     * used to limit the time that this Player stays in Quad Damage mode.
     * 
     * @return The current value of this Player's Quad Damage counter.
     */
    public int getQdCounter() {
        return qdCounter;
    }

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////UPDATE METHODS//////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This method performs an update on this Player, i.e. killing it off when
     * it was hit by a Bomb or pick up PowerUps. It is usually invoked by the
     * game's main loop.
     */
    public void update() {
        switch (state) {
        case WON:
        case DEAD: // nothing to do when dead or finished (-> game over)
            return;
        case DYING: // count down until player is dead
            updateDying();
            break;
        default: // otherwise check for powerups & flames
            collide();
        }
        if (chuckMode) {
            if (counter > 0)
                counter--;
            else chuckMode = false;
        }
        if (quadDamage) {
            if (qdCounter > 0)
                qdCounter--;
            else quadDamage = false;
        }
    }

    /**
     * This method updates this Player in case it's state is currently dying. It
     * is meant to be overwritten to add a dying animation and / or sound
     * effect.
     */
    protected void updateDying() {
        if (counter > 0)
            counter--;
        else die();
    }

    /**
     * This method performs a kind of collision detection; it checks for
     * PowerUps that this Player can pick up.
     */
    protected void collide() {
        // calculate the middle point of the player only once
        double posX = this.posX + HALFTILEDIM;
        double posY = this.posY + HALFTILEDIM;
        // try to get some nice stuff from the Level
        switch (lvl.getTile(posX, posY)) {
        case Level.EXIT:
            win();
            break;
        case Level.FIREPLUS:
            lvl.destroyBlock(posX, posY);
            increaseRange();
            break;
        case Level.BOMBPLUS:
            lvl.destroyBlock(posX, posY);
            increaseAmmo();
            break;
        case Level.CHUCKNORRIS:
            lvl.destroyBlock(posX, posY);
            setChuckMode();
            break;
        case Level.QUADDAMAGE:
            lvl.destroyBlock(posX, posY);
            setQuadDamageMode();
            break;
        default:
            break;
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////MOVING & MANIPULATING//////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This method moves this Player relative to his current position. Unlike
     * the setPos() method, there will be collision detection. The Player will
     * at max move STEPSIZE tiles. The params will be interpreted as follows:
     * 
     * dirX: < 0 left, > 0 right, == 0 no movement
     * 
     * dirY: < 0 up, > 0 down, == 0 no movement
     * 
     * It will return the actual amount that the Player has moved.
     * 
     * @param dirX The Player's horizontal direction.
     * @param dirY The Player's vertical direction.
     * @return An array of length 2 containing the actual movement after
     * collision detection; i.e. {movedX, movedY}.
     */
    public byte[] move(byte dirX, byte dirY) {
        // don't move if you're dying, dead or have reached your goal
        if (state == WON || state == DYING || state == DEAD)
            return new byte[] { 0, 0 }; // no movement here
        if (dirX == 0 && dirY == 0) {
            state = IDLE;
            return new byte[] { 0, 0 };
        }

        double deltaX = dirX * STEPSIZE;
        double deltaY = dirY * STEPSIZE;

        // there is some tolerance; the Player is smaller than a tile
        double leftX = this.posX + PLOFFSET;
        double topY = this.posY + PLOFFSET;
        double rightX = leftX + DIM;
        double bottomY = topY + DIM;

        double nextX, nextY; // will store some calcs here

        // moving horizontally first
        if (deltaX > 0) { // wants to move right
            nextX = rightX + deltaX;
            if ((int) rightX != (int) nextX
                    && (lvl.isSolid(nextX, topY) || lvl.isSolid(nextX, bottomY)))
                deltaX = 0;
        } else if (deltaX < 0) { // left
            nextX = leftX + deltaX;
            if ((int) leftX != (int) nextX
                    && (lvl.isSolid(nextX, topY) || lvl.isSolid(nextX, bottomY)))
                deltaX = 0;
        }

        posX += deltaX;
        leftX = posX + PLOFFSET;
        rightX = leftX + DIM;

        // then vertically
        if (deltaY > 0) { // down
            nextY = bottomY + deltaY;
            if ((int) bottomY != (int) nextY
                    && (lvl.isSolid(leftX, nextY) || lvl.isSolid(rightX, nextY)))
                deltaY = 0;
        } else if (deltaY < 0) { // up
            nextY = topY + deltaY;
            if ((int) topY != (int) nextY
                    && (lvl.isSolid(leftX, nextY) || lvl.isSolid(rightX, nextY)))
                deltaY = 0;
        }
        // now actually move -- or not if zero
        posY += deltaY;

        // see if we left the Level
        if (posX < -1) {
            posX = lvl.getWidth();
        } else if (posX > lvl.getWidth()) {
            posX = -1;
        }
        if (posY < -1) {
            posY = lvl.getHeight();
        } else if (posY > lvl.getHeight()) {
            posY = -1;
        }

        // depending on movement -> update state
        if (deltaX != 0 || deltaY != 0)
            state = MOVING;
        else state = IDLE;
        // return the actual movement
        return new byte[] { (byte) Math.signum(deltaX),
                (byte) Math.signum(deltaY) };
    }

    /**
     * This method tells the Player to plant a Bomb on the tile where it is
     * standing. Returns true if it succeeded in doing so.
     * 
     * @return true if Bomb could be planted, false otherwise.
     */
    public boolean putBomb() {
        if (!hasAmmo())
            return false;
        double posX = this.posX + HALFTILEDIM;
        double posY = this.posY + HALFTILEDIM;
        return lvl.putBomb(posX, posY, this);
    }
} // end of class Player
