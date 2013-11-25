package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.Drawing;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot.PointArrayPixellator;
import uk.ac.starlink.ttools.plot2.Glyph;

/**
 * Glyph that represents lines between two points.
 *
 * <p>This is currently grossly inefficient and not thread safe.
 * The implementation will be improved.
 *
 * @author   Mark Taylor
 * @since    25 Nov 2013
 */
public class LineGlyph {

    /** Glyph that paints a single pixel at the origin. */
    public static Glyph POINT = createPointGlyph();

    /**
     * Private constructor prevents instantiation.
     */
    private LineGlyph() {
    }

    /**
     * Returns a glyph to draw a line from the origin to a given point x, y.
     *
     * @param   x  X destination coordinate
     * @param   y  Y destination coordinate
     * @return  line glyph
     */
    public static Glyph getLineGlyph( final int x, final int y ) {
        if ( x == 0 && y == 0 ) {
            return POINT;
        }
        else {
            return new Glyph() {
                public void paintGlyph( Graphics g ) {
                    g.drawLine( 0, 0, x, y );
                }
                public Pixellator getPixelOffsets( Rectangle clip ) {

                    /* This is grossly inefficient. */
                    uk.ac.starlink.ttools.plot.Drawing lineDrawing =
                        new uk.ac.starlink.ttools.plot.Drawing( clip );
                    lineDrawing.drawLine( 0, 0, x, y );
                    return lineDrawing;
                }
            };
        }
    }

    /**
     * Constructs a glyph that paints a single pixel at the origin.
     *
     * @return   point glyph
     */
    private static Glyph createPointGlyph() {
        final Pixellator pointPixer =
            new PointArrayPixellator( new Point[] { new Point( 0, 0 ) } );
        final Pixellator zeroPixer =
            new PointArrayPixellator( new Point[ 0 ] );
        return new Glyph() {
            public void paintGlyph( Graphics g ) {
                g.fillRect( 0, 0, 1, 1 );
            }
            public Pixellator getPixelOffsets( Rectangle clip ) {
                return clip.contains( 0, 0, 1, 1 ) ? pointPixer : zeroPixer;
            }
        };
    }
}
