package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot.ErrorRenderer;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Utility class for generating XYShape objects.
 * Current implementation does not dispense very efficient glyphs.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2015
 */
public class XYShapes {

    private static final XYShape[] XYSHAPES = createXYShapes();

    /**
     * Private constructor prevents instantiation.
     */
    private XYShapes() {
    }

    /**
     * Returns an array of XY shapes suitable for plotting markers
     * with variable X and Y extents.
     *
     * @return  XY shapes
     */
    public static XYShape[] getXYShapes() {
        return XYSHAPES.clone();
    }

    /**
     * Creates an array of XY shapes suitable for plotting markers
     * with variable X and Y extents.
     *
     * @return  XY shapes
     */
    private static XYShape[] createXYShapes() {
        ErrorRenderer[] renderers = ErrorRenderer.getOptionsXYSize();
        int n = renderers.length;
        XYShape[] shapes = new XYShape[ n ];
        for ( int i = 0; i < n; i++ ) {
            shapes[ i ] = createErrorEllipseShape( renderers[ i ] );
        }
        return shapes;
    }

    /**
     * Constructs a shape based on an ellipse-type ErrorRenderer.
     * This is not the most efficient way to do it.
     *
     * @param   renderer  renderer expecting 4 input offsets
     * @return  shape
     */
    private static XYShape
            createErrorEllipseShape( final ErrorRenderer renderer ) {
        return new XYShape( renderer.getName() ) {
            public Glyph createGlyph( short x, short y ) {
                if ( x == 0 && y == 0 ) {
                    return XYShape.POINT;
                }
                else {
                    final int[] xoffs = new int[] { x, -x, 0, 0 };
                    final int[] yoffs = new int[] { 0, 0, -y, y };
                    return new Glyph() {
                        public void paintGlyph( Graphics g ) {
                            renderer.drawErrors( g, 0, 0, xoffs, yoffs );
                        }
                        public Pixer createPixer( Rectangle clip ) {
                            final Pixellator pixellator =
                                renderer.getPixels( clip, 0, 0, xoffs, yoffs );
                            pixellator.start();
                            return new Pixer() {
                                public boolean next() {
                                    return pixellator.next();
                                }
                                public int getX() {
                                    return pixellator.getX();
                                }
                                public int getY() {
                                    return pixellator.getY();
                                }
                            };
                        }
                    };
                }
            }
        };
    }
}
