package uk.ac.starlink.ttools.plot2.layer;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * XYShape for drawing thick lines that start at the origin and terminate
 * at the given X,Y displacement.
 * Acquire a lazily constructed instance from the
 * {@link #getInstance} method.
 *
 * @author   Mark Taylor
 * @since    4 Oct 2021
 */
public class FatLineXYShape extends XYShape {

    private final PixerFactory kernel_;
    private final StrokeKit strokeKit_;
    private static XYShape[] instances_ = new XYShape[ 5 ];
    static {
        instances_[ 0 ] = LineXYShape.INSTANCE;
    }

    /**
     * Constructor.
     *
     * @param  nthick  thickness index &gt;=0
     */
    protected FatLineXYShape( int nthick ) {
        this( "Line" + nthick,
              LineGlyph.createThickKernel( nthick ),
              LineGlyph.createThickStrokeKit( nthick ) );
    }

    /**
     * Constructs a fat line shape given a kernel and stroke kit.
     *
     * @param  name  shape name
     * @param  kernel  line smoothing kernel
     * @param  strokeKit   stroke for drawing line
     */
    private FatLineXYShape( String name,
                            PixerFactory kernel, StrokeKit strokeKit ) {
        super( name, 16, createPointGlyph( kernel, strokeKit ) );
        kernel_ = kernel;
        strokeKit_ = strokeKit;
    }

    protected Glyph createGlyph( short sx, short sy ) {
        return new LineGlyph() {
            public Rectangle getPixelBounds() {
                int x = sx >= 0 ? 0 : sx;
                int w = sx >= 0 ? sx + 1 : 1 - sx;
                int y = sy >= 0 ? 0 : sy;
                int h = sy >= 0 ? sy + 1 : 1 - sy;
                return new Rectangle( x, y, w, h );
            }
            public void drawShape( PixelDrawing drawing ) {
                drawing.drawLine( 0, 0, sx, sy );
            }
            public void paintGlyph( Graphics g, StrokeKit strokeKit ) {
                Graphics2D g2 = (Graphics2D) g;
                Stroke stroke0 = g2.getStroke();
                g2.setStroke( strokeKit.getRound() );
                g2.drawLine( 0, 0, sx, sy );
                g2.setStroke( stroke0 );
            }
        }.toThicker( kernel_, strokeKit_ );
    }

    /**
     * Returns an instance of this class for drawing lines of a given
     * thickness.  A lazily created cached instance may be returned.
     *
     * @param  nthick  line thickness &gt;=0
     * @return  instance
     */
    public static XYShape getInstance( int nthick ) {
        if ( nthick < instances_.length ) {
            XYShape instance = instances_[ nthick ];
            if ( instance == null ) {
                instance = new FatLineXYShape( nthick );
                instances_[ nthick ] = instance;
            }
            return instance;
        }
        else {
            return new FatLineXYShape( nthick );
        }
    }

    /**
     * Returns the special case instance for drawing a zero-length line.
     * This is more efficient than the general instances, and may be used
     * frequently.
     *
     * @param  kernel  smoothing kernel
     * @param  strokeKit  line strokes
     */
    static Glyph createPointGlyph( PixerFactory kernel, StrokeKit strokeKit ) {
        final Stroke stroke = strokeKit.getRound();
        return new Glyph() {
            public Pixer createPixer( Rectangle clip ) {
                return Pixers.createClippedPixer( kernel, clip );
            }
            public void paintGlyph( Graphics g ) {
                Graphics2D g2 = (Graphics2D) g;
                Stroke stroke0 = g2.getStroke();
                g2.setStroke( stroke );
                g2.drawLine( 0, 0, 0, 0 );
                g2.setStroke( stroke0 );
            }
        };
    }
}
