package uk.ac.starlink.topcat.plot;

import java.awt.Rectangle;

/**
 * Pixellator with no points.
 *
 * @author   Mark Taylor
 * @since    21 Aug 2007
 */
public class EmptyPixellator implements Pixellator {

    private static final EmptyPixellator INSTANCE = new EmptyPixellator();

    public Rectangle getBounds() {
        return new Rectangle();
    }

    public int getX() {
        throw new IllegalStateException();
    }

    public int getY() {
        throw new IllegalStateException();
    }

    public void start() {
    }

    public boolean next() {
        return false;
    }

    /**
     * Returns an instance of this class.
     *
     * @return  pixellator
     */
    public static EmptyPixellator getInstance() {
        return INSTANCE;
    }
}
