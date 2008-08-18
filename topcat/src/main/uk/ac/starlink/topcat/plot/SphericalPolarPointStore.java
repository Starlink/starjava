package uk.ac.starlink.topcat.plot;

import uk.ac.starlink.table.ValueStore;
import uk.ac.starlink.table.storage.ArrayPrimitiveStore;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.ttools.plot.Matrices;

/**
 * PointStore implementation for storing spherical polar data.
 * In fact currently the data points are stored in Cartesian coordinates 
 * (X,Y,Z), since spherical decoding is taken care of by the 
 * PointSelection object (probably saving time on replots)
 * but the errors are stored as radial and tangential deltas
 * (since storing the error points would take a lot of extra space).
 *
 * <p>The error points returned by {@link #getErrors} consist of a
 * 1- (radial only), 2- (tangential only) or 3- (tangential followed by
 * radial) pair array of 3-d coordinate arrays.  Each pair is a lower 
 * bound followed by an upper bound along the relevant dimension.
 *
 * @author   Mark Taylor
 * @since    3 Apr 2007
 */
public class SphericalPolarPointStore implements PointStore {

    private final ValueStore valueStore_;
    private final boolean hasTanerr_;
    private final boolean radialLog_;
    private final int npoint_;
    private final int nword_;
    private final int ntanWord_;
    private final int nradWord_;
    private final int nerrorWord_;
    private final int nerror_;
    private final RadialPairReader radialReader_;
    private final double[] coordBuf_;
    private final double[] tanBuf_;
    private final double[] radBuf_;
    private final double[] point_;
    private final double[] centre_;
    private final double[] rawerrs_;
    private final double[][] errors_;
    private final double[] pair_;
    private final double[][] rpair_;
    private final double[] transMatrix_;
    private int ipoint_;
    private double minTanError_;

    /**
     * Constructor.
     *
     * @param   radialMode  type of radial error information to store
     * @param   hasTanerr   whether to store tangential error information
     * @param   radialLog   whether radial coordinates are logarithmic
     * @param   npoint  number of points to store
     */
    public SphericalPolarPointStore( ErrorMode radialMode, boolean hasTanerr,
                                     boolean radialLog, int npoint ) {
        hasTanerr_ = hasTanerr;
        radialLog_ = radialLog;
        npoint_ = npoint;

        /* Set up the object which will interpret radial error information. */
        radialReader_ = getRadialPairReader( radialMode );

        /* Establish some constants. */
        ntanWord_ = hasTanerr ? 1 : 0;
        nradWord_ = radialReader_.getWordCount();
        nerrorWord_ = ntanWord_ + nradWord_;
        nword_ = 3 + nerrorWord_;
        nerror_ = ( hasTanerr ? 4 : 0 )
                + ( radialMode == ErrorMode.NONE ? 0 : 2 );

        /* Initialise some buffers. */
        coordBuf_ = new double[ 3 ];
        tanBuf_ = new double[ ntanWord_ ];
        radBuf_ = new double[ nradWord_ ];
        point_ = new double[ 3 ];
        centre_ = new double[ 3 ];
        rawerrs_ = new double[ nerrorWord_ ];
        errors_ = new double[ nerror_ ][];
        pair_ = new double[ 2 ];
        double[] rlo = new double[ 3 ];
        double[] rhi = new double[ 3 ];
        rpair_ = new double[][] { rlo, rhi };
        transMatrix_ = new double[ 9 ];

        /* Initialise the value store which will hold the values.
         * Other implementations are possible. */
        valueStore_ = new ArrayPrimitiveStore( double.class, nword_ * npoint_ );
        assert double.class.equals( valueStore_.getType() );
    }

    public void storePoint( Object[] coordRow, Object[] errorRow,
                            String label ) {
        long ioff = ipoint_ * (long) nword_;

        /* Copy coordinate values. */
        for ( int i = 0; i < 3; i++ ) {
            coordBuf_[ i ] = CartesianPointStore.doubleValue( coordRow[ i ] );
        }
        valueStore_.put( ioff, coordBuf_, 0, 3 );
        ioff += 3;

        /* Copy tangent error values. */
        int ierr = 0;
        if ( ntanWord_ > 0 ) {
            for ( int i = 0; i < ntanWord_; i++ ) {
                tanBuf_[ i ] =
                    CartesianPointStore.doubleValue( errorRow[ ierr++ ] );
            }
            valueStore_.put( ioff, tanBuf_, 0, ntanWord_ );
            ioff += ntanWord_;
        }

        /* Read radial error values and store values based on these.
         * The raw values are deltas (non-negative numbers giving 
         * differences in radius), but we will store them as factors 
         * (values >=1 giving a radius multiplier).  This allows us
         * to do more of the calculation work here (once) rather than
         * when the point errors are read (possibly multiple times). */
        if ( nradWord_ > 0 ) {
            double r = Math.sqrt( coordBuf_[ 0 ] * coordBuf_[ 0 ]
                                + coordBuf_[ 1 ] * coordBuf_[ 1 ]
                                + coordBuf_[ 2 ] * coordBuf_[ 2 ] );
            if ( radialLog_ ) {
                r = Math.exp( r );
            }
            double r1 = 1.0 / r;
            for ( int i = 0; i < nradWord_; i++ ) {
                double delta =
                    CartesianPointStore.doubleValue( errorRow[ ierr++ ] );
                radBuf_[ i ] = delta > 0.0 ? ( r + delta ) * r1 : 1.0;
            }
            valueStore_.put( ioff, radBuf_, 0, nradWord_ );
            ioff += nradWord_;
        }

        ipoint_++;
    }

    public int getCount() {
        return npoint_;
    }

    public int getNdim() {
        return 3;
    }

    public double[] getPoint( int ipoint ) {
        valueStore_.get( ipoint * (long) nword_, point_, 0, 3 );
        return point_;
    }

    public int getNerror() {
        return nerror_;
    }

    public double[][] getErrors( int ipoint ) {
        if ( nerror_ > 0 ) {
            long off = ipoint * (long) nword_;
            valueStore_.get( off, centre_, 0, 3 );
            off += 3;
            if ( ntanWord_ > 0 ) {
                valueStore_.get( off, tanBuf_, 0, ntanWord_ );
                off += ntanWord_;
            }
            if ( nradWord_ > 0 ) {
                valueStore_.get( off, radBuf_, 0, nradWord_ );
                off += nradWord_;
            }
            calcErrors( centre_, tanBuf_, radBuf_, errors_ );
        }
        return errors_;
    }

    public boolean hasLabels() {
        return false;
    }

    public String getLabel( int ipoint ) {
        return null;
    }

    /**
     * Sets the smallest value for tan error which should generate non-blank
     * tangent error bar points.  The idea is that if the graphical 
     * destination of the points represented by this object is known to
     * have a pixel size/resolution smaller than a given angular distance
     * over its whole range, we can save a lot of work by assuming that
     * certain errors are effectively non-existent.
     *
     * @param   minTanError  minimum non-negligable tangent error in radians
     */
    public void setMinimumTanError( double minTanError ) {
        minTanError_ = Math.max( 0, minTanError );
    }

    /**
     * Calculates the returned error points given the raw error values.
     *
     * @param  centre   central point coordinates
     * @param  tanErrs  array of raw tangent error values as stored
     * @param  radErrs  array of raw radial error values as stored
     * @param  errors   array of error points
     */
    public void calcErrors( double[] centre, double[] tanErrs, double[] radErrs,
                            double[][] errors ) {
        int pointOff = 0;
        if ( ntanWord_ > 0 ) {
            assert ntanWord_ == 1;
            double theta = tanErrs[ 0 ];
            calcTangentErrors( centre, theta, errors, pointOff );
            pointOff += 4;
        }
        if ( nradWord_ > 0 ) {
            radialReader_.convert( radErrs, pair_ );
            calcRadialErrors( centre, pair_, errors, pointOff );
            pointOff += 2;
        }
    }

    /**
     * Calculates tangential error points.
     *
     * @param   centre  coordinates of central point
     * @param   tanErr  error angle in radians
     * @param   points  array into which four error points will be written
     * @param   pointOff  position at which to write to <code>points</code>
     */
    private void calcTangentErrors( double[] centre, double tanErr,
                                    double[][] points, int pointOff ) {
        double[] rotmat;
        if ( tanErr >= minTanError_ &&
             ( rotmat = transformFrom001( centre ) ) != null ) {
            double s = Math.sin( tanErr );
            double c = Math.cos( tanErr );
            points[ pointOff++ ] =
                Matrices.mvMult( rotmat, new double[] { -s, 0, c } );
            points[ pointOff++ ] =
                Matrices.mvMult( rotmat, new double[] { +s, 0, c } );
            points[ pointOff++ ] =
                Matrices.mvMult( rotmat, new double[] { 0, -s, c } );
            points[ pointOff++ ] =
                Matrices.mvMult( rotmat, new double[] { 0, +s, c } );
        }
        else {
            points[ pointOff++ ] = null;
            points[ pointOff++ ] = null;
            points[ pointOff++ ] = null;
            points[ pointOff++ ] = null;
        }
    }

    /**
     * Calculates radial error points.
     *
     * @param  centre  coordinates of central point
     * @param  posFacts  array of factors representing (lo,hi) error values;
     *         each is >1 for a non-blank error value
     * @param  points  array into which two error points will be written
     * @param  pointOff  position at which to write to <code>points</code>
     */
    private void calcRadialErrors( double[] centre, double[] posFacts,
                                   double[][] points, int pointOff ) {
        double cx = centre[ 0 ];
        double cy = centre[ 1 ];
        double cz = centre[ 2 ];
        for ( int iend = 0; iend < 2; iend++ ) {
            double posFact = posFacts[ iend ];
            double[] rerr;
            if ( posFact > 1.0 ) {
                rerr = rpair_[ iend ];
                assert iend == 0 || iend == 1;
                double fact = iend == 0 ? Math.max( 2.0 - posFact, 0.0 )
                                        : posFact;
                double fe;
                if ( radialLog_ ) {
                    double logR = Math.sqrt( cx * cx + cy * cy + cz * cz );
                    fe = Math.max( 1.0 + Math.log( fact ) / logR, 0.0 );
                }
                else {
                    fe = fact;
                }
                rerr[ 0 ] = fe * cx;
                rerr[ 1 ] = fe * cy;
                rerr[ 2 ] = fe * cz;
            }
            else {
                rerr = null;
            }
            points[ pointOff++ ] = rerr;
        }
    }

    /**
     * Returns a transformation matrix which will transform (0,0,1) to
     * the position of a given point.  The transformation is such that 
     * the increasing Y direction near (0,0,1) end up to the north 
     * of the <code>point</code>.
     *
     * @param  point  destination point
     * @return  9-element (3x3) transformation matrix, 
     *          or null if <code>point</code>=(0,0,0)
     */
    private double[] transformFrom001( double[] point ) {
        double px = point[ 0 ];
        double py = point[ 1 ];
        double pz = point[ 2 ];
        double r2 = px * px + py * py + pz * pz;
        if ( r2 == 0 ) {
            return null;
        }
        double rCosTheta = pz;
        double rSinTheta = Math.sqrt( r2 - rCosTheta * rCosTheta );
        double cosPhi = px / rSinTheta;
        double sinPhi = py / rSinTheta;
        double r = Math.sqrt( r2 );
        double cosTheta = rCosTheta / r;
        double sinTheta = rSinTheta / r;

        /* This matrix is Rz(-phi) x Ry(theta), that is a rotation around
         * the Y axis by an angle of theta followed by a rotation around
         * the Z axis by an angle of -phi. */
        transMatrix_[ 0 ] = + r * cosPhi * cosTheta;
        transMatrix_[ 1 ] = - r * sinPhi;
        transMatrix_[ 2 ] = + r * cosPhi * sinTheta;
        transMatrix_[ 3 ] = + r * sinPhi * cosTheta;
        transMatrix_[ 4 ] = + r * cosPhi;
        transMatrix_[ 5 ] = + r * sinPhi * sinTheta;
        transMatrix_[ 6 ] = - r * sinTheta;
        transMatrix_[ 7 ] = 0;
        transMatrix_[ 8 ] = + r * cosTheta;
        return transMatrix_;
    }

    /**
     * Returns a RadialPairReader object suitable for a given ErrorMode.
     *
     * @param  mode  error mode
     * @return   error reader
     */
    private static RadialPairReader getRadialPairReader( ErrorMode mode ) {
        if ( ErrorMode.SYMMETRIC.equals( mode ) ) {
            return new RadialPairReader( 1 ) {
                protected void convert( double[] rawErrors, double[] factors ) {
                    double raw = rawErrors[ 0 ];
                    factors[ 0 ] = raw;
                    factors[ 1 ] = raw;
                }
            };
        }
        else if ( ErrorMode.LOWER.equals( mode ) ) {
            return new RadialPairReader( 1 ) {
                protected void convert( double[] rawErrors, double[] factors ) {
                    factors[ 0 ] = rawErrors[ 0 ];
                    factors[ 1 ] = 1.0;
                }
            };
        }
        else if ( ErrorMode.UPPER.equals( mode ) ) {
            return new RadialPairReader( 1 ) {
                protected void convert( double[] rawErrors, double[] factors ) {
                    factors[ 0 ] = 1.0;
                    factors[ 1 ] = rawErrors[ 0 ];
                }
            };
        }
        else if ( ErrorMode.BOTH.equals( mode ) ) {
            return new RadialPairReader( 2 ) {
                protected void convert( double[] rawErrors, double[] factors ) {
                    factors[ 0 ] = rawErrors[ 0 ];
                    factors[ 1 ] = rawErrors[ 1 ];
                }
            };
        }
        else {
            assert ErrorMode.NONE.equals( mode );
            return new RadialPairReader( 0 ) {
                protected void convert( double[] rawErrors, double[] factors ) {
                    factors[ 0 ] = 1.0;
                    factors[ 1 ] = 1.0;
                }
            };
        }
    }

    /**
     * Helper class which decodes radial error information from the ValueStore.
     */
    private static abstract class RadialPairReader {
        private final double[] buf_;

        /**
         * Constructor.
         *
         * @param   wordCount  number of words from the value store which
         *          are used to store error information for each datum
         */
        public RadialPairReader( int wordCount ) {
            buf_ = new double[ wordCount ];
        }

        /**
         * Returns the number of words in the value store which are used
         * to store error information for each datum.
         *
         * @return   word count
         */
        public int getWordCount() {
            return buf_.length;
        }

        /**
         * Converts values read from the value store into lower and upper
         * error factor values.
         *
         * @param  rawErrors  wordCount-element array read from value store
         * @param  factors    2-element array which on exit holds (lower,upper)
         *         error factors; these are >=1 and 1 is equivalent
         *         to no error information
         */
        protected abstract void convert( double[] rawErrors, double[] factors );
    }
}
