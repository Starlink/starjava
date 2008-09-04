/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     25-FEB-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.util;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.QRDecomposition;
import cern.jet.stat.Gamma;

import uk.ac.starlink.splat.data.AnalyticSpectrum;

/**
 * PolynomialFitter fits a polynomial of given degree to a set of
 * data points using a weighted least squares fit.
 *
 * To use this class create an instance with the data to be
 * fitted. Interpolated positions can then be obtained using the
 * evalYDataArray() and evalYData() methods. A chi squared residual to
 * the fit can be obtained from the getChi() method.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class PolynomialFitter
    implements AnalyticSpectrum
{
    /**
     * The coefficients of the fit.
     */
    private double[] coeffs = null;

    /**
     * The chi square and probability of the fit.
     */
    private double[] chiSquare = new double[2];

    /**
     * Fit a polynomial of a given degree to unweighted data points.
     *
     * @param degree the degree of the polynomial (0 = constant, 1 =
     *               straight line etc.).
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y. These are assumed precise.
     */
    public PolynomialFitter( int degree, double[] x, double[] y )
    {
        // Default weights are 1.
        double[] w = new double[x.length];
        for (int i = 0; i < x.length; i++ ) {
            w[i] = 1.0;
        }
        doFit( degree + 1, x, y, w );
    }

    /**
     * Fit a polynomial of given degree to weighted data points.
     *
     * @param degree the degree of the polynomial (0 = constant, 1 =
     *               straight line etc.).
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights for Y values (0.0 for ignore). For a correct
     *          chi squared estimate these should be the inverse
     *          variances.
     */
    public PolynomialFitter( int degree, double[] x, double[] y, double[] w )
    {
        doFit( degree + 1, x, y, w );
    }

    /**
     * Perform the fit of the data by a polynomial of given number of terms
     * (degree + 1). The technique uses a QR decomposition of the normal
     * equations. 
     *
     * @param nterms the number of possible terms in the polynomial 
     *               (0 = constant, 1 = straight line etc.).
     * @param x array of positions along X. These are assumed precise.
     * @param y array of positions along Y.
     * @param w weights for Y values (0.0 for ignore).
     */
    protected void doFit( int nterms, double[] x, double[] y, double[] w )
    {
        int ndata = x.length;
        double[] p = new double[nterms];
        DenseDoubleMatrix2D A = new DenseDoubleMatrix2D( ndata, nterms );
        DenseDoubleMatrix2D B = new DenseDoubleMatrix2D( ndata, 1 );

        //  Create the design matrix A and the data matrix B (Ax=B).
        for ( int i = 0; i < ndata; i++ ) {

            //  Generate polynomial elements for this row.
            p[0] = 1.0;
            for ( int k = 1; k < nterms; k++ ) {
                p[k] = p[k-1] * x[i];
            }

            //  Add weighted matrix terms for x.
            for ( int j = 0; j < nterms; j++ ) {
                A.setQuick( i, j, p[j] * w[i] );
            }

            //  Add weighted data values for y.
            B.setQuick( i , 0, y[i] * w[i] );
        }

        //  Get QR decomposition of this.
        QRDecomposition qrd;
        try {
            qrd = new QRDecomposition( A );
        } catch ( IllegalArgumentException e ) {
            e.printStackTrace();
            coeffs = null;
            return;
        }

        //  Get the solution (AX=B).
        DoubleMatrix2D X = qrd.solve( B );

        //  Extract required coefficients from X.
        coeffs = new double[nterms];
        for ( int i = 0; i < nterms; i++ ) {
            coeffs[i] = X.get( i, 0 );
        }

        //  Evaluate the chi square.
        evalChi( x, y, w );
    }

    /**
     * Determine a chi-square value and its probability for this fit.
     *
     * @param x array of positions along X.
     * @param y array of positions along Y.
     * @param w weights for Y values.
     */
    protected void evalChi( double[] x, double y[], double[] w )
    {
        chiSquare[0] = 0.0;
        double value = 0.0;
        for ( int i = 0; i < x.length; i++ ) {
            value = ( y[i] - evalYData( x[i] ) ) * w[i];
            chiSquare[0] += value * value;
        }

        //  Get probability of chi square being less than this.
        double nfree = (x.length - coeffs.length);
        chiSquare[1] = Gamma.incompleteGamma( nfree*0.5, chiSquare[0]*0.5 );
    }

    /**
     * Get chi squared and its probability for this fit. These are
     * only indicators of the relative merit of a fit if the weights
     * are the inverse variances of the data.
     *
     * @return double array of two values, the chi square and its
     *         probability of being less than this value (given the
     *         number of degrees of freedom). 
     */
    public double[] getChi()
    {
        return chiSquare;
    }

    /**
     * Get the polynomial coefficients.
     */
    public double[] getCoeffs()
    {
        return coeffs;
    }

    /**
     * Evaluate the polynomial at a set of given X positions.
     *
     * @param x array of X positions at which to evaluate the current
     *          polynomial.
     * @return array of polynomial values at given X's.
     */
    public double[] evalYDataArray( double[] x )
    {
        double[] y = new double[x.length];
        if ( coeffs != null ) {
            for ( int i = 0; i < x.length; i++ ) {
                y[i] = evalYData( x[i] );
            }
        }
        return y;
    }

    /**
     * Evaluate the polynomial at a single position.
     *
     * @param x X position at which to evaluate the current polynomial.
     * @return polynomial value at X
     */
    public double evalYData( double x ) 
    {
        double y = coeffs[0];
        double term = x;
        for ( int i = 1; i < coeffs.length; i++ ) {
            y += term * coeffs[i];
            term *= x;
        }
        return y;
    }

    /**
     *  Calculate an RMS to the given data for the fit.
     *
     * @param x array of X coordinates.
     * @param y array of data values for each X coordinate.
     */
    public double calcRms( double[] x, double[] y )
    {
        double rmssum = 0.0;
        double dy = 0.0;
        for ( int i = 0; i < x.length; i++ ) {
            dy = y[i] - evalYData( x[i] );
            rmssum += dy * dy;
        }
        return Math.sqrt( rmssum / (double)( x.length - 1 ) );
    }

    public String toString()
    {
        StringBuffer buffer = 
            new StringBuffer( "PolynomialFitter[" );
        buffer.append ( "degree = " + ( coeffs.length - 1 ) );
        buffer.append( ", coefficients = " + coeffs[1] );
        for ( int i = 1; i < coeffs.length; i++ ) {
            buffer.append( ", " + coeffs[i] );
        }
        buffer.append( "]" );
        return buffer.toString();
    }
}
