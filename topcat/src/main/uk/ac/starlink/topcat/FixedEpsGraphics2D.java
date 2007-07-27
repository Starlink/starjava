package uk.ac.starlink.topcat.plot;

import java.awt.Graphics;
import java.awt.Shape;
import java.io.IOException;
import java.io.OutputStream;
import org.jibble.epsgraphics.EpsGraphics2D;

/**
 * Graphics2D implementation which can write to Encapsulated PostScript
 *
 * <p>This is a very slight modification of 
 * <code>org.jibble.epsgraphics.EpsGraphics2D</code>
 * which addresses some (apparent?) bugs in that class.
 * The jibble library used to be GPL but is now released under a more
 * restrictive (and expensive) license, so applying a fix to the original
 * would be problematic.
 *
 * @author   Mark Taylor
 * @since    27 Jul 2007
 */
public class FixedEpsGraphics2D extends EpsGraphics2D {

    public FixedEpsGraphics2D( String title, OutputStream out,
                               int xmin, int ymin, int xmax, int ymax )
            throws IOException {
        super( title, out, xmin, xmax, ymin, ymax );
    }

    public FixedEpsGraphics2D( EpsGraphics2D g2 ) {
        super( g2 );
    }

    public void setClip( Shape clip ) {

        /* For some reason (something to do with gsave/grestore) setting the
         * clip seems to reset the colour to black under some circumstances.
         * This workaround appears to sort it out. */
        super.setClip( clip );
        setColor( getColor() );
    }

    public Graphics create() {
        return new FixedEpsGraphics2D( this );
    }
}
