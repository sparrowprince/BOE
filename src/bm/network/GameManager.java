package bm.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import bm.Level;
import bm.Player;
import bm.io.LevelParser;

public class GameManager implements Runnable {
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

    private static final long SECOND = 1000000000; // one second
    private static final long SLEEPTIME = SECOND / 60; // 60 UPS

    private static final int EXACTPOSTIME = 300;
    private int exactPosCounter = EXACTPOSTIME;

    private static final double STEPSIZE = 0.075;

    private boolean running = true;

    private boolean pausePressed = false;
    private boolean paused = false;

    private static final int WAITAFTERWIN = 90;
    private int wawCounter = WAITAFTERWIN;

    private ServerSocket serverSocket;
    private AcceptThread acceptThread;
    private boolean accepting;

    private List<Player> players;
    private List<Socket> clients;
    private List<DataOutputStream> toClients;
    private List<DataInputStream> fromClients;
    private List<boolean[]> keyPressed;

    private List<Long> beforePing;
    private List<Long> ping;

    private Level bLevel;

    public GameManager() {
        initializeLevel("a../map/test.map");
        initializePlayers();
        try {
            initializeNetwork();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void initializeLevel(String pathToMap) {
        try {
            bLevel = new Level(LevelParser.parseMap(pathToMap), true);
        } catch (Exception e) {
            bLevel = new Level(35, 21);
        }
    }

    private void initializePlayers() {
        players = new ArrayList<Player>();
        keyPressed = new ArrayList<boolean[]>();
    }

    private void initializeNetwork() throws IOException {
        serverSocket = new ServerSocket(1337);
        accepting = false;
        clients = new ArrayList<Socket>();
        toClients = new ArrayList<DataOutputStream>();
        fromClients = new ArrayList<DataInputStream>();
        beforePing = new ArrayList<Long>();
        ping = new ArrayList<Long>();
        acceptThread = new AcceptThread();
        new Thread(acceptThread).start();
    }

    public static void main(String[] args) {
        GameManager gameManager = new GameManager();

        new Thread(gameManager).start();
    }

    /**
     * Startet den Spieleloop.
     */
    public void run() {
        long beforeUpdate, updateTime, updateTimeDelta;

        // Vars zum FPS zaehlen
        long upsCounter;
        int ups = 0;
        upsCounter = System.nanoTime();// messen spaeter, ob eine Sek. vergangen
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

            if (System.nanoTime() - upsCounter >= SECOND) {
                // System.out.println(ups + "UPS");
                ups = 0; // lokaler Counter auf 0
                upsCounter = System.nanoTime();
            }

            updateTime = System.nanoTime() - beforeUpdate;

            if (updateTime < SLEEPTIME) {// warte ein wenig....
                try {
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

        // if (player.hasWon()) {
        // if (wawCounter > 0) {
        // wawCounter--;
        // } else if (keyPoller.isAnyKeyDown()) {
        // wawCounter = WAITAFTERWIN;
        // initializeLevel("a../map/test.map");
        // initializePlayers();
        // }
        // } else if (player.isDead()) {
        // if (wawCounter > 0) {
        // wawCounter--;
        // } else if (keyPoller.isAnyKeyDown()) {
        // wawCounter = WAITAFTERWIN;
        // initializeLevel("a../map/test.map");
        // initializePlayers();
        // }
        // }

        if (accepting)
            return;

        bLevel.update();
        byte[] dir = { 0, 0 };
        // check for keyboard input from clients & update players
        for (byte i = 0; i < keyPressed.size(); i++) {
            if (keyPressed.get(i)[LEFT]) {
                dir[0]--;
            }
            if (keyPressed.get(i)[RIGHT]) {
                dir[0]++;
            }
            if (keyPressed.get(i)[UP]) {
                dir[1]--;
            }
            if (keyPressed.get(i)[DOWN]) {
                dir[1]++;
            }
            dir = players.get(i).move(dir[0], dir[1]);
            players.get(i).update();
            sendMovement(dir[0], dir[1], i);
            if (keyPressed.get(i)[ATTACK]) {
                if (players.get(i).putBomb())
                    sendBomb(i);
            }
            dir = new byte[] { 0, 0 };
        }
        if (exactPosCounter > 0) {
            exactPosCounter--;
        } else {
            exactPosCounter = EXACTPOSTIME;
            sendPlayers();
        }
        sendPowerups();
        flushPackets();
    }

    private void flushPackets() {
        try {
            for (int i = 0; i < toClients.size(); i++) {
                toClients.get(i).flush();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendBomb(byte num) {
        Player player = players.get(num);
        int posX = (int) (player.getPosX() + 0.5);
        int posY = (int) (player.getPosY() + 0.5);
        byte range = player.getRange();
        try {
            for (int i = 0; i < toClients.size(); i++) {
                if (player.hasQuadDamage())
                    toClients.get(i).writeByte(BOMB | QUADDAMAGE);
                else toClients.get(i).writeByte(BOMB);
                toClients.get(i).writeInt(posX);
                toClients.get(i).writeInt(posY);
                toClients.get(i).writeByte(range);
                toClients.get(i).writeByte(num);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendMovement(double deltaX, double deltaY, byte num) {
        byte move = MOVE;
        if (deltaX < 0)
            move |= LEFTMOVE;
        else if (deltaX > 0)
            move |= RIGHTMOVE;
        if (deltaY < 0)
            move |= UPMOVE;
        else if (deltaY > 0)
            move |= DOWNMOVE;
        try {
            for (int i = 0; i < toClients.size(); i++) {
                toClients.get(i).writeByte(move);
                toClients.get(i).writeByte(num);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendPlayers(byte num, boolean color) {
        try {
            for (int i = 0; i < players.size(); i++) {
                if (color) {
                    toClients.get(num).writeByte(PLAYER | COLOR);
                    toClients.get(num).writeInt(players.get(i).getFstClr());
                    toClients.get(num).writeInt(players.get(i).getSndClr());
                    toClients.get(num).writeChars(
                            players.get(i).getName() + '\0');
                } else toClients.get(num).writeByte(PLAYER);
                toClients.get(num).writeByte(i);
                toClients.get(num).writeDouble(players.get(i).getPosX());
                toClients.get(num).writeDouble(players.get(i).getPosY());
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendPlayers() {
        for (int i = 0; i < players.size(); i++)
            sendPlayers((byte) i, true);
    }

    private void sendPowerups() {
        try {
            Queue<Short> ups = bLevel.getNewPowerups();
            while (!ups.isEmpty()) {
                byte tile = (byte) ups.poll().shortValue();
                for (int i = 0; i < toClients.size(); i++)
                    toClients.get(i).writeByte(POWERUP | tile);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendPause(boolean pause) {
        try {
            for (int i = 0; i < toClients.size(); i++) {
                if (!pause)
                    toClients.get(i).writeByte(PAUSE | UNPAUSE);
                else toClients.get(i).writeByte(PAUSE);
                toClients.get(i).flush();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void sendLevel(byte num) {
        try {
            toClients.get(num).writeByte(LEVEL);
            toClients.get(num).writeInt(bLevel.getWidth());
            toClients.get(num).writeInt(bLevel.getHeight());
            for (int i = 0; i < bLevel.getWidth(); i++)
                for (int j = 0; j < bLevel.getHeight(); j++)
                    toClients.get(num).writeShort(bLevel.getTile(i, j));
            toClients.get(num).writeByte(END);
            Queue<Short> ups = bLevel.getNewPowerups();
            while (!ups.isEmpty()) {
                byte tile = (byte) ups.poll().shortValue();
                toClients.get(num).writeByte(POWERUP | tile);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private class AcceptThread implements Runnable {

        int num;
        boolean running = true;

        @Override
        public void run() {
            while (running) {
                num = players.size();
                // FIXME
                if (num > 127)
                    break;
                try {
                    acceptNewClient(serverSocket.accept());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        private void acceptNewClient(Socket client) throws IOException {
            accepting = true;
            sendPause(true);

            client.setTcpNoDelay(true);
            clients.add(num, client);
            DataOutputStream toClient = new DataOutputStream(
                    new BufferedOutputStream(client.getOutputStream()));
            DataInputStream fromClient = new DataInputStream(
                    new BufferedInputStream(client.getInputStream()));
            toClients.add(num, toClient);
            fromClients.add(num, fromClient);

            beforePing.add(num, (long) -1);
            ping.add(num, (long) -1);

            toClient.writeByte(num);
            toClient.flush();

            int fstClr = fromClient.readInt() | 0xFF000000;
            int sndClr = fromClient.readInt() | 0xFF000000;

            String name = "";
            char buffer;
            while ((buffer = fromClient.readChar()) != '\0')
                name += buffer;

            int[] spawnPoint = bLevel.getSpawnPoint(num);
            players.add(num, new Player(bLevel, name, fstClr, sndClr,
                    spawnPoint[0], spawnPoint[1]));
            keyPressed.add(num, new boolean[5]);

            sendLevel((byte) num);
            sendPlayers();
            new ClientInputThread(num);

            sendPause(false);
            accepting = false;
        }

        public void stopThread() {
            running = false;
        }

    }

    private class ClientInputThread implements Runnable {

        byte num;
        boolean running = true;

        public ClientInputThread(int num) {
            this.num = (byte) num;
            new Thread(this).start();
        }

        @Override
        public void run() {
            byte keyword, rest;
            while (running) {
                try {
                    toClients.get(num).flush();
                    keyword = fromClients.get(num).readByte();
                    rest = (byte) (keyword & ~KEYWORD);
                    keyword &= KEYWORD;
                    switch (keyword) {
                    case PRESSED:
                        if (rest >= LEFT && rest <= ATTACK) {
                            keyPressed.get(num)[rest] = true;
                        }
                        break;
                    case RELEASED:
                        if (rest >= LEFT && rest <= ATTACK) {
                            keyPressed.get(num)[rest] = false;
                        }
                        break;
                    // case PLAYER: break;
                    // case BOMB: break;
                    // case POWERUP: break;
                    // case LEVEL: break;
                    // case TILE: break;
                    // case END: break;
                    case OK:
                        break;
                    case PAUSE:
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