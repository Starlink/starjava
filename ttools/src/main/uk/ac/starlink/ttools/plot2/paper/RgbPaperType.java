package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.Icon;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Drawing;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Abstract bitmapped PaperType which uses an RgbImage to store graphics.
 * Abstract subclasses are provided for different geometries,
 * and concrete implementations have to provide their own compositing
 * which operates on the supplied RgbImage, using either the BufferedImage
 * or the backing buffer.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public abstract class RgbPaperType implements PaperType {

    private final String name_;
    private final boolean upLayer_;

    /**
     * Constructor.
     *
     * @param  name  paper type name
     * @param  upLayer  true to render layers in ascending order,
     *                  false to do them in descending order
     */
    protected RgbPaperType( String name, boolean upLayer ) {
        name_ = name;
        upLayer_ = upLayer;
    }

    /**
     * Returns true.
     */
    public boolean isBitmap() {
        return true;
    }

    /**
     * Simply calls the {@link RgbPaper#placeDecal} method.
     */
    public void placeDecal( Paper paper, Decal decal ) {
        ((RgbPaper) paper).placeDecal( decal );
    }

    public Icon createDataIcon( Surface surface, Drawing[] drawings,
                                Object[] plans, DataStore dataStore, 
                                boolean requireCached ) {

        /* Create paper. */
        final Rectangle bounds = surface.getPlotBounds();
        final RgbPaper paper = createPaper( bounds );

        /* Paint background. */
        surface.paintBackground( paper.graphics_ );

        /* Draw each of the drawings on the paper in turn. */
        int nlayer = drawings.length;
        for ( int il = 0; il < nlayer; il++ ) {
            int jl = upLayer_ ? il : nlayer - 1 - il;
            drawings[ jl ].paintData( plans[ jl ], paper, dataStore );
        }
        paper.flush();

        /* Return an icon based on the drawn-on paper. */
        return new Icon() {
            public int getIconWidth() {
                return bounds.x + bounds.width;
            }
            public int getIconHeight() {
                return bounds.y + bounds.height;
            }
            public void paintIcon( Component c, Graphics g, int x, int y ) {
                g.drawImage( paper.getRgbImage().getImage(), x, y, null );
            }
        };
    }

    public String toString() {
        return name_;
    }

    /**
     * Creates a paper object for given bounds.
     *
     * @param   bounds  plot bounds
     * @return  new paper instance
     */
    protected abstract RgbPaper createPaper( Rectangle bounds );

    /**
     * Paper for use by this type.
     */
    protected static abstract class RgbPaper implements Paper {

        private final PaperType paperType_;
        private final Rectangle bounds_;
        private final int xpix_;
        private final int ypix_;
        private final RgbImage rgbImage_;
        private final Graphics graphics_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        public RgbPaper( PaperType paperType, Rectangle bounds ) {
            paperType_ = paperType;
            bounds_ = new Rectangle( bounds );
            xpix_ = bounds.width;
            ypix_ = bounds.height;
            rgbImage_ =
                RgbImage.createRgbImage( bounds.width, bounds.height, false );
            graphics_ = rgbImage_.getImage().createGraphics();
            graphics_.setColor( Color.BLACK );
            graphics_.translate( -bounds.x, -bounds.y );
        }

        public PaperType getPaperType() {
            return paperType_;
        }

        /**
         * Returns the RGB image that stores the state of this paper.
         *
         * @return  rgb image
         */
        public RgbImage getRgbImage() {
            return rgbImage_;
        }

        /**
         * Returns the plot bounds.
         *
         * @return  plot bounds
         */
        public Rectangle getBounds() {
            return bounds_;
        }

        /**
         * Does the work for placing a decal.
         * Invoked by {@link RgbPaperType#placeDecal}.
         *
         * @param   decal  graphic to paint
         */
        public void placeDecal( Decal decal ) {
            decal.paintDecal( graphics_ );
        }

        /**
         * Returns the index into the RGB image buffer corresponding to
         * the current state of a pixellator object and an X/Y offset.
         *
         * @param  xoff  offset in X
         * @param  yoff  offset in Y
         * @param  pixer   pixellator
         * @return  buffer offset for current position of pixer
         */
        protected int getPixelIndex( int xoff, int yoff, Pixellator pixer ) {
            int x = xoff + pixer.getX();
            int y = yoff + pixer.getY();
            assert x >= 0 && x < xpix_ && y >= 0 && y < ypix_;
            return x + xpix_ * y;
        }

        /**
         * Called after all drawings have been drawn.
         */
        public abstract void flush();
    }
}
