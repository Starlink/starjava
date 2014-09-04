package uk.ac.starlink.ttools.plot2.paper;

import java.util.Arrays;

/**
 * Represents an algorithm for combining multiple RGBA values to produce
 * a single RGBA value.  The {@link #createBuffer} method produces a buffer
 * containing a vector of pixels for which the compositing operation
 * can be performed on each element.
 *
 * <p>In general RGB values are not pre-multiplied by alpha
 * as used by this class.
 *
 * @author   Mark Taylor
 * @since    12 Feb 2013
 * @see   RgbImage
 */
public abstract class Compositor {

    private static final float FF1 = 1f / 255f;

    /**
     * Implementation which adds colours until the alpha is saturated
     * and then makes no further updates to colour.
     */
    public static final Compositor SATURATION = new SaturationCompositor() {
        public float scaleAlpha( float alpha ) {
            return alpha;
        }
    };

    /**
     * Adjusts an alpha value in accordance with this compositor's policy.
     * It takes an alpha value in the range 0-1 and maps it into the
     * range to be used for output from this compositor.
     *
     * @param  alpha  input alpha in range 0-1
     * @return  adjusted alpha, also in range 0-1
     */
    public abstract float scaleAlpha( float alpha );

    /**
     * Creates a buffer on which compositing operations can be performed.
     *
     * @param   count  number of pixel elements in buffer
     * @return  new buffer
     */
    public abstract Buffer createBuffer( int count );

    /**
     * Buffer of pixels on which compositing operations can be performed.
     */
    public interface Buffer {

        /**
         * Clears all samples from this buffer.
         */
        void clear();

        /**
         * Adds an RGBA sample to one pixel, using separate float scalars.
         *
         * @param  index  pixel index
         * @param  r   red value, 0-1
         * @param  g   green value, 0-1
         * @param  b   blue value, 0-1
         * @param  alpha  alpha value, 0-1
         * @return  true if saturation has been reached, that is further
         *               samples added to this pixel will have no effect
         */
        boolean addSample( int index, float r, float g, float b, float alpha );

        /**
         * Adds an RGBA sample to one pixel, using an RGB integer and
         * separate float value.
         *
         * @param  index  pixel index
         * @param  rgb  integer containing RGB in lower 24 bits;
         *              highest byte is ignored
         * @param  alpha  alpha value, 0-1
         * @return  true if saturation has been reached, that is further
         *               samples added to this pixel will have no effect
         */
        boolean addSample( int index, int rgb, float alpha );

        /**
         * Adds an RGBA sample to one pixel, using an RGBA integer.
         *
         * @param  index  pixel index
         * @param  rgba   integer containing RGBA values one per byte
         * @return  true if saturation has been reached, that is further
         *               samples added to this pixel will have no effect
         */
        boolean addSample( int index, int rgba );

        /**
         * Returns the result of compositing all the added samples
         * as a non-premultiplied ARGB integer.
         * This is suitable for use with {@link java.awt.image.BufferedImage}
         * <code>setRGB</code> methods) or, more efficiently,
         * an {@link RgbImage} buffer.
         *
         * @param  index  pixel index
         * @return   ARGB integer
         */
        int toRgbInt( int index );
    } 
    /**
     * Compositor with boosted saturation.
     * This acts like {@link #SATURATION} except that any pixel with a
     * non-zero alpha has its alpha value boosted to a given minimum.
     * The effect is that even very slightly populated pixels can be
     * visually distinguished from unpopulated pixels, which is not the
     * case for standard saturation composition.
     */
    public static class BoostCompositor extends Compositor {
        private final float boost_;
        private final float boost1_;

        /**
         * Constructor.
         * The boost value must be in the range 0..1; zero is equivalent
         * to {@link #SATURATION}.
         *
         * @param   boost  minimum alpha output for non-empty pixel
         */
        public BoostCompositor( float boost ) {
            boost_ = boost;
            boost1_ = 1f - boost;
            if ( ! ( boost >= 0 && boost <= 1 ) ) {
                throw new IllegalArgumentException( "Boost out of range 0..1" );
            }
        }

        public Buffer createBuffer( int count ) {
            return new SaturationBuffer( this, count );
        }

        public float scaleAlpha( float alpha ) {
            return boost_ + boost1_ * alpha;
        }

        /**
         * Returns the boost value for this compositor.
         *
         * @return  boost value in range 0..1
         */
        public float getBoost() {
            return boost_;
        }
    }

    /**
     * Partial compositor implementation with saturation semantics.
     */
    private static abstract class SaturationCompositor extends Compositor {
        public Buffer createBuffer( int count ) {
            return new SaturationBuffer( this, count );
        }
    }

    /**
     * Compositor.Buffer implementation that provides saturation semantics
     * for adding samples.
     * Internal storage is a float for each channel (R,G,B,A).
     * Packing the values into a 4-byte int would lose precision much too
     * rapidly.
     */
    private static class SaturationBuffer implements Buffer {

        private final Compositor compositor_;
        private final float[] buf_;

        /**
         * Constructor.
         *
         * @param   compositor  compositor
         * @param   count   pixel count
         */
        SaturationBuffer( Compositor compositor, int count ) {
            compositor_ = compositor;
            buf_ = new float[ count * 4 ];
        }

        public void clear() {
            Arrays.fill( buf_, 0f );
        } 

        public boolean addSample( int index, int rgba ) {
            float alpha = byteToFloat( rgba >> 24 );
            return addSample( index, rgba, alpha );
        }

        public boolean addSample( int index, int rgb, float alpha ) {
            float r = byteToFloat( rgb >> 16 );
            float g = byteToFloat( rgb >> 8 );
            float b = byteToFloat( rgb );
            return addSample( index, r, g, b, alpha );
        }

        public boolean addSample( int index, float r, float g, float b,
                                  float alpha ) {
            int ix = index * 4;
            float ta = buf_[ ix ];
            float remain = 1f - ta;
            float weight = Math.min( remain, alpha );
            if ( weight > 0 ) {
                ta += weight;
                buf_[ ix++ ] = ta;
                buf_[ ix++ ] += weight * r;
                buf_[ ix++ ] += weight * g;
                buf_[ ix   ] += weight * b;
            }
            return ta >= 1f;
        }

        public int toRgbInt( int index ) {
            int ix = index * 4;
            float fa = buf_[ ix++ ];
            if ( fa == 0 ) {
                return 0;
            }
            float fr = buf_[ ix++ ];
            float fg = buf_[ ix++ ];
            float fb = buf_[ ix   ];
            if ( fa < 1 ) {
                float a1 = 1f / fa;
                fr *= a1;
                fg *= a1;
                fb *= a1;
            }
            else {
                assert fa == 1;
            }
            return ( floatToByte( compositor_.scaleAlpha( fa ) ) << 24 )
                 | ( floatToByte( fr ) << 16 )
                 | ( floatToByte( fg ) << 8 )
                 | ( floatToByte( fb ) );
        }
    }

    /**
     * Maps an integer in the range 0-255 to a float in the range 0-1.
     * Bits more significant than the first 8 are ignored.
     *
     * @param   i  integer value 
     * @return  float in range 0-1
     */
    public static float byteToFloat( int i ) {
        return ( i & 0xff ) * FF1;
    }

    /**
     * Maps a float in the range 0-1 to an int in the range 0-255.
     *
     * @param   f  float in range 0-1
     * @return   int in range 0-255
     */
    public static int floatToByte( float f ) {
        return Math.round( 255 * f ) & 0xff;
    }

    /**
     * Returns the result of compositing a possibly transparent source pixel
     * over an opaque destination pixel.
     *
     * @param  sRgba  RGBA for source pixel
     * @param  dRgb   RGB for opaque destination pixel
     * @return  RGBA for composition result
     */
    public static int srcOverOpaque( int sRgba, int dRgb ) {
        if ( dRgb == 0 ) {
            return sRgba;
        }
        int sAlphaByte = sRgba >> 24;
        if ( sAlphaByte == 0 ) {
            return dRgb;
        }
        float sAlpha = byteToFloat( sAlphaByte );
        float dAlpha = 1f - sAlpha;
        float sR = byteToFloat( sRgba >> 16 );
        float sG = byteToFloat( sRgba >> 8 );
        float sB = byteToFloat( sRgba );
        float dR = byteToFloat( dRgb >> 16 );
        float dG = byteToFloat( dRgb >> 8 );
        float dB = byteToFloat( dRgb );
        return ( floatToByte( sR * sAlpha + dR * dAlpha ) << 16 )
             | ( floatToByte( sG * sAlpha + dG * dAlpha ) << 8 )
             | ( floatToByte( sB * sAlpha + dB * dAlpha ) )
             | 0xff000000;
    }
}
