package uk.ac.starlink.xdoc.fig;

import java.awt.Graphics;
import java.awt.Shape;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.jibble.epsgraphics.EpsGraphics2D;

/**
 * Graphics2D implementation which can write to Encapsulated PostScript.
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

    /**
     * Constructor with bounds.
     *
     * @param  title  title
     * @param  out  output stream - must be closed to complete plotting
     * @param  xmin  lower X bound for bounding box
     * @param  ymin  lower Y bound for bounding box
     * @param  xmax  upper X bound for bounding box
     * @param  ymax  upper Y bound for bounding box
     */
    public FixedEpsGraphics2D( String title, OutputStream out,
                               int xmin, int ymin, int xmax, int ymax )
            throws IOException {
        super( title, new TerminatedOutputStream( out ),
               xmin, ymin, xmax, ymax );
    }

    /**
     * Clone constructor.
     *
     * @param  g2  instance to copy
     */
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

   
    /**
     * Output stream which appends a newline character after it is closed.
     * This is required for EPS output so that some printers show the page
     * at the end of the input.  EpsGraphics2D does not append such a 
     * newline character.
     */
    private static class TerminatedOutputStream extends FilterOutputStream {

        /**
         * Constructor.
         *
         * @param  out  base output stream
         */
        public TerminatedOutputStream( OutputStream out ) {
            super( out );
        }

        public void close() throws IOException {
            write( '\n' );
            super.close();
        }
    }
}
