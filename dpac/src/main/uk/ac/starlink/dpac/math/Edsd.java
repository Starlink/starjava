package uk.ac.starlink.dpac.math;

import gaia.cu9.tools.parallax.util.PolinomialSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Calculates quantities related to the Exponentially Decreasing
 * Space Density prior for estimating distances from parallaxes.
 *
 * @author   Mark Taylor
 * @since    15 Mar 2018
 */
public class Edsd {

    private final double plx_;
    private final double eplx_;
    private final double l_;
    private final double eplx1_;
    private final Function pdf_;
    private final double rMode_;
    private final double pdfExp0_;

    /**
     * Constructor.
     * The units are either max and kpc or arcsec and parsec.
     *
     * @param   plx   parallax
     * @param   eplx   error in parallax
     * @param   l     distance scale 
     */
    public Edsd( double plx, double eplx, double l ) {
        plx_ = plx;
        eplx_ = eplx;
        l_ = l;
        eplx1_ = 1.0 / eplx;
        rMode_ = calculateBestEstimation();
        pdfExp0_ = pdfExp( rMode_ );
        final double scale = 1.0 / getUnnormalizedProbabilityAt( rMode_ );
        pdf_ = new Function() {
            public double f( double r ) {
                return scale * getUnnormalizedProbabilityAt( r );
            }
        };
    }

    /**
     * Returns the position of the favoured mode of the PDF.
     * This is considered to be the best estimator of the distance.
     *
     * @return   distance PDF mode in kpc or parsec;
     *           the lower one if there are two
     */
    public double getBestEstimation() {
        return rMode_;
    }

    /**
     * Returns the unnormalized PDF (posterior) for this prior
     * evaluated at a given distance.
     *
     * <p>Note this differs by a numerical factor (a function of plx,
     * eplx and l) from the similarly named function in
     * gaia.cu9.tools.parallax.PDF.ExpDecrVolumeDensityDEM.
     * This nominally makes no difference, since the result of this
     * function is declared unnormalised and will generally be scaled
     * before further use.  However, in practice this method will
     * return a definite result for parameter ranges in which the
     * CU9 code faces numerical overflow and returns Infinity.
     *
     * @param  r  distance (in kpc or parsec)
     * @return  unnormalised probability density
     */
    public double getUnnormalizedProbabilityAt( double r ) {
        if ( r > 0 ) {

            /* The factor of eplx1_ here is really irrelevant since the
             * expression is not normalised.  However, it's included to
             * match eq 18 in Bailer-Jones Paper I. */
            /* The offset of pdfExp0_ also represents a factor which
             * is normalised away.  However, if not present, the argument
             * of the exp function can (for parameter combinations actually
             * present in DR2 data) get so large that the function overflows
             * and returns Infinity. */
            return eplx1_ * r * r * Math.exp( pdfExp( r ) - pdfExp0_ );
        }
        else {
            return 0;
        }
    }

    /**
     * Returns the argument of the exponential function that appears in
     * the PDF evaluation.
     *
     * @param   r  distance
     * @param  exponential argument
     */
    private double pdfExp( double r ) {
        double d = ( plx_ - 1. / r ) * eplx1_;
        return -0.5 * d * d - r / l_;
    }

    /**
     * Calculates the position of the favoured mode of the PDF.
     * This is considered to be the best estimator of the distance.
     *
     * @return   distance PDF mode in kpc or parsec;
     *           the lower one if there are two
     */
    private final double calculateBestEstimation() {
        double a2 = -2. * l_;
        double a1 = plx_ * l_ / ( eplx_ * eplx_ );
        double a0 = -l_ / ( eplx_ * eplx_ );
        return PolinomialSolver.solveThirdDegree( a2, a1, a0 );
    }

    /**
     * Returns the PDF corresponding to this prior.
     * This is not normalised in the sense that the area under it is unity,
     * but it is scaled so that the best estimate equals 1.
     *
     * @return  sort-of normalised PDF
     */
    public Function getPdf() {
        return pdf_;
    }

    /**
     * Integrates the PDF to provide a numerical approximation of the
     * cumulative density function.  The output range in distance is
     * between r=0 and the radius above which the PDF is close to zero.
     * The output is normalised, so its value is zero at r=0 and
     * unity at the largest r.
     *
     * <p>Careful when interpolating this.  The spline interpolation
     * for plx=40, eplx=0.75, l=1.35 gives some nasty results at high r.
     * Linear and quadratic interpolations are OK.
     *
     * @param  tol  calculation tolerance
     * @return   normalised numerical CDF samples
     */
    public NumericFunction calculateCdf( double tol ) {
        double rmax = Math.max( 10 * l_, 100 );
        while ( pdf_.f( rmax ) > tol ) {
            rmax *= 1.1;
        }

        /* Set up a list of points at which the function should be sampled
         * for the numerical integration.  The adaptive integration
         * will make sure that the function is sampled correctly between
         * these as long as nothing dramatic happens between in the
         * unspecified regions. */
        List<Double> plist = new ArrayList<Double>();

        /* Origin. */
        plist.add( Double.valueOf( 0.0 ) );

        /* There may be a sharp Gaussian-like peak at rmode.
         * Make sure the maximum is a sample point, and also a few
         * S.D.s each side of it. */
        plist.add( Double.valueOf( rMode_ ) );
        for ( double sigmult : new double[] { 1, 2, 3, 6 } ) {
            plist.add( Double
                      .valueOf( 1.0 / ( 1.0 / rMode_ - sigmult * eplx_ ) ) );
            plist.add( Double
                      .valueOf( 1.0 / ( 1.0 / rMode_ + sigmult * eplx_ ) ) );
        }

        /* There may be another peak near 2*l, though not very sharp. */
        plist.add( Double.valueOf( 1.0 * l_ ) );
        plist.add( Double.valueOf( 2.0 * l_ ) );
        plist.add( Double.valueOf( 3.0 * l_ ) );

        /* Add a grid of evenly spaced points for good measure. */
        int nstep = 10;
        for ( int i = 0; i < nstep; i++ ) {
            double frac = i / (double) nstep;
            plist.add( Double.valueOf( frac * rmax ) );
        }
        double[] points = preparePoints( plist );

        /* Integrate the unnormalised PDF to get an unnormalised CDF. */
        final NumericFunction cdf0 = Integral.integrate( pdf_, points, tol );

        /* Construct and return a normalised CDF. */
        final double scale = 1.0 / cdf0.getY( cdf0.getCount() - 1 );
        return new NumericFunction() {
            public int getCount() {
                return cdf0.getCount();
            }
            public double getX( int i ) {
                return cdf0.getX( i );
            }
            public double getY( int i ) {
                return cdf0.getY( i ) * scale;
            }
        };
    }

    /**
     * Returns a numerical reconstruction of the PDF based on the
     * samples used to perform the CDF integration.
     * This can be used to assess the quality of the integration,
     * at least by eye.
     *
     * @param  cdf   calculated cumulative density function;
     *               this is only used to supply the sample X values
     * @return  PDF evaluated at the CDF X values
     */
    public NumericFunction getSampledPdf( final NumericFunction cdf ) {
        return new NumericFunction() {
            public int getCount() {
                return cdf.getCount();
            }
            public double getX( int i ) {
                return cdf.getX( i );
            }
            public double getY( int i ) {
                return pdf_.f( getX( i ) );
            }
        };
    }

    /**
     * Filters and sorts an array of values so that it is suitable
     * for feeding to the integration routine.
     * Negative and duplicate values are removed and the values
     * are sorted ascending.
     *
     * @param   inList  candidate points
     * @return  points ready for integration routine
     */
    private static double[] preparePoints( List<Double> inList ) {

        /* Remove negative values. */
        List<Double> outList = new ArrayList<Double>();
        for ( Double dobj : inList ) {
            if ( dobj.doubleValue() >= 0 ) {
                outList.add( dobj );
            }
        }

        /* Deduplicate. */
        outList = new ArrayList<Double>( new HashSet<Double>( outList ) );

        /* Copy to a primitive array. */
        int n = outList.size();
        double[] out = new double[ n ];
        for ( int i = 0; i < n; i++ ) {
            out[ i ] = outList.get( i ).doubleValue();
        }

        /* Sort ascending and return. */
        Arrays.sort( out );
        return out;
    }
}
