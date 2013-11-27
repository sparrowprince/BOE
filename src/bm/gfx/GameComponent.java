package bm.gfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.image.VolatileImage;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JFrame;

import bm.Bomb;
import bm.Player;
import bm.input.KeyPoller;
import bm.io.LevelParser;
import bm.sfx.SoundManager;

public class GameComponent extends JComponent implements Runnable {
    private int width = 640;
    private int height = 480;
    private int gameWidth, gameHeight;

    private static final long SECOND = 1000000000; // eine Sekunde in
                                                   // Nanosekunden
    private static final long SLEEPTIME = SECOND / 60; // 60 FPS

    private boolean running = true;

    // im Speicher gehaltenes Bild & zugehoeriges Graphics-Objekt für
    // Double-Buffering
    private Image dbImage;
    private Graphics2D dbg;
    // das gleiche fuer das Spielfeld
    private Image gameImage;
    private Graphics2D gameG;

    private HashMap<Integer, Texture> fpsTextures;

    // managet die Tastatur.. stellt im Grunde eine auf Polling basierende
    // Lösung dar (statt Interrupts)
    private KeyPoller keyPoller;

    // fuer die Pause(ntaste)
    private boolean pausePressed = false;
    private boolean paused = false;
    private int pauseAnimCounter = PAUSEANIM;
    private int pauseAnimFrame;

    private static final int[][] PAUSEDCOLORS = new int[][] {
            { 0xffff0000, 0xffffff00 }, { 0xffff0000, 0xffaaaa00 } };

    private static final int PAUSEANIM = 20;

    private static final Texture[] PAUSED = {
            Texture.drawString("PAUSED").replaceColors(Texture.TEXTCOLORS,
                    PAUSEDCOLORS[0]),
            Texture.drawString("PAUSED").replaceColors(Texture.TEXTCOLORS,
                    PAUSEDCOLORS[1]) };

    // this is for the text shown when you've won

    private static final Texture[] VICTORY = {
            Texture.drawString("VICTORY\n\nPRESS ANY KEY\nTO RESTART")
                    .replaceColors(Texture.TEXTCOLORS, PAUSEDCOLORS[0]),
            Texture.drawString("VICTORY\n\nPRESS ANY KEY\nTO RESTART")
                    .replaceColors(Texture.TEXTCOLORS, PAUSEDCOLORS[1]) };

    private static final int WAITAFTERWIN = 90;
    private int wawCounter = WAITAFTERWIN;

    // this is for when you've died

    private static final Texture[] DEAD = {
            Texture.drawString("GAME OVER\n\nPRESS ANY KEY\nTO RESTART")
                    .replaceColors(Texture.TEXTCOLORS, PAUSEDCOLORS[0]),
            Texture.drawString("GAME OVER\n\nPRESS ANY KEY\nTO RESTART")
                    .replaceColors(Texture.TEXTCOLORS, PAUSEDCOLORS[1]) };

    private SoundManager soundManager;

    private FxPlayer player, player2;

    private HUD hud;

    private FxLevel bLevel;

    private int fps = 0, ups = 0; // wird durch den main-loop gesetzt
    private static final int[] FPSCOLORS = new int[] { 0xffff0000, 0xffffff00 };

    public GameComponent() {
        super();
        // erzeuge unseren KeyPoller
        keyPoller = new KeyPoller();
        addKeyListener(keyPoller);
        setFocusable(true);

        soundManager = SoundManager.staticInstance;
        // soundManager.repeatSound(0);

        this.setSize(width, height);
        initializeLevel("qfuake.map");
        initializeGraphics();
        initializePlayers();
    }

    private void initializeLevel(String pathToMap) {
        try {
            bLevel = new FxLevel(LevelParser.parseMap(pathToMap), width,
                    (int) (height * 0.9));
        } catch (Exception e) {
            bLevel = new FxLevel(17, 11, width, (int) (height * 0.9));
        }
    }

    private void initializeGraphics() {
        // erzeuge das Bild, auf welches das eigentliche Spielfeld gezeichnet
        // wird
        gameWidth = bLevel.getTileDim() * bLevel.getWidth();
        gameHeight = bLevel.getTileDim() * bLevel.getHeight();
        gameImage = createAcceleratedImage(gameWidth, gameHeight);
        dbImage = createAcceleratedImage(width, height);
        // gameImage = new BufferedImage(gameWidth, gameHeight,
        // BufferedImage.TYPE_3BYTE_BGR);
        gameG = (Graphics2D) gameImage.getGraphics();
        // erzeuge die Objekte für Doublebuffering
        // dbImage = new BufferedImage(width, height,
        // BufferedImage.TYPE_3BYTE_BGR);
        dbg = (Graphics2D) dbImage.getGraphics();

        fpsTextures = new HashMap<Integer, Texture>();
    }

    private VolatileImage createAcceleratedImage(int width, int height) {
        VolatileImage vImg = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleVolatileImage(width, height,
                        Transparency.OPAQUE);
        vImg.setAccelerationPriority(1);
        return vImg;
    }

    private void initializePlayers() {
        Player.resetPlayers();
        player = new FxPlayer(bLevel, "tobi", 0xff0000ff, 0xff00ff00, 1, 1);
        hud = new HUD(player, (int) (height * 0.1));
        player2 = new FxPlayer(bLevel, "ulf", 0xff00ff00, 0xff00aa00,
                bLevel.getWidth() - 2, bLevel.getHeight() - 2);
    }

    /**
     * Double Buffering wobei ein Image erstellt wird und im Hintergrund das
     * nächste Bild gemalt wird.
     */
    public void paintBuffer() {
        // zeichne das Lvel
        bLevel.draw(gameG);

        // zeichne den Spieler
        player.draw(gameG);
        player2.draw(gameG);

        int randomOffset = (int) (Math.random() * 2 * Bomb.explodeCount)
                - Bomb.explodeCount * 2;

        dbg.setColor(Color.BLACK);
        dbg.fillRect(0, 0, width, height);
        dbg.drawImage(gameImage, (width - gameWidth) / 2 + randomOffset,
                (height - gameHeight) / 2 + randomOffset, null);

        if (player.hasWon())
            VICTORY[pauseAnimFrame].drawCentered(width, height, -1,
                    bLevel.getTileDim() * 4, dbg);
        else if (player.isDead())
            DEAD[pauseAnimFrame].drawCentered(width, height, -1,
                    bLevel.getTileDim() * 4, dbg);
        else if (paused)
            PAUSED[pauseAnimFrame].drawCentered(width, height, -1,
                    bLevel.getTileDim(), dbg);
        // FIXME
        hud.draw(dbg);

        // nur Debugging: FPS
        Texture fpst = fpsTextures.get(fps);
        if (fpst == null) {
            fpst = Texture.drawString(fps + "FPS\n" + ups + "UPS")
                    .replaceColors(Texture.TEXTCOLORS, FPSCOLORS);
            fpsTextures.put(fps, fpst);
        }
        fpst.draw(width - 32 * 3, 0, -1, 32, dbg);
    }

    private void gameDrawBuffer() {
        Graphics2D g;
        try {
            g = (Graphics2D) this.getGraphics();
            if (g != null && dbImage != null) {
                g.drawImage(dbImage, 0, 0, null);
                g.dispose();
            }
        } catch (Exception e) {
            System.err.println("Error while handling graphics context!");
            e.printStackTrace();
        }
    }

    /**
     * Methode, welche die bevorzugte Groesse dieser JComponent zurueck gibt.
     * 
     * @return: Dimension WIDTH x HEIGHT, welche fest codiert sind.
     */
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    /**
     * Startet das Programm ;-)
     * 
     * @param args Bisher werden alle Parameter ignoriert.
     */
    public static void main(String[] args) {
        // Fenster erstellen
        JFrame frame = new JFrame();
        frame.setTitle("Bombeskalation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GameComponent bGui = new GameComponent();

        frame.add(bGui);
        frame.pack(); // passt die Groesse dem Inhalt an

        // zentriert das Fenster
        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        new Thread(bGui).start();
    }

    /**
     * Startet den Spieleloop.
     */
    public void run() {
        long beforeUpdate, updateTime, updateTimeDelta;

        // Vars zum FPS zaehlen
        long fpsCounter;
        int fps = 0, ups = 0;
        fpsCounter = System.nanoTime();// messen spaeter, ob eine Sek. vergangen
                                       // ist
        updateTime = SLEEPTIME;
        updateTimeDelta = 0;
        while (running) {

            beforeUpdate = System.nanoTime();
            updateTimeDelta += updateTime;

            while (updateTimeDelta >= SLEEPTIME) {
                bombermanUpdate();
                ups++;
                updateTimeDelta -= SLEEPTIME;
            }

            // zeichne alle Objekte auf den Buffer
            paintBuffer();
            // zeichne den Buffer sichtbar fuer den Nutzer
            gameDrawBuffer();

            // haben gezeichnet, ergo..
            fps++;

            if (System.nanoTime() - fpsCounter >= SECOND) {
                this.fps = fps; // update die GLOBALE Variable mit den aktuellen
                                // fps
                this.ups = ups;
                // System.out.println(fps + "FPS");
                fps = ups = 0; // lokaler Counter auf 0
                fpsCounter = System.nanoTime();
            }

            updateTime = System.nanoTime() - beforeUpdate;

            if (updateTime < SLEEPTIME) {// warte ein wenig....
                try {
                    // Thread.sleep(1);
                    Thread.sleep((SLEEPTIME - updateTime) / 1000000);
                    updateTime = System.nanoTime() - beforeUpdate;
                } catch (InterruptedException e) {
                    System.err.println("Unexpedcted Interruption!");
                    e.printStackTrace();
                }
            }

        }

    }

    /**
     * Methode, welche (atm) 60x pro Sekunde aufgerufen wird und (nicht
     * sichtbare) Updates an der Spielumgebung durchführt. Hierzu gehören das
     * Einlesen von Tastatureingaben, Bewegen des Spielerobjekts, Herunterzählen
     * des BombenCounters etc.
     */
    public void bombermanUpdate() {
        // überprüfe, ob das Fenster vergrößert wurde....
        if (width != this.getWidth() || height != this.getHeight()) {
            rescale();
        }

        if (keyPoller.isKeyDown(KeyEvent.VK_PAUSE))
            pausePressed = true;
        else if (pausePressed) {
            pausePressed = false;
            paused = !paused;
            if (paused) {
                soundManager.stopSound(0);
            } else soundManager.repeatSound(0);
        }

        bLevel.update();

        if (player.hasWon()) {
            if (wawCounter > 0) {
                wawCounter--;
            } else if (keyPoller.isAnyKeyDown()) {
                wawCounter = WAITAFTERWIN;
                initializeLevel("a../map/test.map");
                initializeGraphics();
                initializePlayers();
            }
        } else if (player.isDead()) {
            if (wawCounter > 0) {
                wawCounter--;
            } else if (keyPoller.isAnyKeyDown()) {
                wawCounter = WAITAFTERWIN;
                initializeLevel("a../map/test.map");
                initializeGraphics();
                initializePlayers();
            }
        }
        if (paused || player.hasWon() || player.isDead()) {
            if (pauseAnimCounter > 0)
                pauseAnimCounter--;
            else {
                pauseAnimCounter = PAUSEANIM;
                pauseAnimFrame = (pauseAnimFrame + 1) % PAUSED.length;
            }
            return;
        }

        byte dirX = 0;
        byte dirY = 0;
        // überprüfe Tastatureingaben
        if (keyPoller.isKeyDown(KeyEvent.VK_LEFT)) {
            dirX--;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_RIGHT)) {
            dirX++;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_UP)) {
            dirY--;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_DOWN)) {
            dirY++;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_SPACE)) {
            player.putBomb();
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_M)) {
            soundManager.decreaseVolume();
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_P)) {
            soundManager.increaseVolume();
        }

        player.move(dirX, dirY);
        dirX = 0;
        dirY = 0;

        if (keyPoller.isKeyDown(KeyEvent.VK_A)) {
            dirX--;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_D)) {
            dirX++;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_W)) {
            dirY--;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_S)) {
            dirY++;
        }
        if (keyPoller.isKeyDown(KeyEvent.VK_CONTROL)) {
            player2.putBomb();
        }

        player2.move(dirX, dirY);
        player.update();
        player2.update();
        dirX = dirY = 0;
    }

    private void rescale() {
        width = getWidth();
        height = getHeight();
        dbg.dispose();
        gameG.dispose();
        bLevel.updateTileDimensions(width, (int) (height * 0.9));
        hud.updateDimension((int) (height * 0.1));
        player.updateDimensions();
        player2.updateDimensions();
        initializeGraphics();
    }
}
