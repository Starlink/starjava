package uk.ac.starlink.ttools.plot2;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Captioner implementation that writes no text.
 * The non-existent text takes up no space.
 *
 * <p>This is a singleton class, see the static {@link #INSTANCE} member.
 *
 * @author   Mark Taylor
 * @since    13 Jan 2016
 */
public class NullCaptioner implements Captioner {

    /** Sole instance. */
    public static final NullCaptioner INSTANCE = new NullCaptioner();

    /** Private constructor prevents external instantiation. */
    private NullCaptioner() {
    }

    public void drawCaption( String label, Graphics g ) {
    }

    public Rectangle getCaptionBounds( String label ) {
        return new Rectangle( 0, 0, 0, 0 );
    }

    public int getPad() {
        return 0;
    }
}
