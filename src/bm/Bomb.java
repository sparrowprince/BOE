package bm;

import bm.log.Logger;

/**
 * This class provides objects that will be interpreted as Bombs. They are
 * usually held inside a two-dimensional array in objects of the type Level.
 * 
 * These objects cannot be visualized; if you're looking for that, use FxBomb.
 * 
 * @author tobi
 * 
 */
public class Bomb implements Updateable {

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////CONSTANT VARIABLES////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /** Holds the number of all Bombs exploding at this moment. */
    public static int explodeCount = 0;

    /** Constant that a Bomb will interpret as itself still COUNTING down. */
    public static final byte COUNTING = 1;
    /** Constant that a Bomb will interpret as itself EXPLODING. */
    public static final byte EXPLODING = 2;
    /** Constant that a Bomb will interpret as itself being EXPLODED. */
    public static final byte EXPLODED = 0;
    /** Time (in ticks) that it usually takes for a bomb to explode. */
    public static final short COUNTDOWN = 180;
    /** Time (in ticks) that a bomb stays alive after exploding. */
    public static final byte EXPLODINGTIME = 20;

    /** Format for creating a String reporting about this Bomb's creation. */
    public static final String NEWBOMB = "Created new Bomb. Position: (%d, %d). Owner: %s. Range: %d";

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////////FIELDS//////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    protected byte state; // this value holds one of the three value above

    protected int posX, posY; // obviously the position (posX, posY)

    private byte range; // max. range; range >= left etc. pp.
    protected boolean quadDamage; // if true 4 x range and destroys more tiles
    protected byte left, right, top, bottom; // will be calculated when expl.

    private int counter; // name explains it. Is reused.

    protected Level lvl; // the level where this bomb is placed
    protected Player player; // planter of this bomb

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////////CONSTRUCTORS///////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new object of the type Bomb. This is a (non-visualized) entity
     * that counts down a constant time and then "explodes", i.e. it destroys
     * blocks in a certain range.
     * 
     * The range of the Bomb will be determined using the Player's getRange()
     * method. The same goes for this Bomb's Quad Damage mode.
     * 
     * @param posX The x coordinate of this Bomb (in tiles).
     * @param posY The y coordinate of this Bomb (in tiles).
     * @param lvl The Level where this Bomb resides.
     * @param player The Player who planted this Bomb.
     */
    public Bomb(int posX, int posY, Level lvl, Player player) {
        this(posX, posY, lvl, player, player.getRange(), player.hasQuadDamage());
    }

    /**
     * Creates a new object of the type Bomb. This is a (non-visualized) entity
     * that counts down a constant time and then "explodes", i.e. it destroys
     * blocks in a certain range.
     * 
     * @param posX The x coordinate of this Bomb (in tiles).
     * @param posY The y coordinate of this Bomb (in tiles).
     * @param lvl The Level where this Bomb resides.
     * @param player The Player who planted this Bomb.
     * @param range The max. range of this Bomb.
     * @param quadDamage true if this Bomb is in Quad Damage mode.
     */
    public Bomb(int posX, int posY, Level lvl, Player player, byte range,
            boolean quadDamage) {
        state = COUNTING;
        this.posX = posX;
        this.posY = posY;
        this.quadDamage = quadDamage;
        if (quadDamage)
            this.range = (byte) Math.min(4 * range, Player.MAXRANGE);
        else this.range = range;
        this.lvl = lvl;
        this.player = player;
        // player can only plant a certain number of bombs at a time
        player.incrementAmmoCount();
        counter = COUNTDOWN; // start with 'normal' countdown
        left = right = top = bottom = 0; // will be recalculated at expl. time
        if (Logger.verbose())
            Logger.writeln(String.format(NEWBOMB, posX, posY, player.getName(),
                    range));
    }

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////GETTERS & SETTERS/////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns the x coordinate of this Bomb. The x coordinate is specified in
     * tiles.
     * 
     * @return x coordinate of this Bomb.
     */
    public int getPosX() {
        return posX;
    }

    /**
     * Returns the y coordinate of this Bomb. The y coordinate is specified in
     * tiles.
     * 
     * @return y coordinate of this Bomb.
     */
    public int getPosY() {
        return posY;
    }

    /**
     * Returns true is this Bomb is currently exploding.
     * 
     * @return true if state is EXPLODING, false otherwise
     */
    public boolean isExploding() {
        return state == EXPLODING;
    }

    /**
     * Returns true is this Bomb has exploded.
     * 
     * @return true if state is EXPLODED, false otherwise
     */
    public boolean isExploded() {
        return state == EXPLODED;
    }

    /**
     * Returns true is this Bomb is still counting down.
     * 
     * @return true if state is COUNTING, false otherwise
     */
    public boolean isCounting() {
        return state == COUNTING;
    }

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////UPDATE METHODS//////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This method performs an update on this bomb, i.e. counting down until
     * explosion time, destroying tiles when exploding, setting and keeping them
     * on fire etc. It should be called once per game tick.
     */
    public void update() {
        switch (state) {
        case EXPLODED: // nothing to do
            return;
        case COUNTING:
            if (counter > 0)
                updateCountingBomb();
            else explode();
            break;
        case EXPLODING:
            if (counter > 0)
                updateExplodingBomb();
            else remove();
            break;
        }
    }

    /**
     * This method is called every time this Bomb counts down a tick. It can
     * possibly be overridden to add visual effects or similar.
     */
    protected void updateCountingBomb() {
        counter--;
    }

    /**
     * This method detonates this Bomb. It will eventually be called through the
     * update()-method but may as well be invoked manually. This is primarily
     * used by the Level-class to invoke chain reactions.
     */
    public void explode() {
        counter = EXPLODINGTIME; // reusing the counter
        explodeCount++; // static variable; used for shock effects
        state = EXPLODING;
        player.decrementAmmoCount(); // enable player to plant a new Bomb

        // start by destr. the tile underneath, also try to kill Players
        lvl.destroyBlock(posX, posY);
        killPlayer(posX, posY);
        int i, posX, posY;
        // now get boundaries and destroy tiles & kill Players
        for (i = 1; i <= range; i++) {
            // only calculate the current position once
            posX = this.posX - i;
            posY = this.posY;
            // if we reach a solid object, we're probably done!
            if (lvl.isSolid(posX, posY)
                    && (!quadDamage || lvl.getTile(posX, posY) == Level.BEDROCK)) {
                lvl.destroyBlock(posX, posY);
                break;
            }
            // otherwise we keep on counting, destroying and killing Players
            left++;
            lvl.destroyBlock(posX, posY);
            killPlayer(posX, posY);
        }
        // the other loops work exactly the same..
        for (i = 1; i <= range; i++) {
            posX = this.posX + i;
            posY = this.posY;
            if (lvl.isSolid(posX, posY)
                    && (!quadDamage || lvl.getTile(posX, posY) == Level.BEDROCK)) {
                lvl.destroyBlock(posX, posY);
                break;
            }
            right++;
            lvl.destroyBlock(posX, posY);
            killPlayer(posX, posY);
        }
        for (i = 1; i <= range; i++) {
            posX = this.posX;
            posY = this.posY - i;
            if (lvl.isSolid(posX, posY)
                    && (!quadDamage || lvl.getTile(posX, posY) == Level.BEDROCK)) {
                lvl.destroyBlock(posX, posY);
                break;
            }
            top++;
            lvl.destroyBlock(posX, posY);
            killPlayer(posX, posY);
        }
        for (i = 1; i <= range; i++) {
            posX = this.posX;
            posY = this.posY + i;
            if (lvl.isSolid(posX, posY)
                    && (!quadDamage || lvl.getTile(posX, posY) == Level.BEDROCK)) {
                lvl.destroyBlock(posX, posY);
                break;
            }
            bottom++;
            lvl.destroyBlock(posX, posY);
            killPlayer(posX, posY);
        }
    }

    /**
     * This method tries to kill a Player at the specified position. For this,
     * it checks for each Player if the specified position position is 'inside'
     * it. If so, it tells the Player to die. It can possibly be overridden to
     * add visual effects or similar.
     * 
     * @param posX X coordinate of the position where Players should be killed.
     * @param posY Y coordinate of the position where Players should be killed.
     */
    protected void killPlayer(double posX, double posY) {
        posX += Player.HALFTILEDIM;
        posY += Player.HALFTILEDIM;
        for (int i = 0; i < Player.PLAYERS.size(); i++) {
            if (Player.PLAYERS.get(i).withinBounds(posX, posY))
                Player.PLAYERS.get(i).killMe(player);
        }
    }

    /**
     * This method is called each tick as long as this Bomb is still exploding.
     * When it eventually reaches the state of being EXPLODED, it won't be
     * called anymore. It can possibly be overridden to add visual effects or
     * similar.
     */
    protected void updateExplodingBomb() {
        counter--;
    }

    /**
     * This method is eventually called when this Bomb reaches the EXPLODED
     * state. It does some cleanup and ultimately removes the reference to the
     * Bomb itself. It can possibly be overridden to add visual effects or
     * similar.
     */
    protected void remove() {
        state = EXPLODED;
        explodeCount--; // see above, static variable
        // tell the Level to remove reference
        lvl.removeBomb(posX, posY);
    }
}// end of class Bomb
