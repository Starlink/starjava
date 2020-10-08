package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Pixer;

/**
 * Bitmapped 2D PaperType which can render any combination of coloured,
 * opaque and transparent pixels.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class CompositePaperType2D extends RgbPaperType2D {

    private final Compositor compositor_;

    /**
     * Constructor.
     *
     * @param   compositor  compositing strategy for combining
     *                      transparent pixels
     */
    public CompositePaperType2D( Compositor compositor ) {
        super( "PixelComposite", false );
        compositor_ = compositor;
    }

    protected RgbPaper2D createPaper2D( Rectangle bounds ) {
        return new CompositePaper( this, bounds );
    }

    /**
     * Paper implementation for this class.
     */
    private static class CompositePaper extends RgbPaper2D {

        private final float[] frgba_;
        private final Compositor.Buffer composBuf_;
        private Color lastColor_;
        private float lastAlpha_;
        private float lastR_;
        private float lastG_;
        private float lastB_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        CompositePaper( CompositePaperType2D paperType, Rectangle bounds ) {
            super( paperType, bounds );
            frgba_ = new float[ 4 ];
            composBuf_ = paperType.compositor_
                        .createBuffer( bounds.width * bounds.height );
        }

        public boolean canMerge() {
            return false;
        }

        public Paper createSheet() {
            throw new UnsupportedOperationException();
        }

        public void mergeSheet( Paper other ) {
            throw new UnsupportedOperationException();
        }

        /**
         * Override this method so that the decal painting is captured,
         * painted to a temporary buffer, and composited with the 
         * RGB and alpha buffers maintained by this paper.
         */
        @Override
        public void placeDecal( Decal decal ) {
            Rectangle bounds = getBounds();
            boolean hasAlpha = ! decal.isOpaque();

            /* Initialise the buffer with a fully transparent background colour
             * (alpha = 0).  For opaque decals this cannot be painted, 
             * and for transparent decals if it is painted it won't 
             * contribute to the output image.  We can then ignore any 
             * pixels of this colour when copying from the painted decal. */
            int bg = 0;
            RgbImage rgbim =
                RgbImage.createRgbImage( bounds.width, bounds.height,
                                         hasAlpha, bg );
            int[] rgbBuf = rgbim.getBuffer();
            Graphics g = rgbim.getImage().createGraphics();
            g.translate( - bounds.x, - bounds.y );
            decal.paintDecal( g );
            g.dispose();
            int npix = bounds.width * bounds.height;
            if ( hasAlpha ) {
                for ( int i = 0; i < npix; i++ ) {
                    int rgba = rgbBuf[ i ];
                    if ( rgba != bg ) {
                        composBuf_.addSample( i, rgba );
                    }
                }
            }
            else {
                for ( int i = 0; i < npix; i++ ) {
                    int rgb = rgbBuf[ i ];
                    if ( rgb != bg ) {
                        composBuf_.addSample( i, rgb, 1f );
                    }
                }
            }
        }

        protected void placePixels( int xoff, int yoff, Pixer pixer,
                                    Color color ) {

            /* Obtain the RGBA values of the required colour, using caching
             * to avoid unnecessary work. */
            if ( color != lastColor_ ) {
                lastColor_ = color;
                color.getRGBComponents( frgba_ );
                lastR_ = frgba_[ 0 ];
                lastG_ = frgba_[ 1 ];
                lastB_ = frgba_[ 2 ];
                lastAlpha_ = frgba_[ 3 ];
            }

            /* Add samples to the RGBA buffer corresponding to the pixer's
             * footprint. */
            while ( pixer.next() ) {
                composBuf_.addSample( getPixelIndex( xoff, yoff, pixer ),
                                      lastR_, lastG_, lastB_, lastAlpha_ );
            }
        }

        public void flush() {
            int[] rgbs = getRgbImage().getBuffer();
            int npix = rgbs.length;
            for ( int i = 0; i < npix; i++ ) {
                rgbs[ i ] = Compositor.srcOverOpaque( composBuf_.toRgbInt( i ),
                                                      rgbs[ i ] );
            } 
        }
    }
}
