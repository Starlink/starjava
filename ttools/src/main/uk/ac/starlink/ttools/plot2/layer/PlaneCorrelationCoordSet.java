package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.gui.ResourceIcon;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * MultiPointCoordSet for 2-d ellipses defined by coordinate errors
 * and a correlation.
 * This is how Gaia errors are quoted.
 *
 * @author   Mark Taylor
 * @since    5 Apr 2017
 */
public class PlaneCorrelationCoordSet implements CartesianMultiPointCoordSet {

    private static final int NP = 4;
    private static final FloatingCoord XERR_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "xerr", "X Error" )
           .setShortDescription( "Error in X coordinate" )
           .setXmlDescription( new String[] {
                "<p>Error in the X coordinate.",
                "</p>",
            } )
        , true );
    private static final FloatingCoord YERR_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "yerr", "Y Error" )
           .setShortDescription( "Error in Y coordinate" )
           .setXmlDescription( new String[] {
                "<p>Error in the Y coordinate.",
                "</p>",
            } )
        , true );
    private static final FloatingCoord XYCORR_COORD =
        FloatingCoord.createCoord(
            new InputMeta( "xycorr", "XY Correlation" )
           .setShortDescription( "Correlation between X and Y errors" )
           .setXmlDescription( new String[] {
                "<p>Correlation beteween the errors in the X and Y directions.",
                "This is a dimensionless quantity in the range -1..+1,",
                "and is equivalent to the covariance divided by",
                "the product of the X and Y error values themselves.",
                "It corresponds to the <code>*_corr</code> values",
                "supplied in the Gaia source catalogue.",
                "</p>",
            } )
        , true );

    /**
     * Constructor.
     */
    public PlaneCorrelationCoordSet() {
    }

    public Coord[] getCoords() {
        return new Coord[] { XERR_COORD, YERR_COORD, XYCORR_COORD };
    }

    public int getPointCount() {
        return NP;
    }

    public boolean readPoints( Tuple tuple, int icol, double[] xy0,
                               double[][] xyExtras ) {

        /* Read error and correlation values from data. */
        double xerr = XERR_COORD.readDoubleCoord( tuple, icol + 0 );
        double yerr = YERR_COORD.readDoubleCoord( tuple, icol + 1 );
        if ( Double.isNaN( xerr ) || Double.isNaN( yerr ) ||
             ( xerr == 0 && yerr == 0 ) ) {
            return false;
        }
        double xycorr = XYCORR_COORD.readDoubleCoord( tuple, icol + 2 );
        if ( Double.isNaN( xycorr ) ) {
            return false;
        }

        /* Calculate error vectors. */
        double[] ra = new double[ 2 ];
        double[] rb = new double[ 2 ];
        calculateErrorVectors( xerr, yerr, xycorr, ra, rb );
        double rax = ra[ 0 ];
        double ray = ra[ 1 ];
        double rbx = rb[ 0 ];
        double rby = rb[ 1 ];

        /* Turn the error ellipse axes into four plotted points. */
        double x0 = xy0[ 0 ];
        double y0 = xy0[ 1 ];
        double[] xy1 = xyExtras[ 0 ];
        double[] xy2 = xyExtras[ 1 ];
        double[] xy3 = xyExtras[ 2 ];
        double[] xy4 = xyExtras[ 3 ];
        xy1[ 0 ] = x0 + rax;
        xy1[ 1 ] = y0 + ray;
        xy2[ 0 ] = x0 - rax;
        xy2[ 1 ] = y0 - ray;
        xy3[ 0 ] = x0 + rbx;
        xy3[ 1 ] = y0 + rby;
        xy4[ 0 ] = x0 - rbx;
        xy4[ 1 ] = y0 - rby;
        return true;
    }

    /**
     * Calculates the vectors defining an error ellipse from the
     * errors on each axis and a correlation value.
     *
     * @param  xerr   error in X coordinate
     * @param  yerr   error in Y coordinate
     * @param  xycorr  dimensionless X-Y correlation in range -1..+1
     * @param  ra      2-element vector to receive primary radius vector
     * @param  rb      2-element vector to receive secondary radius vector
     */
    public static void calculateErrorVectors( double xerr, double yerr,
                                              double xycorr,
                                              double[] ra, double[] rb ) {

        /* Calculate vectors for principal axes of error ellipse. */
        final double rxa;
        final double rya;
        final double rxb;
        final double ryb;

        /* Obtain covariance from correlation. */
        double xycov = xycorr * xerr * yerr;

        /* General case: non-zero covariance. */
        if ( xycov != 0 ) {

            /* Prepare the covariance matrix
             *    | xerr^2   xycov  |
             *    | xycov    xerr^2 |   */
            double xerr2 = xerr * xerr;
            double yerr2 = yerr * yerr;

            /* Calculate trace and determinant for covariance matrix. */
            double tr = xerr2 + yerr2;
            double det = xerr2 * yerr2 - xycov * xycov;

            /* Calculate eigenvalues. */
            double disc = Math.sqrt( 0.25 * tr * tr - det );
            double la = 0.5 * tr + disc;
            double lb = 0.5 * tr - disc;

            /* Calculate unscaled eigenvectors. */
            double exa = la - yerr2;
            double eya = xycov;
            double exb = lb - yerr2;
            double eyb = xycov;

            /* Calculate scaling for each eigenvector: divide by the modulus
             * of the unscaled vector to turn it into a unit vector,
             * then multiply by the square root of the eigenvalue. */
            double ma = Math.sqrt( la / ( exa * exa + eya * eya ) );
            double mb = Math.sqrt( lb / ( exb * exb + eyb * eyb ) );

            /* Scale the eigenvectors to get vectors for error ellipse
             * principal axes. */
            rxa = ma * exa;
            rya = ma * eya;
            rxb = mb * exb;
            ryb = mb * eyb;
        }

        /* Trivial case: no covariance, ellipse axes are aligned with
         * plotting axes. */
        else {
            rxa = xerr;
            rya = 0;
            rxb = 0;
            ryb = yerr;
        }

        /* Return values in supplied vectors. */
        ra[ 0 ] = rxa;
        ra[ 1 ] = rya;
        rb[ 0 ] = rxb;
        rb[ 1 ] = ryb;
    }

    /**
     * Creates a MultiPointForm that can plot ellipses on the plane,
     * corresponding to this coordset.
     *
     * @return  new form
     */ 
    public static MultiPointForm createForm() {
        boolean canScale = true;
        String descrip = PlotUtil.concatLines( new String[] {
            "<p>Plots an error ellipse",
            "(or rectangle or other similar figure)",
            "defined by errors in the X and Y directions,",
            "and a correlation between the two errors.",
            "</p>",
            "<p>The supplied correlation is a dimensionless value",
            "in the range -1..+1",
            "and is equal to the covariance divided by the product of the",
            "X and Y errors.",
            "The covariance matrix is thus:",
            "<verbatim>",
            "    [  xerr*xerr         xerr*yerr*xycorr  ]",
            "    [  xerr*yerr*xycorr  yerr*yerr         ]",
            "</verbatim>",
            "</p>",
            ( canScale ? MultiPointForm
                        .getDefaultScalingDescription( "ellipse" )
                       : "" ),
            "<p>This plot type is suitable for use with the",
            "<code>&lt;x&gt;_error</code> and",
            "<code>&lt;x&gt;_&lt;y&gt;_corr</code> columns",
            "in the <em>Gaia</em> source catalogue.",
            "</p>",
        } );
        return new CartesianMultiPointForm( "XYCorr",
                                            ResourceIcon.FORM_ELLIPSE_CORR,
                                            descrip,
                                            new PlaneCorrelationCoordSet(),
                                            StyleKeys.ELLIPSE_SHAPE, canScale );
    }
}
