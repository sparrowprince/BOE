package bm.input;

import java.awt.event.KeyEvent;
import java.io.DataOutputStream;
import java.io.IOException;

public class NetworkKeyPoller extends KeyPoller {
    // protocol!
    public static final byte KEYWORD = (byte) 0xF0;
    // keywords are always stored inside the first four bits
    public static final byte PRESSED = 0 << 4;
    public static final byte RELEASED = 1 << 4;
    public static final byte PLAYER = 2 << 4;
    public static final byte BOMB = 3 << 4;
    public static final byte POWERUP = 4 << 4;
    public static final byte LEVEL = 5 << 4;
    public static final byte TILE = 6 << 4;
    public static final byte END = 7 << 4;
    public static final byte OK = (byte) (8 << 4);
    public static final byte PAUSE = (byte) (9 << 4);
    // these are also indices for boolean arrays which indicate keypresses
    public static final byte LEFT = 0;
    public static final byte RIGHT = 1;
    public static final byte UP = 2;
    public static final byte DOWN = 3;
    public static final byte ATTACK = 4;

    private DataOutputStream toServer;

    public NetworkKeyPoller(DataOutputStream toServer) {
        super();
        this.toServer = toServer;
    }

    @Override
    protected void keyPressed(int keyCode) {
        super.keyPressed(keyCode);
        switch (keyCode) {
        case KeyEvent.VK_LEFT:
            sendKeyPress(LEFT);
            break;
        case KeyEvent.VK_RIGHT:
            sendKeyPress(RIGHT);
            break;
        case KeyEvent.VK_UP:
            sendKeyPress(UP);
            break;
        case KeyEvent.VK_DOWN:
            sendKeyPress(DOWN);
            break;
        case KeyEvent.VK_SPACE:
            sendKeyPress(ATTACK);
            break;
        }
    }

    private void sendKeyPress(byte keyCode) {
        try {
            toServer.writeByte(PRESSED | keyCode);
            toServer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void keyReleased(int keyCode) {
        super.keyReleased(keyCode);
        switch (keyCode) {
        case KeyEvent.VK_LEFT:
            sendKeyRelease(LEFT);
            break;
        case KeyEvent.VK_RIGHT:
            sendKeyRelease(RIGHT);
            break;
        case KeyEvent.VK_UP:
            sendKeyRelease(UP);
            break;
        case KeyEvent.VK_DOWN:
            sendKeyRelease(DOWN);
            break;
        case KeyEvent.VK_SPACE:
            sendKeyRelease(ATTACK);
            break;
        }
    }

    private void sendKeyRelease(byte keyCode) {
        try {
            toServer.writeByte(RELEASED | keyCode);
            toServer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
