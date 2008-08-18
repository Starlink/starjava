package uk.ac.starlink.ttools.plot;

/**
 * Calculates X-Y correlation statistics.
 *
 * @author   Mark Taylor (Starlink)
 * @since    15 Jul 2004
 */
public class XYStats {

    private final boolean xLog_;
    private final boolean yLog_;
    private double n;
    private double sX;
    private double sY;
    private double sXX;
    private double sYY;
    private double sXY;
    private double minX;
    private double maxX;
    private final static double LOG10SCALE = 1.0 / Math.log( 10 );

    /**
     * Constructs a new correlation statistics calculator.
     *
     * @param  xLog  whether the X coordinates are being plotted on a
     *         logarithmic scale
     * @param  yLog  whether the Y coordinates are being plotted on a
     *         logarithmic scale
     */
    public XYStats( boolean xLog, boolean yLog ) {
        xLog_ = xLog;
        yLog_ = yLog;
        minX = Double.MAX_VALUE;
        maxX = - Double.MAX_VALUE;
    }

    /**
     * Submits a data point for calculations.
     *
     * @param  x  X coordinate
     * @param  y  Y coordinate
     */
    public void addPoint( double x, double y ) {
        if ( xLog_ ) {
            x = Math.log( x ) * LOG10SCALE;
        }
        if ( yLog_ ) {
            y = Math.log( y ) * LOG10SCALE;
        }
        n++;
        sX += x;
        sY += y;
        sXX += x * x;
        sYY += y * y;
        sXY += x * y;
        if ( x < minX ) {
            minX = x;
        }
        if ( x > maxX ) {
            maxX = x;
        }
    }

    /**
     * Returns the polynomial coefficients of a linear regression line
     * for the submitted data.
     *
     * @return  2-element array: (intercept, gradient)
     */
    public double[] getLinearCoefficients() {
        double s2x = n * sXX - sX * sX;
        double c = ( sXX * sY - sX * sXY ) / s2x;
        double m = ( n * sXY - sX * sY ) / s2x;
        return new double[] { c, m };
    }

    /**
     * Returns the product moment correlation coefficient.
     *
     * @return  correlation coefficient
     */
    public double getCorrelation() {
        double s2x = n * sXX - sX * sX;
        double s2y = n * sYY - sY * sY;
        return ( n * sXY - sX * sY ) / Math.sqrt( s2x * s2y );
    }

    /**
     * Calculates the linear regression line for the submitted points.
     * The endpoints of the line correspond to the lowest and highest
     * X values submitted.
     *
     * @return   4-element array giving the endpoints 
     *           (low-X, low-Y, high-X, high-Y) of a linear regression 
     *           line for these data
     */
    public double[] linearRegressionLine() {
        double[] coeffs = getLinearCoefficients();
        double c = coeffs[ 0 ];
        double m = coeffs[ 1 ];
        double xlo = minX;
        double ylo = m * minX + c;
        double xhi = maxX;
        double yhi = m * maxX + c;
        if ( xLog_ ) {
            xlo = Math.pow( 10.0, xlo );
            xhi = Math.pow( 10.0, xhi );
        }
        if ( yLog_ ) {
            ylo = Math.pow( 10.0, ylo );
            yhi = Math.pow( 10.0, yhi );
        }
        return new double[] { xlo, ylo, xhi, yhi };
    }
}
