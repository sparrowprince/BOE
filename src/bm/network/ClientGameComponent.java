package bm.network;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.image.VolatileImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;

import bm.Bomb;
import bm.gfx.FxLevel;
import bm.gfx.FxPlayer;
import bm.gfx.Texture;
import bm.input.NetworkKeyPoller;
import bm.io.LevelParser;
import bm.sfx.SoundManager;

public class ClientGameComponent extends JComponent implements Runnable {
    // protocol!
    public static final byte KEYWORD = (byte) 0xF0;
    // keywords are always stored inside the first four bits
    public static final byte PRESSED = 0 << 4;
    public static final byte RELEASED = 1 << 4;
    public static final byte PLAYER = 2 << 4;
    public static final byte BOMB = 3 << 4;
    public static final byte QUADDAMAGE = 1;
    public static final byte POWERUP = 4 << 4;
    public static final byte LEVEL = 5 << 4;
    public static final byte TILE = 6 << 4;
    public static final byte END = 7 << 4;
    public static final byte OK = (byte) (8 << 4);
    public static final byte PAUSE = (byte) (9 << 4);
    public static final byte UNPAUSE = 1;
    public static final byte MOVE = (byte) (10 << 4);

    public static final byte COLOR = 1;
    public static final byte LEFTMOVE = 0x01;
    public static final byte RIGHTMOVE = 0x02;
    public static final byte UPMOVE = 0x04;
    public static final byte DOWNMOVE = 0x08;
    // these are also indices for boolean arrays which indicate keypresses
    public static final byte LEFT = 0;
    public static final byte RIGHT = 1;
    public static final byte UP = 2;
    public static final byte DOWN = 3;
    public static final byte ATTACK = 4;

    private int width = 640;
    private int height = 480;
    private int gameWidth, gameHeight;

    private static final long SECOND = 1000000000; // eine Sekunde in
                                                   // Nanosekunden
    private static final long SLEEPTIME = SECOND / 60; // 60 FPS

    private static final double STEPSIZE = 0.075;

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
    private NetworkKeyPoller keyPoller;

    // fuer die Pause(ntaste)
    private boolean pausePressed = false;
    private boolean paused = false;
    private int pauseAnimCounter = PAUSEANIM;
    private int pauseAnimFrame;

    // stats after tab-press
    private boolean showStats = false;

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

    private static final Color STATBG = new Color(0x88000000);

    private byte myID;
    private boolean playerReceived;
    private List<FxPlayer> players;

    private Socket socket;
    private DataOutputStream toServer;
    private DataInputStream fromServer;

    private FxLevel bLevel;

    private int fps = 0, ups = 0; // wird durch den main-loop gesetzt
    private static final int[] FPSCOLORS = new int[] { 0xffff0000, 0xffffff00 };

    private int randomOffset;

    public ClientGameComponent(String playerName, int fstClr, int sndClr,
            String host, int port) {
        super();
        setFocusable(true);
        // soundManager.repeatSound(0);

        this.setSize(width, height);
        initializePlayers();

        try {
            socket = new Socket(host, 1337);
            socket.setTcpNoDelay(true);
            toServer = new DataOutputStream(new BufferedOutputStream(
                    socket.getOutputStream()));
            fromServer = new DataInputStream(new BufferedInputStream(
                    socket.getInputStream()));
            keyPoller = new NetworkKeyPoller(toServer);

            // receive ID
            myID = fromServer.readByte();
            System.out.println("Connected with ID " + myID);

            // send color
            toServer.writeInt(fstClr);
            toServer.writeInt(sndClr);

            // send your name!
            toServer.writeChars(playerName + '\0');

            playerReceived = false;
            new ServerInputThread();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
        addKeyListener(keyPoller);
    }

    private void initializeLevel(String pathToMap) {
        try {
            bLevel = new FxLevel(LevelParser.parseMap(pathToMap), width, height);
        } catch (Exception e) {
            bLevel = new FxLevel(35, 21, width, height);
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
        players = new ArrayList<FxPlayer>();
    }

    private void createNewPlayer(byte num, double posX, double posY,
            String name, int fstClr, int sndClr) {
        players.add(num, new FxPlayer(bLevel, name, fstClr, sndClr, posX, posY));
        if (num == myID)
            playerReceived = true;
    }

    /**
     * Double Buffering wobei ein Image erstellt wird und im Hintergrund das
     * nächste Bild gemalt wird.
     */
    public void paintBuffer() {
        if (bLevel == null || gameG == null || dbg == null)
            return;
        // paint the level
        bLevel.draw(gameG);

        // paint the player
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i) != null)
                players.get(i).draw(gameG);
        }

        dbg.setColor(Color.BLACK);
        dbg.fillRect(0, 0, width, height);
        dbg.drawImage(gameImage, (width - gameWidth) / 2 + randomOffset,
                (height - gameHeight) / 2 + randomOffset, null);

        // if (player.hasWon())
        // VICTORY[pauseAnimFrame].drawCentered(width, height, -1,
        // bLevel.getTileDim() * 4, dbg);
        // else if (player.isDead())
        // DEAD[pauseAnimFrame].drawCentered(width, height, -1,
        // bLevel.getTileDim() * 4, dbg);
        if (paused)
            PAUSED[pauseAnimFrame].drawCentered(width, height, -1,
                    bLevel.getTileDim(), dbg);

        if (showStats) {
            dbg.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.5f));
            dbg.setColor(STATBG);
            int dim = bLevel.getTileDim();
            int offset = dim * 3;
            int width = this.width - bLevel.getTileDim() * 6;
            int height = this.height - bLevel.getTileDim() * 6;
            dbg.fillRoundRect(offset, offset, width, height, dim, dim);
            offset += dim;
            for (int i = 0; i < players.size(); i++) {
                players.get(i).getNameTexture()
                        .draw(offset, offset + (i * dim * 3) / 2, -1, dim, dbg);
                if (players.get(i).isDead())
                    Texture.SKULL.draw(width - dim, offset + (i * dim * 3) / 2,
                            dim, dim, dbg);
                if (players.get(i).isChuck())
                    FxLevel.CHUCKNORRIS_TEXT[0].draw(width - dim * 2, offset
                            + (i * dim * 3) / 2, dim, dim, dbg);
                if (players.get(i).hasQuadDamage())
                    FxLevel.QD1.draw(width - dim * 3, offset + (i * dim * 3)
                            / 2, dim, dim, dbg);
            }
            dbg.setComposite(AlphaComposite.SrcOver);
        }

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
        if (bLevel == null || gameG == null || dbg == null)
            return;
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
        int fstClr = (int) (Math.random() * Integer.MAX_VALUE);
        int sndClr = (int) (Math.random() * Integer.MAX_VALUE);

        // Fenster erstellen
        JFrame frame = new JFrame();
        frame.setTitle("Bombeskalation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        String host = "sparrowprince.dyndns-remote.com";
        host = "localhost";

        ClientGameComponent bGui = new ClientGameComponent("tobi", fstClr,
                sndClr, host, 1337);

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
                updateTimeDelta += (System.nanoTime() - beforeUpdate);
                beforeUpdate = System.nanoTime();
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

            // if (updateTime < SLEEPTIME) {// warte ein wenig....
            // try {
            // Thread.sleep(1);
            // // Thread.sleep((SLEEPTIME - updateTime) / 1000000);
            // updateTime = System.nanoTime() - beforeUpdate;
            // } catch (InterruptedException e) {
            // System.err.println("Unexpedcted Interruption!");
            // e.printStackTrace();
            // }
            // }

        }

    }

    /**
     * Methode, welche (atm) 60x pro Sekunde aufgerufen wird und (nicht
     * sichtbare) Updates an der Spielumgebung durchführt. Hierzu gehören das
     * Einlesen von Tastatureingaben, Bewegen des Spielerobjekts, Herunterzählen
     * des BombenCounters etc.
     */
    public void bombermanUpdate() {
        if (bLevel == null)
            return;
        if (width != this.getWidth() || height != this.getHeight()) {
            rescale();
        }

        if (!playerReceived)
            return;

        if (paused) {
            if (pauseAnimCounter > 0)
                pauseAnimCounter--;
            else {
                pauseAnimCounter = PAUSEANIM;
                pauseAnimFrame = (pauseAnimFrame + 1) % PAUSED.length;
            }
            return;
        }

        bLevel.update();

        for (int i = 0; i < players.size(); i++)
            players.get(i).update();
        if (keyPoller.isKeyDown(KeyEvent.VK_F1))
            showStats = true;
        else showStats = false;
        if (keyPoller.isKeyDown(KeyEvent.VK_M))
            SoundManager.staticInstance.decreaseVolume();
        if (keyPoller.isKeyDown(KeyEvent.VK_P))
            SoundManager.staticInstance.increaseVolume();
        randomOffset = (int) (Math.random() * 2 * Bomb.explodeCount)
                - Bomb.explodeCount * 2;
    }

    private void rescale() {
        width = getWidth();
        height = getHeight();
        dbg.dispose();
        gameG.dispose();
        bLevel.updateTileDimensions(width, height);
        for (int i = 0; i < players.size(); i++)
            players.get(i).updateDimensions();
        initializeGraphics();
    }

    private class ServerInputThread implements Runnable {

        boolean running = true;

        public ServerInputThread() {
            new Thread(this).start();
        }

        @Override
        public void run() {
            byte keyword, rest, num;
            boolean left, right, up, down;
            byte dirX, dirY;
            while (running) {
                try {
                    keyword = (byte) (fromServer.readByte() & 0xFF);
                    rest = (byte) (keyword & ~KEYWORD);
                    keyword &= KEYWORD;
                    switch (keyword) {
                    case MOVE:
                        dirX = dirY = 0;
                        num = (byte) (fromServer.readByte() & 0xFF);
                        left = (rest & LEFTMOVE) != 0;
                        right = (rest & RIGHTMOVE) != 0;
                        up = (rest & UPMOVE) != 0;
                        down = (rest & DOWNMOVE) != 0;
                        if (left)
                            dirX--;
                        if (right)
                            dirX++;
                        if (up)
                            dirY--;
                        if (down)
                            dirY++;
                        players.get(num).move(dirX, dirY);
                        break;
                    case PLAYER:
                        int fstClr = 0,
                        sndClr = 0;
                        String name = "";
                        if (rest == COLOR) {
                            fstClr = fromServer.readInt();
                            sndClr = fromServer.readInt();
                            name = "";
                            char buffer;
                            while ((buffer = fromServer.readChar()) != '\0')
                                name += buffer;
                        }
                        num = (byte) (fromServer.readByte() & 0xFF);
                        double posX = fromServer.readDouble();
                        double posY = fromServer.readDouble();
                        if (num >= players.size() || players.get(num) == null)
                            createNewPlayer(num, posX, posY, name, fstClr,
                                    sndClr);
                        else players.get(num).setPos(posX, posY);
                        break;
                    case BOMB:
                        boolean quadDamage = (rest == QUADDAMAGE);
                        int bombX = fromServer.readInt();
                        int bombY = fromServer.readInt();
                        byte range = (byte) (fromServer.readByte() & 0xFF);
                        num = fromServer.readByte();
                        bLevel.putBomb(bombX, bombY, players.get(num), range,
                                quadDamage);
                        break;
                    case POWERUP:
                        bLevel.setNextPowerup(rest);
                        System.out.println("Powerup " + rest);
                        break;
                    case LEVEL:
                        int w = fromServer.readInt();
                        int h = fromServer.readInt();
                        short[][] tiles = new short[w][h];
                        System.out.println("w" + w + "h" + h);
                        for (int i = 0; i < w; i++)
                            for (int j = 0; j < h; j++)
                                tiles[i][j] = fromServer.readShort();
                        if (fromServer.readByte() != END)
                            System.exit(-1);
                        System.out.println("Received Level.");
                        bLevel = new FxLevel(tiles, width, height, false);
                        initializeGraphics();
                        break;
                    // case TILE: break;
                    // case END: break;
                    case OK:
                        break;
                    case PAUSE:
                        if (rest == UNPAUSE)
                            paused = false;
                        else paused = true;
                        break;
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        public void stopThread() {
            running = false;
        }
    }

}
