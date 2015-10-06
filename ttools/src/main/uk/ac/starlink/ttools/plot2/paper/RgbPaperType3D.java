package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Abstract RgbPaperType subclass for 3-dimensional plots.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public abstract class RgbPaperType3D extends RgbPaperType
                                     implements PaperType3D {

    /**
     * Constructor.
     *
     * @param  name  paper type name
     * @param  upLayer  true to render layers in ascending order,
     *                  false to do them in descending order
     */
    protected RgbPaperType3D( String name, boolean upLayer ) {
        super( name, upLayer );
    }

    public void placeGlyph( Paper paper, double dx, double dy, double dz,
                            Glyph glyph, Color color ) {
        int gx = PlotUtil.ifloor( dx );
        int gy = PlotUtil.ifloor( dy );
        ((RgbPaper3D) paper).placeGlyph( gx, gy, dz, glyph, color );
    }

    protected RgbPaper createPaper( Rectangle bounds ) {
        return createPaper3D( bounds );
    }

    /**
     * Creates a 3D paper object for given bounds.
     *
     * @param  bounds  plot bounds
     * @return  new 3d paper instance
     */
    protected abstract RgbPaper3D createPaper3D( Rectangle bounds );

    /**
     * Paper for use by this type.
     */
    protected static abstract class RgbPaper3D extends RgbPaper {
        private final int x0_;
        private final int y0_;
        private final Rectangle clip_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        public RgbPaper3D( PaperType paperType, Rectangle bounds ) {
            super( paperType, bounds );
            x0_ = bounds.x;
            y0_ = bounds.y;
            clip_ = new Rectangle( bounds );
        }

        /**
         * Places a glyph on this paper.
         *
         * @param  gx  X pixel coordinate
         * @param  gy  Y pixel coordinate
         * @param  dz  depth coordinate, lower value means closer to viewer
         * @param  glyph  graphics shape
         * @param  color  colour
         */
        private void placeGlyph( int gx, int gy, double dz, Glyph glyph,
                                 Color color ) {
            clip_.x -= gx;
            clip_.y -= gy;
            Pixer pixer = glyph.createPixer( clip_ );
            if ( pixer != null ) {
                placePixels( gx - x0_, gy - y0_, dz, pixer, color );
            }
            clip_.x += gx;
            clip_.y += gy;
        }

        /**
         * Paints the pixels of a pixel iterator in a given colour at a given
         * 3d position.  Subclasses implement this method to perform the
         * actual pixel compositing.  The supplied pixer will already
         * have been clipped, so implementations don't need to worry about
         * checking the positions are within the bounds of this paper.
         * Implementations can (and usually should) use the
         * {@link RgbPaperType.RgbPaper#getPixelIndex} method
         * to address pixels of the RgbImage buffer.
         *
         * @param  xoff  X graphics offset
         * @param  yoff  Y graphics offset
         * @param  dz  depth coordinate, lower value means closer to viewer
         * @param  pixer  clipped pixel iterator, not null
         * @param  color  painting colour
         */
        protected abstract void placePixels( int xoff, int yoff, double dz,
                                             Pixer pixer, Color color );
    }
}
