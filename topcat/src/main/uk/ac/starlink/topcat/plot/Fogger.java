package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * Defines how colours are changed at different depths (distances away from
 * the viewer) by interposing a 'fog' which makes the colours gloomier the
 * further away you go.
 *
 * <p>The fogging algorithm comes from Eric Dumont's fog FAQ.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2006
 */
public class Fogger implements ColorTweaker {

    private final double scale_;
    private final int[] fogRgb_ = new int[] { 0xe0, 0xe0, 0xe0 };
    private final float[] work3 = new float[ 3 ];
    private double fogScale_;
    float clarity_;

    /**
     * Constructs a new depth tweaker for rendering distances of the order
     * of a given scale.
     *
     * @param  scale  depth scale distance
     */
    public Fogger( double scale ) {
        scale_ = scale;
    }

    /**
     * Returns the depth scale distance.
     *
     * @return  depth scale distance
     */
    public double getScale() {
        return scale_;
    }

    /**
     * Sets the thickness of the fog.
     * A given value of fogginess corresponds to a given amount of
     * obscuration of a mark at a depth of the given multiple
     * of the scale distance.
     * Default value is zero (no fog).
     *
     * @param  fogginess  intensity of fog per scale distance
     */
    public void setFogginess( double fogginess ) {
        fogScale_ = fogginess / scale_;
    }

    /**
     * Returns the thickness of the fog.
     *
     * @return  intensity of fog per scale distance
     */
    public double getFogginess() {
        return fogScale_ * scale_;
    }

    /**
     * Sets the distance in the depth direction for which subsequent
     * {@link #tweak}s of the colour will configure.
     * For reasonable rendering, <code>z</code> should be greater than
     * or equal to zero, and of order of this renderer's scale distance.
     *
     * @param   z  depth distance
     */
    public void setZ( double z ) {

        /* Calculate the scaled distance. */
        double ked = fogScale_ * z;

        /* Set the clarity value.  Take care not to do unnecessary
         * calculations in the case of no fog, since exp() may be expensive. */
        clarity_ = ked <= 0 ? 1f
                            : (float) Math.exp( - ked );
    }

    /**
     * Gives the effect of fogging one of the RGB components of a colour.
     *
     * @param  value  component value, in range 0-255
     * @param  icomp  index of colour component (0=red, 1=green, 2=blue)
     * @return  fogged version of <code>value</code>
     */
    private int fogComponent( int value, int icomp ) {
        return icomp < 3 
             ? (int) ( ( clarity_ * value 
                     + ( 1f - clarity_ ) * fogRgb_[ icomp ] ) )
             : value;
    }

    public Color tweakColor( Color orig ) {
        return clarity_ == 1f 
             ? orig
             : new Color( fogComponent( orig.getRed(), 0 ),
                          fogComponent( orig.getGreen(), 1 ),
                          fogComponent( orig.getBlue(), 2 ),
                          fogComponent( orig.getAlpha(), 3 ) );
    }

    public void tweakARGB( int[] argb ) {
        if ( clarity_ != 1f ) {
            for ( int i = 0; i < 4; i++ ) {
                argb[ i ] = fogComponent( argb[ i ], i );
            }
        }
    }

    public int tweakARGB( int argb ) {
        int b = ( argb >> 0 ) & 0xff;
        int g = ( argb >> 8 ) & 0xff;
        int r = ( argb >> 16 ) & 0xff;
        int a = ( argb >> 24 ) & 0xff;
        return ( ( fogComponent( b, 0 ) & 0xff ) << 0 )
             | ( ( fogComponent( g, 1 ) & 0xff ) << 8 )
             | ( ( fogComponent( r, 2 ) & 0xff ) << 16 )
             | ( ( fogComponent( a, 3 ) & 0xff ) << 24 );
    }
}
