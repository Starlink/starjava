package uk.ac.starlink.ttools.plot2.paper;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Arrays;
import uk.ac.starlink.ttools.plot.Pixellator;
import uk.ac.starlink.util.DoubleList;

/**
 * Bitmapped 3d PaperType which can render any combination of coloured,
 * opaque and transparent pixels.
 *
 * <p>At each XY position it stores a list of pixels which have been
 * plotted there.
 * Each element in the list contains Z-coordinate, RGB and alpha values,
 * packed efficiently.  When all the glyphs have been painted, the list
 * at each XY position is examined, the pixels in that list are
 * sorted by Z-coordinate, and compositing takes place from front to back.
 *
 * @author   Mark Taylor
 * @since    14 Feb 2013
 */
public class PixelStackPaperType3D extends RgbPaperType3D {

    private final Compositor compositor_;
    private final FloatPacker alphaPacker_;

    /**
     * Constructor.
     *
     * @param   compositor  compositing strategy for combining
     *                      transparent pixels
     * @param  minAlpha  the smallest alpha value that can be represented
     *                   for glyphs
     */
    public PixelStackPaperType3D( Compositor compositor, float minAlpha ) {
        super( "PixelStack", true );
        compositor_ = compositor;

        /* Fix the way that alpha values will be packed into bytes depending
         * on the smallest values required.  Squashing alpha values into a
         * single byte is good for efficiency, but the number of bits available
         * is a bit on the small side, so we have to be clever. */
        alphaPacker_ = minAlpha < 1f / 255f
                     ? createLogFloatPacker( minAlpha )
                     : createLinearFloatPacker();
    }

    protected RgbPaper3D createPaper3D( Rectangle bounds ) {
        return new PixelStackPaper( this, bounds );
    }

    /**
     * Returns a float packer that uses linear scaling over the data range.
     *
     * @return  linear float packer
     */
    private static FloatPacker createLinearFloatPacker() {
        return new FloatPacker() {
            public int floatToByte( float fv ) {
                return (int) Math.ceil( fv * 255f ); // don't round to zero
            }
            public float byteToFloat( int iv ) {
                return Compositor.byteToFloat( iv );
            }
        };
    }

    /**
     * Returns a float packer that stores a floating point value using
     * a byte storing its approximate scaled negative logarithm.
     * This allows smaller values to be stored than using a linear scale.
     * The scaling factor is determined by the smallest value that needs
     * to be stored (corresponds to the difference between the value
     * encoded as 0 and the value encoded as 1).
     *
     * @param   minAlpha  smallest non-zero floating point value that needs
     *                    to be encoded
     * @return  logarithmic float packer
     */
    private static FloatPacker createLogFloatPacker( final float minAlpha ) {
        final double scale = Math.log( minAlpha ) / 255.;
        final double scale1 = 1. / scale;
        return new FloatPacker() {
            public int floatToByte( float fv ) {
                int iv = fv <= minAlpha ? 255
                                        : (int) ( Math.log( fv ) * scale1 );
                assert iv >= 0 && iv <= 255 : fv + " -> " + iv;
                return iv;
            }
            public float byteToFloat( int iv ) {
                float fv = (float) Math.exp( ( iv & 0xff ) * scale );
                assert fv >= 0f && fv <= 1f : iv + " -> " + fv;
                return fv;
            }
        };
    }

    /**
     * Paper implementation for use with this class.
     */
    private static class PixelStackPaper extends RgbPaper3D {

        private final Compositor compositor_;
        private final FloatPacker alphaPacker_;
        private final PixelStack[] stacks_;
        private final float[] frgba_;
        private Color lastColor_;
        private int lastRgb_;
        private float lastAlpha_;

        /**
         * Constructor.
         *
         * @param  paperType  paper type instance creating this paper
         * @param  bounds  plot bounds
         */
        PixelStackPaper( PixelStackPaperType3D paperType, Rectangle bounds ) {
            super( paperType, bounds );
            compositor_ = paperType.compositor_;
            alphaPacker_ = paperType.alphaPacker_;
            stacks_ = new PixelStack[ bounds.width * bounds.height ];
            frgba_ = new float[ 4 ];
        }

        protected void placePixels( int xoff, int yoff, double dz,
                                    Pixellator pixer, Color color ) {
            if ( color != lastColor_ ) {
                lastColor_ = color;
                int rgba = color.getRGB();
                lastRgb_ = rgba & 0x00ffffff;
                color.getRGBComponents( frgba_ ); // only way to get float alpha
                lastAlpha_ = frgba_[ 3 ];
            }
            int rgb = lastRgb_;
            float alpha = lastAlpha_;
            for ( pixer.start(); pixer.next(); ) {
                int index = getPixelIndex( xoff, yoff, pixer );
                PixelStack stack = stacks_[ index ];
                if ( stack == null ) {
                    stack = new PixelStack( alphaPacker_ );
                    stacks_[ index ] = stack;
                }
                stack.addPixel( dz, rgb, alpha );
            }
        }

        public void flush() {
            int[] rgbs = getRgbImage().getBuffer();
            Compositor.Buffer cbuf1 = compositor_.createBuffer( 1 );
            int npix = rgbs.length;
            for ( int i = 0; i < npix; i++ ) {
                PixelStack stack = stacks_[ i ];
                if ( stack != null ) {
                    rgbs[ i ] = Compositor
                               .srcOverOpaque( stack.getStackRgb( cbuf1 ),
                                               rgbs[ i ] );
                }
            }
        }
    }

    /**
     * Implements a list of pixels stored at a given X,Y position.
     * Each grid position in the image potentially has one of these
     * (lazily created when the pixel is first written to).
     */
    private static class PixelStack {

        // Here are some ideas for improving efficiency:
        // 0. Profile to see where the bottlenecks are.
        // 1. Fix DoubleList so that there is direct access to the backing
        //    double[] array, then a copy is not necessary in getStackRgb.
        // 2. Make addPixel smarter by periodically sorting the pixel list
        //    so that it can determine if the added pixel can be ignored
        //    (saturation already reached).  Main question is when the sorts
        //    occur.  Then very long lists for very dense regions can be
        //    truncated.

        private final FloatPacker alphaPacker_;
        private final DoubleList list_;

        /**
         * Constructor.
         *
         * @param  alphaPacker  strategy for packing the float alpha value
         *                      into 8 bits
         */
        PixelStack( FloatPacker alphaPacker ) {
            alphaPacker_ = alphaPacker;
            list_ = new DoubleList();
        }

        /**
         * Add a new pixel to this list.
         *
         * @param   dz  Z-coordinate
         * @param   rgb  RGB colour, only lowest 24 bits significant
         * @param   alpha  alpha value as floating point value
         */
        public void addPixel( double dz, int rgb, float alpha ) {
            list_.add( pack( dz, rgb, alpha ) );
        }

        /**
         * Returns the ARGB integer value which results from compositing
         * all the pixels in this stack in their proper Z-coordinate order.
         *
         * @param  cbuf1  1-pixel compositor buffer using this paper's
         *                compositing strategy, used as workspace
         */
        public int getStackRgb( Compositor.Buffer cbuf1 ) {
            cbuf1.clear();

            /* If there's a single pixel, no sorting is required. */
            int npix = list_.size();
            if ( npix == 1 ) {
                double v = list_.get( 0 );
                int rgba = unpackRgbPackedAlpha( list_.get( 0 ) );
                cbuf1.addSample( 0, rgba,
                                 alphaPacker_.byteToFloat( rgba >> 24 ) );
            }

            /* Otherwise, sort the pixels into z-coordinate order and
             * composite them from near to far. */
            else {
                assert npix > 1;
                double[] array = list_.toDoubleArray();
                Arrays.sort( array );
                boolean done = false;
                for ( int i = 0; i < npix && ! done; i++ ) {
                    int rgba = unpackRgbPackedAlpha( array[ i ] );
                    done = cbuf1
                          .addSample( 0, rgba,
                                      alphaPacker_.byteToFloat( rgba >> 24 ) );
                }
            }

            /* Comvert the buffer contents to an ARGB value and return it. */
            return cbuf1.toRgbInt( 0 );
        }

        /**
         * Packs all the required information about a pixel into one double
         * precision value that can be sorted numerically into Z-coordinate
         * order.
         *
         * <p>We start from the Z coordinate double and simply write
         * the 4 bytes of RGBA over the least significant
         * part of the mantissa.
         * Doing it like this means that the resulting doubles can be sorted
         * like normal primitives.  The sort sequence will be slightly
         * affected by colour, but it's unlikely to show up in a way that
         * will be visually noticeable.
         *
         * <p>Note this only leaves 8 bits for alpha, which is a bit small,
         * since we may need to encode values significantly smaller than
         * 2^-8.  The FloatPacker interface deals with squashing
         * suitable values into 8 bits.
         *
         * <p>Go on, say you're impressed.
         *
         * @param   dz  Z coordinate
         * @param   RGB values in lowest 3 bytes (msb ignored)
         * @param   alpha  alpha value as float in range 0-1
         * @return   packed value which can be sorted numerically as Z
         */
        private double pack( double dz, int rgb, float alpha ) {
            int ialpha = alphaPacker_.floatToByte( alpha );
            int rgba = ( rgb & 0x00ffffff ) | ( ialpha << 24 );
            return ( Double.longBitsToDouble( ( Double.doubleToLongBits( dz )
                                                & 0xffffffff00000000L )
                                            | ( rgba
                                                & 0x00000000ffffffffL ) ) );
        }

        /**
         * Retrieves the Z coordinate from a packed double.
         * All right, it's lost a bit of precision.
         *
         * @param  packed   value packed by pack() method
         * @return  approximate z-coordinate 
         */
        private double unpackZ( double packed ) {
            return Double.longBitsToDouble( Double.doubleToLongBits( packed ) &
                                            0xffffffff00000000L );
        }

        /**
         * Unpacks the RGBA information from a packed double.
         * The 3 lowest bytes contain RGB, and the msb contains the alpha
         * value packed using this paper's alphaPacker.
         *
         * @param  packed   value packed by pack() method
         * @return  encoded RGBA value
         */
        private int unpackRgbPackedAlpha( double packed ) {
            return (int) ( Double.doubleToLongBits( packed )
                           & 0x00000000ffffffffL );
        }
    }

    /**
     * Maps between integers in the range 0-255 and floats in the range 0-1.
     */
    private interface FloatPacker {

        /**
         * Converts a float in the range 0-1 to an int in the range 0-255.
         * The first 24 bits of the result will be zero.
         *
         * @param  fv  float value, assumed in range 0-1
         * @return  int value in range 0-255
         */
        int floatToByte( float fv );

        /**
         * Converts the least significant byte of an int to a float
         * in the range 0-1.
         *
         * @param  iv  int value, only the last 8 bits are used
         * @return  float value in range 0-1
         */
        float byteToFloat( int iv );
    }
}
