package bm.gfx;

import java.awt.Graphics2D;

import bm.Level;
import bm.Player;

public class FxLevel extends Level implements Drawable {

    public static final byte[] EXPLODING = { 60, 55, 50, 45, 40, 35 };

    public static Texture GRASS_TEXT = new Texture(0, 0, 32, 32);
    public static Texture GRASS_GORE = new Texture(96, 0, 32, 32);

    public static Texture STONE_TEXT = new Texture(32, 0, 32, 32);
    public static Texture STONE_GORE = new Texture(128, 0, 32, 32);

    public static Texture BEDROCK_TEXT = new Texture(64, 0, 32, 32);
    public static Texture BEDROCK_GORE = new Texture(160, 0, 32, 32);

    public static Texture EXIT_TEXT = new Texture(0, 32, 32, 32);

    public static Texture STONE_EXPL1 = new Texture(128, 64, 32, 32);
    public static Texture STONE_EXPL2 = new Texture(160, 64, 32, 32);
    public static Texture STONE_EXPL3 = new Texture(128, 96, 32, 32);
    public static Texture STONE_EXPL4 = new Texture(160, 96, 32, 32);
    public static Texture[] STONE_EXPL = { STONE_TEXT, STONE_EXPL1,
            STONE_EXPL2, STONE_EXPL3, STONE_EXPL4 };

    public static Texture FIREPLUS1 = new Texture(64, 32, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { 0xff000000 });
    public static Texture FIREPLUS2 = new Texture(64, 32, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { 0xffffffff });
    public static Texture[] FIREPLUS_TEXT = { FIREPLUS1, FIREPLUS2 };

    public static Texture BOMBPLUS1 = new Texture(32, 32, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { 0xff5400ff });
    public static Texture BOMBPLUS2 = new Texture(32, 32, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { 0xffffea00 });
    public static Texture[] BOMBPLUS_TEXT = { BOMBPLUS1, BOMBPLUS2 };

    public static Texture NORRIS1 = new Texture(128, 128, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { 0xffff0000 });
    public static Texture NORRIS2 = new Texture(128, 128, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { 0xff0000ff });
    public static Texture[] CHUCKNORRIS_TEXT = { NORRIS1, NORRIS2 };

    public static final int QDCOLOR2 = 0xff18e2ee; // bright
    public static final int QDCOLOR1 = 0xff1881ee; // dark
    public static final int QDCOLOR3 = 0xff0d499d; // darkes

    public static final Texture QD1 = new Texture(128, 160, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { QDCOLOR1 });
    public static final Texture QD2 = new Texture(128, 160, 32, 32)
            .replaceColors(Texture.HOUSECOLORS, new int[] { QDCOLOR2 });
    public static final Texture[] QUADDAMAGE_TEXT = { QD1, QD2 };

    public static final short DRAW = (short) (1 << 14);
    public static final short GORE = (short) (1 << 13);

    private static final float GOREPROB = 0.50f;

    public void addGore(double posX, double posY, int radius) {
        addGore((int) posX, (int) posY, radius);
    }

    public void addGore(int posX, int posY, int radius) {
        // bestimme erst die Grenzen
        int leftX = posX - radius;
        int rightX = posX + radius;
        int topY = posY - radius;
        int bottomY = posY + radius;

        // verschiebe die Grenzen, falls sie ausserhalb des Spielfelds liegen
        if (leftX < 0)
            leftX = 0;
        if (rightX >= width)
            rightX = width - 1;
        if (topY < 0)
            topY = 0;
        if (bottomY >= height)
            bottomY = height - 1;

        // setze dann das G-Flag
        for (int i = leftX; i <= rightX; i++) {
            for (int j = topY; j <= bottomY; j++)
                if (Math.random() < GOREPROB)
                    addGore(i, j);
        }

    }

    private void addGore(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return;
        tiles[posX][posY] |= GORE;
    }

    public boolean hasGore(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return false;
        return (tiles[posX][posY] & GORE) != 0;
    }

    private static final byte ANIMDURATION = 20;
    private static int animCounter = ANIMDURATION;
    private static int animFrame = 0;
    private int tileDim;
    private boolean drawAll, drawPwups;

    public FxLevel(int width, int height, int pixelWidth, int pixelHeight,
            boolean spawnPowerups, boolean spawnExit) {
        super(width, height, spawnPowerups, spawnExit);
        updateTileDimensions(pixelWidth, pixelHeight);
        drawAll = drawPwups = true;
    }

    public FxLevel(int width, int height, int pixelWidth, int pixelHeight) {
        this(width, height, pixelWidth, pixelHeight, true, false);
    }

    public FxLevel(short[][] tiles, int pixelWidth, int pixelHeight,
            boolean spawnPowerups) {
        super(tiles, spawnPowerups);
        updateTileDimensions(pixelWidth, pixelHeight);
        drawAll = drawPwups = true;
    }

    public FxLevel(short[][] tiles, int pixelWidth, int pixelHeight) {
        this(tiles, pixelWidth, pixelHeight, true);
    }

    /**
     * Liefert die aktuelle Kantenlaenge aller Kacheln in Pixeln zurueck. Der
     * hier zurueckgegebene Wert wird auch von der draw()-Methode der gleichen
     * Klasse genutzt.
     * 
     * @return Die Kantenlaenge der Kacheln in Pixeln.
     */
    public int getTileDim() {
        return tileDim;
    }

    /**
     * Diese Methode aktualisiert die Kantenlaenge der Kacheln anhand der Breite
     * und Hoehe (in Pixeln) des zur Verfuegung stehenden Bereichs. Konkreter:
     * In der Regel soll ein Objekt dieser Klasse auf ein
     * Panel/Component/BufferedImage gezeichnet werden. Diese Methode passt die
     * Kantenlaenge so an, dass das Spielfeld eben jenes
     * Panel/Component/BufferedImage moeglichst komplett ausfuellt (die Kacheln
     * haben weiterhin quadratische Groesse).
     * 
     * Diese Methode sollte zum Beispiel beim Vergroessern oder Verkleinern des
     * Spielfensters aufgerufen werden.
     * 
     * @param pixelWidth Breite des Bereichs, auf welchen dieses Objekt spaeter
     * gezeichnet werden soll.
     * @param pixelHeight Hoehe des Bereichs, auf welchen dieses Objekt spaeter
     * gezeichnet werden soll
     */
    public void updateTileDimensions(int pixelWidth, int pixelHeight) {
        tileDim = Math.min(pixelWidth / width, pixelHeight / height);
        drawAll = true;
    }

    @Override
    public void update() {
        super.update();
        animCounter--;
        if (animCounter <= 0) {
            animCounter = ANIMDURATION;
            animFrame = (animFrame + 1) % BOMBPLUS_TEXT.length;
            drawPwups = true;
        }
    }

    @Override
    protected void decrementStoneExplosion(int posX, int posY) {
        super.decrementStoneExplosion(posX, posY);
        markForUpdate(posX, posY);
    }

    @Override
    protected void removeStone(int posX, int posY) {
        super.removeStone(posX, posY);
        markForUpdate(posX, posY);
    }

    /**
     * Diese Methode zeichnet das Spielfeld auf den zum Graphics-Objekt
     * gehoerenden Bereich (zum Beispiel ein BufferedImage). Es wird immer nur
     * der Ausschnitt gezeichnet, in dem sich tatsaechlich etwas geaendert hat;
     * darueber muss das Spielfeld ueber die Methode markForUpdateByPixel
     * informiert werden.
     * 
     * @param g Das Graphics-Objekt, welches genutzt wird, um das Level zu
     * zeichnen.
     */
    public void draw(Graphics2D g) {
        short currentTile;
        boolean gore;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (drawAll || markedForUpdate(tiles[i][j])) {
                    if (!drawAll)
                        unmarkForUpdate(i, j); // if-Abfrage spart
                                               // unnoetige
                                               // Bitoperationen
                    currentTile = (short) (tiles[i][j] & TILE); // Betrachtung
                                                                // unabhaengig
                                                                // von D oder F
                                                                // Flags
                    gore = hasGore(i, j);
                    if (currentTile == GRASS)
                        drawGrass(i, j, gore, g);

                    else if (currentTile == STONE || currentTile == HIDDENEXIT)
                        drawStone(i, j, gore, g);

                    else if (currentTile == BEDROCK)
                        drawIndestructible(i, j, gore, g);

                    else if (currentTile == EXIT)
                        drawExit(i, j, g);

                    else if (currentTile == BOMBPLUS)
                        drawBombPlus(i, j, g);

                    else if (currentTile == FIREPLUS)
                        drawFirePlus(i, j, g);

                    else if (currentTile == CHUCKNORRIS)
                        drawNorris(i, j, g);

                    else if (currentTile == QUADDAMAGE)
                        drawQuadDamage(i, j, g);

                    else if (currentTile <= EXPLODING[0]
                            && currentTile >= EXPLODING[1])
                        drawStoneExplosion(i, j, 0, g);

                    else if (currentTile <= EXPLODING[1]
                            && currentTile >= EXPLODING[2])
                        drawStoneExplosion(i, j, 1, g);

                    else if (currentTile <= EXPLODING[2]
                            && currentTile >= EXPLODING[3])
                        drawStoneExplosion(i, j, 2, g);

                    else if (currentTile <= EXPLODING[3]
                            && currentTile >= EXPLODING[4])
                        drawStoneExplosion(i, j, 3, g);

                    else if (currentTile <= EXPLODING[4]
                            && currentTile >= EXPLODING[5])
                        drawStoneExplosion(i, j, 4, g);
                }
            }
        }
        drawAll = drawPwups = false;
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                if (bombs[i][j] != null)
                    ((Drawable) bombs[i][j]).draw((Graphics2D) g);
    }

    // es folgen Methoden zum einzelnen Zeichnen von Kacheln.

    private void drawGrass(int posX, int posY, boolean gore, Graphics2D g) {
        GRASS_TEXT.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
        if (!gore)
            return;
        GRASS_GORE.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
    }

    private void drawStone(int posX, int posY, boolean gore, Graphics2D g) {
        STONE_TEXT.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
        if (!gore)
            return;
        STONE_GORE.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
    }

    private void drawStoneExplosion(int posX, int posY, int framePos,
            Graphics2D g) {
        STONE_EXPL[framePos].draw(posX * tileDim, posY * tileDim, tileDim,
                tileDim, g);
    }

    private void drawIndestructible(int posX, int posY, boolean gore,
            Graphics2D g) {
        BEDROCK_TEXT.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
        if (!gore)
            return;
        BEDROCK_GORE.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
    }

    private void drawExit(int posX, int posY, Graphics2D g) {
        EXIT_TEXT.draw(posX * tileDim, posY * tileDim, tileDim, tileDim, g);
    }

    private void drawBombPlus(int posX, int posY, Graphics2D g) {
        BOMBPLUS_TEXT[animFrame].draw(posX * tileDim, posY * tileDim, tileDim,
                tileDim, g);
    }

    private void drawFirePlus(int posX, int posY, Graphics2D g) {
        FIREPLUS_TEXT[animFrame].draw(posX * tileDim, posY * tileDim, tileDim,
                tileDim, g);
    }

    private void drawNorris(int posX, int posY, Graphics2D g) {
        CHUCKNORRIS_TEXT[animFrame].draw(posX * tileDim, posY * tileDim,
                tileDim, tileDim, g);
    }

    private void drawQuadDamage(int posX, int posY, Graphics2D g) {
        QUADDAMAGE_TEXT[animFrame].draw(posX * tileDim, posY * tileDim,
                tileDim, tileDim, g);
    }

    public void markForUpdate(double posX, double posY, int radius) {
        markForUpdate((int) posX, (int) posY, radius);
    }

    /*
     * eigentliche Methode... fuer Anwender ist es von aussen sicherer, nur
     * obige Methode aufzurufen
     */
    public void markForUpdate(int posX, int posY, int radius) {
        // bestimme erst die Grenzen
        int leftX = posX - radius;
        int rightX = posX + radius;
        int topY = posY - radius;
        int bottomY = posY + radius;

        // verschiebe die Grenzen, falls sie ausserhalb des Spielfelds liegen
        if (leftX < 0)
            leftX = 0;
        if (rightX >= width)
            rightX = width - 1;
        if (topY < 0)
            topY = 0;
        if (bottomY >= height)
            bottomY = height - 1;

        // markiere dann alle Kacheln innerhalb der Grenzen
        for (int i = leftX; i <= rightX; i++) {
            for (int j = topY; j <= bottomY; j++)
                markForUpdate(i, j);
        }

    }

    /* Innere Methode. Von Aussen wird die markForUpdateByPixel-Methode genutzt */
    public void markForUpdate(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return;
        tiles[posX][posY] |= DRAW;
    }

    /*
     * Interne Methode, welche sowohl von obiger Methode als auch (bzw
     * insbesondere) von der draw-Methode genutzt wird.
     */
    private void unmarkForUpdate(int posX, int posY) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height)
            return;
        tiles[posX][posY] &= ~DRAW;

    }

    @Override
    public boolean putBomb(int posX, int posY, Player player) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height
                || hasBomb(posX, posY))
            return false;
        if (!player.hasQuadDamage())
            bombs[posX][posY] = new FxBomb(posX, posY, this, player);
        else bombs[posX][posY] = new QdBomb(posX, posY, this, player);
        return true;
    }

    public boolean putBomb(int posX, int posY, Player player, byte radius,
            boolean quadDamage) {
        if (posX < 0 || posX >= width || posY < 0 || posY >= height
                || hasBomb(posX, posY))
            return false;
        if (!quadDamage)
            bombs[posX][posY] = new FxBomb(posX, posY, this, player, radius,
                    quadDamage);
        else bombs[posX][posY] = new QdBomb(posX, posY, this, player, radius,
                quadDamage);
        return true;
    }

    /**
     * Diese Methode markiert alle Kacheln zum Zeichnen. Das Verhalten ist
     * aequivalent zum Markieren aller einzelnen Kacheln via
     * markForUpdateByPixel, aber weitaus effizienter und daher zu bevorzugen.
     * 
     * Eine sinnvolle Anwendung ist zum Beispiel beim Vergroessern oder
     * Verkleinern des Fensters, in welchem das Spielfeld gezeichnet wird.
     */
    public void markAllForUpdate() {
        drawAll = true;
    }

    /*
     * Liefert true, wenn das D-Flag einer Kachel gesetzt ist.
     */
    private boolean markedForUpdate(short tile) {
        return (tile & DRAW) != 0 || (drawPwups && isPowerup(tile));
    }

    private boolean isPowerup(short tile) {
        tile &= TILE;
        return (tile >= BOMBPLUS && tile <= QUADDAMAGE);
    }

    protected void fillRandomly(boolean spawnExit) {
        super.fillRandomly(spawnExit);
        markAllForUpdate();
    }

    public boolean destroyBlock(int posX, int posY) {
        boolean destroyed = super.destroyBlock(posX, posY);
        if (destroyed)
            markForUpdate(posX, posY);
        return destroyed;
    }

    protected void invokeChainReaction(int posX, int posY) {
        ((FxBomb) bombs[posX][posY]).explode(false);
    }
}
