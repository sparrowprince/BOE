package bm.gfx;

import java.awt.Graphics2D;

/**
 * A simple Interface to ensure Objects implementing it are actually drawable.
 * 
 * @author tobi
 * 
 */
public interface Drawable {
    public void draw(Graphics2D g);
}
