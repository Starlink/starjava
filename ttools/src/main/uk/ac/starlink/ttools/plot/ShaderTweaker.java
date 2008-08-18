package uk.ac.starlink.ttools.plot;

import java.awt.Color;

/**
 * DataColorTweaker implementation which uses an array of Shader objects.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public class ShaderTweaker implements DataColorTweaker {

    private final int ioff_;
    private final int ndim_;
    private final Shader[] shaders_;
    private final Scaler[] scalers_;
    private final double[] knobs_;
    private boolean hasEffect_;

    /**
     * Constructor.
     *
     * @param   ioff    offset into supplied coordinate arrays at which 
     *                  auxiliary data starts
     * @param   shaders array of shaders, one for each aux axis
     * @param   ranges  array of (low,high) range bounds, one for each aux axis
     * @param   logFlags  array of logarithmic scaling flags,
     *                    one for each aux axis
     * @param   flipFlags  array of axis inversion flags,
     *                     one for each aux axis
     */
    public ShaderTweaker( int ioff, Shader[] shaders, double[][] ranges,
                          boolean[] logFlags, boolean[] flipFlags ) {
        ioff_ = ioff;
        shaders_ = shaders;

        /* Create a scaler for each of the auxiliary axes.  These turn 
         * data coordinates into normalised coordinates (0..1). */
        int ndim = shaders_.length;
        knobs_ = new double[ ndim ];
        scalers_ = new Scaler[ ndim ];
        int nd = 0;
        for ( int idim = 0; idim < ndim; idim++ ) {
            knobs_[ idim ] = Double.NaN;
            if ( shaders_[ idim ] != null ) {
                int jdim = ioff + idim;
                scalers_[ idim ] =
                    createScaler( ranges[ jdim ][ 0 ], ranges[ jdim ][ 1 ],
                                  logFlags[ jdim ], flipFlags[ jdim ] );
                nd = idim + 1;
            }
        }
        ndim_ = nd;
    }

    public int getNcoord() {
        return ioff_ + ndim_;
    }

    /**
     * This implementation returns true unless the scaler results in a NaN
     * for any of the coordinates.
     */
    public boolean setCoords( double[] coords ) {
        boolean hasEffect = false;
        for ( int idim = 0; idim < ndim_; idim++ ) {
            if ( shaders_[ idim ]  != null ) {
                int jdim = ioff_ + idim;
                double value = coords[ jdim ];
                if ( Double.isNaN( value ) ) {
                    knobs_[ idim ] = Double.NaN;
                }
                else {
                    hasEffect = true;
                    Scaler scaler = scalers_[ idim ];
                    double sval = scaler.scale( value );
                    if ( scaler.inRange( value ) ) {
                        knobs_[ idim ] = sval;
                        assert sval >= 0.0 && sval <= 1.0;
                    }
                    else if ( Double.isNaN( sval ) ) {
                        return false;
                    }
                    else {
                        knobs_[ idim ] = sval > 1.0 ? 1.0 : 0.0;
                    }
                }
            }
        }
        hasEffect_ = hasEffect;
        return true;
    }

    public void tweakColor( float[] rgba ) {
        if ( hasEffect_ ) {
            for ( int idim = 0; idim < ndim_; idim++ ) {
                Shader shader = shaders_[ idim ];
                if ( shader != null ) {
                    double knob = knobs_[ idim ];
                    if ( ! Double.isNaN( knob ) ) {
                        shader.adjustRgba( rgba, (float) knob );
                    }
                }
            }
        }
    }

    public Color tweakColor( Color orig ) {
        if ( hasEffect_ ) {
            float[] rgba = orig.getRGBComponents( null );
            tweakColor( rgba );
            return new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] );
        }
        else {
            return orig;
        }
    }

    /**
     * Returns a new tweaker suitable for a given plot.  Iff no colour 
     * tweaking will be performed (that is, if such an object would do no work)
     * then null will be returned.
     *
     * @param   ioff    offset into supplied coordinate arrays at which 
     *                  auxiliary data starts
     * @param   state   describes the plot for which this object will be used
     * @return  new tweaker, or null
     */
    public static ShaderTweaker createTweaker( int ioff, PlotState state ) {
        Shader[] shaders = state.getShaders();
        int naux = shaders.length;
        if ( naux == 0 ) {
            return null;
        }
        else {
            int nd = state.getMainNdim();
            double[][] ranges;
            if ( ioff == nd ) {
                ranges = state.getRanges();
            }

            /* If necessary adjust for the case in which there is a different
             * number of geometrical axes than non-auxiliary axes. */
            else {
                ranges = new double[ ioff + naux ][];
                System.arraycopy( state.getRanges(), nd, ranges, ioff, naux );
            }
            return new ShaderTweaker( ioff, shaders, ranges,
                                      state.getLogFlags(),
                                      state.getFlipFlags() );
        }
    }

    /**
     * Factory method for scaler objects which normalise auxiliary coordinate
     * data values.
     *
     * @param   lo   data lower limit
     * @param   hi   data upper limit
     * @param   logFlag  whether logarithmic scaling is to be used
     * @param   flip   whether axis is to be inverted
     */
    private static Scaler createScaler( final double lo, final double hi,
                                        boolean logFlag,
                                        boolean flipFlag ) {
        if ( logFlag ) {
            final double base1 = 1.0 / ( flipFlag ? hi : lo );
            final double scale1 =
                1.0 / ( Math.log( flipFlag ? lo / hi : hi / lo ) );
            return new Scaler( lo, hi ) {
                public double scale( double value ) {
                    return Math.log( value * base1 ) * scale1;
                }
            };
        }
        else {
            final double base = flipFlag ? hi : lo;
            final double scale1 = 1.0 / ( flipFlag ? lo - hi : hi - lo );
            return new Scaler( lo, hi ) {
                public double scale( double value ) {
                    return ( value - base ) * scale1;
                }
            };
        }
    }

    /**
     * Interface for normalising coordinate data values to the range 0..1.
     */
    private static abstract class Scaler {
        private final double lo_;
        private final double hi_;

        /**
         * Constructor.
         *
         * @param   lo   lower bound for data of interest
         * @param   hi   upper bound for data of interest
         */
        public Scaler( double lo, double hi ) {
            lo_ = lo;
            hi_ = hi;
        }

        /**
         * Indicates whether a value is in the coordinate range of interest
         * to this object.
         *
         * @param  value  value to test
         * @return  true iff <code>value</code> is in data range of interest
         */
        public boolean inRange( double value ) {
            return value >= lo_ && value <= hi_;
        }

        /**
         * Maps a value in this Scaler's range of interest to the range 0..1.
         * If <code>inRange(value)</code> then <code>0<=scale(value)<=1</code>.
         * Outside that range results are undefined.
         *
         * @param   value   value in data range of interest
         * @return   value in range 0..1
         */
        public abstract double scale( double value );
    }
}
