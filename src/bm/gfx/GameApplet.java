package bm.gfx;

import java.applet.Applet;
import java.awt.Graphics;



public class GameApplet extends Applet {

    @Override
    public void init() {
        GameComponent gameComponent = new GameComponent();
        add(gameComponent);
        setSize(gameComponent.getWidth(), gameComponent.getHeight());
        new Thread(gameComponent).start();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void paint(Graphics g) {}

}
