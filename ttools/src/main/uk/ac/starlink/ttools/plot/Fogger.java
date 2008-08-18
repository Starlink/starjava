package uk.ac.starlink.ttools.plot;

import java.awt.Color;

/**
 * Defines how colours are changed at different depths (distances away from
 * the viewer) by interposing a 'fog' which makes the colours gloomier the
 * further away you go.
 * 
 * <p>Various methods are provided to fog colours at a given Z coordinate.
 * For reasonable rendering, <code>z</code> should be greater than
 * or equal to zero, and of order of this renderer's scale distance.
 *
 * <p>Instances of this class are not thread safe.
 *
 * <p>The fogging algorithm comes from Eric Dumont's fog FAQ.
 *
 * @author   Mark Taylor
 * @since    18 Jan 2006
 */
public class Fogger {

    private final double scale_;
    private final int[] iFogRgb_;
    private final float[] fFogRgb_;
    private final float[] work3 = new float[ 3 ];
    private double fogScale_;

    /**
     * Constructs a new depth tweaker for rendering distances of the order
     * of a given scale.
     *
     * @param  scale  depth scale distance
     */
    public Fogger( double scale ) {
        scale_ = scale;
        int ifog = 0xff;
        float ffog = ifog / 255f;
        iFogRgb_ = new int[] { ifog, ifog, ifog };
        fFogRgb_ = new float[] { ffog, ffog, ffog };
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
     * Returns the 'clarity' value associated with a given distance
     * <code>z</code> away from the viewer.
     *
     * @param   z  depth coordinate
     * @return  clarity
     */
    private float getClarityAt( double z ) {

        /* Calculate the scaled distance. */
        double ked = fogScale_ * z;

        /* Calculate and return the clarity value. */
        return ked <= 0 ? 1f : (float) Math.exp( - ked );
    }

    /** 
     * Returns the 'clarity' value associated with the distance between
     * two distances away from the viewer.
     *
     * @param   zlo  nearer distance
     * @param   zhi  farther distance
     */
    private float getClarityBetween( double zlo, double zhi ) {
        double ked = Math.max( 0, zhi ) - Math.max( 0, zlo );
        return ked <= 0 ? 1f : (float) Math.exp( - ked );
    }

    /**
     * Gives the effect of fogging one of the integer RGB components
     * of a colour.
     *
     * @param  value  component value, in range 0-255
     * @param  icomp  index of colour component (0=red, 1=green, 2=blue)
     * @return  fogged version of <code>value</code>
     */
    private int fogComponent( float clarity, int value, int icomp ) {
        return icomp < 3 
             ? (int) ( ( clarity * value 
                     + ( 1f - clarity ) * iFogRgb_[ icomp ] ) )
             : value;
    }

    /**
     * Gives the effect of fogging one of the normalised RGB 
     * components of a colour.
     *
     * @param  value  component value, in range 0-1
     * @param  icomp  index of colour component (0=red, 1=green, 2=blue)
     * @return  fogged version of <code>value</code>
     */
    private float fogComponent( float clarity, float value, int icomp ) {
        return icomp < 3
             ? ( ( clarity * value
                 + ( 1f - clarity ) * fFogRgb_[ icomp ] ) )
             : value;
    }

    /**
     * Returns a fogged version of a given colour at a given depth.
     *
     * @param  z       distance at which the colour is being viewed
     * @param  color   original colour
     * @return   fogged colour
     */
    public Color fogAt( double z, Color color ) {
        float clarity = getClarityAt( z );
        return clarity == 1f 
             ? color
             : new Color( fogComponent( clarity, color.getRed(), 0 ),
                          fogComponent( clarity, color.getGreen(), 1 ),
                          fogComponent( clarity, color.getBlue(), 2 ),
                          fogComponent( clarity, color.getAlpha(), 3 ) );
    }

    /**
     * Returns a fogged version of a given colour at a given depth.
     *
     * @param  z       distance at which the colour is being viewed
     * @param  rgba    rgba colour components; these will be modified on
     *                 exit to represent the fogged value
     */
    public void fogAt( double z, int[] rgba ) {
        float clarity = getClarityAt( z );
        if ( clarity != 1f ) {
            for ( int i = 0; i < 4; i++ ) {
                rgba[ i ] = fogComponent( clarity, rgba[ i ], i );
            }
        }
    }

    /**
     * Returns a fogged version of a given colour at a given depth
     * using normalised components.
     *
     * @param  z       distance at which the colour is being viewed
     * @param  rgba    normalised rgba colour components; these will 
     *                 be modified on exit to represent the fogged value
     */
    public void fogAt( double z, float[] rgba ) {
        float clarity = getClarityAt( z );
        if ( clarity != 1f ) {
            for ( int i = 0; i < 4; i++ ) {
                rgba[ i ] = fogComponent( clarity, rgba[ i ], i );
            }
        }
    }

    /**
     * Returns a fogged version of a given colour at a given depth.
     *
     * @param  z       distance at which the colour is being viewed
     * @return   argb packed colour value for the fogged colour
     */
    public int fogAt( double z, int argb ) {
        float clarity = getClarityAt( z );
        if ( clarity == 1f ) {
            return argb;
        }
        else {
            int b = ( argb >> 0 ) & 0xff;
            int g = ( argb >> 8 ) & 0xff;
            int r = ( argb >> 16 ) & 0xff;
            int a = ( argb >> 24 ) & 0xff;
            return ( ( fogComponent( clarity, b, 0 ) & 0xff ) << 0 )
                 | ( ( fogComponent( clarity, g, 1 ) & 0xff ) << 8 )
                 | ( ( fogComponent( clarity, r, 2 ) & 0xff ) << 16 )
                 | ( ( fogComponent( clarity, a, 3 ) & 0xff ) << 24 );
        }
    }

    /**
     * Constructs a DataColorTweaker corresponding to this fogger which 
     * just takes care of fogging.
     *
     * @param   ifog  index in coordinate array of Z coordinate
     * @param   ncoord  size of coordinate array
     */
    public DataColorTweaker createTweaker( final int ifog, final int ncoord ) {
        return new DataColorTweaker() { 
            private double z_;
            public boolean setCoords( double[] coords ) {
                double z = coords[ ifog ];
                if ( Double.isNaN( z ) ) {
                    return false;
                }
                else {
                    z_ = z;
                    return true;
                }
            }
            public int getNcoord() {
                return ncoord;
            }
            public Color tweakColor( Color color ) {
                return fogAt( z_, color );
            }
            public void tweakColor( float[] rgba ) {
                fogAt( z_, rgba );
            }
        };
    }

    /**
     * Constructs a DataColorTweaker based on an existing one which appends
     * the affect of this fogger.
     *
     * @param   ifog   index in coordinate array of Z coordinate
     * @param   base   color tweaker to be additionally fogged
     */
    public DataColorTweaker createTweaker( final int ifog,
                                           final DataColorTweaker base ) {
        return new DataColorTweaker() {
            private double z_;
            public boolean setCoords( double[] coords ) {
                if ( base.setCoords( coords ) ) {
                    double z = coords[ ifog ];
                    if ( Double.isNaN( z ) ) {
                        return false;
                    }
                    else {
                        z_ = z;
                        return true;
                    }
                }
                else {
                    return false;
                }
            }
            public int getNcoord() {
                return base.getNcoord();
            }
            public Color tweakColor( Color color ) {
                if ( getClarityAt( z_ ) == 1f ) {
                    return base.tweakColor( color );
                }
                else {
                    float[] rgba = color.getRGBComponents( null );
                    tweakColor( rgba );
                    return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ],
                                      rgba[ 3 ] );
                }
            }
            public void tweakColor( float[] rgba ) {
                base.tweakColor( rgba );
                fogAt( z_, rgba );
            }
        };
    }
}
