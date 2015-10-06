package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Abstract RgbPaperType subclass for 2-dimensional plots.
 *
 * <p>Note that the default {@link #placeDecal} implementation
 * paints directly to the RGB Image's graphics context.
 * so that in the presence of decals, using the it is not OK
 * to wait until flush time and then render everything to the image,
 * unless you suitably override <code>placeDecal</code> as well.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public abstract class RgbPaperType2D extends RgbPaperType
                                     implements PaperType2D {

    /**
     * Constructor.
     *
     * @param  name  paper type name
     * @param  upLayer  true to render layers in ascending order,
     *                  false to do them in descending order
     */
    protected RgbPaperType2D( String name, boolean upLayer ) {
        super( name, upLayer );
    }

    public void placeGlyph( Paper paper, double dx, double dy, Glyph glyph,
                            Color color ) {
        int gx = PlotUtil.ifloor( dx );
        int gy = PlotUtil.ifloor( dy );
        ((RgbPaper2D) paper).placeGlyph( gx, gy, glyph, color );
    }

    protected RgbPaper createPaper( Rectangle bounds ) {
        return createPaper2D( bounds );
    }

    /**
     * Creates a 2D paper object for given bounds.
     *
     * @param  bounds  plot bounds
     * @return  new 2d paper instance
     */
    protected abstract RgbPaper2D createPaper2D( Rectangle bounds );

    /**
     * Paper for use by this type.
     */
    protected static abstract class RgbPaper2D extends RgbPaper {
        private final int x0_;
        private final int y0_;
        private final Rectangle clip_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        public RgbPaper2D( PaperType paperType, Rectangle bounds ) {
            super( paperType, bounds );
            x0_ = bounds.x;
            y0_ = bounds.y;
            clip_ = new Rectangle( bounds );
        }

        /**
         * Places a glyph on this paper.
         *
         * @param  gx  X coordinate
         * @param  gy  Y coordinate
         * @param  glyph  graphics shape
         * @param  color  colour
         */
        private void placeGlyph( int gx, int gy, Glyph glyph, Color color ) {
            clip_.x -= gx;
            clip_.y -= gy;
            Pixer pixer = glyph.createPixer( clip_ );
            if ( pixer != null ) {
                placePixels( gx - x0_, gy - y0_, pixer, color );
            }
            clip_.x += gx;
            clip_.y += gy;
        }

        /**
         * Paints the pixels of a pixel iterator in a given colour at a given
         * position.  Subclasses implement this method to perform the
         * actual pixel compositing.  The supplied pixer will already
         * have been clipped, so implementations don't need to worry about
         * checking the positions are within the bounds of this paper.
         * Implementations can (and usually should) use the
         * {@link RgbPaperType.RgbPaper#getPixelIndex} method
         * to address the pixels of the RgbImage buffer.
         *
         * @param  xoff  X offset
         * @param  yoff  Y offset
         * @param  pixer  clipped pixel iterator, not null
         * @param  color  painting colour
         */
        protected abstract void placePixels( int xoff, int yoff,
                                             Pixer pixer, Color color );
    }
}
