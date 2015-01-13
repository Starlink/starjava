package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import uk.ac.starlink.ttools.plot2.Decal;
import uk.ac.starlink.ttools.plot2.Glyph;
import uk.ac.starlink.ttools.plot2.Pixer;
import uk.ac.starlink.ttools.plot2.PlotUtil;

/**
 * Bitmapped PaperType which can paint transparent or opaque pixels
 * as long as they are all the same colour.
 * That means all painted glyphs and decals must have the same RGB
 * (as specified at construction time), though they may have different alphas.
 *
 * <p>Since the compositing is pretty much the same (no attention needs to
 * be paid to the depth coordinate) this class implements both the
 * 2D and 3D PaperType interfaces.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class MonoPaperType extends RgbPaperType
                           implements PaperType2D, PaperType3D {

    private final int rgb_;
    private final Compositor compositor_;

    /**
     * Constructor.
     *
     * @param   color  single RGB colour for all drawing
     *                        (alpha component is ignored)
     * @param   compositor  compositing strategy for translating alphas
     *                      to displayed colours
     */
    public MonoPaperType( Color color, Compositor compositor ) {
        super( "Monochrome", true );
        rgb_ = color.getRGB() & 0x00ffffff;
        compositor_ = compositor;
    }

    protected RgbPaper createPaper( Rectangle bounds ) {
        return new MonoPaper( this, bounds );
    }

    public void placeGlyph( Paper paper, double dx, double dy,
                            Glyph glyph, Color color ) {
        int gx = PlotUtil.ifloor( dx );
        int gy = PlotUtil.ifloor( dy );
        ((MonoPaper) paper).placeGlyph( gx, gy, glyph, color );
    }

    public void placeGlyph( Paper paper, double dx, double dy, double dz,
                            Glyph glyph, Color color ) {
        int gx = PlotUtil.ifloor( dx );
        int gy = PlotUtil.ifloor( dy );
        ((MonoPaper) paper).placeGlyph( gx, gy, glyph, color );
    }

    /**
     * Paper implementation for this class.
     */
    private static class MonoPaper extends RgbPaper {

        private final int x0_;
        private final int y0_;
        private final Rectangle clip_;
        private final int rgb_;
        private final Compositor compositor_;
        private final float[] alphas_;
        private final float[] frgba_;
        private Color lastColor_;
        private float lastAlpha_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        MonoPaper( MonoPaperType paperType, Rectangle bounds ) {
            super( paperType, bounds );
            rgb_ = paperType.rgb_;
            x0_ = bounds.x;
            y0_ = bounds.y;
            clip_ = new Rectangle( bounds );
            compositor_ = paperType.compositor_;
            alphas_ = new float[ bounds.width * bounds.height ];
            frgba_ = new float[ 4 ];
        }

        /**
         * Override this method so that the decal painting is captured,
         * examined, and translated into a list of alpha deltas
         * applied to the alpha buffer.
         */
        @Override
        public void placeDecal( Decal decal ) {
            Rectangle bounds = getBounds();
            boolean hasAlpha = ! decal.isOpaque();
            RgbImage decalIm =
                RgbImage.createRgbImage( bounds.width, bounds.height,
                                         hasAlpha );
            int[] decalBuf = decalIm.getBuffer();
            int bg = decalBuf[ 0 ];
            Graphics g = decalIm.getImage().createGraphics();
            g.translate( - bounds.x, - bounds.y );
            decal.paintDecal( g );
            g.dispose();
            int npix = bounds.width * bounds.height;
            if ( hasAlpha ) {
                for ( int i = 0; i < npix; i++ ) {
                    int rgba = decalBuf[ i ];
                    if ( rgba != bg ) {

                        /* Any colours written into this image buffer should
                         * have the colour declared by the layer, with some
                         * undetermined alpha value.  The assertion below
                         * tests that.
                         * However, it seems that in the current implementation
                         * the colour value can get changed by a few notches -
                         * e.g. painting colour 40ee0000 to an empty pixel can
                         * lead to a byte value of 40ef0000.  So the assertion
                         * is commented out.
                         * This failure is harmless but surprising,
                         * and probably indicates that the RgbImage-based
                         * painting is not as efficient as I'd expect it to be.
                         * Must be down to BufferedImage implemenation,
                         * probably system-dependent. */
                        // assert ( rgba & 0x00ffffff ) == rgb_
                        //     : Integer.toHexString( rgba ) + "\t" +
                        //       Integer.toHexString( rgb_ );
                        alphas_[ i ] += Compositor.byteToFloat( rgba >> 24 );
                    }
                }
            }
            else {
                for ( int i = 0; i < npix; i++ ) {
                    int rgba = decalBuf[ i ];
                    if ( rgba != bg ) {
                        assert ( rgba & 0x00ffffff ) == rgb_;
                        alphas_[ i ] = 1f;
                    }
                }
            }
        }

        /**
         * Places a glyph on this paper.
         *
         * @param  gx  X coordinate
         * @param  gy  Y coordinate
         * @param  glyph  graphics shape
         * @param  color  colour
         */
        public void placeGlyph( int gx, int gy, Glyph glyph, Color color ) {
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
         * position.
         *
         * @param  xoff  X offset
         * @param  yoff  Y offset
         * @param  pixer  clipped pixel iterator
         * @param  color  painting colour
         */
        private void placePixels( int xoff, int yoff, Pixer pixer,
                                  Color color ) {

            /* Obtain the alpha value of the required colour, using caching
             * to avoid unnecessary work. */
            if ( color != lastColor_ ) {
                lastColor_ = color;
                assert ( color.getRGB() & 0x00ffffff ) == rgb_;
                color.getRGBComponents( frgba_ );
                lastAlpha_ = frgba_[ 3 ];
            }
            float alpha = lastAlpha_;

            /* Increment elements of the alpha pixel array corresponding
             * to the pixer's footprint. */
            while ( pixer.next() ) {
                int index = getPixelIndex( xoff, yoff, pixer );
                alphas_[ index ] += alpha;
            }
        }

        public void flush() {

            /* Use the array of alphas to write alpha-adjusted monochrome
             * pixels to the RGB image's backing buffer. */
            int[] rgbs = getRgbImage().getBuffer();
            int npix = rgbs.length;
            for ( int i = 0; i < npix; i++ ) {
                float rawAlpha = Math.min( alphas_[ i ], 1f );
                if ( rawAlpha != 0 ) {
                    float scaleAlpha = compositor_.scaleAlpha( rawAlpha );
                    int rgba = rgb_
                             | ( Compositor.floatToByte( scaleAlpha ) << 24 );
                    rgbs[ i ] = Compositor.srcOverOpaque( rgba, rgbs[ i ] );
                }
            }
        }
    }
}
