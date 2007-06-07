package uk.ac.starlink.topcat.plot;

import java.awt.Color;

/**
 * ColorTweaker implementation which can adjust plotting colours on the
 * basis of a supplied array of values (auxiliary data coordinates).
 *
 * @author   Mark Taylor
 * @since    5 Jun 2007
 */
public class DataColorTweaker implements ColorTweaker {

    private final int ioff_;
    private final int ndim_;
    private final Shader[] shaders_;
    private final Scaler[] scalers_;
    private final double[] knobs_;

    /**
     * Constructor.
     *
     * @param   state   describes the plot for which this object will be used
     * @param   ioff    offset into supplied coordinate arrays at which 
     *                  auxiliary data starts
     */
    public DataColorTweaker( PlotState state, int ioff ) {
        ioff_ = ioff;

        /* Acquire information from PlotState about how colours will be
         * adjusted. */
        shaders_ = state.getShaders();
        int ndim = shaders_.length;
        knobs_ = new double[ ndim ];
        scalers_ = new Scaler[ ndim ];
        double[][] ranges = state.getRanges();
        boolean[] logFlags = state.getLogFlags();
        boolean[] flipFlags = state.getFlipFlags();

        /* Create a scaler for each of the auxiliary axes.  These turn 
         * data coordinates into normalised coordinates (0..1). */
        int nd = 0;
        for ( int idim = 0; idim < ndim; idim++ ) {
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

    /**
     * Configures this object with a coordinate array which determines
     * what colour adjustments subsequent calls to {@link #tweakColor}
     * will perform.  The elements of the coordinate array starting from 
     * <code>ioff</code> (as supplied in the constructor) will be used.
     *
     * <p>The return value indicates whether the supplied coordinates are
     * within the visible data ranges; iff they are outside this range 
     * false will be returned.  Null auxiliary coordinates do not cause 
     * a false return, and neither do they cause any change to the 
     * input colour.  In case of a false return this object is left
     * in an undefined state, so <code>tweakColor</code> should only be
     * called following a successful (true) call of this method.
     *
     * @param  coords   full coordinate array
     */
    public boolean setCoords( double[] coords ) {
        for ( int idim = 0; idim < ndim_; idim++ ) {
            if ( shaders_[ idim ]  != null ) {
                int jdim = ioff_ + idim;
                double value = coords[ jdim ];
                if ( ! Double.isNaN( value ) ) {
                    Scaler scaler = scalers_[ idim ];
                    if ( scaler.inRange( value ) ) {
                        knobs_[ idim ] = scaler.scale( value );
                        assert knobs_[ idim ] >= 0.0 && knobs_[ idim ] <= 1.0;
                    }
                    else {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Adjusts a supplied colour as determined by the last call to
     * {@link #setCoords}.
     *
     * @param  orig  original colour
     * @return   tweaked colour
     */
    public Color tweakColor( Color orig ) {
        if ( ndim_ == 0 ) {
            return orig;
        }
        else {
            float[] rgba = orig.getRGBComponents( null );
            boolean changed = false;
            for ( int idim = 0; idim < ndim_; idim++ ) {
                Shader shader = shaders_[ idim ];
                if ( shader != null ) {
                    double knob = knobs_[ idim ];
                    if ( ! Double.isNaN( knob ) ) {
                        shader.adjustRgba( rgba, (float) knob );
                        changed = true;
                    }
                }
            }
            return changed 
                 ? new Color( rgba[ 0 ], rgba[ 1 ], rgba[ 2 ], rgba[ 3 ] )
                 : orig;
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
