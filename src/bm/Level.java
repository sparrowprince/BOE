package bm;

import java.util.LinkedList;
import java.util.Queue;

import bm.log.Logger;

/**
 * This class provides objects that, as the name suggest, will be interpreted as
 * the Level wherein the game takes place. At the core of this class is a
 * two-dimensional short array which holds the Level's tiles. Level objects are
 * also responsible for storing and managing the game's Bombs.
 * 
 * A tile, in binary representation, constitutes itself as follows:
 * 
 * t X X X X X X X T T T T T T T T
 * 
 * The bit t is a flag.
 * 
 * A tile marked with the t flag is marked for transmit; this is only
 * interesting for a networked game. It tells the Level that this particular
 * tile should be transmitted to whatever recipient may await it (usually a
 * client).
 * 
 * The bits marked with X are unused; they could be used by a subclass (see
 * FxLevel.java for examples on this).
 * 
 * Finally, the right-most 8 bits/the lower byte holds the actual tile, i.e. a
 * value that can be interpreted as GRASS, STONE etc. Refer to this class's
 * constants.
 * 
 * This class offers no visualization or sound effects, only being Updateable
 * and not Drawable. The subclass FxLevel offers a visual representation of
 * levels. You would probably only use objects of the type Level for a server to
 * avoid having to load textures, sounds etc.
 **/
public class Level implements Updateable {

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////CONSTANT VARIABLES////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /* tiles and powerups (powerups are a sort of tile) */

    /** Value representing a tile of GRASS; sort of the 'empty space'. */
    public static final byte GRASS = 0;
    /** Value representing a tile of STONE. Impassable, but destroyable. */
    public static final byte STONE = 1;
    /** Probability for putting a STONE when randomly creating a Level. */
    public static final float STONEPROBABILITY = 0.75f;
    /** Value representing the start of a STONE's counter when exploding. */
    public static final byte EXPLSTART = 60;
    /** Value representing the end of a STONE's counter when exploding. */
    public static final byte EXPLEND = 35;
    /** Value representing a tile of BEDROCK. Impassable and undestroyable. */
    public static final byte BEDROCK = 2;
    /** Value representing a tile holding a visible EXIT. */
    public static final byte EXIT = 3;
    /** Value representing a tile holding an invisible EXIT (underneath STONE). */
    public static final byte HIDDENEXIT = 4;
    /** Value representing a tile holding a BOMBPLUS powerup (increases ammo). */
    public static final byte BOMBPLUS = 5;
    /** Probability of a BOMBPLUS powerup spawning. See spawnPowerup(). */
    public static final float BOPLPROB = 0.2f;
    /** Value representing a tile holding a FIREPLUS powerup (increases range). */
    public static final byte FIREPLUS = 6;
    /** Probability of a FIREPLUS powerup spawning. See spawnPowerup(). */
    private static final float FIPLPROB = 0.2f;
    /** Value representing a tile holding a CHUCKNORRIS powerup (godmode). */
    public static final byte CHUCKNORRIS = 7;
    /** Probability of a CHUCKNORRIS powerup spawning. See spawnPowerup(). */
    private static final float NORRISPROB = 10.01f;
    /** Value representing a tile holding a QUADDAMAGE powerup (4x range). */
    public static final byte QUADDAMAGE = 8;
    /** Probability of a QUADDAMAGE powerup spawning. See spawnPowerup(). */
    public static final float QDPROB = 10.01f;
    /** Array holding all possible powerups. */
    public static final byte[] POWERUPS = { BOMBPLUS, FIREPLUS, CHUCKNORRIS,
            QUADDAMAGE };
    /** Array holding the corresponding probability for each powerup. */
    public static final float[] PWUPPROB = { BOPLPROB, FIPLPROB, NORRISPROB,
            QDPROB };

    /* flag accessing constants */

    /** Holds the t flag (i.e. a tile should be transmitted). */
    public static final short TRANSMIT = (short) (1 << 15);
    /** Holds a 'full' tile, i.e. all T bits are set to one. */
    public static final short TILE = 127; // 0...011111111

    /* some initial/minimum values */

    /** Value holding minimum width/height of a Level. */
    public static final byte MINDIM = 7;
    /** Value holding the number of initially created powerups. */
    public static final byte INITIALPOWERUPS = 10;
    /** Maximum number of loops to avoid while(true) constructs. */
    public static final byte MAXLOOPCOUNT = 100;

    /* some (error) messages for logging etc. */

    /** Format for creating a String reporting about invalid dimension. */
    public static final String DIMERROR = "Cannot create new Level with dimension %dx%d: ";

    /** Format for creating a String reporting about invalid dimension. */
    public static final String MINDIMERROR = DIMERROR
            + "Too small. Defaulting to " + MINDIM + "x" + MINDIM + ".";

    /** Format for creating a String reporting about invalid dimension. */
    public static final String ODDDIMERROR = DIMERROR
            + "Needs to be an odd number. Using %dx%d instead.";

    /** Format for creating a String reporting about an invalid array of tiles. */
    public static final String TILESERROR = "Given tiles are invalid. Defaulting to random level.";

    /** Format for creating a String reporting about this Level's creation. */
    public static final String NEWLEVEL = "Created new Level. Dimension: %dx%d. Powerup creation: %b.";

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////// STATIC METHODS//////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This method validates a tile. Only the least significant byte (which
     * holds the value of a tile) is validated, not it's flags. Returns true if
     * the value represents a valid tile.
     * 
     * @param tile Tile to be validated.
     * @return true if tile is valid, false otherwise.
     */
    public static final boolean isValidTile(short tile) {
        tile &= TILE;
        return (tile >= GRASS && tile <= QUADDAMAGE)
                || (tile >= EXPLEND && tile <= EXPLSTART);
    }

    /**
     * This method validates a two dimensional array of tiles (basically a Level
     * itself). Only the least significant bytes (which holds the value of a
     * tile) are validated, not the flags. Returns true if every tile of the
     * given array is valid.
     * 
     * @param tiles Tiles to be validated.
     * @return true if all tiles are valid, false otherwise.
     */
    public static final boolean validTiles(short[][] tiles) {
        if (tiles.length <= 0)
            return false;
        if (tiles[0].length <= 0)
            return false;
        for (int i = 0; i < tiles.length; i++)
            for (int j = 0; j < tiles[0].length; j++)
                if (!isValidTile(tiles[i][j]))
                    return false;
        return true;
    }

    /**
     * This method randomly spawns a powerup from the array POWERUPS. The
     * probability for the powerup POWERUPS[i] to spawn is *
     * PWUPPROB[i]/POWERUPS.length. In case no powerup spawns, a GRASS tile is
     * returned.
     * 
     * @return A randomly chosen value from POWERUPS or a GRASS tile.
     */
    private static final short spawnPowerup() {
        // first step: choose a powerup that may spawn
        int index = (int) (Math.random() * POWERUPS.length);
        // second step: determine if it will spawn
        if (Math.random() <= PWUPPROB[index])
            return POWERUPS[index];
        return GRASS; // default to GRASS
    }

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////////FIELDS//////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    protected int width, height; // dimension of tiles
    protected final short[][] tiles; // holds all the tiles of this Level
    protected final Bomb[][] bombs; // holds all the Bombs of this Level

    private final int[][] spawnPoints; // holds all the predefined spawn points

    private final LinkedList<Short> nextPowerups; // holds the next powerups
    private boolean spawnPowerups; // true this Level randomly creates powerups

    // /////////////////////////////////////////////////////////////////////////
    // //////////////////////////CONSTRUCTORS///////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new Level with the dimension
     * 
     * Math.max(width, MINDIM) x Math.max(height, MINDIM).
     * 
     * The values of the tiles are chosen randomly. An exit is created if the
     * param spawnExit is true.
     * 
     * You can specify if the new Level should randomly spawn powerups on its
     * own or be dependent on the user to provide them.
     * 
     * @param width The width of the new Level in tiles.
     * @param height The width of the new Level in tiles.
     * @param spawnPowerups true is this Level should randomly create powerups.
     * @param spawnExit true if this Level should spawn a (HIDDEN)EXIT tile
     * somewhere
     */
    public Level(int width, int height, boolean spawnPowerups, boolean spawnExit) {
        if (width < MINDIM || height < MINDIM) {
            width = Math.max(width, MINDIM);
            height = Math.max(height, MINDIM);
            Logger.writeerrln(String.format(MINDIMERROR, width, height));
        } else if (width % 2 == 0 || height % 2 == 0) {
            int wrongWidth = width;
            int wrongHeight = height;
            width = width + 1 - width % 2;
            height = height + 1 - height % 2;
            Logger.writeerrln(String.format(ODDDIMERROR, wrongWidth,
                    wrongHeight, width, height));
        }
        this.width = width;
        this.height = height;
        this.spawnPowerups = spawnPowerups;
        // create array for tiles & bombs
        tiles = new short[width][height];
        bombs = new Bomb[width][height];
        nextPowerups = initializePowerups();
        spawnPoints = initializeDefaultSpawnpoints();
        // fill the array with random tiles
        fillRandomly(spawnExit);
        if (Logger.verbose())
            Logger.writeln(String
                    .format(NEWLEVEL, width, height, spawnPowerups));
    }

    /**
     * Creates a new Level with the dimension
     * 
     * Math.max(width, MINDIM) x Math.max(height, MINDIM).
     * 
     * The values of the tiles are chosen randomly. There will be no exit. The
     * newly created Level will randomly spawn powerups.
     * 
     * @param width The width of the new Level in tiles.
     * @param height The width of the new Level in tiles.
     */
    public Level(int width, int height) {
        this(width, height, true, false);
    }

    /**
     * Tries to create a new Level from a given two-dimensional array. If either
     * width or height of the given array are smaller than MINDIM, this
     * constructor default to creating a randomized Level.
     * 
     * You can specify if the new Level should randomly spawn powerups on its
     * own or be dependent on the user to provide them.
     * 
     * @param tiles The two-dimensional array to create the Level from.
     * @param spawnPowerups true is this Level should randomly create powerups.
     */
    public Level(short[][] tiles, boolean spawnPowerups) {
        // check if dimensions are okay
        if (tiles.length < MINDIM || tiles[0].length < MINDIM) {
            width = MINDIM;
            height = MINDIM;
            Logger.writeerrln(String.format(MINDIMERROR, width, height));
            // replace the given array with a randomly filled one
            tiles = new short[this.width][this.height];
            fillRandomly(true);
        }
        // check if array holds valid tiles
        else if (!validTiles(tiles)) {
            Logger.writeerrln(TILESERROR);
            // replace the given array with a randomly filled one
            tiles = new short[tiles.length][tiles[0].length];
            fillRandomly(true);
        }
        // at this point, we proceed as above
        this.tiles = tiles;
        this.width = tiles.length;
        this.height = tiles[0].length;
        this.spawnPowerups = spawnPowerups;
        bombs = new Bomb[width][height];
        nextPowerups = initializePowerups();
        spawnPoints = initializeDefaultSpawnpoints();
    }

    /**
     * Tries to create a new Level from a given two-dimensional array. If either
     * width or height of the given array are smaller than MINDIM, this
     * constructor default to creating a randomized Level.
     * 
     * The newly created Level will randomly spawn powerups.
     * 
     * @param tiles The two-dimensional array to create the Level from.
     * @param spawnPowerups true is this Level should randomly create powerups.
     */
    public Level(short[][] tiles) {
        this(tiles, true);
    }

    /**
     * This method initializes the Level's powerup Queue. If spawnPowerups is
     * true, the Queue will be filled with INITIALPOWERUPS randomly generated
     * powerups. The resulting Queue is returned.
     * 
     * @return The Level's initial powerup Queue.
     */
    private LinkedList<Short> initializePowerups() {
        LinkedList<Short> nextPowerups = new LinkedList<Short>();
        if (!spawnPowerups)
            return nextPowerups;
        for (int i = 0; i < INITIALPOWERUPS; i++) {
            nextPowerups.offer(spawnPowerup());
        }
        return nextPowerups;
    }

    /**
     * This method initializes the Level's default spawn points. The are the
     * four corners of this Level. The corresponding two-dimensional array is
     * returned.
     * 
     * @return The Level's initial spawn points.
     */
    private final int[][] initializeDefaultSpawnpoints() {
        return new int[][] { { 1, 1 }, { width - 2, 1 }, { 1, height - 2 },
                { width - 2, height - 2 } };
    }

    /**
     * This method fills the tiles array randomly. The basic algorithm is to put
     * a BEDROCK every second row and column. The four default spawn points are
     * always left free. Apart from that, the method spawns STONE or GRASS tiles
     * randomly according to STONEPROBABILITY. Everything is surrounded by a
     * ring of BEDROCK tiles.
     * 
     * If spawnExit is true, an EXIT (or HIDDENEXIT) tile will be randomly
     * spawned within the inner third of this Level.
     * 
     * @param spawnExit true if an exit should be created.
     */
    protected void fillRandomly(boolean spawnExit) {
        // create a ring of BEDROCK tiles
        for (int i = 0; i < width; i++) {
            // top and bottom
            tiles[i][0] = tiles[i][height - 1] = BEDROCK;
        }
        for (int j = 1; j < height - 1; j++) {
            // left and right
            tiles[0][j] = tiles[width - 1][j] = BEDROCK;
        }

        for (int i = 1; i < width - 1; i++) {
            for (int j = 1; j < height - 1; j++) {
                if ((j % 2 == 0) && (i % 2 == 0))
                    // put a BEDROCK every second row & column
                    tiles[i][j] = BEDROCK;

                // take some special care of the default spawn points
                else if ((i == 3 || i == width - 4)
                        && ((j >= 1 && j <= 3) || (j >= height - 4 && j <= height - 2)))
                    tiles[i][j] = STONE;
                else if ((j == 3 || j == height - 4)
                        && ((i >= 1 && i < 3) || (i > width - 4 && i <= width - 2)))
                    tiles[i][j] = STONE;

                // fill everything but the spawn points randomly
                else if ((i >= 4 && i <= width - 5)
                        || (j >= 4 && j <= height - 5)) {
                    if (Math.random() <= STONEPROBABILITY)
                        tiles[i][j] = STONE;

                    else tiles[i][j] = GRASS;
                }

                else tiles[i][j] = GRASS;
            }
        }
        if (!spawnExit)
            return;
        // let's try to spawn an exit..
        // want a padding of min. 3 blocks but prefer a third of this Level
        int xOffset = Math.max(3, width / 3);
        int yOffset = Math.max(3, height / 3);
        int i = xOffset + (int) (Math.random() * (width - 2 * xOffset));
        int j = yOffset + (int) (Math.random() * (height - 2 * yOffset));

        // if the random position is BEDROCK, advance by one tile
        if (tiles[i][j] == BEDROCK)
            i++;

        // see if the exit should be hidden or not
        if (tiles[i][j] == STONE)
            tiles[i][j] = HIDDENEXIT;
        else tiles[i][j] = EXIT;
    }

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////////////GETTERS & SETTERS/////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * Returns the width of this Level in tiles.
     * 
     * @return width of this Level in tiles.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this Level in tiles.
     * 
     * @return height of this Level in tiles.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns (and removes) the next powerup from this Level's powerup Queue.
     * If the powerup Queue is empty, a GRASS tile is returned.
     * 
     * @return The next powerup to be spawned.
     */
    private short getNextPowerup() {
        if (spawnPowerups) {
            // set the t flag so clients will receive the new item
            nextPowerups.offer(markForTransmit(spawnPowerup()));
        }
        if (!nextPowerups.isEmpty())
            return nextPowerups.poll();
        else return GRASS;
    }

    /**
     * Inserts a new powerup into this Level's powerup Queue. If the powerup is
     * not a valid tile according to isValidTile(tile), it will not be inserted.
     * 
     * @param tile The new powerup to insert in this Level's powerup Queue.
     */
    public void setNextPowerup(short tile) {
        if (!isValidTile(tile))
            return;
        nextPowerups.offer(tile);
    }

    /**
     * Returns a copy of this Level's powerup Queue.
     * 
     * @return A copy of this Level's powerup Queue.
     */
    public Queue<Short> getAllPowerups() {
        Queue<Short> nextPowerups = new LinkedList<Short>();
        for (int i = 0; i < this.nextPowerups.size(); i++)
            nextPowerups.offer(this.nextPowerups.get(i));
        return nextPowerups;
    }

    /**
     * Returns a Queue holding a subset of this Level's powerup Queue. It will
     * contain all the powerups that have been marked for transmit.
     * 
     * This Level's powerups will all be unmarked for transmit.
     * 
     * @return A Queue holding all powerups to be transmitted.
     */
    public Queue<Short> getNewPowerups() {
        Queue<Short> newPowerups = new LinkedList<Short>();
        short tile;
        for (int i = 0; i < nextPowerups.size(); i++) {
            tile = nextPowerups.get(i);
            if (markedForTransmit(tile)) {
                tile = unmarkForTransmit(tile);
                newPowerups.add(tile);
                nextPowerups.set(i, tile);
            }
        }
        return newPowerups;
    }

    /**
     * Returns the value of the tile at position (posX, posY). The returned tile
     * is cleared of all flags. If (posX, posY) is out of the bounds of this
     * Level, a GRASS tile will be returned.
     * 
     * @param posX The x coordinate of the tile to be returned. Cast to integer.
     * @param posY The y coordinate of the tile to be returned. Cast to integer.
     * @return The tile at position (posX, posY) without any flags.
     */
    public short getTile(double posX, double posY) {
        return getTile((int) posX, (int) posY);
    }

    /**
     * Returns the value of the tile at position (posX, posY). The returned tile
     * is cleared of all flags. If (posX, posY) is out of the bounds of this
     * Level, a GRASS tile will be returned.
     * 
     * @param posX The x coordinate of the tile to be returned.
     * @param posY The y coordinate of the tile to be returned.
     * @return The tile at position (posX, posY) without any flags.
     */
    public short getTile(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return GRASS;
        return (short) (tiles[posX][posY] & TILE);
    }

    /**
     * Sets the tile at position (posX, posY). If the specified position is
     * outside of the bounds of this Level or if the specified value for the new
     * tile is invalid according to isValidTile(tile), the call to this method
     * is ignored.
     * 
     * @param posX The x coordinate of the tile to be set. Cast to integer.
     * @param posY The y coordinate of the tile to be set. Cast to integer.
     * @param tile New value of the tile at (posX, posY).
     */
    public void setTile(double posX, double posY, short tile) {
        setTile((int) posX, (int) posY, tile);
    }

    /**
     * Sets the tile at position (posX, posY). If the specified position is
     * outside of the bounds of this Level or if the specified value for the new
     * tile is invalid according to isValidTile(tile), the call to this method
     * is ignored.
     * 
     * @param posX The x coordinate of the tile to be set.
     * @param posY The y coordinate of the tile to be set.
     * @param tile New value of the tile at (posX, posY).
     */
    public void setTile(int posX, int posY, short tile) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height
                || !isValidTile(tile))
            return;
        tiles[posX][posY] = tile;
    }

    /**
     * Destroys the tile at position (posX, posY). If the specified position is
     * outside of the bounds of this Level, the call to this method is ignored
     * and false is returned. If there is a Bomb at (posX, posY), it will
     * explode as well (chain reaction). If the tile is successfully destroyed,
     * true is returned.
     * 
     * @param posX The x coordinate of the tile to be destroyed. Cast to
     * integer.
     * @param posY The x coordinate of the tile to be destroyed. Cast to
     * integer.
     * @return true is the tile was destroyed.
     */
    public boolean destroyBlock(double posX, double posY) {
        return destroyBlock((int) posX, (int) posY);
    }

    /**
     * Destroys the tile at position (posX, posY). If the specified position is
     * outside of the bounds of this Level, the call to this method is ignored
     * and false is returned. If there is a Bomb at (posX, posY), it will
     * explode as well (chain reaction). If the tile is successfully destroyed,
     * true is returned.
     * 
     * @param posX The x coordinate of the tile to be destroyed.
     * @param posY The x coordinate of the tile to be destroyed.
     * @return true is the tile was destroyed.
     */
    public boolean destroyBlock(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return false;
        if (bombs[posX][posY] != null && !bombs[posX][posY].isExploding())
            invokeChainReaction(posX, posY);
        short tile = getTile(posX, posY);
        if (tile == STONE) {
            tiles[posX][posY] &= ~TILE;
            tiles[posX][posY] |= EXPLSTART;
            return true;
        } else if (tile == HIDDENEXIT) {
            tiles[posX][posY] &= ~TILE;
            tiles[posX][posY] |= EXIT;
            return true;
        } else if (tile == BOMBPLUS || tile == FIREPLUS || tile == CHUCKNORRIS
                || tile == QUADDAMAGE) {
            tiles[posX][posY] &= ~TILE;
            tiles[posX][posY] |= GRASS;
            return true;
        }
        return false;
    }

    /**
     * Helper method for destroyBlock(posX, posY). All it does if to tell the
     * Bomb at position (posX, posY) to explode. Is meant to be overwritten by
     * subclasses for more control over corresponding visual effects (see
     * FxLevel).
     * 
     * @param posX The x coordinate of the Bomb to be destroyed.
     * @param posY The y coordinate of the Bomb to be destroyed.
     */
    protected void invokeChainReaction(int posX, int posY) {
        bombs[posX][posY].explode();
    }

    /**
     * Returns true if the tile at position (posX, posY) is considered to be
     * solid, i.e. if it is a STONE (or HIDDENEXIT) or BEDROCK tile or has a
     * solid, i.e. not exploded, Bomb. If the specified position is outside of
     * the bounds of this Level, false is returned.
     * 
     * @param posX The x coordinate of the tile to be checked. Cast to integer.
     * @param posY The y coordinate of the tile to be checked. Cast to integer.
     * @return true is the tile at (posX, posY) is solid.
     */
    public boolean isSolid(double posX, double posY) {
        return isSolid((int) posX, (int) posY);
    }

    /**
     * Returns true if the tile at position (posX, posY) is considered to be
     * solid, i.e. if it is a STONE (or HIDDENEXIT) or BEDROCK tile or has a
     * solid, i.e. not exploded, Bomb. If the specified position is outside of
     * the bounds of this Level, false is returned.
     * 
     * @param posX The x coordinate of the tile to be checked.
     * @param posY The y coordinate of the tile to be checked.
     * @return true is the tile at (posX, posY) is solid.
     */
    public boolean isSolid(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return false;
        short tile = getTile(posX, posY);
        return (tile == BEDROCK || tile == STONE || tile == HIDDENEXIT
                || (tile > EXPLEND && tile <= EXPLSTART) || (hasBomb(posX, posY))
                && bombs[posX][posY].isCounting());
    }

    /**
     * This method tries to put a new Bomb at the specified position. A Bomb
     * cannot be put at position (posX, posY) if either the position is outside
     * of the bounds of this Level or if this Level already has a Bomb at that
     * position. The parameters for range and quadDamage are determined using
     * the Player's getters.
     * 
     * If a new Bomb was successfully place, true is returned.
     * 
     * @param posX X coordinate of the new Bomb to be placed. Cast to integer.
     * @param posY Y coordinate of the new Bomb to be placed. Cast to integer.
     * @param player Player object planting the new Bomb.
     * @return true is Bomb could be placed, false otherwise.
     */
    public boolean putBomb(double posX, double posY, Player player) {
        return putBomb((int) posX, (int) posY, player);
    }

    /**
     * This method tries to put a new Bomb at the specified position. A Bomb
     * cannot be put at position (posX, posY) if either the position is outside
     * of the bounds of this Level or if this Level already has a Bomb at that
     * position. The parameters given for range and quadDamage are used instead
     * of the Player's.
     * 
     * If a new Bomb was successfully place, true is returned.
     * 
     * @param posX X coordinate of the new Bomb to be placed. Cast to integer.
     * @param posY Y coordinate of the new Bomb to be placed. Cast to integer.
     * @param player Player object planting the new Bomb.
     * @param range Maximum range of the new Bomb to be placed.
     * @param quadDamage Specifies if the Bomb is in Quad Damage mode.
     * @return true is Bomb could be placed, false otherwise.
     */
    public boolean putBomb(double posX, double posY, Player player, byte range,
            boolean quadDamage) {
        return putBomb((int) posX, (int) posY, player, range, quadDamage);
    }

    /**
     * This method tries to put a new Bomb at the specified position. A Bomb
     * cannot be put at position (posX, posY) if either the position is outside
     * of the bounds of this Level or if this Level already has a Bomb at that
     * position. The parameters for range and quadDamage are determined using
     * the Player's getters.
     * 
     * If a new Bomb was successfully place, true is returned.
     * 
     * @param posX X coordinate of the new Bomb to be placed.
     * @param posY Y coordinate of the new Bomb to be placed.
     * @param player Player object planting the new Bomb.
     * @return true is Bomb could be placed, false otherwise.
     */
    public boolean putBomb(int posX, int posY, Player player) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height
                || hasBomb(posX, posY))
            return false;
        bombs[posX][posY] = new Bomb(posX, posY, this, player);
        return true;
    }

    /**
     * This method tries to put a new Bomb at the specified position. A Bomb
     * cannot be put at position (posX, posY) if either the position is outside
     * of the bounds of this Level or if this Level already has a Bomb at that
     * position. The parameters given for range and quadDamage are used instead
     * of the Player's.
     * 
     * If a new Bomb was successfully place, true is returned.
     * 
     * @param posX X coordinate of the new Bomb to be placed.
     * @param posY Y coordinate of the new Bomb to be placed.
     * @param player Player object planting the new Bomb.
     * @param range Maximum range of the new Bomb to be placed.
     * @param quadDamage Specifies if the Bomb is in Quad Damage mode.
     * @return true is Bomb could be placed, false otherwise.
     */
    public boolean putBomb(int posX, int posY, Player player, byte range,
            boolean quadDamage) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height
                || hasBomb(posX, posY))
            return false;
        bombs[posX][posY] = new Bomb(posX, posY, this, player, range,
                quadDamage);
        return true;
    }

    /**
     * This method tries to remove a Bomb at the specified position. The
     * affected Bomb object is 'nulled out' and thus removed from being updated.
     * If there is no Bomb at the specified position or if the position if out
     * of the bounds of this Level, the call to this method is ignored.
     * 
     * @param posX X coordinate of the Bomb to be removed. Cast to integer.
     * @param posY Y coordinate of the Bomb to be removed. Cast to integer.
     */
    public void removeBomb(double posX, double posY) {
        removeBomb((int) posX, (int) posY);
    }

    /**
     * This method tries to remove a Bomb at the specified position. The
     * affected Bomb object is 'nulled out' and thus removed from being updated.
     * If there is no Bomb at the specified position or if the position if out
     * of the bounds of this Level, the call to this method is ignored.
     * 
     * @param posX X coordinate of the Bomb to be removed.
     * @param posY Y coordinate of the Bomb to be removed.
     */
    public void removeBomb(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return;
        bombs[posX][posY] = null;
    }

    /**
     * Returns true if this Level has a Bomb at the specified position. The
     * state of the Bomb (i.e. COUNTING, EXPLODING, EXPLODED -- see class Bomb)
     * is ignored. If the specified position is out of the bounds of this Level
     * or if there is no Bomb at the specified position, false is returned.
     * 
     * @param posX X coordinate to be checked for an existing Bomb. Cast to
     * integer.
     * @param posY Y coordinate to be checked for an existing Bomb. Cast to
     * integer.
     * @return true if a Bomb at (posX, posY) exists, false otherwise.
     */
    public boolean hasBomb(double posX, double posY) {
        return hasBomb((int) posX, (int) posY);
    }

    /**
     * Returns true if this Level has a Bomb at the specified position. The
     * state of the Bomb (i.e. COUNTING, EXPLODING, EXPLODED -- see class Bomb)
     * is ignored. If the specified position is out of the bounds of this Level
     * or if there is no Bomb at the specified position, false is returned.
     * 
     * @param posX X coordinate to be checked for an existing Bomb.
     * @param posY Y coordinate to be checked for an existing Bomb.
     * @return true if a Bomb at (posX, posY) exists, false otherwise.
     */
    public boolean hasBomb(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return false;
        return (bombs[posX][posY] != null);
    }

    /**
     * Returns a spawn point for the num'th Player object. A predefined value
     * from this Level's spawn point array is selected if possible (for a
     * randomly created Level, the four corners are the four available spawn
     * points. If no spawn point is predefined for the num'th Player, this
     * method tries to select a random, suitable spawn point; if none can be
     * found after a predefined number of steps (see MAXLOOPCOUNT), any spawn
     * point is returned.
     * 
     * This method should be used with care; if a deterministic outcome for
     * values of num >= 4 is desired, one should provide this Level with a
     * predefined array of spawn points upon construction.
     * 
     * @param num The number of the Player whose spawn point is requested.
     * @return Spawn point for Player num in form of an array: [posX, posY].
     */
    public int[] getSpawnPoint(int num) {
        if (num >= 0 && num <= 3)
            return spawnPoints[num];
        int[] spawnPoint = new int[2];
        for (int i = 0; i < MAXLOOPCOUNT; i++) {
            spawnPoint[0] = (int) (Math.random() * width);
            spawnPoint[1] = (int) (Math.random() * height);
            // see if there is a free L-shaped area
            if (!isSolid(spawnPoint[0], spawnPoint[1])
                    && ((!isSolid(spawnPoint[0] + 1, spawnPoint[1]) && (!isSolid(
                            spawnPoint[0], spawnPoint[1] - 1)
                            || !isSolid(spawnPoint[0], spawnPoint[1] + 1)
                            || !isSolid(spawnPoint[0] + 1, spawnPoint[1] - 1) || !isSolid(
                                spawnPoint[0] + 1, spawnPoint[1] + 1)))

                            || (!isSolid(spawnPoint[0] - 1, spawnPoint[1]) && (!isSolid(
                                    spawnPoint[0], spawnPoint[1] - 1)
                                    || !isSolid(spawnPoint[0],
                                            spawnPoint[1] + 1)
                                    || !isSolid(spawnPoint[0] - 1,
                                            spawnPoint[1] - 1) || !isSolid(
                                        spawnPoint[0] - 1, spawnPoint[1] + 1)))

                            || (!isSolid(spawnPoint[0], spawnPoint[1] - 1) && (!isSolid(
                                    spawnPoint[0] - 1, spawnPoint[1] - 1) || !isSolid(
                                    spawnPoint[0] + 1, spawnPoint[1] - 1)))

                    || (!isSolid(spawnPoint[0], spawnPoint[1] + 1) && (!isSolid(
                            spawnPoint[0] - 1, spawnPoint[1] + 1) || !isSolid(
                            spawnPoint[0] + 1, spawnPoint[1] + 1)))))
                break;
        }
        return spawnPoint;
    }

    // /////////////////////////////////////////////////////////////////////////
    // ///////////////GETTERS & SETTERS FOR FLAGS///////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    /**
     * This method takes the given tile, marks it to be transmitted (i.e. sets
     * the t flag) and returns the resulting tile.
     * 
     * @param tile The tile to be marked for transmit.
     * @return The given tile with its t flag set to 1.
     */
    public short markForTransmit(short tile) {
        return (short) (tile | TRANSMIT);
    }

    /**
     * This method takes the given tile, unmarks it to be transmitted (i.e.
     * unsets the t flag) and returns the resulting tile.
     * 
     * @param tile The tile to be unmarked for transmit.
     * @return The given tile with its t flag set to 0.
     */
    public short unmarkForTransmit(short tile) {
        return (short) (tile & TRANSMIT);
    }

    /**
     * Returns true if the given tile is marked for transmit (i.e. has its t
     * flag set to 1).
     * 
     * @param tile The tile to be checked.
     * @return true if the tile is marked for transmit, false otherwise.
     */
    public boolean markedForTransmit(short tile) {
        return (tile & TRANSMIT) != 0;
    }

    // /////////////////////////////////////////////////////////////////////////
    // /////////////////////////UPDATE METHODS//////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////

    @Override
    /**
     * Performs an update on this Level, e.g. update the animation for powerups
     * or decrement the counter for an exploding stone. This method should be
     * called each game tick.
     */
    public void update() {
        short tile;
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                tile = getTile(i, j);
                if (tile > EXPLEND && tile <= EXPLSTART) {
                    decrementStoneExplosion(i, j);
                } else if (tile == EXPLEND)
                    removeStone(i, j);
                if (bombs[i][j] != null)
                    bombs[i][j].update();
            }
    }

    /**
     * Decrements the counter for an exploding stone at the given position. This
     * method should not be called on its own as there is no check as to whether
     * the value at (posX, posY) is valid for this action.
     * 
     * @param posX X coordinate of the tile whose counter is to be decremented.
     * @param posY Y coordinate of the tile whose counter is to be decremented.
     */
    protected void decrementStoneExplosion(int posX, int posY) {
        short tile = getTile(posX, posY);
        tiles[posX][posY] &= ~TILE;
        tiles[posX][posY] |= (tile - 1);
    }

    /**
     * This method is called when a stone explosion reaches its end (specified
     * by EXPLEND). It actually removes the stone and replaces it with the next
     * powerup from this Level's powerup queue.
     * 
     * @param posX X coordinate of the stone to ultimately remove.
     * @param posY Y coordinate of the stone to ultimately remove.
     */
    protected void removeStone(int posX, int posY) {
        tiles[posX][posY] &= ~TILE;
        tiles[posX][posY] |= getNextPowerup();
    }

}// end of class Level
